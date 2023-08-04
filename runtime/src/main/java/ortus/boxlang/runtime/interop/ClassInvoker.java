package ortus.boxlang.runtime.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ClassInvoker {

	private final Class<?> loadedClass;

	public ClassInvoker( Class<?> loadedClass ) {
		this.loadedClass = loadedClass;
	}

	public Object getInstance() throws Throwable {
		MethodHandles.Lookup	lookup		= MethodHandles.lookup();
		MethodHandle			constructor	= lookup.findConstructor( loadedClass, MethodType.methodType( void.class ) );
		return constructor.invoke();
	}

	public Object invokeConstructor( Object... args ) throws Throwable {
		MethodHandles.Lookup	lookup		= MethodHandles.lookup();
		MethodHandle			constructor	= lookup.findConstructor( loadedClass,
				MethodType.methodType( void.class, args.getClass() ) );
		return constructor.invokeWithArguments( args );
	}

	public Object invoke( String methodName, Object... args ) throws Throwable {
		MethodHandles.Lookup	lookup	= MethodHandles.lookup();
		MethodHandle			method	= lookup.findVirtual( loadedClass, methodName,
				MethodType.methodType( Object.class, args.getClass() ) );
		return method.invokeWithArguments( args );
	}
}
