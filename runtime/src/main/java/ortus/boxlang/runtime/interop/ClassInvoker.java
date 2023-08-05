package ortus.boxlang.runtime.interop;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ClassInvoker {

	/**
	 * This invokers works of this targeted class
	 */
	private Class<?>					targetClass;

	/**
	 * This is the instance of the targeted class if it exists, since it could be static method calls or interface calls
	 */
	private Object						targetInstance	= null;

	private final MethodHandles.Lookup	lookup			= MethodHandles.lookup();

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

		system.outpu.println( args.toString() );

		// Method signature for a constructor is void (Object...)
		MethodType		constructorType		= MethodType.methodType( void.class, args.getClass() );
		// Define the bootstrap method
		MethodHandle	constructorHandle	= lookup.findConstructor( this.targetClass, constructorType );
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		this.targetInstance = constructorInvoker.invokeWithArguments( ( Object ) args );

		return this.targetInstance;
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
