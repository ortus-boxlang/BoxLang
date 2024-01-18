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
package ortus.boxlang.runtime.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This service is in charge of managing BoxLang modules
 */
public class ModuleService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The location of the core modules in the runtime resources: {@code src/main/resources/modules}
	 */
	private static final Path	CORE_MODULES	= ClassDiscovery.getPathFromResource( "modules" );

	/**
	 * List locations to search for modules
	 */
	private List<Path>			modulePaths		= new ArrayList<>( List.of( CORE_MODULES ) );

	/**
	 * Logger
	 */
	private static final Logger	logger			= LoggerFactory.getLogger( ModuleService.class );

	// private Map<Key, ModuleRecord> modules = new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The runtime instance
	 */
	public ModuleService( BoxRuntime runtime ) {
		super( runtime );

		// Register external module locations from config
		// Path moduleExternalPath = Paths.get( runtime.getConfiguration().runtime.modulesDirectory );
		// if ( Files.exists( moduleExternalPath ) && Files.isDirectory( moduleExternalPath ) ) {
		// modulePaths.add( runtime.getConfiguration().runtime.modulesDirectory );
		// logger.info( "ModuleService: Added external module path: {}", moduleExternalPath );
		// }

		// Register all modules now
		registerAllModules();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters(s) / Setters(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the list of module paths
	 *
	 * @return the modulePaths
	 */
	public List<Path> getModulePaths() {
		return this.modulePaths;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		logger.info( "ModuleService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		// unloadAll();
		logger.info( "ModuleService.onShutdown()" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Registrations
	 * --------------------------------------------------------------------------
	 */

	void registerAllModules() {
		// registerCoreModules();
	}

	void registerModule( Key name ) {
		// if( !modules.containsKey( name ) ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Activations
	 * --------------------------------------------------------------------------
	 */

	void activateAllModules() {
		// activateCoreModules();
	}

	void activateModule( Key name ) {
		// if( modules.containsKey( name ) ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Unloading
	 * --------------------------------------------------------------------------
	 */

	void unloadAll() {
		// unload all
	}

	void unloadModule( Key name ) {
		// if( modules.containsKey( name ) ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Add a module path to the list of paths to search for modules.
	 * This has to be an absolute path on disk or a relative path to the runtime resources.
	 *
	 * @param path The string path to add, package resources path or absolute path
	 *
	 * @return The ModuleService instance
	 */
	public ModuleService addModulePath( String path ) {
		// Check if the path is null or blank
		if ( path == null || path.isBlank() ) {
			return this;
		}

		// If it's a package path convert it to slashes
		if ( path.contains( "." ) ) {
			path = path.replace( ".", "/" );
		}

		return addModulePath( Paths.get( path ) );
	}

	/**
	 * Add a module {@link Path} to the list of paths to search for modules.
	 *
	 * @param path The {@link Path} to add
	 *
	 * @return The ModuleService instance
	 */
	public ModuleService addModulePath( Path path ) {
		// Check if the path is null or blank
		if ( path == null || path.toString().isBlank() ) {
			return this;
		}

		if ( !Files.exists( path ) ) {
			throw new BoxRuntimeException( "Module path does not exist: " + path );
		}

		// Add a module path to the list
		this.modulePaths.add( path );

		return this;
	}
}
