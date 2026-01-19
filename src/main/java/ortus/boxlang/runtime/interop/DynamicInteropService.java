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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.ClassBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCasterLoose;
import ortus.boxlang.runtime.dynamic.casters.TimeCaster;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyService;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.JavaMethod;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.StructMapWrapper;
import ortus.boxlang.runtime.types.exceptions.AbstractClassException;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.NoConstructorException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.util.BooleanRef;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.ObjectRef;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * This class is used to provide a way to dynamically and efficiently interact with the java layer from the within a BoxLang environment.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and return the results!
 *
 * To create a new dynamic object, you can use the following:
 *
 * <pre>{@code
 *
 * DynamicObject target = new DynamicObject( String.class );
 * DynamicObject target = DynamicObject.of( String.class );
 * DynamicObject target = new DynamicObject( new String() );
 * DynamicObject target = DynamicObject.of( new String() );
 * }
 * </pre>
 *
 * You can then use the following methods to invoke methods on the class:
 * - {@code invokeConstructor( Object... args )} - Invoke a constructor on the class, and store the instance for future method calls
 * - {@code invokeStaticMethod( String methodName, Object... args )} - Invoke a static method on the class
 * - {@code invoke( String methodName, Object... args )} - Invoke a method on the instance of the class
 */
public class DynamicInteropService {

	public static enum MATCH_TYPE {
	    // This is an exact match of class name. So Map == Map, but HashMap != Map, etc
		EXACT,
	    // This is an assignable match of class name. So Map == Map, and HashMap == Map, etc
		ASSIGNABLE,
	    // This is a coercion of the VALUE to match the required type, so 1 == "1" and "yes" == true, etc
		COERCE
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Helper for all class utility methods from apache commons lang 3
	 */
	public static final Class<ClassUtils>							CLASS_UTILS			= ClassUtils.class;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * These keys are always available on all throwables. If the key is not found, then we return an empty string
	 */
	private static final Set<Key>									exceptionKeys		= new HashSet<>( Arrays.asList(
	    Key.message,
	    Key.detail,
	    Key.type,
	    Key.tagContext,
	    Key.extendedinfo,
	    Key.stackTrace,
	    Key.cause,
	    Key.errorcode
	) );

	/**
	 * This is a map of primitive types to their native Java counterparts
	 * so we can do the right casting for primitives
	 */
	private static final Map<Class<?>, Class<?>>					PRIMITIVE_MAP;

	/**
	 * This is a map of wrapper types to their native Java primitives counterparts
	 * so we can do the right casting for wrappers
	 */
	private static final Map<Class<?>, Class<?>>					WRAPPERS_MAP;

	/**
	 * This is the method handle lookup
	 *
	 * @see https://docs.oracle.com/javase/21/docs/api/java/lang/invoke/MethodHandles.Lookup.html
	 */
	private static final MethodHandles.Lookup						METHOD_LOOKUP;

	/**
	 * This caches the method handles for the class so we don't have to look them up every time
	 */
	private static final ConcurrentHashMap<String, MethodRecord>	methodHandleCache	= new ConcurrentHashMap<>( 32 );

	/**
	 * Name of key to get length of native arrays
	 */
	private static Key												lengthKey			= Key.of( "length" );

	/**
	 * Empty arguments array
	 */
	public static final Object[]									EMPTY_ARGS			= new Object[] {};

	/**
	 * This enables or disables the method handles cache
	 */
	private static Boolean											handlesCacheEnabled	= true;

	/**
	 * This is the class locator
	 */
	private static ClassLocator										classLocator		= null;

	/**
	 * This is the function service for invoking functions
	 */
	private static FunctionService									functionService		= null;

	/**
	 * Coercion maps
	 */
	private static List<String>										numberTargets		= List.of( "boolean", "byte", "character", "string" );
	private static List<String>										booleanTargets		= List.of( "string", "character" );

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

		WRAPPERS_MAP	= Map.of(
		    boolean.class, Boolean.class,
		    byte.class, Byte.class,
		    char.class, Character.class,
		    short.class, Short.class,
		    int.class, Integer.class,
		    long.class, Long.class,
		    float.class, Float.class,
		    double.class, Double.class
		);

	}

	/**
	 * --------------------------------------------------------------------------
	 * Setters & Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Verify if the handles cache is enabled or not
	 *
	 * @return the handlesCacheEnabled flag
	 */
	public static Boolean isHandlesCacheEnabled() {
		return handlesCacheEnabled;
	}

	/**
	 * Set the handles cache enabled flag
	 *
	 * @param enabled Enable or not the handles cache
	 */
	public static void setHandlesCacheEnabled( Boolean enabled ) {
		handlesCacheEnabled = enabled;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Invokers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invokes the constructor for the class with the given positional arguments and returns the instance of the object
	 *
	 * @param context     The context to use for the constructor
	 * @param targetClass The Class that you want to invoke a constructor on
	 * @param args        The positional arguments to pass to the constructor
	 *
	 * @throws BoxRuntimeException If the incoming class is an interface
	 *
	 * @return The instance of the class
	 */
	public static <T> T invokeConstructor( IBoxContext context, Class<T> targetClass, Object... args ) {
		Object[]	BLArgs	= null;
		boolean		noInit	= false;

		// Thou shalt not pass!
		if ( isInterface( targetClass ) ) {
			throw new BoxRuntimeException( "Cannot invoke a constructor on an interface: " + targetClass.getName() );
		}

		// check if targetClass is an IClassRunnable, to see if we need to skip the initialization
		// This might be a super class, so we need to skip the initialization
		if ( IClassRunnable.class.isAssignableFrom( targetClass ) ) {
			// This tells us to skip the initialization because it's a super class
			if ( args.length == 1 && args[ 0 ] != null && args[ 0 ].equals( Key.noInit ) ) {
				noInit = true;
			} else {
				BLArgs = args;
			}
			args = EMPTY_ARGS;
		}

		// Unwrap any DynamicObject instances
		unWrapArguments( args );

		// Discover the constructor method handle using the target class and the argument type matching
		MethodHandle	constructorHandle;
		Class<?>[]		argumentClasses			= argumentsToClasses( args );
		Object[]		castedArgumentValues	= new Object[ args.length ];
		Executable		constructor;
		try {
			// TODO: implement caching strategy for constructors
			BooleanRef isCachable = new BooleanRef( true );
			constructor = findMatchingConstructor( context, targetClass, argumentClasses, isCachable, castedArgumentValues, args );
			if ( constructor == null ) {
				throw new NoConstructorException(
				    String.format(
				        "No such constructor found in the class [%s] using [%d] arguments of types [%s]",
				        targetClass.getName(),
				        argumentClasses.length,
				        Arrays.toString( argumentClasses )
				    )
				);
			}
			constructorHandle = METHOD_LOOKUP.unreflectConstructor( ( Constructor<?> ) constructor );
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException(
			    "Error getting constructor for class " + targetClass.getName() + " with arguments classes " + Arrays.toString( argumentClasses ),
			    e
			);
		}

		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		// Invoke Dynamic tries to do argument coercion, so we need to convert the arguments to the right types
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		try {

			@SuppressWarnings( "unchecked" )
			T thisInstance = ( T ) constructorInvoker.invokeWithArguments( expandVarargs( castedArgumentValues, constructor.isVarArgs(), false, constructor ) );

			// If this is a Box Class, some additional initialization is needed
			if ( thisInstance instanceof IClassRunnable boxClass ) {
				return bootstrapBLClass( context, boxClass, BLArgs, null, noInit );
			}

			// Announce it to the world
			BoxRuntime
			    .getInstance()
			    .getInterceptorService()
			    .announce(
			        BoxEvent.AFTER_DYNAMIC_OBJECT_CREATION,
			        () -> Struct.ofNonConcurrent(
			            Key.object, thisInstance,
			            Key.clazz, targetClass
			        )
			    );

			return thisInstance;
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error invoking constructor for class " + targetClass.getName(), e );
		}
	}

	/**
	 * Invokes the constructor for the class with the given named arguments and returns the instance of the object
	 *
	 * @param targetClass The Class that you want to invoke a constructor on
	 * @param args        The named arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 */
	public static <T> T invokeConstructor( IBoxContext context, Class<T> targetClass, Map<Key, Object> args ) {

		// Thou shalt not pass!
		if ( isInterface( targetClass ) ) {
			throw new BoxRuntimeException( "Cannot invoke a constructor on an interface" );
		}
		// check if targetClass is an IClassRunnable
		if ( !IClassRunnable.class.isAssignableFrom( targetClass ) ) {
			throw new BoxRuntimeException( "Cannot use named arguments on a Java constructor." );
		}
		// Method signature for a constructor is void (Object...)
		MethodType		constructorType	= MethodType.methodType( void.class, argumentsToClasses( EMPTY_ARGS ) );
		// Define the bootstrap method
		MethodHandle	constructorHandle;
		try {
			constructorHandle = METHOD_LOOKUP.findConstructor( targetClass, constructorType );
		} catch ( NoSuchMethodException | IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error getting constructor for class " + targetClass.getName(), e );
		}
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		// Invoke Dynamic tries to do argument coercion, so we need to convert the arguments to the right types
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		try {
			@SuppressWarnings( "unchecked" )
			T thisInstance = ( T ) constructorInvoker.invokeWithArguments( EMPTY_ARGS );

			// If this is a Box Class, some additional initialization is needed
			if ( thisInstance instanceof IClassRunnable boxClass ) {
				return bootstrapBLClass( context, boxClass, null, args, false );
			}
			return thisInstance;
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException(
			    "Error invoking constructor for class " + targetClass.getName() +
			        ". Caused by " + e.getMessage(),
			    e.getClass().getName(),
			    e
			);
		}
	}

	/**
	 * Invokes the no-arg constructor for the class with the given arguments and returns the instance of the object
	 *
	 * @param targetClass The Class that you want to invoke a constructor on
	 *
	 * @return The instance of the class
	 */
	public static <T> T invokeConstructor( IBoxContext context, Class<T> targetClass ) {
		return invokeConstructor( context, targetClass, EMPTY_ARGS );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param context     The context to use for the method invocation
	 * @param targetClass The Class that you want to invoke a method on
	 * @param methodName  The name of the method to invoke
	 * @param safe        Whether the method should throw an error or return null if it doesn't exist
	 * @param arguments   The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( IBoxContext context, Class<?> targetClass, String methodName, Boolean safe, Object... arguments ) {
		return invoke( context, targetClass, null, methodName, safe, arguments );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param context        The context to use for the method invocation
	 * @param targetInstance The instance to call the method on
	 * @param methodName     The name of the method to invoke
	 * @param safe           Whether the method should throw an error or return null if it doesn't exist
	 * @param arguments      The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( IBoxContext context, Object targetInstance, String methodName, Boolean safe, Object... arguments ) {
		return invoke( context, targetInstance.getClass(), targetInstance, methodName, safe, arguments );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param context        The context to use for the method invocation
	 * @param targetClass    The Class that you want to invoke a method on
	 * @param targetInstance The instance to call the method on, or null if it's static
	 * @param safe           Whether the method should throw an error or return null if it doesn't exist
	 * @param methodName     The name of the method to invoke
	 * @param arguments      The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( IBoxContext context, Class<?> targetClass, Object targetInstance, String methodName, Boolean safe, Object... arguments ) {
		return invoke( context, null, targetClass, targetInstance, methodName, safe, arguments );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param context        The context to use for the method invocation
	 * @param targetClass    The Class that you want to invoke a method on
	 * @param targetInstance The instance to call the method on, or null if it's static
	 * @param safe           Whether the method should throw an error or return null if it doesn't exist
	 * @param methodName     The name of the method to invoke
	 * @param arguments      The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke(
	    IBoxContext context,
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    Object targetInstance,
	    String methodName,
	    Boolean safe,
	    Object... arguments ) {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new BoxRuntimeException( "Method name cannot be null or empty." );
		}

		// Unwrap any DynamicObject instances
		unWrapArguments( arguments );

		// Get the invoke dynamic method handle from our cache and discovery techniques
		MethodRecord	methodRecord;
		Class<?>[]		argumentClasses			= argumentsToClasses( arguments );
		// The final values will be calcuted while finding the method handle and placed by reference in this array
		Object[]		castedArgumentValues	= new Object[ arguments.length ];

		try {
			methodRecord	= getMethodHandle(
			    context,
			    dynamicObject,
			    targetClass,
			    targetInstance,
			    methodName,
			    argumentClasses,
			    castedArgumentValues,
			    arguments
			);
			// May have been populated by getMethodHandle
			targetInstance	= ( dynamicObject == null ? targetInstance : dynamicObject.getTargetInstance() );
		} catch ( RuntimeException e ) {
			if ( safe ) {
				return null;
			} else {
				e.printStackTrace();
				throw new BoxRuntimeException( "Error getting method [" + methodName + "] for class [" + targetClass.getName() + "]", e );
			}
		}

		// If we are calling an instance method, but have no instance, and have the dynamicobject reference, then initialize it if there is a no-arg constructor
		// We are doing this here as well as inside discoverMethodHandle because when the method handle is cached, the discover method will not be run again
		if ( !methodRecord.isStatic() && targetInstance == null && dynamicObject != null ) {
			// Check if the dynamic object has a no-arg constructor
			try {
				targetInstance = invokeConstructor( context, targetClass );
				dynamicObject.setTargetInstance( targetInstance );
			} catch ( NoConstructorException e ) {
				// If there is no no-arg constructor, then we can't call the method
				throw new BoxRuntimeException( "No instance provided for non-static method " + methodName + " for class " + targetClass.getName()
				    + " and there is not an no-arg constructor to call." );
			}
		}

		try {

			// logger.debug(
			// "Executing method handle [" + methodName + "] for class [" + targetClass.getName() +
			// "] with arguments: " + Arrays.toString( arguments )
			// );

			// Execute Static
			if ( methodRecord.isStatic() ) {
				return methodRecord
				    .methodHandle()
				    .invokeWithArguments(
				        expandVarargs( castedArgumentValues, methodRecord.method().isVarArgs(), false, methodRecord.method() )
				    );
			}

			// Use a spread strategy to avoid binding if possible due to performance considerations
			Object[] finalArgs = expandVarargs( castedArgumentValues, methodRecord.method().isVarArgs(), true, methodRecord.method() );

			// Varargs are a pain, do separately, until I can figure out how to address it property
			if ( methodRecord.method().isVarArgs() && finalArgs.length > 0 ) {
				return methodRecord
				    .methodHandle()
				    .bindTo( targetInstance )
				    .invokeWithArguments(
				        expandVarargs( castedArgumentValues, methodRecord.method().isVarArgs(), true, methodRecord.method() )
				    );
			}

			// Accelerated invoke for up to 7 arguments
			switch ( finalArgs.length ) {
				case 0 -> {
					return methodRecord.methodHandle().invoke( targetInstance );
				}
				case 1 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ] );
				}
				case 2 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ] );
				}
				case 3 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ], finalArgs[ 2 ] );
				}
				case 4 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ], finalArgs[ 2 ], finalArgs[ 3 ] );
				}
				case 5 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ], finalArgs[ 2 ], finalArgs[ 3 ], finalArgs[ 4 ] );
				}
				case 6 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ], finalArgs[ 2 ], finalArgs[ 3 ], finalArgs[ 4 ],
					    finalArgs[ 5 ] );
				}
				case 7 -> {
					return methodRecord.methodHandle().invoke( targetInstance, finalArgs[ 0 ], finalArgs[ 1 ], finalArgs[ 2 ], finalArgs[ 3 ], finalArgs[ 4 ],
					    finalArgs[ 5 ], finalArgs[ 6 ] );
				}
				default -> {
					return methodRecord
					    .methodHandle()
					    .bindTo( targetInstance )
					    .invokeWithArguments(
					        expandVarargs( castedArgumentValues, methodRecord.method().isVarArgs(), true, methodRecord.method() )
					    );
				}
			}

		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException(
			    "Error invoking method " + methodName + "() for class " + targetClass.getName() +
			        ". Due to: " + e.getMessage(),
			    e.getClass().getName(),
			    e
			);
		}
	}

	/**
	 * Take an array of arguments, and if the method is using varargs, expand the array at the end
	 *
	 * @param arguments The arguments to expand
	 * @param isVarargs Whether the method is varargs
	 *
	 * @return The expanded arguments
	 */
	public static Object[] expandVarargs( Object[] arguments, boolean isVarargs, boolean isInstance, Executable executable ) {
		int paramCount = executable.getParameterCount();

		// If we are calling a vararg method, but omitted the last argument, then we need to add in the empty array here
		if ( executable.isVarArgs() && paramCount > arguments.length ) {
			// create new argument array with an array of the varargs type at the end
			Object[] expandedArgs = new Object[ arguments.length + 1 ];
			System.arraycopy( arguments, 0, expandedArgs, 0, arguments.length );
			// The last argument is an array of the varargs type
			expandedArgs[ arguments.length ]	= java.lang.reflect.Array.newInstance( executable.getParameterTypes()[ paramCount - 1 ].getComponentType(), 0 );
			arguments							= expandedArgs;
		}

		// If it's not varargs, or it's an instance method, then just return the arguments
		// I don't understand why instance method don't want the varargs expanded, but this is what makes the tests pass
		if ( !isVarargs || isInstance ) {
			return arguments;
		}
		if ( arguments.length == 0 ) {
			throw new BoxRuntimeException( "Varargs method requires at least one argument." );
		}
		Object[]	lastArgumentValues;
		Object		lastArgument	= arguments[ arguments.length - 1 ];
		if ( lastArgument instanceof Array arr ) {
			lastArgumentValues = arr.toArray();
		} else if ( lastArgument == null ) {
			throw new BoxRuntimeException( "Varargs method requires an non-null array as the last argument." );
		} else if ( lastArgument.getClass().isArray() ) {
			// convert the array to an Object array
			lastArgumentValues = ArrayCaster.cast( lastArgument ).toArray();
		} else {
			throw new BoxRuntimeException( "Varargs method requires an array as the last argument.  You passed [" + lastArgument.getClass().getName() + "]" );
		}

		Object[] expandedArgs = new Object[ arguments.length - 1 + lastArgumentValues.length ];
		System.arraycopy( arguments, 0, expandedArgs, 0, arguments.length - 1 ); // Copy all but the last argument
		System.arraycopy( lastArgumentValues, 0, expandedArgs, arguments.length - 1, lastArgumentValues.length ); // Copy the last argument
		return expandedArgs;
	}

	/**
	 * Invokes a static method with the given name and arguments on a class or an interface
	 *
	 * @param context     The context to use for the method invocation
	 * @param targetClass The class you want to invoke a method on
	 * @param methodName  The name of the method to invoke
	 * @param arguments   The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invokeStatic( IBoxContext context, DynamicObject dynamicObject, Class<?> targetClass, String methodName, Object... arguments ) {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new BoxRuntimeException( "Method name cannot be null or empty." );
		}

		// Unwrap any DynamicObject instances
		unWrapArguments( arguments );
		Object[]		castedArgumentValues	= new Object[ arguments.length ];
		Class<?>[]		argumentClasses			= argumentsToClasses( arguments );
		MethodRecord	methodRecord			= getMethodHandle(
		    context,
		    dynamicObject,
		    targetClass,
		    null,
		    methodName,
		    argumentClasses,
		    castedArgumentValues,
		    arguments
		);

		// Discover and Execute it baby!
		try {
			return methodRecord
			    .methodHandle()
			    .invokeWithArguments( expandVarargs( castedArgumentValues, methodRecord.method().isVarArgs(), false, methodRecord.method() ) );
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException(
			    "Error invoking method " + methodName + "() for class " + targetClass.getName() +
			        ". Caused by " + e.getMessage(),
			    e.getClass().getName(),
			    e
			);
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
	 * @param targetClass The class you want to look for a field on
	 * @param fieldName   The name of the field to get
	 *
	 * @return The value of the field wrapped in an Optional
	 */
	public static Optional<Object> getField( Class<?> targetClass, String fieldName ) {
		return getField( targetClass, ( Object ) null, fieldName );
	}

	/**
	 * Get the value of a public or public static field on a class or instance
	 *
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to get
	 *
	 * @return The value of the field wrapped in an Optional
	 */
	public static Optional<Object> getField( Object targetInstance, String fieldName ) {
		return getField( targetInstance.getClass(), targetInstance, fieldName );
	}

	/**
	 * Get the value of a public or public static field on a class or instance
	 *
	 * @param targetClass    The class you want to look for a field on
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to get
	 *
	 * @return The value of the field wrapped in an Optional
	 */
	public static Optional<Object> getField( Class<?> targetClass, Object targetInstance, String fieldName ) {

		// Discover the field with no case sensitivity
		Field			field	= findField( targetClass, fieldName );
		// Now get the method handle for the field to execute
		MethodHandle	fieldHandle;
		try {
			// If we getting a field from the java super class of a BoxClass, we need to defer to the actual
			// BoxClass to get the file handle due to Java security.
			if ( targetInstance != null && !field.getDeclaringClass().equals( targetInstance.getClass() )
			    && targetInstance instanceof IClassRunnable boxClass ) {
				fieldHandle = boxClass.lookupPrivateField( field );
			} else {
				// If the field's declaring class is not public (package-private, protected, or private abstract parent),
				// but the target class is public, we need to use a lookup from the target class to access the field
				if ( !Modifier.isPublic( field.getDeclaringClass().getModifiers() ) && Modifier.isPublic( targetClass.getModifiers() ) ) {
					// Use MethodHandles.privateLookupIn to get access through the public subclass
					MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn( targetClass, METHOD_LOOKUP );
					fieldHandle = privateLookup.unreflectGetter( field );
				} else {
					fieldHandle = METHOD_LOOKUP.unreflectGetter( field );
				}
			}
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error getting field " + fieldName + " for class " + targetClass.getName(), e );
		}
		Boolean isStatic = Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance
		if ( !isStatic && targetInstance == null ) {
			throw new BoxRuntimeException(
			    "You are trying to get a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		try {
			return Optional.ofNullable(
			    isStatic
			        ? fieldHandle.invoke()
			        : fieldHandle.invoke( targetInstance )
			);
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException(
			    "Error getting field " + fieldName + " for class " + targetClass.getName(),
			    e.getClass().getName(),
			    e
			);
		}
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to get
	 * @param defaultValue   The default value to return if the field doesn't exist
	 *
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public static Optional<Object> getField( Object targetInstance, String fieldName, Object defaultValue ) {
		return getField( targetInstance.getClass(), fieldName, defaultValue );
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param targetClass  The class you want to look for a field on
	 * @param fieldName    The name of the field to get
	 * @param defaultValue The default value to return if the field doesn't exist
	 *
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public static Optional<Object> getField( Class<?> targetClass, String fieldName, Object defaultValue ) {
		return getField( targetClass, null, fieldName, defaultValue );
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param targetClass    The class you want to look for a field on
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to get
	 * @param defaultValue   The default value to return if the field doesn't exist
	 *
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public static Optional<Object> getField( Class<?> targetClass, Object targetInstance, String fieldName, Object defaultValue ) {
		try {
			return getField( targetClass, targetInstance, fieldName );
		} catch ( BoxLangException e ) {
			return Optional.ofNullable( defaultValue );
		}
	}

	/**
	 * Set the value of a public or public static field on a class or instance
	 *
	 * @param targetClass The class you want to look for a field on
	 * @param fieldName   The name of the field to set
	 * @param value       The value to set the field to
	 */
	public static void setField( Class<?> targetClass, String fieldName, Object value ) {
		setField( targetClass, null, fieldName, value );
	}

	/**
	 * Set the value of a public or public static field on a class or instance
	 *
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to set
	 * @param value          The value to set the field to
	 */
	public static void setField( Object targetInstance, String fieldName, Object value ) {
		setField( targetInstance.getClass(), targetInstance, fieldName, value );
	}

	/**
	 * Set the value of a public or public static field on a class or instance
	 *
	 * @param targetClass    The class you want to look for a field on
	 * @param targetInstance The instance you want to look for a field on
	 * @param fieldName      The name of the field to set
	 * @param value          The value to set the field to
	 */
	public static void setField( Class<?> targetClass, Object targetInstance, String fieldName, Object value ) {
		// Discover the field with no case sensitivity
		Field			field	= findField( targetClass, fieldName );
		MethodHandle	fieldHandle;
		try {
			fieldHandle = METHOD_LOOKUP.unreflectSetter( field );
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error setting field " + fieldName + " for class " + targetClass.getName(), e );
		}
		Boolean isStatic = Modifier.isStatic( field.getModifiers() );

		// If it's not static, we need a target instance, verify it's not null
		if ( !isStatic && targetInstance == null ) {
			throw new BoxRuntimeException(
			    "You are trying to set a public field but there is not instance set on the invoker, please make sure the [invokeConstructor] has been called."
			);
		}

		Class<?> fieldType = field.getType();
		// TODO: Add context parameter to this method so we can avoid RequestBoxContext.runInContext()
		RequestBoxContext.runInContext( ( ctx ) -> {
			try {
				Object coercedValue = DynamicInteropService.coerceValue( ctx, value, fieldType );

				if ( isStatic ) {
					fieldHandle.invokeWithArguments( coercedValue );
				} else {
					fieldHandle.bindTo( targetInstance ).invokeWithArguments( coercedValue );
				}
				return null;
			} catch ( RuntimeException e ) {
				throw e;
			} catch ( Throwable e ) {
				throw new BoxRuntimeException(
				    "Error setting field " + fieldName + " for class " + targetClass.getName(),
				    e.getClass().getName(),
				    e
				);
			}
		} );

	}

	/**
	 * Find a field by name with no case-sensitivity (upper case) in the class
	 *
	 * @param targetClass The class to find the field in
	 * @param fieldName   The name of the field to find
	 *
	 * @return The field if discovered
	 */
	public static Field findField( Class<?> targetClass, String fieldName ) {
		return getFieldsAsStream( targetClass )
		    .filter( target -> target.getName().equalsIgnoreCase( fieldName ) )
		    .findFirst()
		    .orElseThrow( () -> new NoFieldException(
		        String.format( "No such field [%s] found in the class [%s].", fieldName, targetClass.getName() )
		    ) );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name
	 *
	 * @param targetClass The class to check
	 * @param fieldName   The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public static Boolean hasField( Class<?> targetClass, String fieldName ) {
		return getFieldNames( targetClass ).contains( fieldName );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name and no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to check
	 * @param fieldName   The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public static Boolean hasFieldNoCase( Class<?> targetClass, String fieldName ) {
		return getFieldNamesNoCase( targetClass ).contains( fieldName.toUpperCase() );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name and no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to check
	 * @param fieldName   The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public static Boolean hasPublicFieldNoCase( Class<?> targetClass, String fieldName ) {
		return getPublicFieldNamesNoCase( targetClass ).contains( fieldName.toUpperCase() );
	}

	/**
	 * Get an array of fields of all the public fields for the given class
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return The fields in the class
	 */
	public static Field[] getFields( Class<?> targetClass ) {
		Set<Field> allFields = new HashSet<>();
		allFields.addAll( new HashSet<>( List.of( targetClass.getFields() ) ) );
		allFields.addAll( new HashSet<>( List.of( targetClass.getDeclaredFields() ) ) );
		return allFields.toArray( new Field[ 0 ] );
	}

	/**
	 * Get an array of fields of all the public fields for the given class
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return The fields in the class
	 */
	public static Field[] getPublicFields( Class<?> targetClass ) {
		return targetClass.getFields();
	}

	/**
	 * Get a stream of fields of all the public fields for the given class
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return The stream of fields in the class
	 */
	public static Stream<Field> getFieldsAsStream( Class<?> targetClass ) {
		return Stream.of( getFields( targetClass ) );
	}

	/**
	 * Get a stream of fields of all the public fields for the given class
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return The stream of fields in the class
	 */
	public static Stream<Field> getPublicFieldsAsStream( Class<?> targetClass ) {
		return Stream.of( getPublicFields( targetClass ) );
	}

	/**
	 * Get a list of field names for the given class with case-sensitivity
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return A list of field names
	 */
	public static List<String> getFieldNames( Class<?> targetClass ) {
		return getFieldsAsStream( targetClass )
		    .map( Field::getName )
		    .toList();

	}

	/**
	 * Get a list of field names for the given class with no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return A list of field names
	 */
	public static List<String> getFieldNamesNoCase( Class<?> targetClass ) {
		return getFieldsAsStream( targetClass )
		    .map( Field::getName )
		    .map( String::toUpperCase )
		    .toList();
	}

	/**
	 * Get a list of field names for the given class with no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to get the fields for
	 *
	 * @return A list of field names
	 */
	public static List<String> getPublicFieldNamesNoCase( Class<?> targetClass ) {
		return getPublicFieldsAsStream( targetClass )
		    .map( Field::getName )
		    .map( String::toUpperCase )
		    .toList();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Class Discovery Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Find a class by name with no case-sensitivity (upper case) in the class
	 *
	 * @param targetClass The class to find the class in
	 * @param className   The name of the class to find
	 *
	 * @return The class if discovered
	 */
	public static Class<?> findClass( Class<?> targetClass, String className ) {
		return getClassesAsStream( targetClass )
		    .filter( target -> target.getSimpleName().equalsIgnoreCase( className ) )
		    .findFirst()
		    .orElseThrow( () -> new NoFieldException(
		        String.format( "No such inner class [%s] found in the class [%s].", className, targetClass.getName() )
		    ) );
	}

	/**
	 * Verifies if the class has a public or public static Class with the given name
	 *
	 * @param targetClass The class to check
	 * @param className   The name of the Class to check
	 *
	 * @return True if the Class exists, false otherwise
	 */
	public static Boolean hasClass( Class<?> targetClass, String className ) {
		return getClassNames( targetClass ).contains( className );
	}

	/**
	 * Verifies if the class has a public or public static Class with the given name and no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to check
	 * @param className   The name of the Class to check
	 *
	 * @return True if the Class exists, false otherwise
	 */
	public static Boolean hasClassNoCase( Class<?> targetClass, String className ) {
		return getClassNamesNoCase( targetClass ).contains( className.toUpperCase() );
	}

	/**
	 * Get an array of classes for the given class
	 *
	 * @param targetClass The class to get the classes for
	 *
	 * @return The classes in the class
	 */
	public static Class<?>[] getClasses( Class<?> targetClass ) {
		return targetClass.getClasses();
	}

	/**
	 * Get a stream of Classes for the given class
	 *
	 * @param targetClass The class to get the Classes for
	 *
	 * @return The stream of Classes in the class
	 */
	public static Stream<Class<?>> getClassesAsStream( Class<?> targetClass ) {
		return Stream.of( getClasses( targetClass ) );
	}

	/**
	 * Get a list of Class names for the given class with case-sensitivity
	 *
	 * @param targetClass The class to get the Classes for
	 *
	 * @return A list of Class names
	 */
	public static List<String> getClassNames( Class<?> targetClass ) {
		return getClassesAsStream( targetClass )
		    .map( Class::getSimpleName )
		    .toList();

	}

	/**
	 * Get a list of Class names for the given class with no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to get the Classes for
	 *
	 * @return A list of Class names
	 */
	public static List<String> getClassNamesNoCase( Class<?> targetClass ) {
		return getClassesAsStream( targetClass )
		    .map( Class::getSimpleName )
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
	 * @param context            The context to use for the method invocation
	 * @param dynamicObject      The dynamic object to get the method handle for
	 * @param targetClass        The class to get the method handle for
	 * @param methodName         The name of the method to get the handle for
	 * @param argumentsAsClasses The array of arguments as classes to map
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The method handle representing the method signature
	 *
	 */
	public static MethodRecord getMethodHandle(
	    IBoxContext context,
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    Object targetInstance,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    Object[] castedArgumentValues,
	    Object... arguments ) {

		// We use the method signature as the cache key
		String			cacheKey		= targetClass.hashCode() + methodName + Arrays.hashCode( argumentsAsClasses );
		MethodRecord	methodRecord	= methodHandleCache.get( cacheKey );
		boolean			fromCache		= true;

		// Double lock to avoid race-conditions
		if ( methodRecord == null || !handlesCacheEnabled ) {
			synchronized ( cacheKey.intern() ) {
				if ( methodRecord == null || !handlesCacheEnabled ) {
					BooleanRef isCachable = new BooleanRef( true );
					// The process of discovering the method handle will cast/coerce the arguments, so there's no need to do it again in this code path
					methodRecord	= discoverMethodHandle(
					    context,
					    dynamicObject,
					    targetClass,
					    targetInstance,
					    methodName,
					    argumentsAsClasses,
					    isCachable,
					    castedArgumentValues,
					    arguments );
					fromCache		= false;
					if ( isCachable.get() ) {
						methodHandleCache.put( cacheKey, methodRecord );
					}
				}
			}
		}

		if ( fromCache ) {
			// coerce argument types here since we're using a cached method handle
			coerceArguments(
			    context,
			    unBoxTypes( methodRecord.method().getParameterTypes() ),
			    unBoxTypes( argumentsAsClasses ),
			    castedArgumentValues,
			    arguments,
			    methodRecord.method().isVarArgs(),
			    BooleanRef.of( true ),
			    new AtomicInteger( 0 )
			);
		}
		return methodRecord;
	}

	/**
	 * Discovers the method to invoke for the given method name and arguments according to two algorithms:
	 *
	 * 1. Exact Match : Matches the incoming argument class types to the method signature
	 * 2. Discovery : Matches the incoming argument class types to the method signature by discovery of matching method names and argument counts
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to discover the method for
	 * @param targetInstance     The instance to discover the method for (Can be null for static methods or interfaces)
	 * @param methodName         The name of the method to discover
	 * @param argumentsAsClasses The array of arguments as classes to map
	 * @param arguments          The arguments to pass to the method
	 * @param isCachable         A reference to a boolean to set if the method handle is cachable
	 *
	 * @throws NoMethodException If the method cannot be found by any means
	 *
	 * @return The method record representing the method signature and metadata
	 *
	 */
	public static MethodRecord discoverMethodHandle(
	    IBoxContext context,
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    Object targetInstance,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {

		// Our target we must find using our dynamic rules:
		// - case insensitivity
		// - argument count
		// - argument class asignability
		// - argument coercion
		Method			targetMethod	= findMatchingMethod(
		    context,
		    targetClass,
		    methodName,
		    argumentsAsClasses,
		    isCachable,
		    castedArgumentValues,
		    arguments
		);
		MethodHandle	targetHandle;

		// If we are calling an instance method, but have no instance, and have the dynamicobject reference, then initialize it if there is a no-arg constructor
		if ( targetMethod != null && targetInstance == null && dynamicObject != null && !Modifier.isStatic( targetMethod.getModifiers() ) ) {
			// Check if the dynamic object has a no-arg constructor
			try {
				targetInstance = invokeConstructor( context, targetClass );
				dynamicObject.setTargetInstance( targetInstance );
			} catch ( NoConstructorException e ) {
				// If there is no no-arg constructor, then we can't call the method
				throw new BoxRuntimeException( "No instance provided for non-static method " + methodName + " for class " + targetClass.getName()
				    + " and there is not an no-arg constructor to call." );
			}
		}

		// Verify we can access the method, if we can't then we need to go up the inheritance chain to find it or die
		// This happens when objects implement default method interfaces usually
		try {
			targetMethod = ( Method ) checkAccess(
			    context,
			    targetClass,
			    targetInstance,
			    targetMethod,
			    methodName,
			    argumentsAsClasses,
			    isCachable,
			    castedArgumentValues,
			    arguments
			);
		} catch ( NoMethodException e ) {
			throw new BoxRuntimeException( "Error checking method access" + methodName + " for class " + targetClass.getName(), e );
		}

		// Verify if the targetMethod is null, if it is then we need to die because there is no method anywhere we can match it to
		if ( targetMethod == null ) {
			throw new NoMethodException(
			    String.format(
			        "No such method [%s] found in the class [%s] using [%d] arguments of types [%s]",
			        methodName,
			        targetClass.getName(),
			        argumentsAsClasses.length,
			        Arrays.toString( argumentsAsClasses )
			    )
			);
		}

		try {
			// There is a special workaround required for BoxClasses extending a Java class who want to call a super method.
			// In this case, the targetIntance will be the boxClass, but the targetClass will be the super class.
			// We must delegate to a special lookupPrivate() methos which MUST LIVE PHYSICALLY INSIDE the actual sub class
			// otherwise the JVM will deny access to getting the method handle.
			if ( targetInstance != null && !targetMethod.getDeclaringClass().equals( targetInstance.getClass() )
			    && targetInstance instanceof IClassRunnable boxClass ) {
				targetHandle = boxClass.lookupPrivateMethod( targetMethod );
			} else {
				// For methods declared in the target class, use unreflect
				targetHandle = METHOD_LOOKUP.unreflect( targetMethod );
			}

			return new MethodRecord(
			    methodName,
			    targetMethod,
			    targetHandle,
			    Modifier.isStatic( targetMethod.getModifiers() ),
			    argumentsAsClasses.length
			);
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException( "Error building MethodRecord for " + methodName + " for class " + targetClass.getName(), e );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Constructor Introspection Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a HashSet of constructors of the given class
	 *
	 * @param targetClass The class to get the constructors for
	 * @param callable    Whether to get only callable constructors (anything but private). If null or false all constructors are returned
	 *
	 * @return A unique set of callable constructors
	 */
	public static Set<Executable> getConstructors( Class<?> targetClass, Boolean callable ) {
		Set<Executable> allConstructors = new HashSet<>();

		// Collect all the constructors from the class and it's super classes
		allConstructors.addAll( new HashSet<>( List.of( targetClass.getConstructors() ) ) );
		allConstructors.addAll( new HashSet<>( List.of( targetClass.getDeclaredConstructors() ) ) );

		// If callable, filter out the private constructors
		if ( Boolean.TRUE.equals( callable ) ) {
			allConstructors = allConstructors.stream()
			    .filter( method -> Modifier.isPublic( method.getModifiers() ) )
			    .collect( Collectors.toSet() );
		}

		return allConstructors;
	}

	/**
	 * Get a stream of constructors of the given class
	 *
	 * @param targetClass The class to get the constructors for
	 * @param callable    Whether to get only callable constructors (anything but private). If null or false all constructors are returned
	 *
	 * @return A stream of unique callable constructors
	 */
	public static Stream<Executable> getConstructorsAsStream( Class<?> targetClass, Boolean callable ) {
		return getConstructors( targetClass, callable ).stream();
	}

	/**
	 * Find a constructor by the given arguments as classes and return it if it exists
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to find the constructor for
	 * @param argumentsAsClasses The parameter types of the constructor to find
	 * @param arguments          The arguments to pass to the constructor
	 *
	 * @return The constructor if it exists
	 */
	public static Constructor<?> findMatchingConstructor(
	    IBoxContext context,
	    Class<?> targetClass,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {
		return ( Constructor<?> ) findMatchingExecutable(
		    context,
		    getConstructorsAsStream( targetClass, true ),
		    targetClass,
		    null,
		    argumentsAsClasses,
		    isCachable,
		    castedArgumentValues,
		    arguments
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Method Introspection Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method checks if the passed in instance has access to the the passed in target method.
	 * If it does, it just returns it, else it tries to find the method that we can access via recursion usin the
	 * following algorithm:
	 * 1. By interfaces first as it could be a default method in all implemented interfaces
	 * 2. By inheritance, try to get the method from the parent class, which repeats this algorithm for the parent class
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to check
	 * @param targetInstance     The instance to check
	 * @param targetMethod       The method to check
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The method if it's accessible, else it tries to find the method in the parent class of the targetClass
	 */
	private static Executable checkAccess(
	    IBoxContext context,
	    Class<?> targetClass,
	    Object targetInstance,
	    Executable targetMethod,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {

		if ( targetMethod != null ) {
			boolean isStatic = Modifier.isStatic( targetMethod.getModifiers() );
			if ( !targetMethod.canAccess( isStatic ? null : targetInstance ) ) {
				// 1. Let's try to find it by interfaces first as it could be a default method
				var methodByInterface = findMatchingMethodByInterfaces(
				    context,
				    targetClass,
				    methodName,
				    argumentsAsClasses,
				    isCachable,
				    castedArgumentValues,
				    arguments );
				if ( methodByInterface != null ) {
					return methodByInterface;
				}

				// 2. Try to get the method from the parent class of the targetClass until we can access or we die
				Class<?> superClass = targetClass.getSuperclass();
				targetMethod = findMatchingExecutable(
				    context,
				    getMethodsAsStream( targetClass, true ),
				    superClass,
				    methodName,
				    argumentsAsClasses,
				    isCachable,
				    castedArgumentValues,
				    arguments
				);
				return checkAccess(
				    context,
				    superClass,
				    targetInstance,
				    targetMethod,
				    methodName,
				    argumentsAsClasses,
				    isCachable,
				    castedArgumentValues,
				    arguments );
			}
		}
		return targetMethod;
	}

	/**
	 * Get a HashSet of methods of all the unique callable method signatures for the given class
	 *
	 * @param targetClass The class to get the methods for
	 * @param callable    Whether to get only callable methods (anything but private). If null or false all methods are returned
	 *
	 * @return A unique set of callable methods
	 */
	public static Set<Executable> getMethods( Class<?> targetClass, boolean callable ) {
		Set<Method> allMethods = new HashSet<>();

		// Collect all the methods from the class and it's super classes
		Collections.addAll( allMethods, targetClass.getMethods() );
		Collections.addAll( allMethods, targetClass.getDeclaredMethods() );

		var methodStream = allMethods.stream();
		// If callable, filter out the private methods
		if ( callable ) {
			methodStream = methodStream.filter( method -> Modifier.isPublic( method.getModifiers() ) );
		}

		return methodStream.collect( Collectors.toSet() );
	}

	/**
	 * Get a stream of methods of all the unique callable method signatures for the given class
	 *
	 * @param targetClass The class to get the methods for
	 * @param callable    Whether to get only callable methods (anything but private). If null or false all methods are returned
	 *
	 * @return A stream of unique callable methods
	 */
	public static Stream<Executable> getMethodsAsStream( Class<?> targetClass, Boolean callable ) {
		return getMethods( targetClass, callable ).stream();
	}

	/**
	 * Get a method by name for the given class
	 *
	 * @param targetClass The class to get the methods for
	 * @param name        The name of the method to get
	 * @param callable    Whether to get only a callable methods (anything but private). If null or false, then any visibility is returned
	 *
	 * @return The Method object
	 */
	public static Executable getMethod( Class<?> targetClass, String name, Boolean callable ) {
		return getMethodsAsStream( targetClass, callable )
		    .parallel()
		    .filter( method -> method.getName().equalsIgnoreCase( name ) )
		    .findFirst()
		    .orElseThrow( () -> new NoMethodException(
		        String.format( "No such method [%s] found in the class [%s].", name, targetClass.getName() )
		    ) );
	}

	/**
	 * Get a list of method names for the given class
	 *
	 * @param targetClass The class to get the methods for
	 * @param callable    Whether to get only a callable methods (anything but private). If null or false, then any visibility is returned
	 *
	 * @return A list of method names
	 */
	public static List<String> getMethodNames( Class<?> targetClass, Boolean callable ) {
		return getMethodsAsStream( targetClass, callable )
		    .parallel()
		    .map( Executable::getName )
		    .toList();
	}

	/**
	 * Get a list of method names for the given class with no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to get the methods for
	 * @param callable    Whether to get only a callable methods (anything but private). If null or false, then any visibility is returned
	 *
	 * @return A list of method names with no case
	 */
	public static List<String> getMethodNamesNoCase( Class<?> targetClass, Boolean callable ) {
		return getMethodsAsStream( targetClass, callable )
		    .parallel()
		    .map( Executable::getName )
		    .map( String::toUpperCase )
		    .toList();
	}

	/**
	 * Verifies if the class has a public or public static method with the given name
	 *
	 * @param targetClass The class to check
	 * @param methodName  The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public static Boolean hasMethod( Class<?> targetClass, String methodName ) {
		return getMethodNames( targetClass, true ).contains( methodName );
	}

	/**
	 * Verifies if the class has a public or public static method with the given name and no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to check
	 * @param methodName  The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public static Boolean hasMethodNoCase( Class<?> targetClass, String methodName ) {
		return getMethodNamesNoCase( targetClass, true ).contains( methodName.toUpperCase() );
	}

	/**
	 * This method is used to verify if the class has the same method signature as the incoming one with no case-sensitivity (upper case)
	 * We follow the following rules:
	 * - The method name must match
	 * - The number of arguments must match
	 * - The argument types must match (1st try, if not)
	 * - The argument types must be assignable (2nd try)
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to check
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @throws NoMethodException If the method is not found and safe is false
	 *
	 * @return The matched method signature if it exists or null if it doesn't
	 */
	public static Method findMatchingMethod(
	    IBoxContext context,
	    Class<?> targetClass,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {
		return ( Method ) findMatchingExecutable(
		    context,
		    getMethodsAsStream( targetClass, true ),
		    targetClass,
		    methodName,
		    argumentsAsClasses,
		    isCachable,
		    castedArgumentValues,
		    arguments );
	}

	/**
	 * This method is used to verify if the class has the same method signature as the incoming one with no case-sensitivity (upper case)
	 * We follow the following rules:
	 * - The method name must match
	 * - The number of arguments must match
	 * - The argument types must match (1st try, if not)
	 * - The argument types must be assignable (2nd try)
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to check
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @throws NoMethodException If the method is not found and safe is false
	 *
	 * @return The matched method signature if it exists or null if it doesn't
	 */
	public static Executable findMatchingExecutable(
	    IBoxContext context,
	    Stream<Executable> availableExecutables,
	    Class<?> targetClass,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {

		// Try to get the executables that match by name and number of arguments first.
		List<Executable> targetExecutables = availableExecutables
		    .filter( executable -> methodName == null || executable.getName().equalsIgnoreCase( methodName ) )
		    // We either need to have the same number of arguments or a varargs method with one less argument
		    // This allows the array of varargs to be omitted when empty
		    .filter( executable -> executable.getParameterCount() == argumentsAsClasses.length
		        || ( executable.isVarArgs() && executable.getParameterCount() == argumentsAsClasses.length + 1 ) )
		    .toList();

		// If list is empty return false
		if ( targetExecutables.isEmpty() ) {
			return null;
		}

		// 1. Exact Match : Matches the incoming argument class types to the executable signature
		return targetExecutables
		    .stream()
		    // has to have the same arg types
		    .filter(
		        executable -> hasMatchingParameterTypes(
		            context,
		            executable,
		            MATCH_TYPE.EXACT,
		            argumentsAsClasses,
		            isCachable,
		            null,
		            castedArgumentValues,
		            arguments ) )
		    // .peek( executable -> System.out.println( "Found EXACT method [" + executable + "]" ) )
		    // Sort first matches who's argument array is the same as the number of parameters in the executable, which will make ambiguous varargs methods sort last
		    .sorted( Comparator.comparingInt( executable -> executable.getParameterCount() == argumentsAsClasses.length ? 0 : 1 ) )
		    .findFirst()
		    // 2: If no exact match, try loose coercion
		    .or( () -> targetExecutables
		        .stream()
		        .map(
		            executable -> {
			            var matchScore = new AtomicInteger( 0 );
			            if ( hasMatchingParameterTypes(
			                context,
			                executable,
			                MATCH_TYPE.ASSIGNABLE,
			                argumentsAsClasses,
			                isCachable,
			                matchScore,
			                castedArgumentValues,
			                arguments
			            ) ) {
				            return CoerceAttempt.of( executable, matchScore, castedArgumentValues );
			            } else {
				            return null;
			            }
		            } )
		        // remove our non-matches
		        .filter( executable -> executable != null )
		        // .peek( executable -> System.out.println( "Found ASSIGNABLE method [" + executable + "]" ) )
		        // sort based on best match
		        .sorted( Comparator.comparingInt( coerceAttempt -> coerceAttempt.matchScore().get() ) )
		        // Sort first matches who's argument array is the same as the number of parameters in the executable, which will make ambiguous varargs methods sort last
		        // .sorted( Comparator.comparingInt( executable -> executable.getParameterCount() == argumentsAsClasses.length ? 0 : 1 ) )
		        .findFirst()
		        // map our stream back to just the executable
		        .map( coerceAttempt -> {
			        return coerceAttempt.executable();
		        } )
		        .or( () -> targetExecutables
		            .stream()
		            // Test the executables. Return null for a non-match. For matching executables, return the executable and the match score
		            .map( executable -> {
			            var matchScore				= new AtomicInteger( 0 );
			            Object[] attemptCastedArgumentValues = new Object[ arguments.length ];
			            if ( hasMatchingParameterTypes(
			                context,
			                executable,
			                MATCH_TYPE.COERCE,
			                argumentsAsClasses,
			                isCachable,
			                matchScore,
			                attemptCastedArgumentValues,
			                arguments
			            ) ) {
				            return CoerceAttempt.of( executable, matchScore, attemptCastedArgumentValues );
			            } else {
				            return null;
			            }

		            } )
		            // remove our non-matches
		            .filter( executable -> executable != null )
		            // .peek( executable -> System.out.println( "Found COERCE method [" + executable + "]" ) )
		            // sort based on best match
		            .sorted( Comparator.comparingInt( coerceAttempt -> coerceAttempt.matchScore().get() ) )
		            // get the first one
		            .findFirst()
		            // map our stream back to just the executable
		            .map( coerceAttempt -> {
			            // copy the argument array in the attempt over the castedArgumentValues array
			            System.arraycopy( coerceAttempt.castedArgumentValues(), 0, castedArgumentValues, 0, coerceAttempt.castedArgumentValues().length );
			            return coerceAttempt.executable();
		            } )
		        )
		    )
		    .orElse( null );
	}

	/**
	 * Try to find a matching method by interfaces
	 *
	 * @param context            The context to use for the method invocation
	 * @param targetClass        The class to check it's interfaces
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The matched method signature or null if not found
	 */
	public static Executable findMatchingMethodByInterfaces(
	    IBoxContext context,
	    Class<?> targetClass,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    Object[] castedArgumentValues,
	    Object... arguments ) {
		// Since we haven't found the method in the class, we need to try to find it in the interfaces
		// since interfaces have default methods and we can't access them directly
		Class<?>[] interfaces = targetClass.getInterfaces();
		return Arrays.stream( interfaces )
		    .map( interfaceClass -> findMatchingMethod(
		        context,
		        interfaceClass,
		        methodName,
		        argumentsAsClasses,
		        isCachable,
		        castedArgumentValues,
		        arguments
		    ) )
		    .filter( Objects::nonNull )
		    .findFirst()
		    .orElse( null );
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
	 * --------------------------------------------------------------------------
	 * Argument Helpers
	 * --------------------------------------------------------------------------
	 */

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
			return null;
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
		// Convert the arguments to an array of classes without using streams
		Class<?>[] classes = new Class<?>[ args.length ];
		for ( int i = 0; i < args.length; i++ ) {
			classes[ i ] = argumentToClass( args[ i ] );
		}
		return classes;
	}

	/**
	 * Unwrap an object if it's inside a DynamicObject instance
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
	 * Unwrap any DynamicObject instances in the arguments
	 *
	 * @param arguments The arguments to unwrap
	 *
	 * @return The unwrapped arguments
	 */
	private static void unWrapArguments( Object[] arguments ) {
		for ( int j = 0; j < arguments.length; j++ ) {
			arguments[ j ] = unWrap( arguments[ j ] );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Implementation of IReferencable
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param context     The context to use for the dereference
	 * @param targetClass The class to dereference and look for the value on
	 * @param name        The name of the key to dereference
	 * @param safe        If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested object
	 */
	public static Object dereference( IBoxContext context, Class<?> targetClass, Key name, Boolean safe ) {
		return dereference( context, targetClass, null, name, safe );
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param context        The context to use for the dereference
	 * @param targetInstance The instance to dereference and look for the value on
	 * @param name           The name of the key to dereference
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested object
	 */
	public static Object dereference( IBoxContext context, Object targetInstance, Key name, Boolean safe ) {
		return dereference( context, targetInstance.getClass(), targetInstance, name, safe );
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param context        The context to use for the dereference
	 * @param targetClass    The class to dereference and look for the value on
	 * @param targetInstance The instance to dereference and look for the value on
	 * @param name           The name of the key to dereference
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested object
	 */
	@SuppressWarnings( "unchecked" )
	public static Object dereference( IBoxContext context, Class<?> targetClass, Object targetInstance, Key name, Boolean safe ) {
		// If the object is referencable, allow it to handle the dereference
		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.dereference( context, name, safe );
		}

		// This check allows us to lazy-create meta for BoxLang types the first time it is requested
		if ( name.equals( BoxMeta.key ) ) {
			if ( targetInstance != null && targetInstance instanceof IType type ) {
				return type.getBoxMeta();
			}
			// We pass either the class or instance
			return new GenericMeta( targetInstance != null ? targetInstance : targetClass );
		}

		// Double check because a java super dereference from a boxClass will have a different targetClass.
		if ( Map.class.isAssignableFrom( targetClass ) && targetInstance instanceof Map map ) {
			// If it's a raw Map, we may need to get creative since it's possible to have a mix of Key and String instances in some cases,
			// but the map itself may also have a completelyk different key class
			var structMapWrap = StructMapWrapper.of( map );
			if ( structMapWrap.containsKey( name ) ) {
				return structMapWrap.get( name );
			}

			throw new KeyNotFoundException(
			    String.format( "The Map [%s] has no key [%s]. The existing keys are [%s]",
			        ClassUtils.getCanonicalName( targetClass ),
			        name.getName(),
			        map.keySet().stream().map( Object::toString ).collect( Collectors.joining( ", " ) )
			    )
			);

			// Special logic so we can treat exceptions as referencable. Possibly move to helper
		} else if ( targetInstance instanceof List list ) {
			Integer index = Array.validateAndGetIntForDereference( name, list.size(), safe );
			// non-existant indexes or keys which could not be turned into an int return null when dereferencing safely
			if ( safe && ( index == null || Math.abs( index ) > list.size() || index == 0 ) ) {
				return null;
			}
			if ( index < 0 ) {
				return list.get( list.size() + index );
			}
			return list.get( index - 1 );
		} else if ( targetInstance != null && targetInstance.getClass().isArray() ) {
			int arrLen = java.lang.reflect.Array.getLength( targetInstance );
			if ( name.equals( lengthKey ) ) {
				return arrLen;
			}

			Integer index = Array.validateAndGetIntForDereference( name, arrLen, safe );
			// non-existant indexes or keys which could not be turned into an int return null when dereferencing safely
			if ( safe && ( index == null || Math.abs( index ) > arrLen || index == 0 ) ) {
				return null;
			}
			if ( index < 0 ) {
				return java.lang.reflect.Array.get( targetInstance, arrLen + index );
			}
			return java.lang.reflect.Array.get( targetInstance, index - 1 );
		} else if ( targetInstance instanceof Throwable t && exceptionKeys.contains( name ) ) {
			// Throwable.message always delegates through to the message field
			if ( name.equals( Key.message ) ) {
				return t.getMessage();
			} else if ( name.equals( Key.cause ) ) {
				return t.getCause();
			} else if ( name.equals( Key.stackTrace ) ) {
				StringWriter	sw	= new StringWriter();
				PrintWriter		pw	= new PrintWriter( sw );
				t.printStackTrace( pw );
				return sw.toString();
			} else if ( name.equals( Key.tagContext ) ) {
				return ExceptionUtil.buildTagContext( t );
			} else if ( targetInstance instanceof BoxLangException ble ) {
				if ( name.equals( Key.detail ) ) {
					return ble.getDetail();
				} else if ( name.equals( Key.type ) ) {
					return ble.getType();
				} else if ( targetInstance instanceof CustomException ce && name.equals( Key.errorcode ) ) {
					return ce.getErrorCode();
				} else if ( ble instanceof BoxRuntimeException bre && name.equals( Key.extendedinfo ) ) {
					return bre.getExtendedInfo();
				} else {
					return "";
				}
			} else {
				// CFML returns "" for Throwable.detail, etc
				return "";
			}
			// Special logic for accessing strings as array. Possibly move to helper
		} else if ( targetInstance instanceof String s && name instanceof IntKey intKey ) {
			Integer index = Array.validateAndGetIntForDereference( intKey, s.length(), safe );

			// non-existant indexes or keys which could not be turned into an int return null when dereferencing safely
			if ( safe && ( index == null || Math.abs( index ) > s.length() || index == 0 ) ) {
				return null;
			}
			if ( index < 0 ) {
				return s.substring( s.length() + index, s.length() + index + 1 );
			}
			return s.substring( index - 1, index );
			// Special logic for native arrays. Possibly move to helper
		} else if ( hasAccessibleField( targetInstance, targetClass, name.getName() ) ) {
			// If we have the field, return its value, even if it's null
			return getField( targetClass, targetInstance, name.getName() ).orElse( null );
		} else if ( hasClassNoCase( targetClass, name.getName() ) ) {
			return DynamicObject.of( findClass( targetClass, name.getName() ) );
		} else if ( targetClass.isEnum() ) {
			return Enum.valueOf( ( Class<Enum> ) targetClass, name.getName() );
		} else if ( getMethodNamesNoCase( targetClass, true ).contains( name.getName().toUpperCase() ) ) {
			// If this class has a method with the same name, return it as a JavaMethod
			return new JavaMethod(
			    name,
			    new DynamicObject( targetClass ).setTargetInstance( targetInstance )
			);
		}

		// For Java objects, we also allow accessing the getName() method as obj.name, etc
		CastAttempt<IStruct> structAttempt = StructCasterLoose.attempt( targetInstance != null ? targetInstance : targetClass );
		if ( structAttempt.wasSuccessful() ) {
			IStruct struct = structAttempt.get();
			if ( struct.containsKey( name ) ) {
				return struct.get( name );
			}
		}

		if ( safe ) {
			return null;
		}

		// Field not found anywhere
		if ( targetInstance != null ) {
			throw new KeyNotFoundException(
			    String.format( "The instance [%s] has no public field or inner class [%s]. The allowed fields are [%s]",
			        ClassUtils.getCanonicalName( targetClass ),
			        name.getName(),
			        getFieldNames( targetClass )
			    )
			);
		} else {
			throw new KeyNotFoundException(
			    String.format( "The instance [%s] has no static field or inner class [%s]. The allowed fields are [%s]",
			        ClassUtils.getCanonicalName( targetClass ),
			        name.getName(),
			        getFieldNames( targetClass )
			    )
			);
		}
	}

	private static boolean hasAccessibleField( Object targetInstance, Class<?> targetClass, String fieldName ) {
		// Get all fields on the class
		Optional<Field> possibleMatch = getFieldsAsStream( targetClass ).filter( field -> field.getName().equalsIgnoreCase( fieldName ) ).findFirst();
		// If the field doesn't exist, early exit
		if ( !possibleMatch.isPresent() ) {
			return false;
		}
		Field field = possibleMatch.get();
		// As sort-of dumb work around for a BoxClass's ability to reference super.protectedField when extending java, we'll assume it's accessible if accessing on a BoxClass and the field is from this class
		// This doesn't really test the true calling context, but we'd need to proxy to the actual box class for that to work.
		if ( targetInstance instanceof IClassRunnable && !field.getDeclaringClass().equals( targetInstance.getClass() ) ) {
			return true;
		}
		// For all other cases, we'll just check if the field is public
		return Modifier.isPublic( field.getModifiers() );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetClass         The class to dereference and look for the invocable on
	 * @param context             The IBoxContext in which the function will be executed
	 * @param name                The name of the key to dereference, which becomes the method name
	 * @param positionalArguments The arguments to pass to the invokable
	 * @param safe                If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    IBoxContext context,
	    Key name,
	    Object[] positionalArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( dynamicObject, targetClass, null, context, name, positionalArguments, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetInstance      The instance to dereference and look for the invocable on
	 * @param context             The IBoxContext in which the function will be executed
	 * @param name                The name of the key to dereference, which becomes the method name
	 * @param positionalArguments The arguments to pass to the invokable
	 * @param safe                If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Object targetInstance,
	    IBoxContext context,
	    Key name,
	    Object[] positionalArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( dynamicObject, targetInstance.getClass(), targetInstance, context, name, positionalArguments, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetClass         The class to dereference and look for the invocable on
	 * @param targetInstance      The instance to dereference and look for the invocable on
	 * @param context             The IBoxContext in which the function will be executed
	 * @param name                The name of the key to dereference, which becomes the method name
	 * @param positionalArguments The arguments to pass to the invokable
	 * @param safe                If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    Object targetInstance,
	    IBoxContext context,
	    Key name,
	    Object[] positionalArguments,
	    Boolean safe ) {

		// If the object is referencable, allow it to handle the dereference itself
		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, positionalArguments, safe );
		}

		// If we have an instance, we can try to see if it accepts member methods
		// Unless the method is already defined on the class
		if ( targetInstance != null ) {
			ObjectRef<Object>	ref					= ObjectRef.of( targetInstance );
			MemberDescriptor	memberDescriptor	= getFunctionService().getMemberMethod( context, name, ref );
			if ( memberDescriptor != null ) {
				targetInstance = ref.get();
				return memberDescriptor.invoke( context, targetInstance, positionalArguments );
			}
		}

		// If the method requested is not found, return null if safe is true
		if ( safe && !hasMethod( targetClass, name.getName() ) ) {
			return null;
		}

		// Special case for calling `getClass()` on a NON-instance created via createObject()
		// This is to avoid a null exception since `getClass()` is being called when no instance is there yet.
		if ( name.equals( Key.getClass ) ) {
			return targetClass;
		}

		// Else let's do an invoke dynamic
		return invoke( context, dynamicObject, targetClass, targetInstance, name.getName(), safe, positionalArguments );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetClass    The class to assign the field on
	 * @param context        The IBoxContext in which the function will be executed
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    IBoxContext context,
	    Key name,
	    Map<Key, Object> namedArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( dynamicObject, targetClass, null, context, name, namedArguments, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetInstance The instance to assign the field on
	 * @param context        The IBoxContext in which the function will be executed
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Object targetInstance,
	    IBoxContext context,
	    Key name,
	    Map<Key, Object> namedArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( dynamicObject, targetInstance.getClass(), targetInstance, context, name, namedArguments, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param targetClass    The class to assign the field on
	 * @param targetInstance The instance to assign the field on
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke(
	    DynamicObject dynamicObject,
	    Class<?> targetClass,
	    Object targetInstance,
	    IBoxContext context,
	    Key name,
	    Map<Key, Object> namedArguments,
	    Boolean safe ) {

		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		if ( targetInstance != null ) {
			ObjectRef<Object>	ref					= ObjectRef.of( targetInstance );
			MemberDescriptor	memberDescriptor	= getFunctionService().getMemberMethod( context, name, ref );
			if ( memberDescriptor != null ) {
				targetInstance = ref.get();
				return memberDescriptor.invoke( context, targetInstance, namedArguments );
			}
		}

		// Check if the namedArguments has the `argumentCollection` key.
		// And if it's an Array, delegeate to the positional arguments
		// This use-case can come from invoke( javaObject, "method", [1, 2, 3] )
		if ( namedArguments.containsKey( Key.argumentCollection ) ) {
			Object argumentCollection = namedArguments.get( Key.argumentCollection );
			if ( argumentCollection instanceof Array castedArray ) {
				return dereferenceAndInvoke( dynamicObject, targetClass, targetInstance, context, name, castedArray.toArray(), safe );
			}
		} else if ( namedArguments.isEmpty() ) {
			// If the namedArguments is empty, we can just call the method with no arguments
			return dereferenceAndInvoke( dynamicObject, targetClass, targetInstance, context, name, new Object[ 0 ], safe );
		}

		throw new BoxRuntimeException( "Methods on Java objects cannot be called with named arguments" );
	}

	/**
	 * Assign a value to a field
	 *
	 * @param targetClass The class to assign the field on
	 * @param name        The name of the field to assign
	 * @param value       The value to assign
	 */
	public static Object assign( IBoxContext context, Class<?> targetClass, Key name, Object value ) {
		return assign( context, targetClass, null, name, value );
	}

	/**
	 * Assign a value to a field
	 *
	 * @param targetInstance The instance to assign the field on
	 * @param name           The name of the field to assign
	 * @param value          The value to assign
	 */
	public static Object assign( IBoxContext context, Object targetInstance, Key name, Object value ) {
		return assign( context, targetInstance.getClass(), targetInstance, name, value );
	}

	/**
	 * Assign a value to a field
	 *
	 * @param targetClass    The class to assign the field on
	 * @param targetInstance The instance to assign the field on
	 * @param name           The name of the field to assign
	 * @param value          The value to assign
	 */
	@SuppressWarnings( "unchecked" )
	public static Object assign( IBoxContext context, Class<?> targetClass, Object targetInstance, Key name, Object value ) {

		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.assign( context, name, value );
		} else if ( targetInstance != null && targetInstance.getClass().isArray() ) {
			int			arrLen		= java.lang.reflect.Array.getLength( targetInstance );
			Integer		index		= Array.validateAndGetIntForAssign( name, arrLen, true );
			Class<?>	arrayType	= targetInstance.getClass().getComponentType();

			// Attempt to cast the value to the array type. This allows a string "C" to be set into a char[] array, or a int 42 to be set into a long[] array
			if ( value != null && !value.getClass().isAssignableFrom( arrayType ) ) {
				value = GenericCaster.cast( context, value, arrayType.getName() );
			}

			java.lang.reflect.Array.set( targetInstance, index - 1, value );
			return value;
		} else if ( targetInstance instanceof List list ) {
			Integer index = Array.validateAndGetIntForAssign( name, list.size(), false );
			if ( index > list.size() ) {
				// If the index is larger than the array, pad the array with nulls
				for ( int i = list.size(); i < index; i++ ) {
					list.add( null );
				}
			}
			list.set( index - 1, value );
			return value;
		} else if ( targetInstance instanceof Map ) {
			// If it's a raw Map, then we use the original value as the key
			( ( Map<Object, Object> ) targetInstance ).put( name.getOriginalValue(), value );
			return value;
		}

		try {
			setField( targetClass, targetInstance, name.getName(), value );
		} catch ( Throwable e ) {
			// CFML ignores Throwable.foo = "bar"
			if ( targetInstance instanceof Throwable ) {
				return value;
			}
			throw e;
		}
		return value;
	}

	/**
	 * Verifies if the target calss is an interface or not
	 *
	 * @param targetClass The class to check
	 *
	 * @return True if the class is an interface, false otherwise
	 */
	public static boolean isInterface( Class<?> targetClass ) {
		return targetClass.isInterface() || BoxInterface.class.isAssignableFrom( targetClass );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Verifies if the method has the same parameter types as the incoming ones using our matching algorithms.
	 * - Exact Match : Matches the incoming argument class types to the method signature
	 * - Discovery : Matches the incoming argument class types to the method signature by discovery of matching method names and argument counts
	 * - Coercive : Matches the incoming argument class types to the method signature by coercion of the argument types
	 *
	 * @see https://commons.apache.org/proper/commons-lang/javadocs/api-release/index.html
	 *
	 * @param context            The context to use for the method invocation
	 * @param method             The method to check
	 * @param argumentsAsClasses The arguments to check
	 * @param arguments          The arguments to pass to the method
	 * @param exact              Exact matching or loose matching with coercion
	 * @param isCachable         If the method lookup is cachable
	 * @param matchScore         The score of the match (only needed for coerce type). The int will be incremented by the level of coercion needed to match. A lower score is a better match.
	 *
	 * @return True if the method has the same parameter types, false otherwise
	 */
	private static boolean hasMatchingParameterTypes(
	    IBoxContext context,
	    Executable method,
	    MATCH_TYPE matchType,
	    Class<?>[] argumentsAsClasses,
	    BooleanRef isCachable,
	    AtomicInteger matchScore,
	    Object[] castedArgumentValues,
	    Object... arguments ) {

		// Get param types to test
		Class<?>[] methodParams = method.getParameterTypes();
		// If this is varargs, then we'll have one extra param which we need to ignore for now
		if ( methodParams.length > argumentsAsClasses.length ) {
			methodParams = Arrays.copyOf( methodParams, methodParams.length - 1 );
		}

		// unbox types here so we can do a proper comparison
		unBoxTypes( methodParams );
		unBoxTypes( argumentsAsClasses );

		switch ( matchType ) {
			case EXACT :
				if ( !Arrays.equals( methodParams, argumentsAsClasses ) ) {
					return false;
				}
				// copy arguments into castedArgumentValues
				System.arraycopy( arguments, 0, castedArgumentValues, 0, arguments.length );
				return true;
			case ASSIGNABLE :
				// Penalize method calls where we've omitted the varargs
				if ( method.getParameterCount() != argumentsAsClasses.length ) {
					matchScore.addAndGet( 10 );
				}

				// If we cannot assign the arguments to the method parameters, return false
				if ( !ClassUtils.isAssignable( argumentsAsClasses, methodParams ) ) {
					return false;
				}

				// Iterate over the method parameters and arguments to see if they match
				// If they do not match, we will penalize the match score
				for ( int i = 0; i < methodParams.length; i++ ) {
					// Add 1 to our match score for each argument that is not an exact match
					if ( !methodParams[ i ].equals( argumentsAsClasses[ i ] ) ) {
						matchScore.addAndGet( 1 );
					}
					// If any of the methodParams is `Object` then just exit out, it's too broad, we will try dynamic coercion
					if ( methodParams[ i ] == Object.class ) {
						return false;
					}

				}
				// copy arguments into castedArgumentValues
				System.arraycopy( arguments, 0, castedArgumentValues, 0, arguments.length );
				return true;
			case COERCE :
				// Penalize method calls where we've omitted the varargs
				if ( method.isVarArgs() && method.getParameterCount() > argumentsAsClasses.length ) {
					matchScore.addAndGet( 5 );
				}
				return coerceArguments(
				    context,
				    methodParams,
				    argumentsAsClasses,
				    castedArgumentValues,
				    arguments,
				    method.isVarArgs(),
				    isCachable,
				    matchScore
				);
		}

		return false;
	}

	/**
	 * Turn int into Integer, boolean into Boolean, etc
	 *
	 * @param types The types to unbox, modified by reference
	 */
	public static Class<?>[] unBoxTypes( Class<?>[] types ) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[ i ] != null ) {
				types[ i ] = WRAPPERS_MAP.getOrDefault( types[ i ], types[ i ] );
			}
		}
		return types;
	}

	/**
	 * This is used to coerce arguments to the appropriate types for the method execution if needed
	 *
	 * @param context            The context to use for the method invocation
	 * @param methodParams       The method parameter types
	 * @param argumentsAsClasses The arguments to check
	 * @param arguments          The arguments to pass to the method
	 * @param isVarArgs          If the method is a varargs method
	 *
	 * @return True if the arguments were coerced, false otherwise
	 */
	public static Boolean coerceArguments(
	    IBoxContext context,
	    Class<?>[] methodParams,
	    Class<?>[] argumentsAsClasses,
	    Object[] castedArgumentValues,
	    Object[] arguments,
	    Boolean isVarArgs,
	    BooleanRef isCachable,
	    AtomicInteger matchScore ) {
		var coerced = false;
		for ( int i = 0; i < methodParams.length; i++ ) {
			// bail if i has exceeded the lengh of arguments
			if ( i >= arguments.length ) {
				break;
			}

			// Skip null arguments
			if ( arguments[ i ] == null ) {
				coerced						= true;
				castedArgumentValues[ i ]	= arguments[ i ];
				continue;
			}

			// If the argument has been coerced already we can do quick assignability check
			if ( methodParams[ i ].isAssignableFrom( arguments[ i ].getClass() ) ) {
				coerced						= true;
				castedArgumentValues[ i ]	= arguments[ i ];

				// Penalize `Object` matches since they are too broad
				if ( methodParams[ i ] == Object.class ) {
					// If the method parameter is Object, we will not coerce it, but we
					// will penalize the match score since it's too broad
					// This gives the actual type a chance to be matched
					// with a more specific method later on
					matchScore.addAndGet( 2 );
				}
				// Penalize classes that aren't an exact match
				else if ( !methodParams[ i ].equals( arguments[ i ].getClass() ) ) {
					matchScore.addAndGet( 1 );
				}

				continue;
			}

			// If this method is a var args method and we are at the last argument
			// and the argument is an array, we need to convert it to the varargs type
			if ( isVarArgs && i == methodParams.length - 1 ) {
				CastAttempt<Object> castedArray = GenericCaster.attempt( context, arguments[ i ], getReadableType( methodParams[ i ] ) );
				if ( castedArray.wasSuccessful() ) {
					coerced						= true;
					castedArgumentValues[ i ]	= castedArray.get();
					continue;
				} else {
					coerced = false;
					break;
				}
			}

			// Else we need to coerce the argument
			Optional<?> attempt = coerceAttempt( context, methodParams[ i ], argumentsAsClasses[ i ], arguments[ i ], isCachable, matchScore );
			if ( attempt.isPresent() ) {
				coerced						= true;
				castedArgumentValues[ i ]	= attempt.get();
			} else {
				coerced = false;
				break;
			}
		}
		return coerced;
	}

	/**
	 * Tries to coerce a value to the expected type value
	 *
	 * @param context   The context to use for the method invocation
	 * @param expected  The expected type class
	 * @param actual    The actual type class
	 * @param value     The value to coerce
	 * @param isVarArgs If the method is a varargs method
	 *
	 * @return The coerced value or empty if it can't be coerced
	 */
	private static Optional<?> coerceAttempt(
	    IBoxContext context,
	    Class<?> expected,
	    Class<?> actual,
	    Object value,
	    BooleanRef isCachable,
	    AtomicInteger matchScore ) {
		String	expectedClass	= expected.getSimpleName().toLowerCase();
		String	actualClass		= actual.getSimpleName().toLowerCase();

		// Check if we have a boxed type and we just need an unboxed type. We can't unbox it here since we need to return an Object, but the unboxing will happen automatically when it's used
		if ( PRIMITIVE_MAP.containsKey( actual ) && PRIMITIVE_MAP.get( actual ).equals( expected ) ) {
			// Do not increment matchScore. This is basically a perfect match.
			return Optional.of( value );
		}

		// logger.debug( "Starting Coerce attempt for [" + expected + "] from [" + actual + "] with value [" + value.toString() + "]" );

		// EXPECTED: NUMBER
		// Verify if the expected and actual type is a Number, we can coerce it
		// Use the expected caster to coerce the value to the actual type
		if ( Number.class.isAssignableFrom( expected ) && Number.class.isAssignableFrom( actual ) ) {
			// logger.debug( "Coerce attempt: Both numbers, using generic caster to " + expectedClass );
			CastAttempt<Object> numberAttempt = GenericCaster.attempt( context, value, expectedClass );
			if ( numberAttempt.wasSuccessful() ) {
				// Assigable from one number to another is a very close match, increment 1
				matchScore.addAndGet( 1 );
				return numberAttempt.toOptional();
			}
		}

		// EXPECTED: Key
		// To help with interacting with core BL classes, if the target method requires a Key then cast simple values
		if ( Key.class.isAssignableFrom( expected ) ) {
			// logger.debug( "Coerce attempt: Both numbers, using generic caster to " + expectedClass );
			CastAttempt<Key> keyAttempt = KeyCaster.attempt( value );
			if ( keyAttempt.wasSuccessful() ) {
				// Type casting is a looser match, increment 2
				matchScore.addAndGet( 2 );
				return keyAttempt.toOptional();
			}
		}

		// EXPECTED: BOOLEAN
		// If we expect a boolean, we can coerce it from a boolean, number or string
		if ( Boolean.class.isAssignableFrom( expected )
		    &&
		    ( Boolean.class.isAssignableFrom( actual )
		        ||
		        booleanTargets.contains( actualClass )
		        ||
		        Number.class.isAssignableFrom( actual ) ) ) {

			// logger.debug( "Coerce attempt: Castable to boolean " + actualClass );
			CastAttempt<Boolean> booleanAttempt = BooleanCaster.attempt( value );
			if ( booleanAttempt.wasSuccessful() ) {
				// Type casting is a looser match, increment 2
				matchScore.addAndGet( 2 );
				return booleanAttempt.toOptional();
			}
		}

		// EXPECTED: java.time.LocalTime
		// If we expect a LocalTime and we have a `DateTime` instance
		if ( LocalTime.class.isAssignableFrom( expected ) && actualClass.equals( "datetime" ) ) {
			// logger.debug( "Coerce attempt: Castable to LocalTime " + actualClass );
			CastAttempt<LocalTime> localTimeAttempt = TimeCaster.attempt( value );
			if ( localTimeAttempt.wasSuccessful() ) {
				// Casting is relatively accurate
				matchScore.addAndGet( 1 );
				return localTimeAttempt.toOptional();
			}
		}

		// EXPECTED: java.util.Date
		// If we expect a Date and we have a `DateTime` instance
		if ( Date.class.isAssignableFrom( expected ) && actualClass.equals( "datetime" ) ) {
			// logger.debug( "Coerce attempt: Castable to Date " + actualClass );
			CastAttempt<DateTime> dateTimeAttempt = DateTimeCaster.attempt( value );
			if ( dateTimeAttempt.wasSuccessful() ) {
				// Casting is relatively accurate
				matchScore.addAndGet( 1 );
				return Optional.ofNullable( dateTimeAttempt.get().toDate() );
			}
		}

		// EXPECTED: java.sql.Time
		// If we expect a Time and we have a `DateTime` instance
		if ( Time.class.isAssignableFrom( expected ) && actualClass.equals( "datetime" ) ) {
			// logger.debug( "Coerce attempt: Castable to Time " + actualClass );
			CastAttempt<LocalTime> timeAttempt = TimeCaster.attempt( value );
			if ( timeAttempt.wasSuccessful() ) {
				// Casting is relatively accurate
				matchScore.addAndGet( 1 );
				return Optional.ofNullable( Time.valueOf( timeAttempt.get() ) );
			}
		}

		// EXPECTED: java.sql.Timestamp
		// If we expect a Timestamp and we have a `DateTime` instance
		if ( Timestamp.class.isAssignableFrom( expected ) && actualClass.equals( "datetime" ) ) {
			// logger.debug( "Coerce attempt: Castable to Timestamp " + actualClass );
			CastAttempt<DateTime> dateTimeAttempt = DateTimeCaster.attempt( value );
			if ( dateTimeAttempt.wasSuccessful() ) {
				// Casting is relatively accurate
				matchScore.addAndGet( 1 );
				return Optional.ofNullable( Timestamp.valueOf( dateTimeAttempt.get().getWrapped().toLocalDateTime() ) );
			}
		}

		// EXPECTED: STRING
		if ( expectedClass.equals( "string" ) ) {
			// logger.debug( "Coerce attempt: Castable to String " + actualClass );
			CastAttempt<String> stringAttempt = StringCaster.attempt( value );
			if ( stringAttempt.wasSuccessful() ) {
				// Type casting is a looser match, increment 2
				matchScore.addAndGet( 2 );
				return stringAttempt.toOptional();
			}
		}

		// Expected: Numeric and Actual: String
		// If the expected type is a number and the actual is a string, we can TRY to coerce it
		if ( Number.class.isAssignableFrom( expected ) && actualClass.equals( "string" ) ) {
			// logger.debug( "Coerce attempt: Castable to Number from String " + actualClass );
			CastAttempt<Object> numberAttempt = GenericCaster.attempt( context, value, expectedClass );
			if ( numberAttempt.wasSuccessful() ) {
				// Type casting from string to number is an even looser match, increment 3
				matchScore.addAndGet( 3 );
				return numberAttempt.toOptional();
			}
		}

		// Allow Arrays to be coerced to native arrays
		if ( value instanceof Array arr && expected.isArray() ) {
			// Start with a short circuit check for Object[], which is easy
			if ( expected.getComponentType() == Object.class ) {
				// It's not safe to cache this method handle as other incoming arrays may not be castable to the same type
				isCachable.set( false );
				// Type casting from array to array is loose-ish match, increment 2
				matchScore.addAndGet( 2 );
				return Optional.of( arr.toArray() );
			} else {
				// For other specific types of native arrays, see if we can coerce the elements
				// This will be String[] or int[][], etc
				String				readableType	= getReadableType( expected );
				CastAttempt<Object>	attempt			= GenericCaster.attempt( context, arr, readableType );
				if ( attempt.wasSuccessful() ) {
					// Type casting from array to array is loose-ish match, increment 2
					matchScore.addAndGet( 2 );
					// It's not safe to cache this method handle as other incoming arrays may not be castable to the same type
					isCachable.set( false );

					// array casting can give back an Array instance or a native array depending on whether the type was primitive or not
					Object newArr = attempt.get();
					// If it's a BL array, get the native array out of it
					if ( newArr instanceof Array nar ) {
						return Optional.of( nar.toArray() );
					}
					// Otherwise, it's already a native array
					return Optional.of( newArr );
				}
			}
		}

		// If actual is a Class instance and expected is not, then see if the actual class is assignable to expected, if so we want to attempt to instantiate it using the no-arg constructor
		if ( value instanceof Class<?> cls && !isInterface( cls ) && expected != Class.class ) {
			// See if the class is assignable to the expected type
			if ( expected.isAssignableFrom( cls ) ) {
				try {
					// Try to instantiate it using the no-arg constructor
					Object instance = invokeConstructor( context, cls );
					// Type casting from Class to instance is a pretty loose match
					matchScore.addAndGet( 4 );
					return Optional.of( instance );
				} catch ( BoxRuntimeException e ) {
					// Ignore things like NoConstructorException
					// This is a bit of a wide net, but there are many reasons why instantiation could fail, and in any case, it just means we can't coerce it so we ignore it.
					// We could do more checks above before attempting to instantiate, but then we'd have to duplicate quite a lot of logic to actually check the constructors available on the class, etc
				}
			}
		}

		// EXPECTED: FunctionInterfaces and/or SAMs
		Class<?> functionalInterface = InterfaceProxyService.getFunctionalInterface( expected );
		// If the target is a functional interface and the actual value is a Funcion or Runnable, coerce it
		// To the functional interface
		if ( functionalInterface != null && ( value instanceof IClassRunnable || value instanceof Function ) ) {

			// Type casting from Class or Function to functional interface is a loose-ish match, increment 2
			matchScore.addAndGet( 2 );

			// logger.debug( "Coerce attempt: Castable to Functional Interface " + actualClass );
			return Optional.of(
			    InterfaceProxyService.isCoreProxy( expected.getSimpleName() )
			        ? InterfaceProxyService.buildCoreProxy( functionalInterface, context, value, null )
			        : InterfaceProxyService.buildGenericProxy( context, value, null, new Class[] { functionalInterface }, functionalInterface.getClassLoader() )
			);
		}

		// logger.debug( "Coerce attempt FAILED for [" + expected + "] from [" + actual + "] with value [" + value.toString() + "]" );

		return Optional.empty();
	}

	/**
	 * Small util for creating human readable name for type. Specifically will take a native array and give you String[] or int[][] etc
	 *
	 * @param clazz The class to get the readable type for
	 *
	 * @return The readable type
	 */
	private static String getReadableType( Class<?> clazz ) {
		if ( !clazz.isArray() ) {
			return clazz.getSimpleName(); // Return the class name if it's not an array
		}

		StringBuilder arrayType = new StringBuilder();
		while ( clazz.isArray() ) {
			arrayType.append( "[]" ); // Append "[]" for each array dimension
			clazz = clazz.getComponentType(); // Get the component type
		}

		arrayType.insert( 0, clazz.getName() ); // Add base type
		return arrayType.toString();
	}

	/**
	 * Lazy load this to avoid static intitlizer deadlocks on startup
	 * Most of this is done lazy to avoid static init deadlocks
	 */
	private static FunctionService getFunctionService() {
		if ( functionService == null ) {
			synchronized ( DynamicInteropService.class ) {
				if ( functionService == null ) {
					functionService = BoxRuntime.getInstance().getFunctionService();
				}
			}
		}
		return functionService;
	}

	/**
	 * Lazy load ClassLocator as well
	 * Most of this is done lazy to avoid static init deadlocks
	 */
	private static ClassLocator getClassLocator() {
		if ( classLocator == null ) {
			synchronized ( DynamicInteropService.class ) {
				if ( classLocator == null ) {
					classLocator = BoxRuntime.getInstance().getClassLocator();
				}
			}
		}
		return classLocator;
	}

	/**
	 * This method is used to make sure BoxLang classes are loaded correctly with their appropriate:
	 * - Super class
	 * - Interfaces
	 * - Abstract methods
	 * - Constructor
	 * - Imports
	 * - Etc.
	 *
	 * @param context        The context to use for the constructor
	 * @param boxClass       The class to bootstrap
	 * @param positionalArgs The positional arguments to pass to the constructor
	 * @param namedArgs      The named arguments to pass to the constructor
	 * @param noInit         Whether to skip the initialization of the class or not
	 *
	 * @return The instance of the class
	 */
	@SuppressWarnings( "unchecked" )
	private static <T> T bootstrapBLClass( IBoxContext context, IClassRunnable boxClass, Object[] positionalArgs, Map<Key, Object> namedArgs, boolean noInit ) {
		// This class context is really only used while boostrapping the pseudoConstructor. It will NOT be used as a parent
		// context once the boxClass is initialized. Methods called on this boxClass will have access to the variables/this scope via their
		// FunctionBoxContext, but their parent context will be whatever context they are called from.
		IBoxContext classContext = new ClassBoxContext( context, boxClass );
		// Bootstrap the pseudoConstructor
		classContext.pushTemplate( boxClass );

		try {
			// First, we load the super class if it exists
			Object superClassObject = boxClass.getAnnotations().get( Key._EXTENDS );
			if ( superClassObject != null ) {
				String superClassName = StringCaster.cast( superClassObject );
				if ( superClassName != null && superClassName.length() > 0 && !superClassName.toLowerCase().startsWith( "java:" ) ) {
					// Recursively load the super class
					IClassRunnable _super = ( IClassRunnable ) getClassLocator().load( classContext,
					    superClassName,
					    classContext.getCurrentImports()
					)
					    // Constructor args are NOT passed. Only the outermost class gets to use those
					    .invokeConstructor( classContext, new Object[] { Key.noInit } )
					    .unWrapBoxLangClass();

					// Check for final annotation and throw if we're trying to extend a final class
					if ( _super.getAnnotations().get( Key._final ) != null ) {
						throw new BoxRuntimeException( "Cannot extend final class: " + _super.bxGetName() );
					}
					// Set in our super class
					boxClass.setSuper( _super );
				}
			}

			// Run the pseudo constructor
			boxClass.pseudoConstructor( classContext );

			// Now that UDFs are defined, let's enforce any interfaces
			Object oInterfaces = boxClass.getAnnotations().get( Key._IMPLEMENTS );
			if ( oInterfaces != null ) {
				List<String> interfaceNames = ListUtil.asList( StringCaster.cast( oInterfaces ), "," )
				    .stream()
				    .map( String::valueOf )
				    .map( String::trim )
				    // ignore anything starting with java: (case insensitive)
				    .filter( name -> !name.toLowerCase().startsWith( "java:" ) )
				    .toList();

				for ( String interfaceName : interfaceNames ) {
					BoxInterface thisInterface = ( BoxInterface ) getClassLocator().load( classContext, interfaceName, classContext.getCurrentImports() )
					    .unWrapBoxLangClass();
					boxClass.registerInterface( thisInterface );
				}

			}

			if ( !noInit ) {
				if ( boxClass.getAnnotations().get( Key._ABSTRACT ) != null ) {
					throw new AbstractClassException( "Cannot instantiate an abstract class: " + boxClass.bxGetName() );
				}
				if ( boxClass.getSuper() != null ) {
					BoxClassSupport.validateAbstractMethods( boxClass, boxClass.getSuper().getAllAbstractMethods() );
				}
				// Call constructor
				// look for initMethod annotation
				Object	initMethod	= boxClass.getAnnotations().get( Key.initMethod );
				Key		initKey;
				if ( initMethod != null ) {
					initKey = Key.of( StringCaster.cast( initMethod ) );
				} else {
					initKey = Key.init;
				}
				if ( boxClass.dereference( context, initKey, true ) != null ) {
					Object result;
					if ( positionalArgs != null ) {
						result = boxClass.dereferenceAndInvoke( classContext, initKey, positionalArgs, false );
					} else {
						result = boxClass.dereferenceAndInvoke( classContext, initKey, namedArgs, false );
					}
					// CF returns the actual result of the constructor, but I'm not sure it makes sense or if people actually ever
					// return anything other than "this".
					if ( result != null ) {
						// This cast will fail if the init returns something like a string
						return ( T ) result;
					}
				} else {
					// implicit constructor

					if ( positionalArgs != null && positionalArgs.length == 1 && positionalArgs[ 0 ] instanceof IStruct named ) {
						namedArgs = named;
					} else if ( positionalArgs != null && positionalArgs.length > 0 ) {
						throw new BoxRuntimeException( "Implicit constructor only accepts named args or a single Struct as a positional arg." );
					}

					if ( namedArgs != null ) {
						if ( namedArgs.containsKey( Key.argumentCollection ) && namedArgs.get( Key.argumentCollection ) instanceof IStruct argCollection ) {
							// Create copy of named args, merge in argCollection without overwriting, and delete arg collection key from copy of namedargs
							namedArgs = new HashMap<>( namedArgs );
							for ( Map.Entry<Key, Object> entry : argCollection.entrySet() ) {
								if ( !namedArgs.containsKey( entry.getKey() ) ) {
									namedArgs.put( entry.getKey(), entry.getValue() );
								}
							}
							namedArgs.remove( Key.argumentCollection );
						} else if ( namedArgs.containsKey( Key.argumentCollection ) && namedArgs.get( Key.argumentCollection ) instanceof Array argArray ) {
							// Create copy of named args, merge in argCollection without overwriting, and delete arg collection key from copy of namedargs
							namedArgs = new HashMap<>( namedArgs );
							for ( int i = 0; i < argArray.size(); i++ ) {
								namedArgs.put( Key.of( i + 1 ), argArray.get( i ) );
							}
							namedArgs.remove( Key.argumentCollection );
						}
						// loop over args and invoke setter methods for each
						for ( Map.Entry<Key, Object> entry : namedArgs.entrySet() ) {
							// not a great way to pre-create/cache these keys since they're really based on whatever crazy args the user gives us.
							// If this becomes a performance issue, we can look at caching the expected keys in the boxClass in a map where the key is the
							// propery
							// name
							// and
							// the value is the key of the setter (basically the inverse of the setterlookup map)
							boxClass.dereferenceAndInvoke( classContext, Key.of( "set" + entry.getKey().getName() ), new Object[] { entry.getValue() }, false );
						}
					}
				}
			}
		} finally {
			// This is for any output written in the pseudoconstructor that needs to be flushed
			classContext.flushBuffer( false );
			classContext.popTemplate();
		}

		// We have a fully initialized class, so we can return it
		return ( T ) boxClass;
	}

	/**
	 * Coerce a single value to the expected type
	 * Unlike our BL Casters, this is directly tied to Java types for our Java interop. It uses
	 * the same logic used for method argument coercion, but just on a single value.
	 * 
	 * Throws a BoxCastException if the value cannot be coerced to the expected type.
	 * 
	 * @param context The context to use for the method invocation
	 * @param value   The value to coerce
	 * @param type    The expected type
	 * 
	 * @return The coerced value
	 */
	public static Object coerceValue(
	    IBoxContext context,
	    Object value,
	    Class<?> type ) {

		// Check for nulls
		if ( value == null ) {
			if ( type.isPrimitive() ) {
				throw new BoxRuntimeException( "Cannot coerce null to primitive type [ " + type.getName() + " ]." );
			}
			return null;
		}

		Object[]	args	= new Object[] { value };
		boolean		success	= coerceArguments(
		    context,
		    unBoxTypes( new Class<?>[] { type } ),
		    unBoxTypes( new Class<?>[] { value.getClass() } ),
		    args,
		    args,
		    false,
		    BooleanRef.of( true ),
		    new AtomicInteger( 0 )
		);
		if ( !success ) {
			throw new BoxCastException( "Value of type [ " + TypeUtil.getObjectName( value )
			    + " ] which could not be coerced to [ " + type.getName() + " ]." );
		}
		return args[ 0 ];
	}

	private record CoerceAttempt( Executable executable, AtomicInteger matchScore, Object[] castedArgumentValues ) {

		public static CoerceAttempt of( Executable executable, AtomicInteger matchScore, Object[] castedArgumentValues ) {
			return new CoerceAttempt( executable, matchScore, castedArgumentValues );
		}
	}

}
