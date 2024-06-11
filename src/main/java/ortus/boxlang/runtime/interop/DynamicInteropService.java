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

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.ClassBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCasterLoose;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.NoConstructorException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * This class is used to provide a way to dynamically and efficiently interact with the java layer from the within a BoxLang environment.
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
public class DynamicInteropService {

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

	private static Set<Key>											exceptionKeys		= new HashSet<>( Arrays.asList(
	    BoxLangException.messageKey,
	    BoxLangException.detailKey,
	    BoxLangException.typeKey,
	    BoxLangException.tagContextKey,
	    BoxRuntimeException.ExtendedInfoKey,
	    Key.stackTrace
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
	 * @see https://docs.oracle.com/javase/11/docs/api/java/lang/invoke/MethodHandles.Lookup.html
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
	private static ClassLocator										classLocator		= ClassLocator.getInstance();

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
			if ( args.length == 1 && args[ 0 ].equals( Key.noInit ) ) {
				noInit = true;
			} else {
				BLArgs = args;
			}
			args = EMPTY_ARGS;
		}

		// Unwrap any ClassInvoker instances
		unWrapArguments( args );

		// Discover the constructor method handle using the target class and the argument type matching
		MethodHandle constructorHandle;
		try {
			constructorHandle = METHOD_LOOKUP.unreflectConstructor(
			    findMatchingConstructor( targetClass, argumentsToClasses( args ), args )
			);
		} catch ( IllegalAccessException e ) {
			throw new BoxRuntimeException(
			    "Error getting constructor for class " + targetClass.getName() + " with arguments classes " + Arrays.toString( argumentsToClasses( args ) ),
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
			T thisInstance = ( T ) constructorInvoker.invokeWithArguments( args );

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
			        Struct.of(
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
			throw new BoxRuntimeException( "Error invoking constructor for class " + targetClass.getName(), e );
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
	 * Reusable method for bootstrapping IClassRunnables
	 *
	 * @param boxClass The class to bootstrap
	 * @param args     The arguments to pass to the constructor
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
			// First, we load an super class
			Object superClassObject = boxClass.getAnnotations().get( Key._EXTENDS );
			if ( superClassObject != null ) {
				String superClassName = StringCaster.cast( superClassObject );
				if ( superClassName != null && superClassName.length() > 0 && !superClassName.toLowerCase().startsWith( "java:" ) ) {
					// Recursivley load the super class
					IClassRunnable _super = ( IClassRunnable ) classLocator.load( classContext,
					    superClassName,
					    classContext.getCurrentImports()
					)
					    // Constructor args are NOT passed. Only the outermost class gets to use those
					    .invokeConstructor( classContext, new Object[] { Key.noInit } )
					    .unWrapBoxLangClass();

					// Set in our super class
					boxClass.setSuper( _super );
				}
			}

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
					BoxInterface thisInterface = ( BoxInterface ) classLocator.load( classContext, interfaceName, classContext.getCurrentImports() )
					    .unWrapBoxLangClass();
					boxClass.registerInterface( thisInterface );
				}

			}

			if ( !noInit ) {
				if ( boxClass.getAnnotations().get( Key._ABSTRACT ) != null ) {
					throw new BoxRuntimeException( "Cannot instantiate an abstract class: " + boxClass.getName() );
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
						namedArgs = named.getWrapped();
					} else if ( positionalArgs != null && positionalArgs.length > 0 ) {
						throw new BoxRuntimeException( "Implicit constructor only accepts named args or a single Struct as a positional arg." );
					}

					if ( namedArgs != null ) {
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
		return ( T ) boxClass;
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param targetClass The Class that you want to invoke a method on
	 * @param methodName  The name of the method to invoke
	 * @param safe        Whether the method should throw an error or return null if it doesn't exist
	 * @param arguments   The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( Class<?> targetClass, String methodName, Boolean safe, Object... arguments ) {
		return invoke( targetClass, null, methodName, safe, arguments );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param targetInstance The instance to call the method on
	 * @param methodName     The name of the method to invoke
	 * @param safe           Whether the method should throw an error or return null if it doesn't exist
	 * @param arguments      The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( Object targetInstance, String methodName, Boolean safe, Object... arguments ) {
		return invoke( targetInstance.getClass(), targetInstance, methodName, safe, arguments );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param targetClass    The Class that you want to invoke a method on
	 * @param targetInstance The instance to call the method on, or null if it's static
	 * @param safe           Whether the method should throw an error or return null if it doesn't exist
	 * @param methodName     The name of the method to invoke
	 * @param arguments      The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invoke( Class<?> targetClass, Object targetInstance, String methodName, Boolean safe, Object... arguments ) {
		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new BoxRuntimeException( "Method name cannot be null or empty." );
		}

		// Unwrap any ClassInvoker instances
		unWrapArguments( arguments );

		// Get the invoke dynamic method handle from our cache and discovery techniques
		MethodRecord methodRecord;
		try {
			methodRecord = getMethodHandle( targetClass, targetInstance, methodName, argumentsToClasses( arguments ), arguments );
		} catch ( RuntimeException e ) {
			if ( safe ) {
				return null;
			} else {
				e.printStackTrace();
				throw new BoxRuntimeException( "Error getting method " + methodName + " for class " + targetClass.getName(), e );
			}
		}

		// If it's not static, we need a target instance
		if ( !methodRecord.isStatic() && targetInstance == null ) {
			throw new BoxRuntimeException(
			    "You can't call invoke on a null target instance. Use [invokeStatic] instead or set the target instance manually or via the constructor."
			);
		}

		// Discover and Execute it baby!
		try {
			return methodRecord.isStatic()
			    ? methodRecord.methodHandle().invokeWithArguments( arguments )
			    : methodRecord.methodHandle().bindTo( targetInstance ).invokeWithArguments( arguments );
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error invoking method " + methodName + " for class " + targetClass.getName(), e );
		}
	}

	/**
	 * Invokes a static method with the given name and arguments on a class or an interface
	 *
	 * @param targetClass The class you want to invoke a method on
	 * @param methodName  The name of the method to invoke
	 * @param arguments   The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public static Object invokeStatic( Class<?> targetClass, String methodName, Object... arguments ) {

		// Verify method name
		if ( methodName == null || methodName.isEmpty() ) {
			throw new BoxRuntimeException( "Method name cannot be null or empty." );
		}

		// Unwrap any ClassInvoker instances
		unWrapArguments( arguments );

		// Discover and Execute it baby!
		try {
			return getMethodHandle( targetClass, null, methodName, argumentsToClasses( arguments ), arguments )
			    .methodHandle()
			    .invokeWithArguments( arguments );
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error invoking method " + methodName + " for class " + targetClass.getName(), e );
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
				fieldHandle = METHOD_LOOKUP.unreflectGetter( field );
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
			throw new BoxRuntimeException( "Error getting field " + fieldName + " for class " + targetClass.getName(), e );
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

		try {
			if ( isStatic ) {
				fieldHandle.invokeWithArguments( value );
			} else {
				fieldHandle.bindTo( targetInstance ).invokeWithArguments( value );
			}
		} catch ( RuntimeException e ) {
			throw e;
		} catch ( Throwable e ) {
			throw new BoxRuntimeException( "Error setting field " + fieldName + " for class " + targetClass.getName(), e );
		}
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
	 * @param targetClass        The class to get the method handle for
	 * @param methodName         The name of the method to get the handle for
	 * @param argumentsAsClasses The array of arguments as classes to map
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The method handle representing the method signature
	 *
	 */
	public static MethodRecord getMethodHandle(
	    Class<?> targetClass,
	    Object targetInstance,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    Object... arguments ) {

		// We use the method signature as the cache key
		String			cacheKey		= targetClass.hashCode() + methodName + Arrays.hashCode( argumentsAsClasses );
		MethodRecord	methodRecord	= methodHandleCache.get( cacheKey );

		// Double lock to avoid race-conditions
		if ( methodRecord == null || !handlesCacheEnabled ) {
			synchronized ( cacheKey.intern() ) {
				if ( methodRecord == null || !handlesCacheEnabled ) {
					methodRecord = discoverMethodHandle( targetClass, targetInstance, methodName, argumentsAsClasses, arguments );
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
	 * @param targetClass        The class to discover the method for
	 * @param targetInstance     The instance to discover the method for (Can be null for static methods or interfaces)
	 * @param methodName         The name of the method to discover
	 * @param argumentsAsClasses The array of arguments as classes to map
	 * @param arguments          The arguments to pass to the method
	 *
	 * @throws NoMethodException If the method cannot be found by any means
	 *
	 * @return The method record representing the method signature and metadata
	 *
	 */
	public static MethodRecord discoverMethodHandle(
	    Class<?> targetClass,
	    Object targetInstance,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    Object... arguments ) {

		// Our target we must find using our dynamic rules:
		// - case insensitivity
		// - argument count
		// - argument class asignability
		// - argument coercion
		Method			targetMethod	= findMatchingMethod( targetClass, methodName, argumentsAsClasses, arguments );
		MethodHandle	targetHandle;

		// Verify we can access the method, if we can't then we need to go up the inheritance chain to find it or die
		// This happens when objects implement default method interfaces usually
		try {
			targetMethod = checkAccess( targetClass, targetInstance, targetMethod, methodName, argumentsAsClasses, arguments );
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
	 *
	 * @return A unique set of callable constructors
	 */
	public static Set<Constructor<?>> getConstructors( Class<?> targetClass ) {
		Set<Constructor<?>> allConstructors = new HashSet<>();
		allConstructors.addAll( new HashSet<>( List.of( targetClass.getConstructors() ) ) );
		allConstructors.addAll( new HashSet<>( List.of( targetClass.getDeclaredConstructors() ) ) );
		return allConstructors;
	}

	/**
	 * Get a stream of constructors of the given class
	 *
	 * @param targetClass The class to get the constructors for
	 *
	 * @return A stream of unique callable constructors
	 */
	public static Stream<Constructor<?>> getConstructorsAsStream( Class<?> targetClass ) {
		return getConstructors( targetClass ).stream();
	}

	/**
	 * Find a constructor by the given arguments as classes and return it if it exists
	 *
	 * @param targetClass        The class to find the constructor for
	 * @param argumentsAsClasses The parameter types of the constructor to find
	 * @param arguments          The arguments to pass to the constructor
	 *
	 * @return The constructor if it exists
	 */
	public static Constructor<?> findMatchingConstructor( Class<?> targetClass, Class<?>[] argumentsAsClasses, Object... arguments ) {
		return getConstructorsAsStream( targetClass )
		    .filter( constructor -> constructorHasMatchingParameterTypes( constructor, argumentsAsClasses, arguments ) )
		    .findFirst()
		    .orElseThrow( () -> new NoConstructorException(
		        String.format(
		            "No such constructor found in the class [%s] using [%d] arguments of types [%s]",
		            targetClass.getName(),
		            argumentsAsClasses.length,
		            Arrays.toString( argumentsAsClasses )
		        )
		    ) );
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
	 * @param targetClass        The class to check
	 * @param targetInstance     The instance to check
	 * @param targetMethod       The method to check
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The method if it's accessible, else it tries to find the method in the parent class of the targetClass
	 */
	private static Method checkAccess(
	    Class<?> targetClass,
	    Object targetInstance,
	    Method targetMethod,
	    String methodName,
	    Class<?>[] argumentsAsClasses,
	    Object... arguments ) {

		if ( targetMethod != null ) {
			boolean isStatic = Modifier.isStatic( targetMethod.getModifiers() );
			if ( !targetMethod.canAccess( isStatic ? null : targetInstance ) ) {
				// 1. Let's try to find it by interfaces first as it could be a default method
				var methodByInterface = findMatchingMethodByInterfaces( targetClass, methodName, argumentsAsClasses, arguments );
				if ( methodByInterface != null ) {
					return methodByInterface;
				}

				// 2. Try to get the method from the parent class of the targetClass until we can access or we die
				Class<?> superClass = targetClass.getSuperclass();
				targetMethod = findMatchingMethod( superClass, methodName, argumentsAsClasses, arguments );
				return checkAccess( superClass, targetInstance, targetMethod, methodName, argumentsAsClasses, arguments );
			}
		}
		return targetMethod;
	}

	/**
	 * Get a HashSet of methods of all the unique callable method signatures for the given class
	 *
	 * @param targetClass The class to get the methods for
	 *
	 * @return A unique set of callable methods
	 */
	public static Set<Method> getMethods( Class<?> targetClass ) {
		Set<Method> allMethods = new HashSet<>();
		allMethods.addAll( new HashSet<>( List.of( targetClass.getMethods() ) ) );
		allMethods.addAll( new HashSet<>( List.of( targetClass.getDeclaredMethods() ) ) );
		return allMethods;
	}

	/**
	 * Get a stream of methods of all the unique callable method signatures for the given class
	 *
	 * @param targetClass The class to get the methods for
	 *
	 * @return A stream of unique callable methods
	 */
	public static Stream<Method> getMethodsAsStream( Class<?> targetClass ) {
		return getMethods( targetClass ).stream();
	}

	/**
	 * Get a methods by name for the given class
	 *
	 * @param targetClass The class to get the methods for
	 * @param name        The name of the method to get
	 *
	 * @return The Method object
	 */
	public static Method getMethod( Class<?> targetClass, String name ) {
		return getMethodsAsStream( targetClass )
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
	 *
	 * @return A list of method names
	 */
	public static List<String> getMethodNames( Class<?> targetClass ) {
		return getMethodsAsStream( targetClass )
		    .parallel()
		    .map( Method::getName )
		    .toList();
	}

	/**
	 * Get a list of method names for the given class with no case-sensitivity (upper case)
	 *
	 * @param targetClass The class to get the methods for
	 *
	 * @return A list of method names with no case
	 */
	public static List<String> getMethodNamesNoCase( Class<?> targetClass ) {
		return getMethodsAsStream( targetClass )
		    .parallel()
		    .map( Method::getName )
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
		return getMethodNames( targetClass ).contains( methodName );
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
		return getMethodNamesNoCase( targetClass ).contains( methodName.toUpperCase() );
	}

	/**
	 * This method is used to verify if the class has the same method signature as the incoming one with no case-sensitivity (upper case)
	 *
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
	    Class<?> targetClass, String methodName, Class<?>[] argumentsAsClasses, Object... arguments ) {
		return getMethodsAsStream( targetClass )
		    .filter( method -> method.getName().equalsIgnoreCase( methodName ) )
		    .filter( method -> hasMatchingParameterTypes( method, argumentsAsClasses, arguments ) )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * Try to find a matching method by interfaces
	 *
	 * @param targetClass        The class to check it's interfaces
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return The matched method signature or null if not found
	 */
	public static Method findMatchingMethodByInterfaces( Class<?> targetClass, String methodName, Class<?>[] argumentsAsClasses, Object... arguments ) {
		// Since we haven't found the method in the class, we need to try to find it in the interfaces
		// since interfaces have default methods and we can't access them directly
		Class<?>[] interfaces = targetClass.getInterfaces();
		return Arrays.stream( interfaces )
		    .map( interfaceClass -> findMatchingMethod( interfaceClass, methodName, argumentsAsClasses, arguments ) )
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
	 * Unwrap any ClassInvoker instances in the arguments
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
		if ( Map.class.isAssignableFrom( targetClass ) && targetInstance instanceof Map ) {
			// If it's a raw Map, then we use the original value as the key
			return ( ( Map<Object, Object> ) targetInstance ).get( name.getOriginalValue() );
			// Special logic so we can treat exceptions as referencable. Possibly move to helper
		} else if ( targetInstance instanceof List list ) {
			Integer index = Array.validateAndGetIntForDereference( name, list.size(), safe );
			// non-existant indexes return null when dereferncing safely
			if ( safe && ( index < 1 || index > list.size() ) ) {
				return null;
			}
			return list.get( index - 1 );
		} else if ( targetInstance != null && targetInstance.getClass().isArray() ) {
			Object[] arr = ( ( Object[] ) targetInstance );
			if ( name.equals( lengthKey ) ) {
				return arr.length;
			}

			Integer index = Array.validateAndGetIntForDereference( name, arr.length, safe );
			// non-existant indexes return null when dereferncing safely
			if ( safe && ( index < 1 || index > arr.length ) ) {
				return null;
			}
			return arr[ index - 1 ];
		} else if ( targetInstance instanceof Throwable t && exceptionKeys.contains( name ) ) {
			// Throwable.message always delegates through to the message field
			if ( name.equals( BoxLangException.messageKey ) ) {
				return t.getMessage();
			} else if ( name.equals( Key.stackTrace ) ) {
				StringWriter	sw	= new StringWriter();
				PrintWriter		pw	= new PrintWriter( sw );
				t.printStackTrace( pw );
				return sw.toString();
			} else if ( name.equals( BoxLangException.tagContextKey ) ) {
				return ExceptionUtil.buildTagContext( t );
			} else if ( targetInstance instanceof BoxLangException ble ) {
				if ( name.equals( BoxLangException.detailKey ) ) {
					return ble.getDetail();
				} else if ( name.equals( BoxLangException.typeKey ) ) {
					return ble.getType();
				} else if ( ble instanceof BoxRuntimeException bre && name.equals( BoxRuntimeException.ExtendedInfoKey ) ) {
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
			// non-existant indexes return null when dereferncing safely
			if ( safe && ( index < 1 || index > s.length() ) ) {
				return null;
			}
			return s.substring( index - 1, index );
			// Special logic for native arrays. Possibly move to helper
		} else if ( hasFieldNoCase( targetClass, name.getName() ) ) {
			// If we have the field, return its value, even if it's null
			return getField( targetClass, targetInstance, name.getName() ).orElse( null );
		} else if ( hasClassNoCase( targetClass, name.getName() ) ) {
			return findClass( targetClass, name.getName() );
		} else if ( targetClass.isEnum() ) {
			return Enum.valueOf( ( Class<Enum> ) targetClass, name.getName() );
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
	public static Object dereferenceAndInvoke( Class<?> targetClass, IBoxContext context, Key name, Object[] positionalArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( targetClass, null, context, name, positionalArguments, safe );
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
	public static Object dereferenceAndInvoke( Object targetInstance, IBoxContext context, Key name, Object[] positionalArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( targetInstance.getClass(), targetInstance, context, name, positionalArguments, safe );
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
	public static Object dereferenceAndInvoke( Class<?> targetClass, Object targetInstance, IBoxContext context, Key name, Object[] positionalArguments,
	    Boolean safe ) {

		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, positionalArguments, safe );
		}

		if ( targetInstance != null ) {
			MemberDescriptor memberDescriptor = BoxRuntime.getInstance().getFunctionService().getMemberMethod( context, name, targetInstance );
			if ( memberDescriptor != null ) {
				return memberDescriptor.invoke( context, targetInstance, positionalArguments );
			}
		}

		if ( safe && !hasMethod( targetClass, name.getName() ) ) {
			return null;
		}

		// Special case for calling `getClass()` on a NON-instance created via createObject()
		// This is to avoid a null exception since `getClass()` is being called when no instance is there yet.
		if ( name.equals( Key.getClass ) ) {
			return targetClass;
		}

		return invoke( targetClass, targetInstance, name.getName(), safe, positionalArguments );
	}

	public static Object dereferenceAndInvoke( Class<?> targetClass, IBoxContext context, Key name, Map<Key, Object> namedArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( targetClass, null, context, name, namedArguments, safe );
	}

	public static Object dereferenceAndInvoke( Object targetInstance, IBoxContext context, Key name, Map<Key, Object> namedArguments,
	    Boolean safe ) {
		return dereferenceAndInvoke( targetInstance.getClass(), targetInstance, context, name, namedArguments, safe );
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
	public static Object dereferenceAndInvoke( Class<?> targetClass, Object targetInstance, IBoxContext context, Key name, Map<Key, Object> namedArguments,
	    Boolean safe ) {

		if ( IReferenceable.class.isAssignableFrom( targetClass ) && targetInstance != null && targetInstance instanceof IReferenceable ref ) {
			return ref.dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		if ( targetInstance != null ) {
			MemberDescriptor memberDescriptor = BoxRuntime.getInstance().getFunctionService().getMemberMethod( context, name, targetInstance );
			if ( memberDescriptor != null ) {
				return memberDescriptor.invoke( context, targetInstance, namedArguments );
			}
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
			Object[]	arr		= ( ( Object[] ) targetInstance );
			Integer		index	= Array.validateAndGetIntForAssign( name, arr.length, true );
			arr[ index - 1 ] = value;
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
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

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
	 * Verifies if the method has the same parameter types as the incoming ones using our matching algorithms.
	 * - Exact Match : Matches the incoming argument class types to the method signature
	 * - Discovery : Matches the incoming argument class types to the method signature by discovery of matching method names and argument counts
	 * - Coercive : Matches the incoming argument class types to the method signature by coercion of the argument types
	 *
	 * @see https://commons.apache.org/proper/commons-lang/javadocs/api-release/index.html
	 *
	 * @param method             The method to check
	 * @param argumentsAsClasses The arguments to check
	 * @param arguments          The arguments to pass to the method
	 *
	 * @return True if the method has the same parameter types, false otherwise
	 */
	private static boolean hasMatchingParameterTypes( Method method, Class<?>[] argumentsAsClasses, Object... arguments ) {
		Class<?>[] methodParams = method.getParameterTypes();

		// If we have a different number of parameters, then we don't have a match
		if ( methodParams.length != argumentsAsClasses.length ) {
			return false;
		}

		// Verify assignability including primitive autoboxing
		if ( ClassUtils.isAssignable( argumentsAsClasses, methodParams ) ) {
			return true;
		}

		// Let's do coercive matching if we get here.
		// iterate over the method params and check if the arguments can be coerced to the method params
		var coerced = false;
		for ( int i = 0; i < methodParams.length; i++ ) {

			Optional<?> attempt = coarceAttempt( methodParams[ i ], argumentsAsClasses[ i ], arguments[ i ] );

			if ( attempt.isPresent() ) {
				coerced			= true;
				arguments[ i ]	= attempt.get();
			} else {
				coerced = false;
				break;
			}
		}

		// return if it was coarced or not
		return coerced;
	}

	/**
	 * Tries to coarce a value to the expected type value
	 *
	 * @param expected The expected type class
	 * @param actual   The actual type class
	 * @param value    The value to coarce
	 *
	 * @return The coarced value or empty if it can't be coarced
	 */
	private static Optional<?> coarceAttempt( Class<?> expected, Class<?> actual, Object value ) {
		IBoxContext	context			= BoxRuntime.getInstance().getRuntimeContext();
		String		expectedClass	= expected.getSimpleName().toLowerCase();
		String		actualClass		= actual.getSimpleName().toLowerCase();

		// Primitive to Wrapper Type
		expected	= WRAPPERS_MAP.getOrDefault( expected, expected );
		actual		= WRAPPERS_MAP.getOrDefault( actual, actual );

		// EXPECTED: NUMBER
		// Verify if the expected and actual type is a Number, we can coarce it
		// Use the expected caster to coarce the value to the actual type
		if ( Number.class.isAssignableFrom( expected ) && Number.class.isAssignableFrom( actual ) ) {
			return Optional.of(
			    GenericCaster.cast( context, value, expectedClass )
			);
		}
		// // If it's a number and the actual is in the numberTargets list, we can coarce it
		// if ( Number.class.isAssignableFrom( expected ) && numberTargets.contains( actualClass ) ) {
		// return Optional.of(
		// GenericCaster.cast( context, value, expectedClass )
		// );
		// }

		// EXPECTED: BOOLEAN
		// If it's a boolean and the actual is in the booleanTargets list, we can coarce it
		if ( Boolean.class.isAssignableFrom( expected )
		    &&
		    booleanTargets.contains( actualClass )
		    &&
		    Number.class.isAssignableFrom( actual ) ) {
			return Optional.of(
			    BooleanCaster.cast( value )
			);
		}

		// EXPECTED: STRING
		if ( expectedClass.equals( "string" ) ) {
			return Optional.of(
			    StringCaster.cast( value )
			);
		}

		return Optional.empty();
	}

	/**
	 * Verifies if the constructor has the same parameter types as the incoming ones
	 * using our matching algorithms.
	 * - Exact Match : Matches the incoming argument class types to the constructor signature
	 * - Discovery : Matches the incoming argument class types to the constructor signature by discovery of matching argument counts
	 * - Coercive : Matches the incoming argument class types to the constructor signature by coercion of the argument types
	 *
	 * @param constructor        The constructor to check
	 * @param argumentsAsClasses The arguments to check
	 * @param arguments          The arguments to pass to the constructor
	 *
	 * @return True if the constructor has the same parameter types, false otherwise
	 */
	private static boolean constructorHasMatchingParameterTypes( Constructor<?> constructor, Class<?>[] argumentsAsClasses, Object... arguments ) {
		Class<?>[] constructorParams = constructor.getParameterTypes();

		// If we have a different number of parameters, then we don't have a match
		if ( constructorParams.length != argumentsAsClasses.length ) {
			return false;
		}

		// Verify assignability including primitive autoboxing
		return ClassUtils.isAssignable( argumentsAsClasses, constructorParams );
	}
}
