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
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
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
	public static final String		MODULE_MAPPING_PREFIX				= "/bxModules/";
	public static final String		MODULE_MAPPING_INVOCATION_PREFIX	= "bxModules.";

	/**
	 * The module conventions
	 */
	public static final String		MODULE_DESCRIPTOR					= "ModuleConfig.bx";
	public static final String		MODULE_BIFS							= "bifs";
	public static final String		MODULE_COMPONENTS					= "components";
	public static final String		MODULE_LIBS							= "libs";
	public static final String		MODULE_PACKAGE_PREFIX				= "modules";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * List locations to search for modules
	 */
	private List<Path>				modulePaths							= new ArrayList<>();

	/**
	 * Logger
	 */
	private static final Logger		logger								= LoggerFactory.getLogger( ModuleService.class );

	/**
	 * Module registry
	 */
	private Map<Key, ModuleRecord>	registry							= new ConcurrentHashMap<>();

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
		super( runtime, Key.moduleService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters(s) / Setters(s)
	 * --------------------------------------------------------------------------
	 */

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
		logger.debug( "+ Starting up Module Service..." );

		// Register external module locations from the config
		runtime.getConfiguration().runtime.modulesDirectory.forEach( this::addModulePath );

		// Register all modules
		registerAll();

		// Activate all modules
		activateAll();

		// Announce it
		announce(
		    BoxEvent.ON_MODULE_SERVICE_STARTUP,
		    Struct.of( "moduleService", this )
		);

		// Let it be known!
		logger.info( "+ Module Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "moduleservice-startup" ) );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether the shutdown is forced
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// Announce it
		announce(
		    BoxEvent.ON_MODULE_SERVICE_SHUTDOWN,
		    Struct.of( "moduleService", this )
		);

		// Unload all modules
		unloadAll();

		logger.debug( "+ Module Service shutdown" );
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
		// If we detect more than 10 modules, do it async
		this.registry
		    .keySet()
		    .stream()
		    .forEach( this::register );

		// Log it
		logger.debug(
		    "+ Module Service: Registered [{}] modules in [{}] ms",
		    this.registry.size(),
		    BoxRuntime.timerUtil.stopAndGetMillis( timerLabel )
		);

		// Announce it
		announce(
		    BoxEvent.AFTER_MODULE_REGISTRATIONS,
		    Struct.of( "moduleRegistry", this.registry )
		);
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

		// Get the module record and context of execution for modules
		// Which is separate from anything else
		var	moduleRecord	= this.registry.get( name );
		var	runtimeContext	= runtime.getRuntimeContext();

		// Announce it
		announce(
		    BoxEvent.PRE_MODULE_REGISTRATION,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Load the ModuleConfig.bx file
		moduleRecord.loadDescriptor( runtimeContext );

		// Check if the module is disabled, if so, skip it
		if ( moduleRecord.isDisabled() ) {
			logger.warn(
			    "+ Module Service: Module [{}] is disabled, skipping registration",
			    moduleRecord.name
			);
			return;
		}

		// Configure the module
		moduleRecord.register( runtimeContext );

		// Log registration time
		moduleRecord.registrationTime = BoxRuntime.timerUtil.stopAndGetMillis( timerLabel );

		// Announce it
		announce(
		    BoxEvent.POST_MODULE_REGISTRATION,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		logger.debug(
		    "+ Module Service: Registered module [{}@{}] in [{}] ms from [{}]",
		    moduleRecord.name.getName(),
		    moduleRecord.version,
		    moduleRecord.registrationTime,
		    moduleRecord.physicalPath
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Activations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Activate all modules that are not disabled
	 */
	void activateAll() {
		var timerLabel = "moduleservice-activateallmodules";
		BoxRuntime.timerUtil.start( timerLabel );

		// If we detect more than 10 modules, do it async
		this.registry
		    .keySet()
		    .stream()
		    .forEach( this::activate );

		// Log it
		logger.debug(
		    "+ Module Service: Activated [{}] modules in [{}] ms",
		    this.registry.size(),
		    BoxRuntime.timerUtil.stopAndGetMillis( timerLabel )
		);

		// Announce it
		announce(
		    BoxEvent.AFTER_MODULE_ACTIVATIONS,
		    Struct.of( "moduleRegistry", this.registry )
		);
	}

	/**
	 * Activate a module
	 *
	 * @param name The name of the module to activate
	 *
	 * @throws BoxRuntimeException If the module is not in the module registry
	 */
	void activate( Key name ) {
		var timerLabel = "moduleservice-activate-" + name.getName();
		BoxRuntime.timerUtil.start( timerLabel );

		// Check if the module is in the registry
		if ( !this.registry.containsKey( name ) ) {
			throw new BoxRuntimeException(
			    "Cannot activate the module [" + name + "] is not in the module registry." +
			        "Valid modules are: " + this.registry.keySet().toString()
			);
		}

		// Check if the module is already activated
		if ( this.registry.get( name ).isActivated() ) {
			logger.warn(
			    "+ Module Service: Module [{}] is already activated, skipping re-activation",
			    name
			);
			return;
		}

		// Check if the module is disabled
		if ( this.registry.get( name ).isDisabled() ) {
			logger.debug(
			    "+ Module Service: Module [{}] is disabled, skipping activation",
			    name
			);
			return;
		}

		// Get the module record and context of execution for modules
		// Which is separate from anything else
		var	moduleRecord	= this.registry.get( name );
		var	runtimeContext	= runtime.getRuntimeContext();

		// Announce it
		announce(
		    BoxEvent.PRE_MODULE_LOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Activate it
		moduleRecord.activate( runtimeContext );

		// Finalized
		moduleRecord.activationTime = BoxRuntime.timerUtil.stopAndGetMillis( timerLabel );

		// Announce it
		announce(
		    BoxEvent.POST_MODULE_LOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		logger.debug(
		    "+ Module Service: Activated module [{}@{}] in [{}] ms",
		    moduleRecord.name.getName(),
		    moduleRecord.version,
		    moduleRecord.activationTime
		);
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
	 * Unload a module if it exists, else it's ignored
	 *
	 * @param name The name of the module to unload
	 */
	void unload( Key name ) {
		// Check if the module is in the registry or it's already deactivated
		if ( !this.registry.containsKey( name ) || !this.registry.get( name ).isActivated() ) {
			return;
		}

		// Get the module record and context of execution for modules
		// Which is separate from anything else
		var	moduleRecord	= this.registry.get( name );
		var	runtimeContext	= runtime.getRuntimeContext();

		// Announce it
		announce(
		    BoxEvent.PRE_MODULE_UNLOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Call onUnload()
		moduleRecord.unload( runtimeContext );

		// Announce it
		announce(
		    BoxEvent.POST_MODULE_UNLOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		logger.debug(
		    "+ Module Service: Unload module [{}@{}]",
		    moduleRecord.name,
		    moduleRecord.version
		);

		// Remove it
		this.registry.remove( name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the module registry
	 */
	public Map<Key, ModuleRecord> getRegistry() {
		return this.registry;
	}

	/**
	 * Get a list of module names in the registry
	 *
	 * @return The list of module names
	 */
	public List<Key> getModuleNames() {
		return new ArrayList<>( this.registry.keySet() );
	}

	/**
	 * Get a module record from the registry
	 *
	 * @param name The name of the module to get
	 *
	 * @return The module record or null if not found
	 */
	public ModuleRecord getModuleRecord( Key name ) {
		return this.registry.get( name );
	}

	/**
	 * Retrieves the module settings for a requested module
	 *
	 * @param name The name of the module to get settings for
	 *
	 * @return The settings for the module as a struct
	 */
	public IStruct getModuleSettings( Key name ) {
		ModuleRecord moduleRecord = getModuleRecord( name );
		if ( moduleRecord == null ) {
			throw new BoxRuntimeException( String.format( "The module [%s] is not registered in the current runtime", name.getName() ) );
		}
		return moduleRecord.settings;
	}

	/**
	 * Verify if we have a module in the registry
	 *
	 * @param name The name of the module to verify
	 *
	 * @return True if the module is in the registry, false otherwise
	 */
	public boolean hasModule( Key name ) {
		return this.registry.containsKey( name );
	}

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

		// Verify if the directory exists, else create it, if we can
		if ( !Files.exists( path ) ) {
			try {
				Files.createDirectories( path );
			} catch ( IOException e ) {
				if ( e instanceof FileSystemException && e.getMessage().contains( "Read-only file system" ) ) {
					logger.warn( "ModuleService: Cannot create module path [{}] as it is on a read-only file system", path.toString() );
					return this;
				} else {
					throw new BoxRuntimeException( "Error creating module path: " + path.toString(), e );
				}
			}
		}

		// Verify it is a directory
		if ( Files.isDirectory( path ) ) {
			// Add a module path to the list
			this.modulePaths.add( path );
			logger.debug( "+ ModuleService: Added an external module path: [{}]", path.toString() );
		} else {
			logger.warn( "ModuleService: Requested addModulePath [{}] does not exist or is not a directory", path.toString() );
		}

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

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
		    .map( filePath -> new ModuleRecord( filePath.toString() ) )
		    // Collect the stream into the module registry
		    .forEach( moduleRecord -> this.registry.put( moduleRecord.name, moduleRecord ) );
	}
}
