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
package ortus.boxlang.runtime.loader.resolvers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
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
		return resolve( context, name, List.of() );
	}

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches with imports.
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified name OR imported alias of the class to resolve
	 * @param imports The list of imports to use
	 *
	 * @return An optional class object representing the class if found
	 */
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<ImportDefinition> imports ) {
		String fullyQualifiedName = expandFromImport( context, name, imports );
		return findFromModules( fullyQualifiedName, imports )
		    .or( () -> findFromSystem( fullyQualifiedName, imports ) );
	}

	/**
	 * Load a class from the registered runtime module class loaders
	 *
	 * @param fullyQualifiedName The fully qualified path of the class to load
	 * @param imports            The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( String fullyQualifiedName, List<ImportDefinition> imports ) {
		return Optional.ofNullable( null );
	}

	/**
	 * Load a class from the system class loader
	 *
	 * @param fullyQualifiedName The fully qualified path of the class to load
	 * @param imports            The list of imports to use
	 *
	 * @return The {@link ClassLocation} record wrapped in an optional if found, empty otherwise
	 */
	public Optional<ClassLocation> findFromSystem( String fullyQualifiedName, List<ImportDefinition> imports ) {
		Class<?> clazz;
		try {
			clazz = getSystemClassLoader().loadClass( fullyQualifiedName );
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
	 * Checks if the import has the given class name as :
	 * - an alias
	 * - as a class
	 * - as a multi-import (java.util.*) definition
	 *
	 * @param thisImport The import to check
	 * @param className  The class name to check
	 *
	 * @return True if the import has the class name, false otherwise
	 *
	 * @throws IOException
	 */
	@Override
	protected boolean importHas( ImportDefinition thisImport, String className ) {
		if ( thisImport.isMultiImport() ) {
			// We can't interrogate the JDK due to limitations in the JDK itself
			if ( thisImport.className().startsWith( "java." ) || thisImport.className().startsWith( "javax." ) ) {
				// Here we do a simple check to see if the class name can be loaded
				// from the system class loader. This is not a perfect check, but it
				// will do for now.
				try {
					Class.forName( thisImport.getFullyQualifiedClass( className ), false, getSystemClassLoader() );
					return true;
				} catch ( ClassNotFoundException e ) {
					return false;
				}
			} else {
				try {
					return ClassDiscovery
					    .getClassFilesAsStream( thisImport.getPackageName(), false )
					    .anyMatch( clazzName -> ClassUtils.getShortClassName( clazzName ).equalsIgnoreCase( className ) );
				} catch ( IOException e ) {
					e.printStackTrace();
					throw new RuntimeException( "Could not discover classes in package [" + thisImport.getPackageName() + "]", e );
				}
			}
		}
		// Not a multi-import, check if the class name matches the alias
		return thisImport.alias().equalsIgnoreCase( className );
	}

	/**
	 * Get the system class loader
	 *
	 * @return The system class loader
	 */
	private static ClassLoader getSystemClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

}
