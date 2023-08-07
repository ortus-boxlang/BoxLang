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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and forget!
 *
 * TODO:
 * - Is there a way to cache method handles?
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
	 * This is the method handle lookup for the class
	 */
	private static final MethodHandles.Lookup				METHOD_LOOKUP;

	/**
	 * The bound class for this invoker
	 */
	private Class<?>										targetClass;

	/**
	 * The bound instance for this invoker (if any)
	 */
	private Object											targetInstance		= null;

	/**
	 * This is a map of method handles for the class
	 */
	private final ConcurrentHashMap<String, MethodHandle>	methodHandleCache	= new ConcurrentHashMap<>( 32 );

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
	 * @return the handlesCacheEnabled
	 */
	public Boolean getHandlesCacheEnabled() {
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
	 * Invokes the constructor for the class with the given arguments and returns the instance of the object
	 *
	 * TODO
	 * [ ] - Cache the constructor handle
	 *
	 * @param args The arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 *
	 * @throws Throwable             If the constructor cannot be invoked
	 * @throws IllegalStateException If the class is an interface, you can't call a constructor on an interface
	 */
	public Object invokeConstructor( Object... args ) throws Throwable {

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

		return this.targetInstance;
	}

	/**
	 * Invokes a public method on a class or interface with the given name and arguments
	 *
	 * TODO:
	 * [ ] - Test against interfaces
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation or null if the method is void
	 *
	 * @throws Throwable
	 * @throws IllegalArgumentException If the method name is null or empty
	 */
	public Object invoke( String methodName, Object... arguments ) throws Throwable {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new IllegalArgumentException( "Method name cannot be null or empty." );
		}

		// Discover and Execute it baby!
		return getMethodHandle( methodName, argumentsToClasses( arguments ) )
		        .bindTo( this.targetInstance )
		        .invokeWithArguments( arguments );
	}

	/**
	 * Invokes a static method with the given name and arguments
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation or null if the method is void
	 *
	 * @throws Throwable
	 */
	public Object invokeStatic( String methodName, Object... arguments ) throws Throwable {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new IllegalArgumentException( "Method name cannot be null or empty." );
		}

		// Discover and Execute it baby!
		return getMethodHandle( methodName, argumentsToClasses( arguments ) )
		        .invokeWithArguments( arguments );
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
	public MethodHandle getMethodHandle( String methodName, Class<?>[] argumentsAsClasses )
	        throws RuntimeException, NoSuchMethodException, IllegalAccessException {

		// We use the method signature as the cache key
		String			cacheKey		= methodName + Objects.hash( methodName, Arrays.toString( argumentsAsClasses ) );
		MethodHandle	methodHandle	= methodHandleCache.get( cacheKey );

		// Double lock to avoid race-conditions
		if ( methodHandle == null || !handlesCacheEnabled ) {
			synchronized ( methodHandleCache ) {
				if ( methodHandle == null || !handlesCacheEnabled ) {
					methodHandle = discoverMethodHandle( methodName, argumentsAsClasses );
					methodHandleCache.put( cacheKey, methodHandle );
				}
			}
		}

		return methodHandle;
	}

	/**
	 * Discovers the method handle for the given method name and arguments according to two algorithms:
	 *
	 * 1. Exact Match : Matches the incoming argument class types to the method signature
	 * 2. Discovery : Matches the incoming argument class types to the method signature by discovery of matching method names and argument counts
	 *
	 * @param methodName         The name of the method to discover
	 * @param argumentsAsClasses The array of arguments as classes to map
	 *
	 * @return The method handle representing the method signature
	 *
	 * @throws NoSuchMethodException  If the method cannot be found using the discovery algorithm
	 * @throws IllegalAccessException If the method handle cannot be accessed
	 */
	public MethodHandle discoverMethodHandle( String methodName, Class<?>[] argumentsAsClasses )
	        throws NoSuchMethodException, IllegalAccessException {
		// Our target we must find!
		Method targetMethod;

		// 1: Exact Match
		try {
			// This can fail if the arguments are not the exact same class types
			targetMethod = this.targetClass.getMethod( methodName, argumentsAsClasses );
			return METHOD_LOOKUP.findVirtual(
			        this.targetClass,
			        methodName,
			        MethodType.methodType( targetMethod.getReturnType(), argumentsAsClasses )
			);
		} catch ( NoSuchMethodException e ) {
			// 2: Let's go by discovery now
			return Arrays.stream( this.targetClass.getMethods() )
			        // Do it fast!
			        .parallel()
			        // filter by the method name we need
			        .filter( method -> method.getName().equals( methodName ) )
			        // Now by the number of arguments we have
			        .filter( method -> method.getParameterCount() == argumentsAsClasses.length )
			        // TODO: Filter by Argument Cast Matching
			        // Give me the first one found
			        .findFirst()
			        // Convert it to the method handle
			        .map( ClassInvoker::toMethodHandle )
			        // Or throw an exception
			        .orElseThrow( () -> new NoSuchMethodException(
			                String.format(
			                        "No such method [%s] found in the class [%s] using [%d] arguments.",
			                        methodName,
			                        this.targetClass.getName(),
			                        argumentsAsClasses.length
			                )
			        ) );
		}
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
		// System.out.println( "argumentsToClasses -> " + Arrays.toString( args ) );
		// Convert the arguments to an array of classes
		return Arrays.stream( args )
		        .map( ClassInvoker::argumentToClass )
		        .toArray( Class<?>[]::new );
	}
}
