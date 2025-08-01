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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocation;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This class is the base class for all resolvers.
 */
public class BaseResolver implements IClassResolver {

	/**
	 * The name of a resolver
	 */
	protected String		name		= "";

	/**
	 * The prefix of a resolver
	 */
	protected String		prefix		= "";

	/**
	 * The runtime that this resolver is associated with
	 */
	protected BoxRuntime	runtime;

	/**
	 * The class locator that this resolver is associated with
	 */
	protected ClassLocator	classLocator;

	/**
	 * The import cache
	 */
	protected Set<String>	importCache	= ConcurrentHashMap.newKeySet();

	/**
	 * Logging Class
	 */
	protected BoxLangLogger	logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 *
	 * @param name   The name of the resolver
	 * @param prefix The prefix of the resolver
	 */
	protected BaseResolver( String name, String prefix, ClassLocator classLocator ) {
		this.name			= name;
		this.prefix			= prefix.toLowerCase();
		this.classLocator	= classLocator;
		this.runtime		= classLocator.getRuntime();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Each resolver has a unique human-readable name
	 *
	 * @return The resolver name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Each resolver has a unique prefix which is used to call it. Do not add the
	 * {@code :}
	 * ex: java:, bx:, wirebox:, custom:
	 *
	 * @return The prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Get the import cache
	 *
	 * @return The importCache set
	 */
	public Set<String> getImportCache() {
		return this.importCache;
	}

	/**
	 * Get the import cache size
	 *
	 * @return The importCache size
	 */
	public Integer getImportCacheSize() {
		return this.importCache.size();
	}

	/**
	 * Clear the import cache
	 */
	public void clearImportCache() {
		this.importCache.clear();
	}

	/**
	 * Get the runtime
	 */
	public BoxRuntime getRuntime() {
		return this.runtime;
	}

	/**
	 * Get the class locator
	 */
	public ClassLocator getClassLocator() {
		return this.classLocator;
	}

	/**
	 * Get the Runtime Configuration
	 */
	public Configuration getConfiguration() {
		return getRuntime().getConfiguration();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Each resolver has a way to resolve the class it represents.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 *
	 * @return An optional class object representing the class if found
	 */
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name ) {
		throw new BoxRuntimeException( "Implement the [resolve] method in your own resolver" );
	}

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches with imports.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 * @param imports The list of imports to use
	 *
	 * @return An optional class object representing the class if found
	 */
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<ImportDefinition> imports, IStruct properties ) {
		throw new BoxRuntimeException( "Implement the [resolve] method in your own resolver" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Import Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Tries to expand the full class name using the import aliases given. If the class
	 * name is not found as an import, we return the original class name.
	 *
	 * @param context   The current context of execution
	 * @param className The name of the class to resolve
	 * @param imports   The list of imports to use
	 *
	 * @return The resolved class name or the original class name if not found
	 */
	public String expandFromImport( IBoxContext context, String className, List<ImportDefinition> imports ) {
		var fullyQualifiedName = imports.stream()
		    // Discover import by matching the resolver prefix and the class name or alias or multi-import
		    // This runs from concrete resolvers: bx, java, etc.
		    // So if the resolver prefix matches, we continue, else we skip it.
		    .filter( thisImport -> importApplies( thisImport ) && importHas( context, thisImport, className ) )
		    // Return the first one, the first one wins
		    .findFirst()
		    // Convert the import to a fully qualified class name
		    .map( targetImport -> {
			    String fqn = targetImport.getFullyQualifiedClass( className );
			    this.importCache.add( className + ":" + fqn );
			    return fqn;
		    } )
		    // Nothing found, return the original class name
		    .orElse( className );

		// Security check
		BoxRuntime.getInstance().getConfiguration().security.isClassAllowed( fullyQualifiedName );

		// Return the fully qualified class name
		return fullyQualifiedName;
	}

	/**
	 * Checks if the import has the given class. This method is used for single imports only
	 * Ex: {@code import java.util.ArrayList;}
	 *
	 * @param context    The current context of execution
	 * @param thisImport The import to check
	 * @param className  The class name to check
	 *
	 * @return True if the import has the class name, false otherwise
	 */
	protected boolean importHas( IBoxContext context, ImportDefinition thisImport, String className ) {
		String cacheKey = className + ":" + thisImport.getFullyQualifiedClass( className );

		// Verify cache
		if ( this.importCache.contains( cacheKey ) ) {
			return true;
		}

		// Not a multi-import, check if the class name matches the alias
		return thisImport.isMultiImport()
		    ? importHasMulti( context, thisImport, className )
		    : thisImport.alias().equalsIgnoreCase( className )
		        // the module name may actually be part of the class name. foo@bar.bx
		        || ( thisImport.isModuleImport() && thisImport.alias().concat( "@" + thisImport.moduleName() ).equalsIgnoreCase( className ) );
	}

	/**
	 * Checks if the import has the given class name as a multi-import
	 *
	 * @param context    The current context of execution
	 * @param thisImport The import to check
	 * @param className  The class name to check
	 *
	 * @return True if the import has the class name, false otherwise
	 */
	protected boolean importHasMulti( IBoxContext context, ImportDefinition thisImport, String className ) {
		try {
			return ClassDiscovery
			    .getClassFilesAsStream( thisImport.getPackageName(), false )
			    .anyMatch( clazzName -> ClassUtils.getShortClassName( clazzName ).equalsIgnoreCase( className ) );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Could not discover classes in package [" + thisImport.getPackageName() + "]", e );
		}
	}

	/**
	 * Checks if the import applies to this resolver. Applicable imports have a
	 * resolver prefix that matches this resolver's prefix, or no prefix at all
	 *
	 * @param thisImport The import to check
	 *
	 * @return True if the import applies to this resolver
	 */
	protected boolean importApplies( ImportDefinition thisImport ) {
		return thisImport.resolverPrefix() == null || thisImport.resolverPrefix().equalsIgnoreCase( this.prefix );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utilities
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the BoxLang system class loader used to load the runtime
	 *
	 * @return The BL system class loader
	 */
	protected static DynamicClassLoader getSystemClassLoader() {
		return BoxRuntime.getInstance().getRuntimeLoader();
	}

	/**
	 * Get the resolver loggers
	 */
	protected BoxLangLogger getLogger() {
		if ( this.logger == null ) {
			synchronized ( this ) {
				if ( this.logger == null ) {
					this.logger = BoxRuntime.getInstance().getLoggingService().getLogger( "resolver" );
				}
			}
		}
		return this.logger;
	}

}
