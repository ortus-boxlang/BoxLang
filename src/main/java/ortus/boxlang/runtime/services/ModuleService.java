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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
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
	private static final String	CORE_MODULES	= "modules";

	/**
	 * The core modules file system. This is used to load modules from the runtime resources.
	 * This is only used in jar mode.
	 */
	private FileSystem			coreModulesFileSystem;

	/**
	 * List locations to search for modules
	 */
	private List<Path>			modulePaths		= new ArrayList<>();

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

		if ( runtime.inJarMode() ) {
			try {
				buildCoreModulesFileSystem();
			} catch ( IOException | URISyntaxException e ) {
				throw new BoxRuntimeException( "Cannot build core modules file system from the runtime jar, this is really bad :(", e );
			}
		}

		// Add the core modules path to the list of module paths
		addCoreModulesPath();

		// Register external module locations from the config
		// addModulePath( Paths.get( runtime.getConfiguration().runtime.modulesDirectory ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters(s) / Setters(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the core modules file system if it's in jar mode
	 *
	 * @return The coreModulesFileSystem or null if not in jar mode
	 */
	public FileSystem getCoreModulesFileSystem() {
		return this.coreModulesFileSystem;
	}

	/**
	 * Check if the runtime is in jar mode
	 *
	 * @return true if in jar mode, false otherwise
	 */
	public boolean inJarMode() {
		return this.coreModulesFileSystem != null;
	}

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
		registerAllModules();
		logger.info( "ModuleService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		if ( this.coreModulesFileSystem != null ) {
			try {
				this.coreModulesFileSystem.close();
			} catch ( Exception e ) {
				logger.error( "Error closing core modules file system", e );
			}
		}
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
	 * @param path The {@link Path} to add. It can be relative or absolute.
	 *
	 * @return The ModuleService instance
	 */
	public ModuleService addModulePath( Path path ) {
		// Check if the path is null or blank
		if ( path == null || path.toString().isBlank() ) {
			return this;
		}

		// Convert to absolute path if it's not already
		path = path.toAbsolutePath();

		// Verify or throw up, we don't ignore, because this is a pretty big deal if you're trying to load modules
		if ( !Files.exists( path ) || !Files.isDirectory( path ) ) {
			throw new BoxRuntimeException( "Module path does not exist or is not a directory " + path.toString() );
		}

		// Add a module path to the list
		this.modulePaths.add( path );

		logger.atDebug().log( "ModuleService: Added an external module path: {}", path.toString() );

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The core modules are the modules that are included in the runtime resources.
	 * This method will detect if your are in dev or jar mode and add it accordingly.
	 */
	private void addCoreModulesPath() {
		URL coreModulesUrl = ModuleService.class.getClassLoader().getResource( CORE_MODULES );

		// Check if the resource exists
		if ( coreModulesUrl == null ) {
			throw new BoxRuntimeException( "Core Modules not found, something is really wrong " + coreModulesUrl );
		}

		// Jar Mode
		if ( this.runtime.inJarMode() ) {
			try {
				this.modulePaths.add( getCoreModulesFileSystem().getPath( coreModulesUrl.toString() ) );
			} catch ( Exception e ) {
				e.printStackTrace();
				throw new BoxRuntimeException(
				    String.format( "Cannot build an Path/URI from the discovered resource %s", coreModulesUrl ),
				    e
				);
			}
		}
		// Non Jar Mode
		else {
			try {
				this.modulePaths.add( Paths.get( coreModulesUrl.toURI() ) );
			} catch ( Exception e ) {
				throw new BoxRuntimeException(
				    String.format( "Cannot build an Path/URI from the discovered resource %s", coreModulesUrl ),
				    e
				);
			}
		}

		logger.atDebug().log( "ModuleService: Added core modules path: {}", this.modulePaths.toString() );
	}

	/**
	 * Build the core modules file system from the runtime jar
	 *
	 * @return The core modules file system
	 *
	 * @throws IOException        If there is an error building the file system
	 * @throws URISyntaxException If there is an error in the URI syntax
	 */
	private FileSystem buildCoreModulesFileSystem() throws IOException, URISyntaxException {
		URL coreModulesUrl = ModuleService.class.getClassLoader().getResource( CORE_MODULES );

		try {
			this.coreModulesFileSystem = FileSystems.getFileSystem( coreModulesUrl.toURI() );
		} catch ( FileSystemNotFoundException | URISyntaxException e ) {
			this.coreModulesFileSystem = FileSystems.newFileSystem( coreModulesUrl.toURI(), new HashMap<>() );
		}

		return this.coreModulesFileSystem;
	}
}
