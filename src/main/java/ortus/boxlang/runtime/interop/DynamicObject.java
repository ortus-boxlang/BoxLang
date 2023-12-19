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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

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
 */
public class DynamicObject implements IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Helper for all class utility methods from apache commons lang 3
	 */
	public static final Class<ClassUtils>					CLASS_UTILS			= ClassUtils.class;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	Set<Key>												exceptionKeys		= new HashSet<Key>( Arrays.asList(
	    BoxLangException.messageKey,
	    BoxLangException.detailKey,
	    BoxLangException.typeKey,
	    BoxLangException.tagContextKey,
	    ApplicationException.ExtendedInfoKey
	) );
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
	 * Name of key to get length of native arrays
	 */
	private Key												lengthKey			= Key.of( "length" );

	/**
	 * Function service
	 */
	private FunctionService									functionService		= BoxRuntime.getInstance().getFunctionService();

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
	 * @param targetClass The class to create the invoker for
	 */
	public DynamicObject( Class<?> targetClass ) {
		this.targetClass = targetClass;
	}

	/**
	 * Create a new class invoker for the given instance
	 *
	 * @param targetInstance The instance to create the invoker for
	 */
	public DynamicObject( Object targetInstance ) {
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
	public static DynamicObject of( Class<?> targetClass ) {
		return new DynamicObject( targetClass );
	}

	/**
	 * Static factory method to create a new class invoker for the given instance. Mostly used for nice fluent chaining
	 *
	 * @param targetInstance The instance to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static DynamicObject of( Object targetInstance ) {
		return new DynamicObject( targetInstance );
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
	 *
	 * @return The Dynamic Object
	 */
	public DynamicObject setHandlesCacheEnabled( Boolean handlesCacheEnabled ) {
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
	 *
	 * @return The Dynamic Object
	 */
	public DynamicObject setTargetClass( Class<?> targetClass ) {
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
	 *
	 * @return The Dynamic Object
	 */
	public DynamicObject setTargetInstance( Object targetInstance ) {
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
	 */
	public DynamicObject invokeConstructor( Object... args ) {

		// Thou shalt not pass!
		if ( isInterface() ) {
			throw new BoxRuntimeException( "Cannot invoke a constructor on an interface" );
		}

		// Unwrap any ClassInvoker instances
		unWrapArguments( args );
		// Method signature for a constructor is void (Object...)
		MethodType		constructorType	= MethodType.methodType( void.class, argumentsToClasses( args ) );
		// Define the bootstrap method
		MethodHandle	constructorHandle;
		try {
			constructorHandle = METHOD_LOOKUP.findConstructor( this.targetClass, constructorType );
		} catch ( NoSuchMethodException | IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error getting constructor for class " + this.targetClass.getName(), e );
		}
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		// Invoke Dynamic tries to do argument coercion, so we need to convert the arguments to the right types
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		try {
			this.targetInstance = constructorInvoker.invokeWithArguments( args );
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error invoking constructor for class " + this.targetClass.getName(), e );
		}

		return this;
	}

	/**
	 * Invokes the no-arg constructor for the class with the given arguments and stores the instance of the object
	 * into the {@code targetInstance} property for future method calls.
	 * *
	 *
	 * @return The instance of the class
	 *
	 */
	public DynamicObject invokeConstructor() {
		return invokeConstructor( new Object[] {} );
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
	 * @return The result of the method invocation
	 *
	 */
	public Object invoke( String methodName, Object... arguments ) {
		return JavaInvocationService.invoke( this.getTargetClass(), this.getTargetInstance(), methodName, false, arguments );
	}

	/**
	 * Invokes a static method with the given name and arguments on a class or an interface
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 *
	 */
	public Object invokeStatic( String methodName, Object... arguments ) {

		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new BoxRuntimeException( "Method name cannot be null or empty." );
		}

		// Unwrap any ClassInvoker instances
		unWrapArguments( arguments );

		// Discover and Execute it baby!
		try {
			return getMethodHandle( methodName, argumentsToClasses( arguments ) )
			    .methodHandle()
			    .invokeWithArguments( arguments );
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error invoking method " + methodName + " for class " + this.targetClass.getName(), e );
		}
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
	 */
	public Optional<Object> getField( String fieldName ) {
		// Discover the field with no case sensitivity
		Field			field	= findField( fieldName );
		// Now get the method handle for the field to execute
		MethodHandle	fieldHandle;
		try {
			fieldHandle = METHOD_LOOKUP.unreflectGetter( field );
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error getting field " + fieldName + " for class " + this.targetClass.getName(), e );
		}
		Boolean isStatic = Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance
		if ( !isStatic && !hasInstance() ) {
			throw new BoxRuntimeException(
			    "You are trying to get a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		try {
			return Optional.ofNullable(
			    isStatic
			        ? fieldHandle.invoke()
			        : fieldHandle.invoke( this.targetInstance )
			);
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error getting field " + fieldName + " for class " + this.targetClass.getName(), e );
		}
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param fieldName    The name of the field to get
	 * @param defaultValue The default value to return if the field doesn't exist
	 *
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public Optional<Object> getField( String fieldName, Object defaultValue ) {
		try {
			return getField( fieldName );
		} catch ( BoxLangException e ) {
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
	 */
	public DynamicObject setField( String fieldName, Object value ) {
		// Discover the field with no case sensitivity
		Field			field	= findField( fieldName );
		MethodHandle	fieldHandle;
		try {
			fieldHandle = METHOD_LOOKUP.unreflectSetter( field );
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error setting field " + fieldName + " for class " + this.targetClass.getName(), e );
		}
		Boolean isStatic = Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance, verify it's not null
		if ( !isStatic && !hasInstance() ) {
			throw new BoxRuntimeException(
			    "You are trying to set a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		try {
			if ( isStatic ) {
				fieldHandle.invokeWithArguments( value );
			} else {
				fieldHandle.bindTo( this.targetInstance ).invokeWithArguments( value );
			}
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error setting field " + fieldName + " for class " + this.targetClass.getName(), e );
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
	 */
	public Field findField( String fieldName ) {
		return getFieldsAsStream()
		    .filter( target -> target.getName().equalsIgnoreCase( fieldName ) )
		    .findFirst()
		    .orElseThrow( () -> new NoFieldException(
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
	 */
	public MethodRecord getMethodHandle( String methodName, Class<?>[] argumentsAsClasses ) {

		// We use the method signature as the cache key
		// TODO: look at just using string's hashcode directly
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
	 */
	public MethodRecord discoverMethodHandle( String methodName, Class<?>[] argumentsAsClasses ) {
		// Our target we must find using our dynamic rules:
		// - case insensitivity
		// - argument count
		// - argument class asignability
		Method targetMethod = findMatchingMethod( methodName, argumentsAsClasses );

		try {
			return new MethodRecord(
			    methodName,
			    targetMethod,
			    METHOD_LOOKUP.unreflect( targetMethod ),
			    Modifier.isStatic( targetMethod.getModifiers() ),
			    argumentsAsClasses.length
			);
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error getting method handle for method " + methodName + " for class " + this.targetClass.getName(), e );
		}
	}

	/**
	 * Get a HashSet of methods of all the unique callable method signatures for the given class
	 *
	 * @return A unique set of callable methods
	 */
	public Set<Method> getMethods() {
		Set<Method> allMethods = new HashSet<>();
		allMethods.addAll( new HashSet<>( List.of( this.targetClass.getMethods() ) ) );
		allMethods.addAll( new HashSet<>( List.of( this.targetClass.getDeclaredMethods() ) ) );
		return allMethods;
	}

	/**
	 * Get a stream of methods of all the unique callable method signatures for the given class
	 *
	 * @return A stream of unique callable methods
	 */
	public Stream<Method> getMethodsAsStream() {
		return getMethods().stream();
	}

	/**
	 * Get a list of method names for the given class
	 *
	 * @return A list of method names
	 */
	public List<String> getMethodNames() {
		return getMethodsAsStream()
		    .parallel()
		    .map( Method::getName )
		    .toList();
	}

	/**
	 * Get a list of method names for the given class with no case-sensitivity (upper case)
	 *
	 * @return A list of method names with no case
	 */
	public List<String> getMethodNamesNoCase() {
		return getMethodsAsStream()
		    .parallel()
		    .map( Method::getName )
		    .map( String::toUpperCase )
		    .toList();
	}

	/**
	 * Verifies if the class has a public or public static method with the given name
	 *
	 * @param methodName The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public Boolean hasMethod( String methodName ) {
		return getMethodNames().contains( methodName );
	}

	/**
	 * Verifies if the class has a public or public static method with the given name and no case-sensitivity (upper case)
	 *
	 * @param methodName The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public Boolean hasMethodNoCase( String methodName ) {
		return getMethodNamesNoCase().contains( methodName.toUpperCase() );
	}

	/**
	 * This method is used to verify if the class has the same method signature as the incoming one with no case-sensitivity (upper case)
	 *
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 *
	 * @return The matched method signature
	 *
	 */
	public Method findMatchingMethod( String methodName, Class<?>[] argumentsAsClasses ) {
		return getMethodsAsStream()
		    .parallel()
		    .filter( method -> method.getName().equalsIgnoreCase( methodName ) )
		    .filter( method -> hasMatchingParameterTypes( method, argumentsAsClasses ) )
		    .findFirst()
		    .orElseThrow( () -> new NoMethodException(
		        String.format(
		            "No such method [%s] found in the class [%s] using [%d] arguments of types [%s]",
		            methodName,
		            this.targetClass.getName(),
		            argumentsAsClasses.length,
		            Arrays.toString( argumentsAsClasses )
		        )
		    ) );
	}

	/**
	 * Verifies if the method has the same parameter types as the incoming ones
	 *
	 * @see https://commons.apache.org/proper/commons-lang/javadocs/api-release/index.html
	 *
	 * @param method             The method to check
	 * @param argumentsAsClasses The arguments to check
	 *
	 * @return True if the method has the same parameter types, false otherwise
	 */
	private static boolean hasMatchingParameterTypes( Method method, Class<?>[] argumentsAsClasses ) {
		Class<?>[] methodParams = Arrays
		    .stream( method.getParameters() )
		    .map( Parameter::getType )
		    .toArray( Class<?>[]::new );

		if ( methodParams.length != argumentsAsClasses.length ) {
			return false;
		}

		// Verify assignability including primitive autoboxing
		return ClassUtils.isAssignable( argumentsAsClasses, methodParams );

		// return IntStream.range( 0, methodParameters.length )
		// .allMatch( i -> ClassUtils.isAssignable( argumentsAsClasses[ i ], methodParameters[ i ].getType() ) );
	}

	/**
	 * Utility method to convert a method to a method handle
	 *
	 * @param method The method to convert
	 *
	 * @return The method handle representing the method or an exception if it fails
	 *
	 */
	public static MethodHandle toMethodHandle( Method method ) {
		try {
			return METHOD_LOOKUP.unreflect( method );
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error creating MethodHandle for method [" + method + "]", e );
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
		    .map( DynamicObject::argumentToClass )
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
		if ( param instanceof DynamicObject invoker ) {
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
	 * Instance method to unwrap itself
	 *
	 * @return The target instance or class, depending which one is set
	 */
	public Object unWrap() {
		if ( hasInstance() ) {
			return getTargetInstance();
		} else {
			return getTargetClass();
		}
	}

	/**
	 * Unwrap any ClassInvoker instances in the arguments
	 *
	 * @param arguments The arguments to unwrap
	 *
	 * @return The unwrapped arguments
	 */
	void unWrapArguments( Object[] arguments ) {
		for ( int j = 0; j < arguments.length; j++ ) {
			arguments[ j ] = unWrap( arguments[ j ] );
		}
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
	 * This immutable record represents an executable method handle and it's metadata.
	 * This record is the one that is cached in the {@link DynamicObject#methodCache} map.
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
	 * --------------------------------------------------------------------------
	 * Implementation of IReferencable
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param name The name of the key to dereference
	 * @param safe If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested object
	 */
	@SuppressWarnings( "unchecked" )
	public Object dereference( Key name, Boolean safe ) {

		// This check allows us to lazy-create meta for BoxLang types the first time it is requested
		if ( name.equals( BoxMeta.key ) ) {
			if ( hasInstance() && getTargetInstance() instanceof IType type ) {
				return type.getBoxMeta();
			}
			return new GenericMeta( getTargetInstance() );
		}

		// If the object is referencable, allow it to handle the dereference
		if ( hasInstance() && getTargetInstance() instanceof IReferenceable ref ) {
			return ref.dereference( name, safe );
		}

		if ( getTargetInstance() instanceof Map ) {
			// If it's a raw Map, then we use the original value as the key
			return ( ( Map<Object, Object> ) getTargetInstance() ).get( name.getOriginalValue() );
			// Special logic so we can treat exceptions as referencable. Possibly move to helper
		} else if ( getTargetInstance() instanceof List list ) {
			Integer index = Array.validateAndGetIntForDerefernce( name, list.size(), safe );
			// non-existant indexes return null when dereferncing safely
			if ( safe && ( index < 1 || index > list.size() ) ) {
				return null;
			}
			return list.get( index - 1 );
		} else if ( hasInstance() && getTargetInstance().getClass().isArray() ) {
			Object[] arr = ( ( Object[] ) getTargetInstance() );
			if ( name.equals( lengthKey ) ) {
				return arr.length;
			}

			Integer index = Array.validateAndGetIntForDerefernce( name, arr.length, safe );
			// non-existant indexes return null when dereferncing safely
			if ( safe && ( index < 1 || index > arr.length ) ) {
				return null;
			}
			return arr[ index - 1 ];
		} else if ( getTargetInstance() instanceof Throwable t && exceptionKeys.contains( name ) ) {
			// Throwable.message always delegates through to the message field
			if ( name.equals( BoxLangException.messageKey ) ) {
				return t.getMessage();
			} else {
				// CFML returns "" for Throwable.detail, etc
				return "";
			}
			// Special logic for native arrays. Possibly move to helper
		} else if ( hasField( name.getName() ) ) {
			// If we have the field, return it's value, even if it's null
			return getField( name.getName() ).orElse( null );
		}

		if ( safe ) {
			return null;
		}

		// Field not found anywhere
		if ( hasInstance() ) {
			throw new KeyNotFoundException(
			    String.format( "The instance [%s] has no public field [%s].  The allowed fields are [%s]",
			        ClassUtils.getCanonicalName( getTargetClass() ),
			        name.getName(),
			        getFieldNames()
			    )
			);
		} else {
			throw new KeyNotFoundException(
			    String.format( "The instance [%s] has no static field [%s].  The allowed fields are [%s]",
			        ClassUtils.getCanonicalName( getTargetClass() ),
			        name.getName(),
			        getFieldNames()
			    )
			);
		}
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name                The name of the key to dereference, which becomes the method name
	 * @param positionalArguments The arguments to pass to the invokable
	 * @param safe                If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {

		if ( hasInstance() && getTargetInstance() instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, positionalArguments, safe );
		}

		if ( hasInstance() ) {
			MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, getTargetInstance() );
			if ( memberDescriptor != null ) {
				return memberDescriptor.invoke( context, getTargetInstance(), positionalArguments );
			}
		}

		// member functions on java objects
		// temp workaround for test src\test\java\TestCases\phase2\ObjectLiteralTest.java
		if ( getTargetInstance() instanceof Boolean bool && name.equals( Key.of( "yesNoFormat" ) ) ) {
			return bool ? "Yes" : "No";
		}
		// temp workaround for test src\test\java\TestCases\phase2\ObjectLiteralTest.java
		if ( getTargetInstance() instanceof String str && name.equals( Key.of( "len" ) ) ) {
			return str.length();
		}

		if ( safe && !hasMethod( name.getName() ) ) {
			return null;
		}

		return invoke( name.getName(), positionalArguments );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		if ( hasInstance() && getTargetInstance() instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		if ( hasInstance() ) {
			MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, getTargetInstance() );
			if ( memberDescriptor != null ) {
				return memberDescriptor.invoke( context, getTargetInstance(), namedArguments );
			}
		}

		// member functions on java objects
		// temp workaround for test src\test\java\TestCases\phase2\ObjectLiteralTest.java
		if ( getTargetInstance() instanceof Boolean bool && name.equals( Key.of( "yesNoFormat" ) ) ) {
			return bool ? "Yes" : "No";
		}
		// temp workaround for test src\test\java\TestCases\phase2\ObjectLiteralTest.java
		if ( getTargetInstance() instanceof String str && name.equals( Key.of( "len" ) ) ) {
			return str.length();
		}

		throw new BoxRuntimeException( "Java objects cannot be called with named argumments" );
	}

	/**
	 * Assign a value to a field
	 *
	 * @param name  The name of the field to assign
	 * @param value The value to assign
	 */
	@SuppressWarnings( "unchecked" )
	public Object assign( Key name, Object value ) {

		if ( hasInstance() && getTargetInstance() instanceof IReferenceable ref ) {
			ref.assign( name, value );
		} else if ( hasInstance() && getTargetInstance().getClass().isArray() ) {
			Object[]	arr		= ( ( Object[] ) getTargetInstance() );
			Integer		index	= Array.validateAndGetIntForAssign( name, arr.length, true );
			arr[ index - 1 ] = value;
			return value;
		} else if ( getTargetInstance() instanceof List list ) {
			Integer index = Array.validateAndGetIntForAssign( name, list.size(), false );
			if ( index > list.size() ) {
				// If the index is larger than the array, pad the array with nulls
				for ( int i = list.size(); i < index; i++ ) {
					list.add( null );
				}
			}
			list.set( index - 1, value );
			return value;
		} else if ( getTargetInstance() instanceof Map ) {
			// If it's a raw Map, then we use the original value as the key
			( ( Map<Object, Object> ) getTargetInstance() ).put( name.getOriginalValue(), value );
			return value;
		}

		try {
			setField( name.getName(), value );
		} catch ( Throwable e ) {
			// CFML ignores Throwable.foo = "bar"
			if ( getTargetInstance() instanceof Throwable ) {
				return value;
			}
			throw e;
		}
		return value;
	}
}
