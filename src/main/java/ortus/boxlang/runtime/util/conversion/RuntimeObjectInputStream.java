package ortus.boxlang.runtime.util.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class RuntimeObjectInputStream extends ObjectInputStream {

	public RuntimeObjectInputStream( InputStream in ) throws IOException {
		super( in );
	}

	/**
	 * Override the resolveClass method to use the context class loader instead of stack walking to the nearest classloader
	 */
	@Override
	protected Class<?> resolveClass( ObjectStreamClass desc ) throws IOException, ClassNotFoundException {
		String className = desc.getName();
		try {
			return Class.forName( className, false, Thread.currentThread().getContextClassLoader() );
		} catch ( ClassNotFoundException e ) {
			// Fallback to the system class loader
			return super.resolveClass( desc );
		}
	}

}
