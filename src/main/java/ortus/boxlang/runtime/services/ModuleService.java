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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This service is in charge of managing BoxLang modules
 */
public class ModuleService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The module mapping and invocation prefixes
	 */
	public static final String				MODULE_MAPPING_PREFIX				= "/bxModules/";
	public static final String				MODULE_MAPPING_INVOCATION_PREFIX	= "bxModules.";

	/**
	 * The module descriptor file name
	 */
	public static final String				MODULE_DESCRIPTOR					= "ModuleConfig.cfc";
	// public static final String MODULE_DESCRIPTOR = "ModuleConfig.bx";

	/**
	 * The location of the core modules in the runtime resources: {@code src/main/resources/modules}
	 */
	public static final String				CORE_MODULES						= "modules";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The core modules file system. This is used to load modules from the runtime resources.
	 * This is only used in jar mode.
	 */
	private FileSystem						coreModulesFileSystem;

	/**
	 * List locations to search for modules
	 */
	private List<Path>						modulePaths							= new ArrayList<>();

	/**
	 * Logger
	 */
	private static final Logger				logger								= LoggerFactory.getLogger( ModuleService.class );

	/**
	 * Module registry
	 */
	private Map<Key, ModuleRecord>			registry							= new ConcurrentHashMap<>();

	/**
	 * Module Service Events
	 */
	private static final Map<String, Key>	MODULE_EVENTS						= Stream.of(
	    "afterModuleRegistrations",
	    "preModuleRegistration",
	    "postModuleRegistration",
	    "afterModuleActivations",
	    "preModuleLoad",
	    "postModuleLoad",
	    "preModuleUnload",
	    "postModuleUnload",
	    "onModuleServiceStartup",
	    "onModuleServiceShutdown"
	).collect( Collectors.toMap(
	    eventName -> eventName,
	    Key::of
	) );

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

		// Add the module service events
		runtime.getInterceptorService().registerInterceptionPoint( MODULE_EVENTS.values().toArray( Key[]::new ) );
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
	 * Get the list of module paths registered in the module service
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
		BoxRuntime.timerUtil.start( "moduleservice-startup" );
		logger.atInfo().log( "+ Starting up Module Service..." );

		// Register external module locations from the config
		runtime.getConfiguration().runtime.modulesDirectory.forEach( this::addModulePath );

		// Register all modules
		registerAll();

		// Activate all modules
		// activateAllModules();

		// Announce it
		announce(
		    MODULE_EVENTS.get( "onModuleServiceStartup" ),
		    Struct.of( "moduleService", this )
		);

		// Let it be known!
		logger.atInfo().log( "+ Module Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "moduleservice-startup" ) );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		// Announce it
		announce(
		    MODULE_EVENTS.get( "onModuleServiceShutdown" ),
		    Struct.of( "moduleService", this )
		);

		// Shutdown the core modules file system if it's in jar mode
		if ( this.coreModulesFileSystem != null ) {
			try {
				this.coreModulesFileSystem.close();
			} catch ( Exception e ) {
				logger.error( "Error closing core modules file system", e );
			}
		}

		// Unload all modules
		// unloadAll();

		logger.atInfo().log( "+ Module Service shutdown" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Registrations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Scans all possible module locations and registers all modules found
	 * This method doesn't activate the modules, it just registers them
	 */
	void registerAll() {
		var timerLabel = "moduleservice-registerallmodules";
		BoxRuntime.timerUtil.start( timerLabel );

		// Scan for modules and build the registration records
		buildRegistry();

		// Register each module now
		this.registry
		    .keySet()
		    .stream()
		    .forEach( this::register );

		// Log it
		logger.atInfo().log(
		    "+ Module Service: Registered [{}] modules in [{}] ms",
		    this.registry.size(),
		    BoxRuntime.timerUtil.stopAndGetMillis( timerLabel )
		);

		// Announce it
		announce( MODULE_EVENTS.get( "afterModuleRegistrations" ), Struct.of( "moduleRegistry", this.registry ) );
	}

	/**
	 * Register a module. This method doesn't activate the module, it just registers it.
	 * Duplicate modules are not allowed, first one wins.
	 * The module must be in the module registry or it will throw an exception.
	 *
	 * @param name The name of the module to register
	 *
	 * @throws BoxRuntimeException If the module is not in the module registry
	 */
	void register( Key name ) {
		var timerLabel = "moduleservice-register-" + name.getName();
		BoxRuntime.timerUtil.start( timerLabel );

		// Check if the module is in the registry
		if ( !this.registry.containsKey( name ) ) {
			throw new BoxRuntimeException(
			    "Cannot register the module [" + name + "] is not in the module registry." +
			        "Valid modules are: " + this.registry.keySet().toString()
			);
		}

		// Get the module record
		var moduleRecord = this.registry.get( name );

		// Announce it
		announce(
		    MODULE_EVENTS.get( "preModuleRegistration" ),
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Load the ModuleConfig.bx file
		moduleRecord.loadDescriptor( runtime.getRuntimeContext() );

		// Check if the module is disabled
		if ( moduleRecord.isDisabled() ) {
			logger.atInfo().log(
			    "+ Module Service: Module [{}] is disabled, skipping registration",
			    moduleRecord.name
			);
			return;
		}

		// Register the mapping in the runtime
		runtime
		    .getConfiguration().runtime
		    .registerMapping( moduleRecord.mapping, moduleRecord.physicalPath.toString() );

		// Call the configure method if it exists
		if ( moduleRecord.moduleConfig.getThisScope().containsKey( Key.configure ) ) {
			moduleRecord.moduleConfig.dereferenceAndInvoke(
			    runtime.getRuntimeContext(),
			    Key.configure,
			    new Object[] {},
			    false
			);
		}

		// Finalize
		moduleRecord.registrationTime = runtime.timerUtil.stopAndGetMillis( timerLabel );

		// Announce it
		announce(
		    MODULE_EVENTS.get( "postModuleRegistration" ),
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		logger.atInfo().log(
		    "+ Module Service: Registered module [{}@{}] in [{}] ms from [{}]",
		    moduleRecord.name,
		    moduleRecord.version,
		    moduleRecord.registrationTime,
		    moduleRecord.physicalPath
		);
	}

	/**
	 * Get the module registry
	 */
	public Map<Key, ModuleRecord> getRegistry() {
		return this.registry;
	}

	/**
	 * This method scans all possible module locations and builds the module registry
	 * of all modules found. This method doesn't activate the modules, it just registers them.
	 * Duplicate modules are not allowed, first one wins.
	 */
	private void buildRegistry() {
		this.modulePaths
		    .stream()
		    // Walks the path and returns a stream of discovered modules for this path
		    .flatMap( path -> {
			    try {
				    return Files.walk( path, 1 );
			    } catch ( IOException e ) {
				    throw new BoxRuntimeException( "Error walking module path: " + path.toString(), e );
			    }
		    } )
		    // Exclude the path if it is a root path in the `modulePaths` list
		    .filter( filePath -> !this.modulePaths.contains( filePath ) )
		    // Only module folders
		    .filter( Files::isDirectory )
		    // Only where a ModuleConfig.bx exists in the root
		    .filter( filePath -> Files.exists( filePath.resolve( MODULE_DESCRIPTOR ) ) )
		    // Filter out already registered modules
		    .filter( filePath -> !this.registry.containsKey( Key.of( filePath.getFileName().toString() ) ) )
		    // Convert each filePath to a discovered ModuleRecord
		    .map( filePath -> new ModuleRecord( Key.of( filePath.getFileName().toString() ), filePath.toString() ) )
		    // Collect the stream into the module registry
		    .forEach( moduleRecord -> this.registry.put( moduleRecord.name, moduleRecord ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Activations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Activate all modules
	 */
	void activateAll() {
		// activateCoreModules();
	}

	/**
	 * Activate a module
	 *
	 * @param name The name of the module to activate
	 */
	void activate( Key name ) {
		// if( modules.containsKey( name ) ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Unloading
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Unload all modules
	 */
	void unloadAll() {
		this.registry
		    .keySet()
		    .stream()
		    .forEach( this::unload );
	}

	/**
	 * Unload a module
	 *
	 * @param name The name of the module to unload
	 */
	void unload( Key name ) {
		// if( modules.containsKey( name ) ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Add a module path to the list of paths to search for modules.
	 * This has to be an absolute path on disk or a relative path to the runtime resources using forward slashes.
	 *
	 * @param path The string path to add, package resources path or absolute path using forward slashes.
	 *
	 * @return The ModuleService instance
	 */
	public ModuleService addModulePath( String path ) {
		// Check if the path is null or blank
		if ( path == null || path.isBlank() ) {
			return this;
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

		// Verify
		if ( Files.exists( path ) && Files.isDirectory( path ) ) {
			// Add a module path to the list
			this.modulePaths.add( path );
			logger.atDebug().log( "+ ModuleService: Added an external module path: [{}]", path.toString() );
		} else {
			logger.atWarn().log( "ModuleService: Requested addModulePath [{}] does not exist or is not a directory", path.toString() );
		}

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
