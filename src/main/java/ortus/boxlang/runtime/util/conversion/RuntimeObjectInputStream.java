package ortus.boxlang.runtime.util.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * This class extends ObjectInputStream to resolve classes using the context class loader.
 * 
 * It overrides the resolveClass method to prioritize the context classloader
 */
public class RuntimeObjectInputStream extends ObjectInputStream {

	/**
	 * Constructor that takes an InputStream and initializes the custom ObjectInputStream.
	 */
	public RuntimeObjectInputStream( InputStream in ) throws IOException {
		super( in );
	}

	/**
	 * Override the resolveClass method to use the context class loader instead of stack walking to the nearest classloader
	 * If the class cannot be found using the context class loader, it falls back to the system class loader.
	 *
	 * @param desc the descriptor of the class to resolve
	 * 
	 * @return the resolved class
	 * 
	 * @throws IOException            if an I/O error occurs while reading the class descriptor
	 * @throws ClassNotFoundException if the class cannot be found
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
