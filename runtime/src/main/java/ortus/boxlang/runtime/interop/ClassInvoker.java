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

import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.scopes.Key;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
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
import java.util.stream.Stream;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and return the results!
 *
 * To create a new class invoker you can use the following:
 *
 * <pre>{@code
 *
 * ClassInvoker target = new ClassInvoker( String.class );
 * ClassInvoker target = ClassInvoker.of( String.class );
 * ClassInvoker target = new ClassInvoker( new String() );
 * ClassInvoker target = ClassInvoker.of( new String() );
 * }
 * </pre>
 *
 * You can then use the following methods to invoke methods on the class:
 * - {@code invokeConstructor( Object... args )} - Invoke a constructor on the class, and store the instance for future method calls
 * - {@code invokeStaticMethod( String methodName, Object... args )} - Invoke a static method on the class
 * - {@code invoke( String methodName, Object... args )} - Invoke a method on the instance of the class
 *
 * TODO:
 * [ ] - Set public fields
 */
public class ClassInvoker implements IReferenceable {

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

		// Unwrap any ClassInvoker instances
		unWrapArguments( args );
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

		// Unwrap any ClassInvoker instances
		unWrapArguments( arguments );

		// Get the invoke dynamic method handle from our cache and discovery techniques
		MethodRecord methodRecord = getMethodHandle( methodName, argumentsToClasses( arguments ) );

		// If it's not static, we need a target instance
		if ( Boolean.FALSE.equals( methodRecord.isStatic() ) && !hasInstance() ) {
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

		// Unwrap any ClassInvoker instances
		unWrapArguments( arguments );

		// Discover and Execute it baby!
		return Optional.ofNullable(
		        getMethodHandle( methodName, argumentsToClasses( arguments ) )
		                .methodHandle()
		                .invokeWithArguments( arguments )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Field Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the value of a public or public static field on a class or instance
	 *
	 * @param fieldName The name of the field to get
	 *
	 * @return The value of the field wrapped in an Optional
	 *
	 * @throws Throwable
	 * @throws NoSuchFieldException  If the field doesn't exist
	 * @throws IllegalStateException If the field is not static and the target instance is null
	 */
	public Optional<Object> getField( String fieldName ) throws Throwable {
		// Discover the field with no case sensitivity
		Field			field		= findField( fieldName );
		// Now get the method handle for the field to execute
		MethodHandle	fieldHandle	= METHOD_LOOKUP.unreflectGetter( field );
		Boolean			isStatic	= Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance
		if ( Boolean.FALSE.equals( isStatic ) && !hasInstance() ) {
			throw new IllegalStateException(
			        "You are trying to get a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		return Optional.ofNullable(
		        Boolean.TRUE.equals( isStatic )
		                ? fieldHandle.invoke()
		                : fieldHandle.invoke( this.targetInstance )
		);
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param fieldName    The name of the field to get
	 * @param defaultValue The default value to return if the field doesn't exist
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public Optional<Object> getField( String fieldName, Object defaultValue ) throws Throwable {
		try {
			return getField( fieldName );
		} catch ( NoSuchFieldException | IllegalStateException e ) {
			return Optional.ofNullable( defaultValue );
		}
	}

	/**
	 * Set the value of a public or public static field on a class or instance
	 *
	 * @param fieldName The name of the field to set
	 * @param value     The value to set the field to
	 *
	 * @return The class invoker
	 *
	 * @throws IllegalStateException If the field is not static and the target instance is null
	 */
	public ClassInvoker setField( String fieldName, Object value ) throws Throwable {
		// Discover the field with no case sensitivity
		Field			field		= findField( fieldName );
		MethodHandle	fieldHandle	= METHOD_LOOKUP.unreflectSetter( field );
		Boolean			isStatic	= Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance, verify it's not null
		if ( Boolean.FALSE.equals( isStatic ) && !hasInstance() ) {
			throw new IllegalStateException(
			        "You are trying to set a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		if ( Boolean.TRUE.equals( isStatic ) ) {
			fieldHandle.invokeWithArguments( value );

		} else {
			fieldHandle.bindTo( this.targetInstance ).invokeWithArguments( value );
		}

		return this;
	}

	/**
	 * Find a field by name with no case-sensitivity (upper case) in the class
	 *
	 * @param fieldName The name of the field to find
	 *
	 * @return The field if discovered
	 *
	 * @throws NoSuchFieldException If the field cannot be found
	 */
	public Field findField( String fieldName ) throws NoSuchFieldException {
		return getFieldsAsStream()
		        .filter( target -> target.getName().equalsIgnoreCase( fieldName ) )
		        .findFirst()
		        .orElseThrow( () -> new NoSuchFieldException(
		                String.format( "No such field [%s] found in the class [%s].", fieldName, this.targetClass.getName() )
		        ) );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name
	 *
	 * @param fieldName The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public Boolean hasField( String fieldName ) {
		return getFieldNames().contains( fieldName );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name and no case-sensitivity (upper case)
	 *
	 * @param fieldName The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public Boolean hasFieldNoCase( String fieldName ) {
		return getFieldNamesNoCase().contains( fieldName.toUpperCase() );
	}

	/**
	 * Get an array of fields of all the public fields for the given class
	 *
	 * @return The fields in the class
	 */
	public Field[] getFields() {
		return this.targetClass.getFields();
	}

	/**
	 * Get a stream of fields of all the public fields for the given class
	 *
	 * @return The stream of fields in the class
	 */
	public Stream<Field> getFieldsAsStream() {
		return Stream.of( getFields() );
	}

	/**
	 * Get a list of field names for the given class with case-sensitivity
	 *
	 * @return A list of field names
	 */
	public List<String> getFieldNames() {
		return getFieldsAsStream()
		        .map( Field::getName )
		        .toList();

	}

	/**
	 * Get a list of field names for the given class with no case-sensitivity (upper case)
	 *
	 * @return A list of field names
	 */
	public List<String> getFieldNamesNoCase() {
		return getFieldsAsStream()
		        .map( Field::getName )
		        .map( String::toUpperCase )
		        .toList();

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
	public Set<Method> getCallableMethods() {
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
	 * Unwrap an object if it's inside a ClassInvoker instance
	 *
	 * @param param The object to unwrap
	 *
	 * @return The target instance or class, depending which one is set
	 */
	public static Object unWrap( Object param ) {
		if ( param instanceof ClassInvoker ) {
			ClassInvoker invoker = ( ClassInvoker ) param;
			if ( invoker.hasInstance() ) {
				return invoker.getTargetInstance();
			} else {
				return invoker.getTargetClass();
			}
		} else {
			return param;
		}
	}

	/**
	 * Unwrap any ClassInvoker instances in the arguments
	 *
	 * @param arguments
	 *
	 * @return
	 */
	void unWrapArguments( Object[] arguments ) {
		for ( int j = 0; j < arguments.length; j++ ) {
			arguments[ j ] = unWrap( arguments[ j ] );
		}
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

	/**
	 * Verifies if the class invoker has an instance or not
	 *
	 * @return True if it has an instance, false otherwise
	 */
	public Boolean hasInstance() {
		return this.targetInstance != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Implementation of IReferencable
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @return The requested obect
	 */
	public Object dereference( Key name ) throws KeyNotFoundException {
		try {
			Optional<Object> result = getField( name.getName() );

			// Handle full null support
			if ( result.isPresent() ) {
				return result.get();
			}
		} catch ( Throwable e ) {
			throw new RuntimeException( e );
		}

		// Field not found anywhere
		if ( hasInstance() ) {
			throw new KeyNotFoundException(
			        String.format( "The instance [%s] has no public field [%s].", getTargetInstance().getClass().getName(),
			                name.getName() )
			);
		} else {
			throw new KeyNotFoundException(
			        String.format( "The class [%s] has no static field [%s].", getTargetClass().getName(), name.getName() )
			);
		}
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( Key name, Object[] arguments ) throws KeyNotFoundException {
		try {
			return invoke( name.getName(), arguments ).orElse( null );
		} catch ( Throwable e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Safely dereference this object by a key and return the value, or null if not found
	 *
	 * @return The requested object or null
	 */
	public Object safeDereference( Key name ) {
		try {
			return getField( name.getName() ).orElse( null );
		} catch ( Throwable e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Assign a value to a field
	 *
	 * @return The requested scope
	 */
	public void assign( Key name, Object value ) {
		try {
			setField( name.getName(), value );
		} catch ( Throwable e ) {
			throw new RuntimeException( e );
		}
	}
}
