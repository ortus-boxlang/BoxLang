/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.config.ConfigLoader;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.RuntimeBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.IService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.util.MathUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.util.Timer;

/**
 * Represents the top level runtime container for box lang. Config, global
 * scopes, mappings, threadpools, etc all go here.
 * All threads, requests, invocations, etc share this.
 */
public class BoxRuntime implements java.io.Closeable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Constants
	 * --------------------------------------------------------------------------
	 */

	/***
	 * The default runtime home directory
	 */
	private static final Path					DEFAULT_RUNTIME_HOME	= Paths.get( System.getProperty( "user.home" ), ".boxlang" );

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static BoxRuntime					instance;

	/**
	 * Logger for the runtime
	 */
	private Logger								logger;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant								startTime;

	/**
	 * Debug mode; defaults to false
	 */
	private Boolean								debugMode				= false;

	/**
	 * The runtime context
	 */
	private IBoxContext							runtimeContext;

	/**
	 * The BoxLang configuration class
	 */
	private Configuration						configuration;

	/**
	 * The path to the configuration file to load as overrides
	 */
	private String								configPath;

	/**
	 * The runtime home directory.
	 * This is where the runtime can store logs, modules, configurations, etc.
	 * By default this is the user's home directory + {@code .boxlang}
	 */
	private Path								runtimeHome;

	/**
	 * Runtime global services.
	 * This can be used to store ANY service and make it available to the entire
	 * runtime as a singleton.
	 */
	private ConcurrentHashMap<Key, IService>	globalServices			= new ConcurrentHashMap<>();

	/**
	 * Version information about the runtime: Lazy Loaded
	 */
	private IStruct								versionInfo;

	/**
	 * A set of the allowed file extensions the runtime can execute
	 */
	private Set<String>							runtimeFileExtensions	= new HashSet<>( Arrays.asList( ".bx", ".bxm", ".bxs" ) );

	/**
	 * The runtime class loader
	 */
	private DynamicClassLoader					runtimeLoader;

	/**
	 * The CLI Options that where used to start the runtime, if any.
	 * This can be null if not in a CLI environment
	 */
	private CLIOptions							cliOptions;

	/**
	 * --------------------------------------------------------------------------
	 * Services
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The interceptor service in charge of core runtime events
	 */
	private InterceptorService					interceptorService;

	/**
	 * The function service in charge of all BIFS
	 */
	private FunctionService						functionService;

	/**
	 * The function service in charge of all BIFS
	 */
	private ComponentService					componentService;

	/**
	 * The application service in charge of all applications
	 */
	private ApplicationService					applicationService;

	/**
	 * The async service in charge of all async operations and executors
	 */
	private AsyncService						asyncService;

	/**
	 * The Cache service in charge of all cache managers and providers
	 */
	private CacheService						cacheService;

	/**
	 * The Module service in charge of all modules
	 */
	private ModuleService						moduleService;

	/**
	 * The Scheduler service in charge of all schedulers
	 */
	private SchedulerService					schedulerService;

	/**
	 * The datasource manager which stores a registry of configured datasources.
	 */
	private DatasourceService					dataSourceService;

	/**
	 * The global class locator service
	 */
	private ClassLocator						classLocator;

	/**
	 * Singleton Logging Service that manages all logging events, appenders and more
	 * Please note that it does not adhere to the IService to avoid chicken-and-egg issues
	 */
	private LoggingService						loggingService;

	/**
	 * --------------------------------------------------------------------------
	 * Public Fields
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The timer utility class
	 */
	public static final Timer					timerUtil				= new Timer();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	protected BoxRuntime() {
		// Used for testing ONLY
	}

	/**
	 * Static constructor for the runtime
	 *
	 * @param debugMode   true if the runtime should be started in debug mode
	 * @param configPath  The path to the configuration file to load as overrides
	 * @param runtimeHome The path to the runtime home directory
	 * @param options     The CLI Options that were used to start the runtime or
	 *                    null if not started via CLI
	 */
	private BoxRuntime( Boolean debugMode, String configPath, String runtimeHome, CLIOptions options ) {
		Map<String, String> envVars = System.getenv();

		// Seed the CLI Options (This can be null)
		this.cliOptions = options;

		// Debug mode 1st check via ENV vars
		if ( debugMode == null ) {
			String debugModeEnv = envVars.get( "BOXLANG_DEBUG" );
			if ( debugModeEnv != null ) {
				debugMode = Boolean.parseBoolean( debugModeEnv );
			}
		}

		// Seed if passed, arguements override ENV vars
		if ( debugMode != null ) {
			this.debugMode = debugMode;
		}

		// Seed the runtime home
		if ( runtimeHome != null && runtimeHome.length() > 0 ) {
			this.runtimeHome = Paths.get( runtimeHome );
		} else {
			this.runtimeHome = DEFAULT_RUNTIME_HOME;
		}

		// Seed the override config path, it can be null
		this.configPath	= configPath;

		// Seed startup properties
		this.startTime	= Instant.now();
	}

	/**
	 * Hierarchical loading of the configuration
	 *
	 * @param debugMode  The debug mode to load in the configuration
	 * @param configPath The path to the configuration file to load as overrides,
	 *                   this can be null
	 */
	private void loadConfiguration( Boolean debugMode, String configPath ) {
		ConfigLoader loader = ConfigLoader.getInstance();
		// 1. Load Core Configuration file : resources/config/boxlang.json
		this.configuration = loader.loadCore();

		// Announce so any runtime additions can incorporate settings or override them.
		this.interceptorService.announce(
		    BoxEvent.ON_CONFIGURATION_LOAD,
		    Struct.of( "config", this.configuration ) );

		// 2. Runtime Home Override? Check runtime home for a
		// ${boxlang-home}/config/boxlang.json
		String runtimeHomeConfigPath = Paths.get( getRuntimeHome().toString(), "config", "boxlang.json" ).toString();
		if ( Files.exists( Path.of( runtimeHomeConfigPath ) ) ) {
			IStruct appliedConfig = loader.mergeEnvironmentOverrides( loader.deserializeConfig( runtimeHomeConfigPath ) );
			this.configuration.process( appliedConfig );
			this.interceptorService.announce(
			    BoxEvent.ON_CONFIGURATION_OVERRIDE_LOAD,
			    Struct.of( "config", this.configuration, "configOverride", runtimeHomeConfigPath ) );
		}

		// 3. CLI or ENV Config Path Override, which comes via the arguments
		if ( configPath != null ) {
			IStruct appliedConfig = loader.mergeEnvironmentOverrides( loader.deserializeConfig( configPath ) );
			this.configuration.process( appliedConfig );
			this.interceptorService.announce(
			    BoxEvent.ON_CONFIGURATION_OVERRIDE_LOAD,
			    Struct.of( "config", this.configuration, "configOverride", configPath ) );
		}

		// Finally verify if we overwrote the debugmode in one of the configs above
		if ( debugMode == null ) {
			this.debugMode = this.configuration.debugMode;
			this.loggingService.getRootLogger().debug( "+ DebugMode detected in config, overriding to {}", this.debugMode );
		} else {
			// Make sure our runtime debug mode propagates or the logging service reconfiguration will reset the log levels to the defaults
			this.configuration.debugMode = debugMode;
		}

		// Reconfigure the logging services
		this.loggingService.reconfigure();
	}

	/**
	 * Ensure the BoxLang Home is created and ready for use
	 */
	private void ensureHomeAssets() {
		// Ensure the runtime home directory exists
		FileSystemUtil.createDirectoryIfMissing( this.runtimeHome );

		// Add the following directories to the runtime home if they don't exist
		// Common directories to ensure
		List<String> directories = Arrays.asList( "classes", "config", "global", "logs", "lib", "modules" );
		directories.forEach( dir -> FileSystemUtil.createDirectoryIfMissing( this.runtimeHome.resolve( dir ) ) );

		// Global Directories to ensure
		List<String> globalDirectories = Arrays.asList( "classes", "components", "schedulers" );
		globalDirectories.forEach( dir -> FileSystemUtil.createDirectoryIfMissing( this.runtimeHome.resolve( "global" ).resolve( dir ) ) );

		// Generate a seed file if missing
		Path seedPath = this.runtimeHome.resolve( "config" ).resolve( ".seed" );
		if ( Files.notExists( seedPath ) ) {
			try {
				Files.write( seedPath, EncryptionUtil.generateKeyAsString().getBytes() );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Could not create runtime home seed file at [" + seedPath + "]", e );
			}
		}

		// Copy config/boxlang.json if missing
		FileSystemUtil.copyResourceToPath(
		    "/config/boxlang.json",
		    this.runtimeHome.resolve( "config" ).resolve( "boxlang.json" ),
		    false
		);
		// Always copy version.properties from META-INF
		FileSystemUtil.copyResourceToPath(
		    "/META-INF/boxlang/version.properties",
		    this.runtimeHome.resolve( "version.properties" ),
		    true
		);
		// Copy Maven-related files
		FileSystemUtil.copyResourceToPath(
		    "/maven/maven.md",
		    this.runtimeHome.resolve( "maven.md" ),
		    true
		);
		FileSystemUtil.copyResourceToPath(
		    "/maven/pom.xml",
		    this.runtimeHome.resolve( "pom.xml" ),
		    false
		);
	}

	/**
	 * This is the startup of the runtime called internally by the constructor
	 * once the instance is set in order to avoid circular dependencies.
	 *
	 * Any logic that requires any services or operations to be seeded first, then
	 * go here.
	 *
	 * @param debugMode True if the runtime should be started in debug mode
	 */
	private void startup( Boolean debugMode ) {
		// Internal timer
		timerUtil.start( "runtime-startup" );

		// Startup the Logging Service: Unique as it's not an IService
		this.loggingService = LoggingService.getInstance( this ).configureBasic( debugMode );

		// Startup basic logging
		// Here is where LogBack looks via ServiceLoader for a `Configurator` class
		// Which in our case is our {@link LoggingConfigurator} class.
		// We can now log the startup
		this.loggingService.getRootLogger().info( "+ Starting up BoxLang Runtime" );

		// Create the Runtime Services
		this.interceptorService	= new InterceptorService( this );
		this.asyncService		= new AsyncService( this );
		this.cacheService		= new CacheService( this );
		this.functionService	= new FunctionService( this );
		this.componentService	= new ComponentService( this );
		this.applicationService	= new ApplicationService( this );
		this.moduleService		= new ModuleService( this );
		this.schedulerService	= new SchedulerService( this );
		this.dataSourceService	= new DatasourceService( this );

		// Initiate the Class Locator Service in charge of doing all the class
		// resolutions
		this.classLocator		= ClassLocator.getInstance( this );

		// Load the configurations and overrides
		loadConfiguration( debugMode, this.configPath );
		// Anythying below might use configuration items

		// Assign the logger now that we have a configuration and directory paths to create new loggers
		this.logger = this.loggingService.getRuntimeLogger();

		// Ensure home assets
		ensureHomeAssets();

		// Load the Dynamic Class Loader for the runtime
		this.runtimeLoader = new DynamicClassLoader(
		    Key.runtime,
		    getConfiguration().getJavaLibraryPaths(),
		    this.getClass().getClassLoader(),
		    true );

		// Announce it to the services
		this.interceptorService.onConfigurationLoad();
		this.asyncService.onConfigurationLoad();
		this.cacheService.onConfigurationLoad();
		this.functionService.onConfigurationLoad();
		this.componentService.onConfigurationLoad();
		this.applicationService.onConfigurationLoad();
		this.moduleService.onConfigurationLoad();
		this.schedulerService.onConfigurationLoad();
		this.dataSourceService.onConfigurationLoad();

		// Seed Mathematical Precision for the runtime
		MathUtil.setHighPrecisionMath( getConfiguration().useHighPrecisionMath );

		// Announce Startup to Services only
		this.asyncService.onStartup();
		this.interceptorService.onStartup();
		this.functionService.onStartup();
		this.componentService.onStartup();
		this.applicationService.onStartup();

		// Create our runtime context that will be the granddaddy of all contexts that
		// execute inside this runtime
		this.runtimeContext = new RuntimeBoxContext();
		// Now startup the modules so we can have a runtime context available to them
		this.moduleService.onStartup();
		// Now the cache service can be started, this allows for modules to register
		// caches
		this.cacheService.onStartup();
		// Now all schedulers can be started, this allows for modules to register
		// schedulers
		this.schedulerService.onStartup();
		// Now the datasource manager can be started, this allows for modules to
		// register datasources
		this.dataSourceService.onStartup();

		// Global Services are now available, start them up
		this.globalServices.values()
		    .parallelStream()
		    .forEach( IService::onStartup );

		// Initialize the Runtime Context for operation.
		// This is done in order to avoid chicken-and-egg issues with modules
		this.runtimeContext.startup();

		// Runtime Started log it
		this.logger.debug(
		    "+ BoxLang Runtime Started at [{}] in [{}]ms",
		    Instant.now(),
		    timerUtil.stopAndGetMillis( "runtime-startup" ) );

		// Announce it baby! Runtime is up
		this.interceptorService.announce(
		    BoxEvent.ON_RUNTIME_START );
	}

	/**
	 * --------------------------------------------------------------------------
	 * getInstance() methods
	 * --------------------------------------------------------------------------
	 * The entry point into the runtime
	 */

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * A very simple variation for NON CLI applications using just the debug mode.
	 *
	 * @param debugMode True if the runtime should be started in debug mode
	 *
	 * @return A BoxRuntime instance
	 *
	 */
	public static BoxRuntime getInstance( Boolean debugMode ) {
		return getInstance( debugMode, null );
	}

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * Another variation for NON-cli applications using just the debug mode and a
	 * config override.
	 *
	 * @param debugMode  True if the runtime should be started in debug mode
	 * @param configPath The path to the configuration file to load as overrides
	 *
	 * @return A BoxRuntime instance
	 *
	 */
	public static BoxRuntime getInstance( Boolean debugMode, String configPath ) {
		return getInstance( debugMode, configPath, DEFAULT_RUNTIME_HOME.toString() );
	}

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * This is for NON-cli applications using just a runtime home directory and
	 * config path.
	 * The debug mode will be identified by ENV or configuration.
	 *
	 * @param configPath  The path to the configuration file to load as overrides
	 * @param runtimeHome The path to the runtime home directory where all the
	 *                    runtime assets are stored
	 *
	 * @return A BoxRuntime instance
	 *
	 */
	public static BoxRuntime getInstance( String configPath, String runtimeHome ) {
		return getInstance( null, configPath, runtimeHome );
	}

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * This method is used exclusively to start a CLI runtime instance.
	 *
	 * @param options The CLI Options that were used to start the runtime or null if
	 *                not started via CLI
	 *
	 * @return A BoxRuntime instance
	 */
	public static BoxRuntime getInstance( CLIOptions options ) {
		return getInstance( options.debug(), options.configFile(), options.runtimeHome(), options );
	}

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * This variation doesn't use the CLIOptions as most likely this method is used
	 * by NON-CLI applications.
	 *
	 * @param debugMode   True if the runtime should be started in debug mode
	 * @param configPath  The path to the configuration file to load as overrides
	 * @param runtimeHome The path to the runtime home directory where all the
	 *                    runtime assets are stored
	 *
	 * @return A BoxRuntime instance
	 */
	public static BoxRuntime getInstance( Boolean debugMode, String configPath, String runtimeHome ) {
		return getInstance( debugMode, configPath, runtimeHome, null );
	}

	/**
	 * Get or startup a BoxLang Runtime instance.
	 * <p>
	 * This method uses all the possible parameters to start the runtime.
	 *
	 * @param debugMode   True if the runtime should be started in debug mode
	 * @param configPath  The path to the configuration file to load as overrides
	 * @param runtimeHome The path to the runtime home directory where all the
	 *                    runtime assets are stored
	 * @param options     The CLI Options that were used to start the runtime or
	 *                    null if not started via CLI
	 *
	 * @return A BoxRuntime instance
	 */
	public static BoxRuntime getInstance( Boolean debugMode, String configPath, String runtimeHome, CLIOptions options ) {
		if ( instance == null ) {
			synchronized ( BoxRuntime.class ) {
				if ( instance == null ) {
					instance = new BoxRuntime( debugMode, configPath, runtimeHome, options );
				}
			}
			// We split in order to avoid circular dependencies on the runtime
			instance.startup( debugMode );
		}
		return instance;
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been
	 * started yet.
	 *
	 * @return BoxRuntime instance or null if not started
	 */
	public static BoxRuntime getInstance() {
		return getInstance( ( Boolean ) null );
	}

	/**
	 * Check if the runtime has been started
	 *
	 * @return true if the runtime has been started
	 */
	public static Boolean hasInstance() {
		return instance != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Service Access Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the global class locator service
	 */
	public ClassLocator getClassLocator() {
		return this.classLocator;
	}

	/**
	 * Get the async service
	 *
	 * @return {@link AsyncService} or null if the runtime has not started
	 */
	public AsyncService getAsyncService() {
		return asyncService;
	}

	/**
	 * Get the cache service
	 *
	 * @return {@link CacheService} or null if the runtime has not started
	 */
	public CacheService getCacheService() {
		return cacheService;
	}

	/**
	 * Get the scheduler service
	 *
	 * @return {@link SchedulerService} or null if the runtime has not started
	 */
	public SchedulerService getSchedulerService() {
		return schedulerService;
	}

	/**
	 * Get the function service
	 *
	 * @return {@link FunctionService} or null if the runtime has not started
	 */
	public FunctionService getFunctionService() {
		return functionService;
	}

	/**
	 * Get the component service
	 *
	 * @return {@link ComponentService} or null if the runtime has not started
	 */
	public ComponentService getComponentService() {
		return componentService;
	}

	/**
	 * Get the interceptor service
	 *
	 * @return {@link InterceptorService} or null if the runtime has not started
	 */
	public InterceptorService getInterceptorService() {
		return interceptorService;
	}

	/**
	 * Get the application service
	 *
	 * @return {@link ApplicationService} or null if the runtime has not started
	 */
	public ApplicationService getApplicationService() {
		return applicationService;
	}

	/**
	 * Get the module service
	 *
	 * @return {@link ModuleService} or null if the runtime has not started
	 */
	public ModuleService getModuleService() {
		return moduleService;
	}

	/**
	 * Get the datasource manager for this runtime.
	 *
	 * @return {@link DatasourceService} or null if the runtime has not started
	 */
	public DatasourceService getDataSourceService() {
		return dataSourceService;
	}

	/**
	 * Get the logging service
	 */
	public LoggingService getLoggingService() {
		return loggingService;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get runtime class loader
	 *
	 * @return {@link DynamicClassLoader} or null if the runtime has not started
	 */
	public DynamicClassLoader getRuntimeLoader() {
		return instance.runtimeLoader;
	}

	/**
	 * Get the runtime file extensions registered in the runtime
	 *
	 * @return A set of file extensions
	 */
	public Set<String> getRuntimeFileExtensions() {
		return instance.runtimeFileExtensions;
	}

	/**
	 * Register new file extensions with the runtime
	 *
	 * @param extensions A list of extensions to incorporate into the runtime
	 *
	 */
	public void registerFileExtensions( String... extensions ) {
		instance.runtimeFileExtensions.addAll( Arrays.asList( extensions ) );
	}

	/**
	 * Get the runtime context
	 *
	 * @return The runtime context
	 */
	public IBoxContext getRuntimeContext() {
		return this.runtimeContext;
	}

	/**
	 * Get the configuration
	 *
	 * @return {@link Configuration} or null if the runtime has not started
	 */
	public Configuration getConfiguration() {
		return instance.configuration;
	}

	/**
	 * Get the start time of the runtime
	 *
	 * @return the runtime start time, or null if not started
	 */
	public Instant getStartTime() {
		return instance.startTime;
	}

	/**
	 * Get the runtime home directory
	 *
	 * @return the runtime home directory, or null if not started
	 */
	public Path getRuntimeHome() {
		return instance.runtimeHome;
	}

	/**
	 * Verifies if the runtime is in debug mode
	 *
	 * @return true if the runtime is in debug mode, or null if not started
	 */
	public Boolean inDebugMode() {
		return instance.debugMode;
	}

	/**
	 * Get the CLI Options that were used to start the runtime. This can be null if
	 * not started via CLI
	 *
	 * @return The CLI Options or null if not started via CLI
	 */
	public CLIOptions getCliOptions() {
		return instance.cliOptions;
	}

	/**
	 * Check if the runtime is in CLI mode or not
	 *
	 * @return true if in CLI mode, false otherwise
	 */
	public boolean inCLIMode() {
		return instance.cliOptions != null;
	}

	/**
	 * Check if the runtime is in jar mode or not
	 *
	 * @return true if in jar mode, false otherwise
	 */
	public boolean inJarMode() {
		return BoxRuntime.class.getResource( "BoxRuntime.class" ).getProtocol().equals( "jar" );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data short-hand for
	 * {@link #getInterceptorService()}.announce()
	 *
	 * @param state The Key state to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		getInterceptorService().announce( state, data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data short-hand for
	 * {@link #getInterceptorService()}.announce()
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public void announce( String state, IStruct data ) {
		getInterceptorService().announce( state, data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data short-hand for
	 * {@link #getInterceptorService()}.announce()
	 *
	 * @param state The Key state to announce
	 * @param data  The data to announce
	 */
	public void announce( BoxEvent state, IStruct data ) {
		getInterceptorService().announce( state, data );
	}

	/**
	 * Shut down the runtime gracefully
	 */
	public synchronized void shutdown() {
		shutdown( false );
	}

	/**
	 * Closeable interface method
	 */
	public void close() {
		shutdown();
	}

	/**
	 * Shut down the runtime with the option to force it
	 *
	 * @force If true, forces the shutdown of the runtime, nothing will be
	 *        gracefully shutdown
	 */
	public synchronized void shutdown( Boolean force ) {
		if ( instance == null ) {
			return;
		}
		instance.logger.debug( "Shutting down BoxLang Runtime..." );

		// Announce it globally!
		instance.interceptorService.announce(
		    BoxEvent.ON_RUNTIME_SHUTDOWN,
		    Struct.of( "runtime", this, "force", force ) );

		// Shutdown the global services first
		this.globalServices.values()
		    .parallelStream()
		    .forEach( service -> service.onShutdown( force ) );

		// Shutdown the services
		instance.applicationService.onShutdown( force );
		instance.moduleService.onShutdown( force );
		instance.cacheService.onShutdown( force );
		instance.asyncService.onShutdown( force );
		instance.functionService.onShutdown( force );
		instance.componentService.onShutdown( force );
		instance.interceptorService.onShutdown( force );
		instance.schedulerService.onShutdown( force );
		instance.dataSourceService.onShutdown( force );

		// Shutdown logging
		instance.logger.debug( "+ BoxLang Runtime has been shutdown" );
		instance.loggingService.shutdown();

		// Shutdown the runtime
		instance = null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Global Service Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a global service from the runtime
	 *
	 * @param name The name of the service to get
	 *
	 * @return The service or null if not found
	 */
	public IService getGlobalService( Key name ) {
		return this.globalServices.get( name );
	}

	/**
	 * Alias to use strings
	 */
	public IService getGlobalService( String name ) {
		return getGlobalService( Key.of( name ) );
	}

	/**
	 * Has a global service been set
	 *
	 * @param name The name of the service to check
	 *
	 * @return true if the service exists
	 */
	public boolean hasGlobalService( Key name ) {
		return this.globalServices.containsKey( name );
	}

	/**
	 * Alias to use strings
	 */
	public boolean hasGlobalService( String name ) {
		return hasGlobalService( Key.of( name ) );
	}

	/**
	 * Put a global service into the runtime
	 * If a service for this key was already set, return the original value.
	 *
	 * @param name    The name of the service to set
	 * @param service The service to set
	 */
	public IService putGlobalService( Key name, IService service ) {
		return this.globalServices.put( name, service );
	}

	/***
	 * Alias to use strings
	 */
	public IService putGlobalService( String name, IService service ) {
		return putGlobalService( Key.of( name ), service );
	}

	/**
	 * Remove a global service from the runtime
	 *
	 * @param name The name of the service to remove
	 *
	 * @return The service that was removed, or null if it was not found
	 */
	public IService removeGlobalService( Key name ) {
		return this.globalServices.remove( name );
	}

	/**
	 * Alias to use strings
	 */
	public IService removeGlobalService( String name ) {
		return removeGlobalService( Key.of( name ) );
	}

	/**
	 * Get the keys of all loaded global services
	 */
	public Key[] getGlobalServiceKeys() {
		return this.globalServices.keySet().toArray( new Key[ 0 ] );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a Struct of version information from the version.properties
	 */
	public IStruct getVersionInfo() {
		// Lazy Load the version info
		if ( this.versionInfo == null ) {
			Properties properties = new Properties();
			try ( InputStream inputStream = BoxRunner.class
			    .getResourceAsStream( "/META-INF/boxlang/version.properties" ) ) {
				properties.load( inputStream );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			this.versionInfo = Struct.fromMap( properties );
			// Generate a hash of the version info as the unique boxlang runtime id
			this.versionInfo.put( "boxlangId", EncryptionUtil.hash( this.versionInfo ) );
		}
		return this.versionInfo;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Template Execution
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Execute a single template in its own context
	 *
	 * @param templatePath The absolute path to the template to execute
	 */
	public void executeTemplate( String templatePath ) {
		executeTemplate( templatePath, this.runtimeContext, null );
	}

	/**
	 * Execute a single template in its own context
	 *
	 * @param templatePath The absolute path to the template to execute
	 * @param args         The arguments to pass to the template
	 */
	public void executeTemplate( String templatePath, String[] args ) {
		executeTemplate( templatePath, this.runtimeContext, args );
	}

	/**
	 * Execute a single template in an existing context.
	 * This can be a template or a class accoding to its extension
	 * <p>
	 * If it's a template the args will be stored in the request scope
	 * If it's a class the args will be passed to the main method
	 * <p>
	 *
	 * @param templatePath The absolute path to the template to execute
	 * @param context      The context to execute the template in
	 */
	public void executeTemplate( String templatePath, IBoxContext context ) {
		executeTemplate( templatePath, context, null );
	}

	/**
	 * Execute a single template in an existing context.
	 * This can be a template or a class accoding to its extension
	 * <p>
	 * If it's a template the args will be stored in the request scope
	 * If it's a class the args will be passed to the main method
	 * <p>
	 *
	 * @param templatePath The absolute path to the template to execute
	 * @param context      The context to execute the template in
	 * @param args         The arguments to pass to the template
	 */
	public void executeTemplate( String templatePath, IBoxContext context, String[] args ) {
		// If the templatePath is a .cfs, .cfm then use the loadTemplateAbsolute, if
		// it's a .cfc, .bx then use the loadClass
		if ( StringUtils.endsWithAny( templatePath, ".cfc", ".bx" ) ) {
			// Load the class
			Class<IBoxRunnable> targetClass = RunnableLoader.getInstance().loadClass(
			    ResolvedFilePath.of( Paths.get( templatePath ) ),
			    context );
			executeClass( targetClass, templatePath, context, args );
		} else {
			// Load the template
			BoxTemplate targetTemplate = RunnableLoader.getInstance().loadTemplateRelative(
			    context,
			    templatePath,
			    false );
			executeTemplate( targetTemplate, templatePath, context );
		}
	}

	/**
	 * Execute a single template in an existing context using a {@see URL} of the
	 * template to execution
	 *
	 * @param templateURL A URL location to execution
	 * @param context     The context to execute the template in
	 *
	 */
	public void executeTemplate( URL templateURL, IBoxContext context ) {
		String path;
		try {
			path = Path.of( templateURL.toURI() ).toAbsolutePath().toString();
		} catch ( URISyntaxException e ) {
			throw new MissingIncludeException( "Invalid template path to execute.", "", templateURL.toString(), e );
		}
		executeTemplate( path, context, null );
	}

	/**
	 * Execute a single template in its own context using a {@see URL} of the
	 * template to execution
	 *
	 * @param templateURL A URL location to execution
	 *
	 */
	public void executeTemplate( URL templateURL ) {
		executeTemplate( templateURL, this.runtimeContext );
	}

	/**
	 * Execute a single template in its own context using an already-loaded template
	 * runnable
	 *
	 * @param template A template to execute
	 *
	 */
	public void executeTemplate( BoxTemplate template ) {
		executeTemplate( template, this.runtimeContext );
	}

	/**
	 * Execute a target module main method
	 *
	 * @param module The module to execute
	 * @param args   The arguments to pass to the module
	 *
	 * @throws BoxRuntimeException if the module does not exist
	 * @throws BoxRuntimeException If the module is not executable, meaning it
	 *                             doesn't have a main method
	 */
	public void executeModule( String module, String[] args ) {
		// Verify module exists or throw an exception
		Key moduleName = Key.of( module );
		if ( !getModuleService().hasModule( moduleName ) ) {
			throw new BoxRuntimeException( "Can't execute module [" + module + "] as it does not exist." );
		}
		// Execute it
		ScriptingRequestBoxContext scriptingContext = new ScriptingRequestBoxContext( getRuntimeContext() );
		try {
			RequestBoxContext.setCurrent( scriptingContext );
			getModuleService()
			    .getModuleRecord( moduleName )
			    .execute( scriptingContext, args );
		} finally {
			RequestBoxContext.removeCurrent();
			scriptingContext.shutdown();
		}
	}

	/**
	 * Execute a single class by executing it's main method, else throw an exception
	 *
	 * @param targetClass  The class to execute
	 * @param templatePath The path to the template
	 * @param context      The context to execute the class in
	 * @param args         The array of arguments to pass to the main method
	 */
	public void executeClass( Class<IBoxRunnable> targetClass, String templatePath, IBoxContext context, String[] args ) {
		IBoxContext				scriptingContext	= ensureRequestTypeContext( context, Paths.get( templatePath ).toUri() );
		BaseApplicationListener	listener			= scriptingContext.getParentOfType( RequestBoxContext.class )
		    .getApplicationListener();
		Throwable				errorToHandle		= null;
		IClassRunnable			target				= ( IClassRunnable ) DynamicObject.of( targetClass )
		    .invokeConstructor( scriptingContext )
		    .getTargetInstance();

		// Does it have a main method?
		if ( target.getThisScope().containsKey( Key.main ) ) {
			RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			// Fire!!!
			try {
				boolean result = listener.onRequestStart( scriptingContext, new Object[] { templatePath } );
				if ( result ) {
					// Not sure onClassRequest() works here since we already have the class loaded
					target.dereferenceAndInvoke( scriptingContext, Key.main, new Object[] { Array.fromArray( args ) },
					    false );
				}
			} catch ( AbortException e ) {
				try {
					listener.onAbort( scriptingContext, new Object[] { templatePath } );
				} catch ( Throwable ae ) {
					// Opps, an error while handling onAbort
					errorToHandle = ae;
				}
				scriptingContext.flushBuffer( true );
				if ( e.getCause() != null ) {
					// This will always be an instance of CustomException
					throw ( RuntimeException ) e.getCause();
				}
			} catch ( MissingIncludeException e ) {
				try {
					// Not sure this is reachable since this method is probably only reached if the
					// template existed. But just in case.
					if ( !listener.onMissingTemplate( scriptingContext, new Object[] { e.getMissingFileName() } ) ) {
						errorToHandle = e;
					}
				} catch ( Throwable t ) {
					// Opps, an error while handling the missing template error
					errorToHandle = t;
				}
			} catch ( Exception e ) {
				errorToHandle = e;
			} finally {
				try {
					listener.onRequestEnd( scriptingContext, new Object[] { templatePath } );
				} catch ( Throwable e ) {
					// Opps, an error while handling onRequestEnd
					errorToHandle = e;
				}
				scriptingContext.flushBuffer( false );

				if ( errorToHandle != null ) {
					// Log it
					instance.getLoggingService().getExceptionLogger().error( errorToHandle.getMessage(), errorToHandle );

					try {
						if ( !listener.onError( scriptingContext, new Object[] { errorToHandle, "" } ) ) {
							throw errorToHandle;
						}
						// This is a failsafe in case the onError blows up.
					} catch ( Throwable t ) {
						errorToHandle.printStackTrace();
						ExceptionUtil.throwException( t );
					}
				}
				scriptingContext.flushBuffer( false );
				RequestBoxContext.removeCurrent();
				Thread.currentThread().setContextClassLoader( oldClassLoader );
			}
		} else {
			throw new BoxRuntimeException(
			    "Class [" + targetClass.getName() + "] does not have a main method to execute." );
		}

	}

	/**
	 * Execute a single template in an existing context using an already-loaded
	 * template runnable
	 *
	 * @param template A template to execute
	 * @param context  The context to execute the template in
	 */
	public void executeTemplate( BoxTemplate template, IBoxContext context ) {
		executeTemplate( template, template.getRunnablePath().absolutePath().toString(), context );
	}

	/**
	 * Execute a single template in an existing context using an already-loaded
	 * template runnable
	 *
	 * @param template A template to execute
	 * @param context  The context to execute the template in
	 */
	public void executeTemplate( BoxTemplate template, String templatePath, IBoxContext context ) {
		if ( instance.logger.isDebugEnabled() ) {
			instance.logger.debug( "Executing template [{}]", template.getRunnablePath() );
		}

		IBoxContext scriptingContext;
		scriptingContext = ensureRequestTypeContext( context, FileSystemUtil.createFileUri( templatePath ) );
		boolean					shutdownContext	= context != scriptingContext;
		BaseApplicationListener	listener		= scriptingContext
		    .getParentOfType( RequestBoxContext.class )
		    .getApplicationListener();
		Throwable				errorToHandle	= null;
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			boolean result = listener.onRequestStart( scriptingContext, new Object[] { templatePath } );
			if ( result ) {
				// Not sure if this works in this concext, since it's expected to include the
				// template itself, but in this case our template is a loaded
				// class, not a path which may or may not be a file and may or may not be inside
				// of a mapping allowing a relative path to it
				// listener.onRequest( scriptingContext, new Object[] { templatePath } );
				template.invoke( scriptingContext );
			}
		} catch ( AbortException e ) {
			try {
				listener.onAbort( scriptingContext, new Object[] { templatePath } );
			} catch ( Throwable ae ) {
				// Opps, an error while handling onAbort
				errorToHandle = ae;
			}
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} catch ( MissingIncludeException e ) {
			try {
				// Not sure this is reachable since this method is probably only reached if the
				// template existed. But just in case.
				if ( !listener.onMissingTemplate( scriptingContext, new Object[] { e.getMissingFileName() } ) ) {
					errorToHandle = e;
				}
			} catch ( Throwable t ) {
				// Opps, an error while handling the missing template error
				errorToHandle = t;
			}
		} catch ( Exception e ) {
			errorToHandle = e;
		} finally {

			try {
				listener.onRequestEnd( scriptingContext, new Object[] { templatePath } );
			} catch ( Throwable e ) {
				// Opps, an error while handling onRequestEnd
				errorToHandle = e;
			}
			scriptingContext.flushBuffer( false );

			if ( errorToHandle != null ) {
				// Log it
				instance.getLoggingService().getExceptionLogger().error( errorToHandle.getMessage(), errorToHandle );

				try {
					if ( !listener.onError( scriptingContext, new Object[] { errorToHandle, "" } ) ) {
						throw errorToHandle;
					}
					// This is a failsafe in case the onError blows up.
				} catch ( Throwable t ) {
					errorToHandle.printStackTrace();
					ExceptionUtil.throwException( t );
				}
			}
			scriptingContext.flushBuffer( false );
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
			if ( shutdownContext ) {
				scriptingContext.getRequestContext().shutdown();
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Statement + Source Executions
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Execute a single statement
	 *
	 * @param source A string of the statement to execute
	 *
	 */
	public Object executeStatement( String source ) {
		return executeStatement( source, this.runtimeContext );
	}

	/**
	 * Execute a single statement in a specific context
	 *
	 * @param source  A string of the statement to execute
	 * @param context The context to execute the source in
	 * @param type    The type of source to execute
	 *
	 */
	public Object executeStatement( String source, IBoxContext context, BoxSourceType type ) {
		BoxScript scriptRunnable = RunnableLoader.getInstance().loadStatement( context, source, type );
		return executeStatement( scriptRunnable, context );
	}

	/**
	 * Execute a single statement in a specific context
	 *
	 * @param source  A string of the statement to execute
	 * @param context The context to execute the source in
	 *
	 */
	public Object executeStatement( String source, IBoxContext context ) {
		return executeStatement( source, context, BoxSourceType.BOXSCRIPT );
	}

	/**
	 * Execute a single statement in a specific context
	 *
	 * @param scriptRunnable A runnable boxlang script to execute
	 * @param context        The context to execute the source in
	 *
	 * @return The result of the execution, if any, or null
	 */
	public Object executeStatement( BoxScript scriptRunnable, IBoxContext context ) {
		IBoxContext	scriptingContext	= ensureRequestTypeContext( context );
		boolean		shutdownContext		= context != scriptingContext;
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// Fire!!!
			return scriptRunnable.invoke( scriptingContext );
		} catch ( AbortException e ) {
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
			return null;
		} finally {
			scriptingContext.flushBuffer( false );
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
			if ( shutdownContext ) {
				scriptingContext.getRequestContext().shutdown();
			}
		}

	}

	/**
	 * Execute a source string
	 *
	 * @param source A string of source to execute
	 *
	 * @return The result of the execution
	 */
	public Object executeSource( String source ) {
		return executeSource( source, this.runtimeContext );
	}

	/**
	 * Execute a source string
	 *
	 * @param source  A string of source to execute
	 * @param context The context to execute the source in
	 *
	 * @return The result of the execution
	 */
	public Object executeSource( String source, IBoxContext context ) {
		return executeSource( source, context, BoxSourceType.BOXSCRIPT );
	}

	/**
	 * Execute a source strings from an input stream
	 *
	 * @param sourceStream An input stream to read
	 *
	 * @return The result of the execution
	 */
	public Object executeSource( InputStream sourceStream ) {
		return executeSource( sourceStream, this.runtimeContext );
	}

	/**
	 * Execute a source string
	 *
	 * @param source  A string of source to execute
	 * @param context The context to execute the source in
	 * @param type    The type of source to execute. Available options are: BOXSCRIPT, BOXTEMPLATE, CFSCRIPT, CFTEMPLATE
	 *
	 * @return The result of the execution, if any, or null
	 */
	public Object executeSource( String source, IBoxContext context, BoxSourceType type ) {
		// Debugging Timers
		/* timerUtil.start( "execute-" + source.hashCode() ); */
		IBoxContext	scriptingContext	= ensureRequestTypeContext( context );
		BoxScript	scriptRunnable		= RunnableLoader.getInstance().loadSource( scriptingContext, source, type );
		Object		results				= null;

		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// Fire!!!
			results = scriptRunnable.invoke( scriptingContext );
		} catch ( AbortException e ) {
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} finally {
			scriptingContext.flushBuffer( false );
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}

		return results;
	}

	/**
	 * This is our REPL (Read-Eval-Print-Loop) method that allows for interactive
	 * BoxLang execution
	 *
	 * @param sourceStream An input stream to read
	 * @param context      The context to execute the source in
	 */
	public Object executeSource( InputStream sourceStream, IBoxContext context ) {
		IBoxContext		scriptingContext	= ensureRequestTypeContext( context );
		BufferedReader	reader				= new BufferedReader( new InputStreamReader( sourceStream ) );
		String			source;
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			Boolean quiet = reader.ready();
			if ( !quiet ) {
				System.out.println( "██████   ██████  ██   ██ ██       █████  ███    ██  ██████ " );
				System.out.println( "██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██      " );
				System.out.println( "██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███" );
				System.out.println( "██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██" );
				System.out.println( "██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████ " );
				System.out.println( "" );
				System.out.println( "Enter an expression, then hit enter" );
				System.out.println( "Press Ctrl-C to exit" );
				System.out.println( "" );
				System.out.print( "BoxLang> " );
			}
			while ( ( source = reader.readLine() ) != null ) {

				if ( source.toLowerCase().equals( "exit" ) || source.toLowerCase().equals( "quit" ) ) {
					break;
				}

				try {

					BoxScript	scriptRunnable		= RunnableLoader.getInstance().loadStatement( context, source,
					    BoxSourceType.BOXSCRIPT );

					// Fire!!!
					Object		result				= scriptRunnable.invoke( scriptingContext );
					boolean		hadBufferContent	= scriptingContext.getBuffer().length() > 0;
					scriptingContext.flushBuffer( false );
					if ( !hadBufferContent && result != null ) {
						CastAttempt<String> stringAttempt = StringCaster.attempt( result );
						if ( stringAttempt.wasSuccessful() ) {
							System.out.println( stringAttempt.get() );
						} else {
							// check if it's a java array
							if ( result.getClass().isArray() ) {
								result = Array.fromArray( ( Object[] ) result );
							}

							System.out.println( result );
						}
					} else {
						System.out.println();
					}
				} catch ( AbortException e ) {
					scriptingContext.flushBuffer( true );
					if ( e.getCause() != null ) {
						System.out.println( "Abort: " + e.getCause().getMessage() );
					}
				} catch ( Exception e ) {
					e.printStackTrace();
				}

				if ( !quiet ) {
					System.out.print( "BoxLang> " );
				}
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading source stream", e );
		} finally {
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}

		return null;
	}

	/**
	 * Print the transpiled Java code for a given source file.
	 * This is useful for debugging and understanding how the BoxLang code is
	 * transpiled to Java.
	 *
	 * @param filePath The path to the source file
	 */
	public void printTranspiledJavaCode( String filePath ) {
		IBoxpiler		boxpiler	= RunnableLoader.getInstance().getBoxpiler();
		ClassInfo		classInfo	= ClassInfo.forTemplate(
		    ResolvedFilePath.of( "", "", Path.of( filePath ).getParent().toString(), filePath ),
		    BoxSourceType.BOXSCRIPT,
		    boxpiler
		);
		ParsingResult	result		= boxpiler.parseOrFail( Path.of( filePath ).toFile() );
		boxpiler.printTranspiledCode( result, classInfo, System.out );
	}

	/**
	 * Parse source string and print AST as JSON
	 *
	 * @param source A string of source to parse and print AST for
	 *
	 */
	public void printSourceAST( String source ) {
		ParsingResult result = RunnableLoader.getInstance().getBoxpiler().parseOrFail( source, BoxSourceType.BOXSCRIPT, false );
		System.out.println( result.getRoot().toJSON() );
	}

	/**
	 * Parse a script file
	 * This is a passthrough to the Boxpiler parse method
	 *
	 * @param file source file to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( File file ) {
		return new Parser().parse( file );
	}

	/**
	 * Parse a script string
	 * This is a passthrough to the Boxpiler parse method
	 *
	 * @param code       source of the expression to parse
	 * @param sourceType the type of source to parse. Available options are: BOXSCRIPT, BOXTEMPLATE, CFSCRIPT, CFTEMPLATE
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxExpression
	 */
	public ParsingResult parse( String code, BoxSourceType sourceType ) {
		try {
			return new Parser().parse( code, sourceType );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing code", e );
		}
	}

	/**
	 * Parse a script string expression
	 * This is a passthrough to the Boxpiler parse method
	 * Defaults to BoxScript source type. If you want to parse another type, use the other method
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxExpression
	 */
	public ParsingResult parse( String code ) {
		return parse( code, BoxSourceType.BOXSCRIPT );
	}

	/**
	 * Check the given context to see if it has a variables scope. If not, create a
	 * new scripting
	 * context that has a variables scope and return that with the original context
	 * as the parent.
	 *
	 * @param context The context to check
	 *
	 * @return The context with a variables scope
	 */
	private IBoxContext ensureRequestTypeContext( IBoxContext context ) {
		return ensureRequestTypeContext( context, null );
	}

	/**
	 * Check the given context to see if it has a request scope. If not, create a
	 * new scripting
	 * context that has a request scope and return that with the original context as
	 * the parent.
	 *
	 * @param context  The context to check
	 * @param template The template to use for the context if needed
	 *
	 * @return The context with a request scope
	 */
	private IBoxContext ensureRequestTypeContext( IBoxContext context, URI template ) {
		if ( context.getParentOfType( RequestBoxContext.class ) != null ) {
			return context;
		} else if ( template != null ) {
			return new ScriptingRequestBoxContext( context, template );
		} else {
			return new ScriptingRequestBoxContext( context );
		}
	}

}
