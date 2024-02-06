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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This resolver deals with Java classes only.
 * It has two import caches as well to deal with JDK classes and non-JDK classes.
 */
public class JavaResolver extends BaseResolver {

	/**
	 * Singleton instance
	 */
	protected static JavaResolver	instance;

	/**
	 * The JDK class import cache
	 */
	private Set<String>				jdkClassImportCache	= ConcurrentHashMap.newKeySet();

	/**
	 * Logger
	 */
	private static final Logger		logger				= LoggerFactory.getLogger( JavaResolver.class );

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
		// Do we have a explicit module name? path.to.Class@moduleName
		String[]		parts			= fullyQualifiedName.split( "@" );
		ModuleService	moduleService	= BoxRuntime.getInstance().getModuleService();

		// If we have a module name, then we need to load the class from the module explicitly
		if ( parts.length == 2 ) {
			Key		moduleName	= Key.of( parts[ 1 ] );
			String	className	= parts[ 0 ];

			// Verify the module exists, else throw up, as it was an explicit call
			if ( !moduleService.hasModule( moduleName ) ) {
				throw new BoxRuntimeException(
				    String.format(
				        "Module requested [%s] not found when looking for [%s]. Valid modules are: [{}]",
				        moduleName,
				        className,
				        moduleService.getModuleNames()
				    )
				);
			}

			// Get the module class loader and try to load it
			// We don't do safe, because the request was explicit
			try {
				var clazz = moduleService.getModuleRecord( moduleName ).findModuleClass( className, false );
				return Optional.of(
				    new ClassLocation(
				        ClassUtils.getSimpleName( clazz ),
				        this.name,
				        ClassUtils.getPackageName( clazz ),
				        ClassLocator.TYPE_JAVA,
				        clazz,
				        moduleName.getName()
				    )
				);
			} catch ( ClassNotFoundException e ) {
				logger.atError().setCause( e ).log( "Could not find class [{}] in requested module [{}]", className, moduleName.getName() );
				throw new BoxRuntimeException(
				    String.format( "Could not find class [%s] in requested module [%s]", className, moduleName.getName() ),
				    e
				);
			}
		}

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
			        this.name,
			        ClassUtils.getPackageName( clazz ),
			        ClassLocator.TYPE_JAVA,
			        clazz,
			        null
			    )
			);
		} catch ( ClassNotFoundException e ) {
			logger.atError().setCause( e ).log( "Could not find class [{}]", fullyQualifiedName );
			return Optional.empty();
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Import Helpers
	 * --------------------------------------------------------------------------
	 */

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
	 */
	@Override
	protected boolean importHas( ImportDefinition thisImport, String className ) {
		return thisImport.isMultiImport()
		    ? importHasMulti( thisImport, className )
		    : super.importHas( thisImport, className );
	}

	/**
	 * Checks if the import has the given class name as a multi-import.
	 * However, we can't interrogate the JDK due to limitations in the JDK itself.
	 * So we do a simple check to see if the class name can be loaded from the
	 * system class loader. This is not a perfect check, but it will do for now.
	 *
	 * If the class is not a JDK class, we delegate to the super class.
	 *
	 * @param thisImport The import to check
	 * @param className  The class name to check
	 *
	 * @return True if the import has the class name, false otherwise
	 */
	@Override
	protected boolean importHasMulti( ImportDefinition thisImport, String className ) {
		// We can't interrogate the JDK due to limitations in the JDK itself
		if ( thisImport.className().matches( "(?i)(java|javax)\\..*" ) ) {

			logger.atDebug().log( "Checking if [{}] is a JDK class", thisImport.getFullyQualifiedClass( className ) );

			// Do we have it in the cache?
			if ( jdkClassImportCache.contains( thisImport.getFullyQualifiedClass( className ) ) ) {
				return true;
			}

			try {
				Class.forName( thisImport.getFullyQualifiedClass( className ), false, getSystemClassLoader() );
				jdkClassImportCache.add( thisImport.getFullyQualifiedClass( className ) );
				logger.atDebug().log( "Found JDK Class [{}] and added to jdk import cache", thisImport.getFullyQualifiedClass( className ) );

				return true;
			} catch ( ClassNotFoundException e ) {
				logger.atDebug().log( "Could not find JDK Class [{}]", thisImport.getFullyQualifiedClass( className ) );
				return false;
			}
		} else {
			// Use the base resolver
			return super.importHasMulti( thisImport, className );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Import Cache Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Clear the JDK import cache
	 */
	public void clearJdkImportCache() {
		jdkClassImportCache.clear();
	}

	/**
	 * Get the JDK import cache
	 *
	 * @return The JDK import cache
	 */
	public Set<String> getJdkImportCache() {
		return jdkClassImportCache;
	}

	/**
	 * Get the size of the JDK import cache
	 *
	 * @return The size of the JDK import cache
	 */
	public Integer getJdkImportCacheSize() {
		return jdkClassImportCache.size();
	}

}
