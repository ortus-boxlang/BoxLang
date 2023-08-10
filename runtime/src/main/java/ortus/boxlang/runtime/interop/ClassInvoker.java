/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.interop;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and return the results!
 *
 * To create a new class invoker you can use the following:
 * {@pre
 * {@code
 * ClassInvoker target = new ClassInvoker( String.class );
 * ClassInvoker target = ClassInvoker.of( String.class );
 * ClassInvoker target = new ClassInvoker( new String() );
 * ClassInvoker target = ClassInvoker.of( new String() );
 * }}
 *
 * You can then use the following methods to invoke methods on the class:
 * - {@code invokeConstructor( Object... args )} - Invoke a constructor on the class, and store the instance for future method calls
 * - {@code invokeStaticMethod( String methodName, Object... args )} - Invoke a static method on the class
 * - {@code invoke( String methodName, Object... args )} - Invoke a method on the instance of the class
 *
 */
public class ClassInvoker {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This is a map of primitive types to their native Java counterparts
	 * so we can do the right casting for primitives
	 */
	private static final Map<Class<?>, Class<?>>			PRIMITIVE_MAP;

	/**
	 * This is the method handle lookup
	 *
	 * @see https://docs.oracle.com/javase/11/docs/api/java/lang/invoke/MethodHandles.Lookup.html
	 */
	private static final MethodHandles.Lookup				METHOD_LOOKUP;

	/**
	 * The bound class for this invoker
	 */
	private Class<?>										targetClass;

	/**
	 * The bound instance for this invoker (if any)
	 * If this is null, then we are invoking static methods or a constructor has not been called on it yet.
	 */
	private Object											targetInstance		= null;

	/**
	 * This caches the method handles for the class so we don't have to look them up every time
	 */
	private final ConcurrentHashMap<String, MethodRecord>	methodHandleCache	= new ConcurrentHashMap<>( 32 );

	/**
	 * This enables or disables the method handles cache
	 */
	private Boolean											handlesCacheEnabled	= true;

	/**
	 * Static Initializer
	 */
	static {
		METHOD_LOOKUP	= MethodHandles.lookup();
		PRIMITIVE_MAP	= Map.of(
		        Boolean.class, boolean.class,
		        Byte.class, byte.class,
		        Character.class, char.class,
		        Short.class, short.class,
		        Integer.class, int.class,
		        Long.class, long.class,
		        Float.class, float.class,
		        Double.class, double.class
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new class invoker for the given class
	 *
	 * @param targetClass
	 */
	public ClassInvoker( Class<?> targetClass ) {
		this.targetClass = targetClass;
	}

	/**
	 * Create a new class invoker for the given instance
	 *
	 * @param targetInstance
	 */
	public ClassInvoker( Object targetInstance ) {
		this.targetInstance	= targetInstance;
		this.targetClass	= targetInstance.getClass();
	}

	/**
	 * Static factory method to create a new class invoker for the given class. Mostly used for nice fluent chaining
	 *
	 * @param targetClass The class to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static ClassInvoker of( Class<?> targetClass ) {
		return new ClassInvoker( targetClass );
	}

	/**
	 * Static factory method to create a new class invoker for the given instance. Mostly used for nice fluent chaining
	 *
	 * @param targetInstance The instance to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static ClassInvoker of( Object targetInstance ) {
		return new ClassInvoker( targetInstance );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Setters & Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return the handlesCacheEnabled flag
	 */
	public Boolean isHandlesCacheEnabled() {
		return handlesCacheEnabled;
	}

	/**
	 * @param handlesCacheEnabled Enable or not the handles cache
	 */
	public ClassInvoker setHandlesCacheEnabled( Boolean handlesCacheEnabled ) {
		this.handlesCacheEnabled = handlesCacheEnabled;
		return this;
	}

	/**
	 * @return the targetClass
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	/**
	 * @param targetClass the targetClass to set
	 */
	public ClassInvoker setTargetClass( Class<?> targetClass ) {
		this.targetClass = targetClass;
		return this;
	}

	/**
	 * @return the targetInstance
	 */
	public Object getTargetInstance() {
		return targetInstance;
	}

	/**
	 * @param targetInstance the targetInstance to set
	 */
	public ClassInvoker setTargetInstance( Object targetInstance ) {
		this.targetInstance = targetInstance;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Invokers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invokes the constructor for the class with the given arguments and stores the instance of the object
	 * into the {@code targetInstance} property for future method calls.
	 *
	 * @param args The arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 *
	 * @throws Throwable             If the constructor cannot be invoked
	 * @throws IllegalStateException If the class is an interface, you can't call a constructor on an interface
	 */
	public ClassInvoker invokeConstructor( Object... args ) throws Throwable {

		// Thou shalt not pass!
		if ( isInterface() ) {
			throw new IllegalStateException( "Cannot invoke a constructor on an interface" );
		}

		// Method signature for a constructor is void (Object...)
		MethodType		constructorType		= MethodType.methodType( void.class, argumentsToClasses( args ) );
		// Define the bootstrap method
		MethodHandle	constructorHandle	= METHOD_LOOKUP.findConstructor( this.targetClass, constructorType );
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		// Invoke Dynamic tries to do argument coercion, so we need to convert the arguments to the right types
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		this.targetInstance = constructorInvoker.invokeWithArguments( args );

		return this;
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation wrapped in an Optional
	 *
	 * @throws Throwable
	 * @throws IllegalArgumentException If the method name is null or empty
	 * @throws IllegalStateException    If the method handle is not static and the target instance is null
	 */
	public Optional<Object> invoke( String methodName, Object... arguments ) throws Throwable {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new IllegalArgumentException( "Method name cannot be null or empty." );
		}

		// Get the invoke dynamic method handle from our cache and discovery techniques
		MethodRecord methodRecord = getMethodHandle( methodName, argumentsToClasses( arguments ) );

		// If it's not static, we need a target instance
		if ( Boolean.FALSE.equals( methodRecord.isStatic() ) && this.targetInstance == null ) {
			throw new IllegalStateException(
			        "You can't call invoke on a null target instance. Use [invokeStatic] instead or set the target instance manually or via the constructor."
			);
		}

		// Discover and Execute it baby!
		return Optional.ofNullable(
		        Boolean.TRUE.equals( methodRecord.isStatic() )
		                ? methodRecord.methodHandle().invokeWithArguments( arguments )
		                : methodRecord.methodHandle().bindTo( this.targetInstance ).invokeWithArguments( arguments )
		);
	}

	/**
	 * Invokes a static method with the given name and arguments on a class or an interface
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation wrapped in an Optional
	 *
	 * @throws Throwable
	 */
	public Optional<Object> invokeStatic( String methodName, Object... arguments ) throws Throwable {

		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new IllegalArgumentException( "Method name cannot be null or empty." );
		}

		// Discover and Execute it baby!
		return Optional.ofNullable(
		        getMethodHandle( methodName, argumentsToClasses( arguments ) )
		                .methodHandle()
		                .invokeWithArguments( arguments )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Gets the method handle for the given method name and arguments, from the cache if possible
	 * or creates a new one if not found or throws an exception if the method signature doesn't exist
	 *
	 * @param methodName         The name of the method to get the handle for
	 * @param argumentsAsClasses The array of arguments as classes to map
	 *
	 * @return The method handle representing the method signature
	 *
	 * @throws RuntimeException       If the method cannot be found
	 * @throws NoSuchMethodException  If the method or method handle cannot be found
	 * @throws IllegalAccessException If the method handle cannot be accessed
	 */
	public MethodRecord getMethodHandle( String methodName, Class<?>[] argumentsAsClasses )
	        throws RuntimeException, NoSuchMethodException, IllegalAccessException {

		// We use the method signature as the cache key
		String			cacheKey		= methodName + Objects.hash( methodName, Arrays.toString( argumentsAsClasses ) );
		MethodRecord	methodRecord	= methodHandleCache.get( cacheKey );

		// Double lock to avoid race-conditions
		if ( methodRecord == null || !handlesCacheEnabled ) {
			synchronized ( methodHandleCache ) {
				if ( methodRecord == null || !handlesCacheEnabled ) {
					methodRecord = discoverMethodHandle( methodName, argumentsAsClasses );
					methodHandleCache.put( cacheKey, methodRecord );
				}
			}
		}

		return methodRecord;
	}

	/**
	 * Discovers the method to invoke for the given method name and arguments according to two algorithms:
	 *
	 * 1. Exact Match : Matches the incoming argument class types to the method signature
	 * 2. Discovery : Matches the incoming argument class types to the method signature by discovery of matching method names and argument counts
	 *
	 * @param methodName         The name of the method to discover
	 * @param argumentsAsClasses The array of arguments as classes to map
	 *
	 * @return The method record representing the method signature and metadata
	 *
	 * @throws NoSuchMethodException  If the method cannot be found using the discovery algorithm
	 * @throws IllegalAccessException If the method handle cannot be accessed
	 */
	public MethodRecord discoverMethodHandle( String methodName, Class<?>[] argumentsAsClasses )
	        throws NoSuchMethodException, IllegalAccessException {
		// Our target we must find!
		Method targetMethod;

		// 1: Exact Match
		// This can fail if the arguments are not the exact same class types
		// Which can happen for certain generic types, and even some primitive types
		try {
			targetMethod = this.targetClass.getMethod( methodName, argumentsAsClasses );

			// Static Match?
			if ( Modifier.isStatic( targetMethod.getModifiers() ) ) {
				MethodHandle methodHandle = METHOD_LOOKUP.findStatic(
				        this.targetClass,
				        methodName,
				        MethodType.methodType( targetMethod.getReturnType(), argumentsAsClasses )
				);
				return new MethodRecord( methodName, targetMethod, methodHandle, Boolean.TRUE, argumentsAsClasses.length );
			}

			// Virtual Lookup
			MethodHandle methodHandle = METHOD_LOOKUP.findVirtual(
			        this.targetClass,
			        methodName,
			        MethodType.methodType( targetMethod.getReturnType(), argumentsAsClasses )
			);
			return new MethodRecord(
			        methodName,
			        targetMethod,
			        methodHandle,
			        Boolean.FALSE,
			        argumentsAsClasses.length
			);
		} catch ( NoSuchMethodException e ) {

			// 2: Let's go by discovery now
			targetMethod = getCallableMethods()
			        .stream()
			        // Do it fast!
			        .parallel()
			        // filter by the method name we need
			        .filter( method -> method.getName().equals( methodName ) )
			        // .peek( method -> System.out.println( "PeekMethod -> " + method.getName() ) )
			        // Now by the number of arguments we have
			        .filter( method -> method.getParameterCount() == argumentsAsClasses.length )
			        // TODO: Filter by Argument Cast Matching
			        // Right now we take the first match, but @bdw429s mentioned we need to do auto-casting
			        // So that would have to be done here.
			        // Give me the first one found
			        .findFirst()
			        // Or throw an exception
			        .orElseThrow( () -> new NoSuchMethodException(
			                String.format(
			                        "No such method [%s] found in the class [%s] using [%d] arguments.",
			                        methodName,
			                        this.targetClass.getName(),
			                        argumentsAsClasses.length
			                )
			        ) );

			// Return our discovered method
			return new MethodRecord(
			        methodName,
			        targetMethod,
			        toMethodHandle( targetMethod ),
			        Modifier.isStatic( targetMethod.getModifiers() ),
			        argumentsAsClasses.length
			);
		}
	}

	/**
	 * Get a HashSet of methods of all the unique callable method signatures for the given class
	 *
	 * @return A unique set of callable methods
	 */
	private Set<Method> getCallableMethods() {
		Set<Method> allMethods = new HashSet<>();
		allMethods.addAll( new HashSet<>( List.of( this.targetClass.getMethods() ) ) );
		allMethods.addAll( new HashSet<>( List.of( this.targetClass.getDeclaredMethods() ) ) );
		return allMethods;
	}

	/**
	 * Utility method to convert a method to a method handle
	 *
	 * @param method The method to convert
	 *
	 * @return The method handle representing the method or an exception if it fails
	 *
	 * @throws RuntimeException If the method handle cannot be accessed
	 */
	public static MethodHandle toMethodHandle( Method method ) {
		try {
			return METHOD_LOOKUP.unreflect( method );
		} catch ( IllegalAccessException e ) {
			throw new RuntimeException( "Error creating MethodHandle for method [" + method + "]", e );
		}
	}

	/**
	 * Verifies if the target calss is an interface or not
	 *
	 * @return
	 */
	boolean isInterface() {
		return this.targetClass.isInterface();
	}

	/**
	 * Converts the argument(s) to a class representation according to Java casting rules
	 *
	 * @param thisArg The argument to convert
	 *
	 * @return The class representation of the argument
	 */
	public static Class<?> argumentToClass( Object thisArg ) {
		// nulls are always null, why is this even a thing?
		if ( thisArg == null ) {
			return Object.class;
		}
		// If it's a primitive, we need to convert it to the wrapper class
		Class<?> clazz = thisArg.getClass();
		return PRIMITIVE_MAP.getOrDefault( clazz, clazz );
	}

	/**
	 * Converts the arguments to an array of classes
	 *
	 * @param args The arguments to convert
	 *
	 * @return The array of classes
	 */
	public static Class<?>[] argumentsToClasses( Object... args ) {
		// Convert the arguments to an array of classes
		return Arrays.stream( args )
		        .map( ClassInvoker::argumentToClass )
		        // .peek( clazz -> System.out.println( "argumentToClass -> " + clazz ) )
		        .toArray( Class<?>[]::new );
	}

	/**
	 * This immutable record represents an executable method handle and it's metadata.
	 * This record is the one that is cached in the {@link ClassInvoker#methodCache} map.
	 *
	 * @param methodName    The name of the method
	 * @param method        The method representation
	 * @param methodHandle  The method handle to use for invocation
	 * @param isStatic      Whether the method is static or not
	 * @param argumentCount The number of arguments the method takes
	 */
	private record MethodRecord(
	        String methodName,
	        Method method,
	        MethodHandle methodHandle,
	        boolean isStatic,
	        int argumentCount ) {
		// A beautiful java record of our method handle
	}
}
