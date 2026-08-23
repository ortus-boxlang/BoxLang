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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.semver4j.Semver;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;
import ortus.boxlang.runtime.util.DataNavigator;

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

	/**
	 * The conventional folder name for modules nested inside another module (module inception).
	 * A module carrying a folder by this name has those modules discovered, registered and
	 * activated before the module itself.
	 */
	public static final String		MODULE_PACKAGE_PREFIX				= "modules";

	/**
	 * The file extension marking a module packaged as a single jar, placed directly inside a
	 * modules folder instead of following the module directory layout.
	 */
	public static final String		MODULE_JAR_EXTENSION				= ".jar";

	public static final String		MODULE_PUBLIC_FOLDER				= "public";

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
	 * Modules Logger
	 */
	private BoxLangLogger			logger;

	/**
	 * Module registry
	 */
	private Map<Key, ModuleRecord>	registry							= new ConcurrentHashMap<>();

	/**
	 * The BoxLang Semantic version
	 */
	private Semver					runtimeSemver;

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
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger = runtime.getLoggingService().MODULES_LOGGER;
	}

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		BoxRuntime.timerUtil.start( "moduleservice-startup" );
		this.logger.info( "+ Starting up Module Service..." );

		// Store the running BoxLang version
		String runtimeVersion = getRuntime().getVersionInfo().getAsString( Key.version );
		this.runtimeSemver = new Semver( runtimeVersion.equalsIgnoreCase( "@build.version@" ) ? "0.0.0" : runtimeVersion );

		// Register external module locations from the config
		runtime.getConfiguration().modulesDirectory.forEach( this::addModulePath );

		// Register all modules
		registerAll();

		// Activate all modules
		activateAll();

		var metadata = new Struct();
		for ( Entry<Key, ModuleRecord> entrySet : getRegistry().entrySet() ) {
			metadata.put(
			    entrySet.getKey().getName(),
			    UnmodifiableStruct.of(
			        "activatedOn", entrySet.getValue().activatedOn,
			        "activationTime", entrySet.getValue().activationTime,
			        "author", entrySet.getValue().author,
			        "description", entrySet.getValue().description,
			        "enabled", entrySet.getValue().isEnabled(),
			        "invocationPath", entrySet.getValue().invocationPath,
			        "mapping", entrySet.getValue().mapping.toStruct(),
			        "nestedModules", Array.copyOf( entrySet.getValue().nestedModules ),
			        "parentModule", entrySet.getValue().parentModule,
			        "physicalPath", entrySet.getValue().physicalPath.toString(),
			        "publicMapping", entrySet.getValue().publicMapping.toStruct(),
			        "registeredOn", entrySet.getValue().registeredOn,
			        "registrationTime", entrySet.getValue().registrationTime,
			        "version", entrySet.getValue().version
			    )
			);
		}

		runtime.getRuntimeContext()
		    .getScope( Key.server )
		    .getAsStruct( Key.boxlang )
		    .getAsStruct( Key.modules )
		    .addAll( metadata );

		// Announce it
		announce(
		    BoxEvent.ON_MODULE_SERVICE_STARTUP,
		    Struct.of( "moduleService", this )
		);

		// Let it be known!
		this.logger.info( "+ Module Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "moduleservice-startup" ) );
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

		this.logger.info( "+ Module Service shutdown" );
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
	public void registerAll() {
		var timerLabel = "moduleservice-registerallmodules";
		BoxRuntime.timerUtil.start( timerLabel );

		// Scan for modules and build the registration records
		buildRegistry();

		// Register each module now
		// If we detect more than 10 modules, do it async
		this.registry
		    .entrySet()
		    .stream()
		    // Filter out modules which are already registered
		    .filter( m -> m.getValue().registeredOn == null )
		    .forEach( entry -> this.register( entry.getKey() ) );

		// Log it
		this.logger.debug(
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
	public void register( Key name ) {
		var timerLabel = "moduleservice-register-" + name.getName();
		BoxRuntime.timerUtil.start( timerLabel );

		// Check if the module is in the registry
		if ( !this.registry.containsKey( name ) ) {
			var errorMessage = String.format(
			    "Cannot register the module [%s] is not in the module registry. Valid modules are: %s",
			    name,
			    this.registry.keySet().toString()
			);
			this.logger.error( errorMessage );
			throw new BoxRuntimeException( errorMessage );
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

		// Load the ModuleConfig.bx file, if it exists, and process the configuration
		moduleRecord.loadDescriptor( runtimeContext );

		// Verify if we disabled the loading of the module in the runtime config
		// myModule : { enabled = false }
		if ( this.runtime.getConfiguration().modules.containsKey( name ) ) {
			ModuleConfig config = ( ModuleConfig ) this.runtime.getConfiguration().modules.get( name );
			moduleRecord.enabled = config.enabled;
		}

		// Check if the module is disabled, if so, skip it. Any modules nested inside it are
		// skipped with it: their class loaders chain to this one, so they cannot stand alone.
		if ( !moduleRecord.isEnabled() ) {
			this.logger.warn(
			    "+ Module Service: Module [{}] is disabled, skipping registration",
			    moduleRecord.name
			);
			return;
		}

		/**
		 * |--------------------------------------------------------------------------
		 * | Module Inception
		 * |--------------------------------------------------------------------------
		 * Modules nested inside this one register before it does. This module's own config is
		 * resolved first, so its per-child setting overrides are readable by those children,
		 * while its full registration still happens after them.
		 */
		if ( !moduleRecord.nestedModules.isEmpty() ) {
			moduleRecord.resolveModuleConfig( runtimeContext );
			moduleRecord.nestedModules
			    .stream()
			    .map( childName -> Key.of( ( String ) childName ) )
			    .filter( childKey -> this.registry.containsKey( childKey ) && this.registry.get( childKey ).registeredOn == null )
			    .forEach( this::register );
		}

		// Configure the module
		moduleRecord.register( runtimeContext );

		// A jar module may rename itself via `@BoxModule( name = "..." )` once its config is
		// resolved off its class loader, so re-key it under the name it actually registered as.
		rekeyModule( name, moduleRecord );

		// Log registration time
		moduleRecord.registrationTime = BoxRuntime.timerUtil.stopAndGetMillis( timerLabel );

		// Announce it
		announce(
		    BoxEvent.POST_MODULE_REGISTRATION,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		this.logger.info(
		    "+ Module Service: Registered module [{}@{}] in [{}] ms from [{}]",
		    moduleRecord.name.getName(),
		    moduleRecord.version,
		    moduleRecord.registrationTime,
		    moduleRecord.physicalPath
		);
	}

	/**
	 * Move a module to a new registry key when it renamed itself during registration.
	 * <p>
	 * A jar module's discovered name is only its jar base name; its {@code @BoxModule( name )}
	 * annotation, read once the class loader is up, may name it something else. The parent's
	 * nested module list is updated to match so the parent can still target it.
	 *
	 * @param registeredUnder The key the module was registered under
	 * @param moduleRecord    The module record to re-key
	 */
	private void rekeyModule( Key registeredUnder, ModuleRecord moduleRecord ) {
		if ( moduleRecord.name.equals( registeredUnder ) ) {
			return;
		}

		this.registry.remove( registeredUnder );
		this.registry.put( moduleRecord.name, moduleRecord );

		// Keep the parent's child list pointing at the new name
		if ( moduleRecord.parentModule != null ) {
			ModuleRecord parentRecord = this.registry.get( moduleRecord.parentModule );
			if ( parentRecord != null ) {
				parentRecord.nestedModules.remove( registeredUnder.getName() );
				parentRecord.nestedModules.push( moduleRecord.name.getName() );
			}
		}

		this.logger.debug(
		    "+ Module Service: Module discovered as [{}] renamed itself to [{}] via its @BoxModule annotation",
		    registeredUnder.getName(),
		    moduleRecord.name.getName()
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
	public void activateAll() {
		var timerLabel = "moduleservice-activateallmodules";
		BoxRuntime.timerUtil.start( timerLabel );

		// If we detect more than 10 modules, do it async
		this.registry
		    .entrySet()
		    .stream()
		    // Filter out modules which are already activated
		    .filter( m -> m.getValue().activatedOn == null )
		    .forEach( entry -> this.activate( entry.getKey() ) );

		// Log it
		this.logger.info(
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
	public void activate( Key name ) {
		var timerLabel = "moduleservice-activate-" + name.getName();
		BoxRuntime.timerUtil.start( timerLabel );

		// Check if the module is in the registry
		if ( !this.registry.containsKey( name ) ) {
			var errorMessage = String.format(
			    "Cannot activate the module [%s] as it is not in the module registry. Valid modules are: %s",
			    name,
			    this.registry.keySet().toString()
			);
			this.logger.warn( errorMessage );
			throw new BoxRuntimeException( errorMessage );
		}

		// Check if the module is already activated
		if ( this.registry.get( name ).isActivated() ) {
			this.logger.warn(
			    "+ Module Service: Module [{}] is already activated, skipping re-activation",
			    name
			);
			return;
		}

		// Check if the module is disabled
		if ( !this.registry.get( name ).isEnabled() ) {
			this.logger.warn(
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

		/**
		 * |--------------------------------------------------------------------------
		 * | Module Inception Activation
		 * |--------------------------------------------------------------------------
		 * Modules nested inside this one activate before it does, mirroring their
		 * registration order and their class loader parentage.
		 */
		moduleRecord.nestedModules
		    .stream()
		    .map( childName -> Key.of( ( String ) childName ) )
		    .forEach( this::activate );

		/**
		 * |--------------------------------------------------------------------------
		 * | Module Dependencies Activation
		 * |--------------------------------------------------------------------------
		 * This makes sure that all dependencies are activated before the module itself
		 * This is a recursive call to activate all dependencies
		 */
		moduleRecord.dependencies
		    .stream()
		    .forEach( moduleName -> this.activate( Key.of( moduleName ) ) );

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
		this.logger.info(
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
	public void unloadAll() {
		this.registry
		    .keySet()
		    .stream()
		    .forEach( thisKey -> unload( thisKey, true ) );
	}

	/**
	 * Unload a module if it exists, else it's ignored
	 *
	 * @param name The name of the module to unload
	 */
	public void unload( Key name, Boolean removeFromRegistry ) {
		// Check if the module is in the registry or it's already deactivated
		if ( !this.registry.containsKey( name ) || !this.registry.get( name ).isActivated() ) {
			return;
		}

		// Get the module record and context of execution for modules
		// Which is separate from anything else
		var	moduleRecord	= this.registry.get( name );
		var	runtimeContext	= runtime.getRuntimeContext();

		/**
		 * |--------------------------------------------------------------------------
		 * | Module Inception Unloading
		 * |--------------------------------------------------------------------------
		 * Nested modules unload before their parent, the reverse of activation: their class
		 * loaders are parented to this module's loader, which unloading is about to close.
		 * Copied first, since unloading a child mutates this module's nested list when the
		 * child is removed from the registry.
		 */
		Array.copyOf( moduleRecord.nestedModules )
		    .stream()
		    .map( childName -> Key.of( ( String ) childName ) )
		    .forEach( childKey -> unload( childKey, removeFromRegistry ) );

		// Announce it
		announce(
		    BoxEvent.PRE_MODULE_UNLOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// We try/catch it in case it bongs, as we want all modules to unload
		try {
			moduleRecord.unload( runtimeContext );
		} catch ( Exception e ) {
			this.logger.error(
			    "+ Module Service: Error unloading module [{}@{}]: {}",
			    moduleRecord.name,
			    moduleRecord.version,
			    e.getMessage()
			);
		}

		// Announce it
		announce(
		    BoxEvent.POST_MODULE_UNLOAD,
		    Struct.of( "moduleRecord", moduleRecord, "moduleName", name )
		);

		// Log it
		this.logger.info(
		    "+ Module Service: Unloaded module [{}@{}]",
		    moduleRecord.name,
		    moduleRecord.version
		);

		// Remove it
		if ( removeFromRegistry ) {
			this.registry.remove( name );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Reload all modules
	 */
	public void reloadAll() {
		this.registry
		    .keySet()
		    .stream()
		    .forEach( thisKey -> reload( thisKey ) );
		this.logger.debug( "+ Module Service: Reloaded all modules" );
	}

	/**
	 * Helper to reload a module
	 *
	 * @param name The name of the module to reload
	 */
	public synchronized void reload( Key name ) {
		// Check if the module is in the registry
		if ( !this.registry.containsKey( name ) ) {
			var errorMessage = String.format(
			    "Cannot reload the module [%s] as it is not in the module registry. Valid modules are: %s",
			    name,
			    this.registry.keySet().toString()
			);
			this.logger.warn( errorMessage );
			throw new BoxRuntimeException( errorMessage );
		}

		// Unload the module but don't remove it from the registry
		// This is important as we need to keep the module in the registry
		// to be able to reload it
		unload( name, false );
		register( name );
		activate( name );

		this.logger.debug( "+ Module Service: Reloaded module [{}]", name.getName() );
	}

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
	 * @param path The {@link Path} to add. It can be relative or absolute, but it must exist or it is ignored.
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

		// Verify if the directory exists, else ignore it.
		if ( !Files.exists( path ) ) {
			this.logger.debug( "ModuleService: Requested addModulePath [{}] does not exist, ignoring.", path );
			return this;
		}

		// Verify it is a directory
		if ( Files.isDirectory( path ) ) {
			// Add a module path to the list
			this.modulePaths.add( path );
			this.logger.info( "+ ModuleService: Added an external module path: [{}]", path );
		} else {
			this.logger.warn( "ModuleService: Requested addModulePath [{}] does not exist or is not a directory", path );
		}

		return this;
	}

	/**
	 * Verify a module version and the BoxLang version are compatible.
	 * <p>
	 * Rules:
	 * - If the major version is different, then we are not compatible, so throw an exception
	 * - If the minor/path version is different, then we are compatible, but we will log a warning
	 *
	 * @param moduleVersion The version of the module
	 * @param directoryPath The directory path of the module
	 *
	 * @throws BoxRuntimeException If the module requires a different major version of BoxLang
	 */
	public void verifyModuleAndBoxLangVersion( String moduleVersion, Path directoryPath ) {
		// If we are in development mode, we don't care about the version
		if ( this.runtimeSemver.getMajor() == 0 ) {
			return;
		}

		// Early exit if the module version is null or blank
		if ( moduleVersion == null || moduleVersion.isBlank() ) {
			this.logger.warn( "Module [{}] does not have a BoxLang [minimumVersion] specified in the ModuleConfig.bx file", directoryPath.getFileName() );
			return;
		}

		// value must be non-null and a string and have a length
		if ( moduleVersion != null && !moduleVersion.isBlank() ) {
			Semver minimumVersion = new Semver( moduleVersion );

			// Major version check
			// Module minimum version = 3
			// Runtime version = 4 allow it, < 3 throw exception
			if ( this.runtimeSemver.getMajor() < minimumVersion.getMajor() ) {
				var errorMessage = String.format(
				    "Module [%s] requires BoxLang version [%s] but we are running [%s]",
				    directoryPath.getFileName(),
				    minimumVersion,
				    this.runtimeSemver
				);
				this.logger.error( errorMessage );
				throw new BoxRuntimeException( errorMessage );
			}

		}
	}

	/**
	 * This method scans all possible module locations and builds the module registry
	 * of all modules found. This method doesn't activate the modules, it just registers them.
	 * Duplicate modules are not allowed, first one wins.
	 */
	public void buildRegistry() {
		this.modulePaths
		    .stream()
		    .forEach( this::buildRegistryFromPath );
	}

	/**
	 * This method scans a single module path for modules and adds new ones to the module registry
	 * Call this with a directory of external modules you want to register.
	 * This method doesn't register or activate the module. It just adds the path to the registry.
	 *
	 * @param modulesDirectory The path to scan for modules
	 */
	public void buildRegistryFromPath( Path modulesDirectory ) {
		discoverModulesInPath( modulesDirectory, null );
	}

	/**
	 * Scans a single modules folder and adds every module it finds to the registry, recursing into
	 * each discovered module's own {@code modules} folder (module inception).
	 * <p>
	 * A modules folder may hold two kinds of module: conventional module <strong>directories</strong>
	 * (carrying a {@code ModuleConfig.bx} and/or {@code box.json}), and module <strong>jars</strong>
	 * placed directly inside it, whose descriptor is found later via {@link java.util.ServiceLoader}
	 * on the module's own class loader.
	 *
	 * @param modulesDirectory The modules folder to scan
	 * @param parent           The module this folder belongs to, or {@code null} when scanning a
	 *                         top-level configured modules directory
	 */
	private void discoverModulesInPath( Path modulesDirectory, ModuleRecord parent ) {
		try {
			Files.walk( modulesDirectory, 1 )
			    // Exclude the modules folder itself, and any root path in the `modulePaths` list
			    .filter( filePath -> !filePath.equals( modulesDirectory ) )
			    .filter( filePath -> !this.modulePaths.contains( filePath ) )
			    // Either a conventional module folder or a module jar
			    .filter( filePath -> isModuleFolder( filePath ) || isModuleJar( filePath ) )
			    // Filter out already registered modules
			    .filter( filePath -> !this.registry.containsKey( discoverModuleName( filePath ) ) )
			    // Convert each filePath to a discovered ModuleRecord, recursing into its own modules
			    .forEach( filePath -> createDiscoveredModule( filePath, parent ) );
		} catch ( IOException e ) {
			String message = "Error walking and registering module path: " + modulesDirectory.toString();
			this.logger.error( message, e );
			throw new BoxRuntimeException( message, e );
		}
	}

	/**
	 * Builds a {@link ModuleRecord} for a discovered module, links it to its parent (if any), adds
	 * it to the registry, and recurses into its own {@code modules} folder (module inception).
	 *
	 * @param modulePath The module's directory or jar file
	 * @param parent     The module this one is nested inside, or {@code null} for a top-level module
	 *
	 * @return The created module record
	 */
	private ModuleRecord createDiscoveredModule( Path modulePath, ModuleRecord parent ) {
		ModuleRecord moduleRecord = new ModuleRecord( modulePath.toString() );

		// Link the parent/child relationship both ways so each can target the other
		if ( parent != null ) {
			moduleRecord.parentModule = parent.name;
			parent.nestedModules.push( moduleRecord.name.getName() );
			this.logger.debug(
			    "+ Module Service: Discovered module [{}] nested inside module [{}]",
			    moduleRecord.name.getName(),
			    parent.name.getName()
			);
		}

		this.registry.put( moduleRecord.name, moduleRecord );

		// Module Inception: does this module carry modules of its own?
		Path nestedModulesPath = modulePath.resolve( MODULE_PACKAGE_PREFIX );
		if ( Files.isDirectory( nestedModulesPath ) ) {
			discoverModulesInPath( nestedModulesPath, moduleRecord );
		}

		return moduleRecord;
	}

	/**
	 * Resolve the name a module at this path would register under, without building a record.
	 * Used to skip candidates already present in the registry.
	 *
	 * @param modulePath The module's directory or jar file
	 *
	 * @return The module's name
	 */
	private Key discoverModuleName( Path modulePath ) {
		// A jar module's conventional name is the jar's base name; an explicit @BoxModule( name )
		// may rename it later, once its config is resolved off its class loader.
		if ( isModuleJar( modulePath ) ) {
			return Key.of( FilenameUtils.getBaseName( modulePath.getFileName().toString() ) );
		}

		Key		moduleName		= Key.of( modulePath.getFileName().toString() );
		Path	moduleBoxJSON	= modulePath.resolve( ModuleRecord.MODULE_CONFIG_FILE );
		if ( Files.exists( moduleBoxJSON ) ) {
			moduleName = DataNavigator
			    .of( moduleBoxJSON )
			    .from( "boxlang" )
			    .getAsKey( "moduleName", moduleName );
		}

		return moduleName;
	}

	/**
	 * Verify if a path is a conventional module folder: a directory carrying a
	 * {@code ModuleConfig.bx} or a {@code box.json} in its root.
	 * <p>
	 * Pure-Java modules may have only a {@code box.json}; their Java config is detected later
	 * via {@link java.util.ServiceLoader}.
	 *
	 * @param modulePath The path to check
	 *
	 * @return {@code true} if the path is a module folder, {@code false} otherwise
	 */
	public static boolean isModuleFolder( Path modulePath ) {
		return Files.isDirectory( modulePath )
		    && ( Files.exists( modulePath.resolve( MODULE_DESCRIPTOR ) )
		        || Files.exists( modulePath.resolve( ModuleRecord.MODULE_CONFIG_FILE ) ) );
	}

	/**
	 * Verify if a path is a module packaged as a single jar file. Such a jar, placed directly in a
	 * modules folder, is a module in its own right: it gets its own record and its own isolated
	 * class loader, and its {@link ortus.boxlang.runtime.modules.IModuleConfig} is discovered via
	 * {@link java.util.ServiceLoader} on that loader.
	 *
	 * @param modulePath The path to check
	 *
	 * @return {@code true} if the path is a module jar, {@code false} otherwise
	 */
	public static boolean isModuleJar( Path modulePath ) {
		return Files.isRegularFile( modulePath )
		    && modulePath.getFileName().toString().toLowerCase().endsWith( MODULE_JAR_EXTENSION );
	}

	/**
	 * Register and activate a single module. Pass either the directory that contains the
	 * ModuleConfig.bx (the directory name becomes the module name), or a module jar file (the jar's
	 * base name becomes the module name).
	 * <p>
	 * Any modules nested inside the module's own {@code modules} folder are registered and
	 * activated first (module inception).
	 *
	 * @param moduleDirectory The path to the module directory or jar
	 */
	public ModuleService loadModule( Path moduleDirectory ) {
		if ( !Files.isDirectory( moduleDirectory ) && !isModuleJar( moduleDirectory ) ) {
			throw new BoxRuntimeException( "The provided module path is not a directory or a module jar: " + moduleDirectory.toString() );
		}
		Key moduleName = discoverModuleName( moduleDirectory );
		// exit if module already registered
		if ( this.registry.containsKey( moduleName ) ) {
			return this;
		}
		// Build the record and discover any modules nested inside it
		createDiscoveredModule( moduleDirectory, null );
		register( moduleName );
		activate( moduleName );
		return this;
	}

	/**
	 * Register and activate all the modules in a directory. Pass the directory that contains nested module directories.
	 *
	 * @param moduleDirectory The path to scan for modules
	 */
	public ModuleService loadModules( Path modulesDirectory ) {
		buildRegistryFromPath( modulesDirectory );
		registerAll();
		activateAll();
		return this;
	}

}
