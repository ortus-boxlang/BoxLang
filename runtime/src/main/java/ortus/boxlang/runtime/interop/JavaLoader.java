package ortus.boxlang.runtime.interop;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import ortus.boxlang.runtime.BoxPiler;
import ortus.boxlang.runtime.BoxRuntime;

public class JavaLoader {

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
	 * Singleton instance
	 */
	private static JavaLoader instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private JavaLoader() {
		getInstance();
	}

	/**
	 * Get an instance of the JavaLoader
	 *
	 * @return The JavaLoader instance
	 */
	public static synchronized JavaLoader getInstance() {
		if ( instance == null ) {
			instance = new JavaLoader();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public static Class<?> load( String fullyQualifiedClassName ) throws ClassNotFoundException {
		return loadFromModules( fullyQualifiedClassName ).or( () -> loadFromSystem( fullyQualifiedClassName ) )
				.orElseThrow( () -> new ClassNotFoundException(
						String.format( "The requested class [%s] has not been located", fullyQualifiedClassName ) )
				);

	}

	public static Optional<Class<?>> loadFromSystem( String fullyQualifiedClassName ) {
		try {
			return Optional.of( Class.forName( fullyQualifiedClassName ) );
		} catch ( ClassNotFoundException e ) {
			return Optional.empty();
		}
	}

	/**
	 * Load a class from the registered runtime modules
	 *
	 * @param fullyQualifiedClassName
	 *
	 * @return The loaded class or null if not found
	 */
	public static Optional<Class<?>> loadFromModules( String fullyQualifiedClassName ) {
		return Optional.ofNullable( null );
	}

	public static ClassLoader getSystemClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

}
