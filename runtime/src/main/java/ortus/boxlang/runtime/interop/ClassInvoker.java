package ortus.boxlang.runtime.interop;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic:
 *
 * - invokestatic is used to invoke static methods.
 * - invokevirtual is used to invoke public and protected non-static methods via dynamic dispatch.
 * - invokeinterface is similar to invokevirtual except for the method dispatch being based on an interface type.
 * - invokespecial is used to invoke instance initialization methods (constructors) as well as private methods and methods of a superclass of the
 * current class.
 */
public class ClassInvoker {

	// @formatter:off
	private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = Map.of(
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            Character.class, char.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class
    );
	// @formatter:on

	/**
	 * This invokers works of this targeted class
	 */
	private Class<?>								targetClass;

	/**
	 * This is the instance of the targeted class if it exists, since it could be static method calls or interface calls
	 */
	private Object									targetInstance	= null;

	private final MethodHandles.Lookup				lookup			= MethodHandles.lookup();

	public ClassInvoker( Class<?> targetClass ) {
		this.targetClass = targetClass;
	}

	public ClassInvoker( Object targetInstance ) {
		this.targetInstance	= targetInstance;
		this.targetClass	= targetInstance.getClass();
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

	boolean isInterface() {
		return this.targetClass.isInterface();
	}

	public Object invokeConstructor( Object... args ) throws Throwable {
		// Convert the arguments to an array of classes
		Class<?>[]		argTypes			= Arrays.stream( args )
		        .map( ClassInvoker::argumentToClass )
		        .toArray( Class<?>[]::new );
		// Method signature for a constructor is void (Object...)
		MethodType		constructorType		= MethodType.methodType( void.class, argTypes );
		// Define the bootstrap method
		MethodHandle	constructorHandle	= lookup.findConstructor( this.targetClass, constructorType );
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		this.targetInstance = constructorInvoker.invokeWithArguments( args );

		return this.targetInstance;
	}

	public static Class<?> argumentToClass( Object thisArg ) {
		// TODO: Not sure what happens when the arg is null?
		if ( thisArg == null ) {
			return Object.class;
		}
		Class<?> clazz = thisArg.getClass();
		return PRIMITIVE_MAP.getOrDefault( clazz, clazz );
	}

	public Object invoke( String methodName, Object... args ) throws Throwable {
		MethodHandle method = lookup.findVirtual( this.targetClass, methodName,
		        MethodType.methodType( Object.class, args.getClass() ) );
		return method.invokeWithArguments( args );
	}

	public Object invokeStatic( String methodName, Object... args ) throws Throwable {
		MethodHandle method = lookup.findStatic( this.targetClass, methodName,
		        MethodType.methodType( Object.class, args.getClass() ) );
		return method.invokeWithArguments( args );
	}
}
