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
package ortus.boxlang.runtime.loader;

import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;

/**
 * This resolver deals with Java classes only.
 */
public class JavaResolver extends BaseResolver {

	/**
	 * Singleton instance
	 */
	protected static JavaResolver instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private JavaResolver() {
		super( "JavaResolver", "java" );
	}

	/**
	 * Singleton instance
	 *
	 * @return The instance
	 */
	public static synchronized JavaResolver getInstance() {
		if ( instance == null ) {
			instance = new JavaResolver();
		}

		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 *
	 * @return An optional class object representing the class if found
	 */
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name ) {
		return findFromModules( name )
		        .or( () -> findFromSystem( name ) );
	}

	/**
	 * Load a class from the registered runtime module class loaders
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( String name ) {
		return Optional.ofNullable( null );
	}

	/**
	 * Load a class from the system class loader
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The {@link ClassLocation} record wrapped in an optional if found, empty otherwise
	 */
	public Optional<ClassLocation> findFromSystem( String name ) {
		Class<?> clazz;
		try {
			clazz = getSystemClassLoader().loadClass( name );
			return Optional.of(
			        new ClassLocation(
			                ClassUtils.getSimpleName( clazz ),
			                name,
			                ClassUtils.getPackageName( clazz ),
			                ClassLocator.TYPE_JAVA,
			                clazz,
			                null
			        )
			);
		} catch ( ClassNotFoundException e ) {
			return Optional.empty();
		}
	}

	/**
	 * Get the system class loader
	 *
	 * @return The system class loader
	 */
	public static ClassLoader getSystemClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

}
