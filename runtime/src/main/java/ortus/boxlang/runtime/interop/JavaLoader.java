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

import java.util.Optional;

/**
 * This class is responsible for loading Java classes into the runtime.
 * It will try to locate the class in the runtime modules first, then
 * in the system class loader.
 *
 * We can create further class loading hierarchies later, like inline loading, application.cfc etc.
 *
 * For now, we will just load from the system class loader.
 */
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

	public static ClassInvoker load( String fullyQualifiedClassName ) throws ClassNotFoundException {
		return loadFromModules( fullyQualifiedClassName ).or( () -> loadFromSystem( fullyQualifiedClassName ) )
		        .orElseThrow( () -> new ClassNotFoundException(
		                String.format( "The requested class [%s] has not been located", fullyQualifiedClassName ) )
		        );

	}

	public static Optional<ClassInvoker> loadFromSystem( String fullyQualifiedClassName ) {
		try {
			return Optional.of( new ClassInvoker( Class.forName( fullyQualifiedClassName ) ) );
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
	public static Optional<ClassInvoker> loadFromModules( String fullyQualifiedClassName ) {
		return Optional.ofNullable( null );
	}

	public static ClassLoader getSystemClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

}
