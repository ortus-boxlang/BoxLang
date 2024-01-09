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
package ortus.boxlang.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.runtime.config.ConfigLoader;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RuntimeBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.interceptors.ASTCapture;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.Timer;

/**
 * Represents the top level runtime container for box lang. Config, global scopes, mappings, threadpools, etc all go here.
 * All threads, requests, invocations, etc share this.
 */
public class BoxRuntime {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register all the core runtime events here
	 */
	private static final Key[]	RUNTIME_EVENTS	= Key.of(
	    "onRuntimeStart",
	    "onRuntimeShutdown",
	    "onRuntimeConfigurationLoad",
	    "preTemplateInvoke",
	    "postTemplateInvoke",
	    "preFunctionInvoke",
	    "postFunctionInvoke",
	    "onScopeCreation",
	    "onConfigurationLoad",
	    "onConfigurationOverrideLoad",
	    "onParse"
	);

	/**
	 * Singleton instance
	 */
	private static BoxRuntime	instance;

	/**
	 * Logger for the runtime
	 */
	private Logger				logger;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant				startTime;

	/**
	 * Debug mode; defaults to false
	 */
	private Boolean				debugMode		= false;

	/**
	 * The runtime context
	 */
	private IBoxContext			runtimeContext;

	/**
	 * The BoxLang configuration class
	 */
	private Configuration		configuration;

	/**
	 * The path to the configuration file to load as overrides
	 */
	private String				configPath;

	/**
	 * --------------------------------------------------------------------------
	 * Services
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The interceptor service in charge of core runtime events
	 */
	private InterceptorService	interceptorService;

	/**
	 * The function service in charge of all BIFS
	 */
	private FunctionService		functionService;

	private ApplicationService	applicationService;

	/**
	 * The async service in charge of all async operations and executors
	 */
	private AsyncService		asyncService;

	/**
	 * The Cache service in charge of all cache managers and providers
	 */
	private CacheService		cacheService;

	/**
	 * --------------------------------------------------------------------------
	 * Public Fields
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The timer utility class
	 */
	public static final Timer	timerUtil		= new Timer();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	protected BoxRuntime() {
		// Used for testing ONLY
	}

	/**
	 * Static constructor
	 *
	 * @param debugMode  true if the runtime should be started in debug mode
	 * @param configPath The path to the configuration file to load as overrides
	 */
	private BoxRuntime( Boolean debugMode, String configPath ) {
		// Internal timer
		timerUtil.start( "runtime-startup" );

		// Seed if passed
		if ( debugMode != null ) {
			this.debugMode = debugMode;
		}

		// Startup basic logging
		LoggingConfigurator.configure( this.debugMode );
		this.logger = LoggerFactory.getLogger( BoxRuntime.class );

		// We can now log the startup
		this.logger.atInfo().log( "+ Starting up BoxLang Runtime" );

		// Seed startup properties
		this.startTime			= Instant.now();

		// Create the Runtime Services
		this.interceptorService	= new InterceptorService( this, RUNTIME_EVENTS );
		this.asyncService		= new AsyncService( this );
		this.cacheService		= new CacheService( this );
		this.functionService	= new FunctionService( this );
		this.applicationService	= new ApplicationService( this );
		this.configPath			= configPath;

	}

	/**
	 * Hierarchical loading of the configuration
	 *
	 * @param configPath The path to the configuration file to load as overrides
	 */
	private void loadConfiguration( Boolean debugMode, String configPath ) {
		// Load Core Configuration file
		this.configuration = ConfigLoader.getInstance().loadCore();
		this.interceptorService.announce( "onConfigurationLoad", Struct.of( "config", this.configuration ) );

		// User-HOME Override? Check user home for a ${user.home}/.boxlang/config.json
		String userHomeConfigPath = Paths.get( System.getProperty( "user.home" ) )
		    .resolve( ".boxlang" )
		    .resolve( "config.json" )
		    .toString();
		if ( new File( userHomeConfigPath ).exists() ) {
			this.configuration.process( ConfigLoader.getInstance().deserializeConfig( userHomeConfigPath ) );
			this.interceptorService.announce( "onConfigurationOverrideLoad", Struct.of( "config", this.configuration, "configOverride", userHomeConfigPath ) );
		}

		// CLI or ENV Config Path Override?
		if ( configPath != null ) {
			this.configuration.process( ConfigLoader.getInstance().deserializeConfig( configPath ) );
			this.interceptorService.announce( "onConfigurationOverrideLoad", Struct.of( "config", this.configuration, "configOverride", configPath ) );
		}

		// Config DebugMode Override if null
		if ( debugMode == null ) {
			this.debugMode = this.configuration.debugMode;
			// Reconfigure the logging if enabled
			if ( this.debugMode ) {
				LoggingConfigurator.configure( debugMode );
			}
			this.logger.atInfo().log( "+ DebugMode detected in config, overriding to {}", this.debugMode );
		}
		if ( this.debugMode ) {
			this.interceptorService.register(
			    DynamicObject.of( new ASTCapture( false, true ) ),
			    Key.of( "onParse" )
			);
		}
	}

	/**
	 * This is the startup of the runtime called internally by the constructor
	 * once the instance is set in order to avoid circular dependencies.
	 *
	 * Any logic that requires any services or operations to be seeded first, then go here.
	 */
	private void startup() {
		// Load the configurations and overrides
		loadConfiguration( debugMode, configPath );

		// Announce Startup to Services only
		this.interceptorService.onStartup();
		this.asyncService.onStartup();
		this.cacheService.onStartup();
		this.functionService.onStartup();
		this.applicationService.onStartup();

		// Create our runtime context that will be the granddaddy of all contexts that execute inside this runtime
		this.runtimeContext = new RuntimeBoxContext();

		// Runtime Started log it
		this.logger.atInfo().log(
		    "+ BoxLang Runtime Started at [{}] in [{}]",
		    Instant.now(),
		    timerUtil.stop( "runtime-startup" )
		);

		// Announce it baby! Runtime is up
		this.interceptorService.announce( "onRuntimeStart", new Struct() );
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @param debugMode true if the runtime should be started in debug mode
	 *
	 * @return BoxRuntime
	 *
	 */
	public static synchronized BoxRuntime getInstance( Boolean debugMode ) {
		return getInstance( debugMode, null );
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @param debugMode  true if the runtime should be started in debug mode
	 * @param configPath The path to the configuration file to load as overrides
	 *
	 * @return BoxRuntime
	 *
	 */
	public static synchronized BoxRuntime getInstance( Boolean debugMode, String configPath ) {
		if ( instance == null ) {
			instance = new BoxRuntime( debugMode, configPath );
			// We split in order to avoid circular dependencies on the runtime
			instance.startup();
		}
		return instance;
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @return BoxRuntime
	 */
	public static BoxRuntime getInstance() {
		return getInstance( null );
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
	 * Get the function service
	 *
	 * @return {@link FunctionService} or null if the runtime has not started
	 */
	public FunctionService getFunctionService() {
		return functionService;
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
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

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
	 * Verifies if the runtime is in debug mode
	 *
	 * @return true if the runtime is in debug mode, or null if not started
	 */
	public Boolean inDebugMode() {
		return instance.debugMode;
	}

	/**
	 * Announce an event with the provided {@link Struct} of data short-hand for {@link #getInterceptorService()}.announce()
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public void announce( String state, Struct data ) {
		getInterceptorService().announce( Key.of( state ), data );
	}

	/**
	 * Shut down the runtime
	 */
	public synchronized void shutdown() {
		instance.logger.atInfo().log( "Shutting down BoxLang Runtime..." );

		// Announce it globally!
		instance.interceptorService.announce( "onRuntimeShutdown", new Struct() );

		// Shutdown the services
		instance.asyncService.onShutdown();
		instance.cacheService.onShutdown();
		instance.functionService.onShutdown();
		instance.interceptorService.onShutdown();

		// Shutdown logging
		instance.logger.atInfo().log( "+ BoxLang Runtime has been shutdown" );

		// Shutdown the runtime
		instance = null;
	}

	/**
	 * Execute a single template in its own context
	 *
	 * @param templatePath The absolute path to the template to execute
	 *
	 */
	public void executeTemplate( String templatePath ) {
		executeTemplate( templatePath, runtimeContext );
	}

	/**
	 * Execute a single template in an existing context
	 *
	 * @param templatePath The absolute path to the template to execute
	 * @param context      The context to execute the template in
	 *
	 */
	public void executeTemplate( String templatePath, IBoxContext context ) {
		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BoxTemplate targetTemplate = RunnableLoader.getInstance().loadTemplateAbsolute( runtimeContext, Paths.get( templatePath ) );
		executeTemplate( targetTemplate, context );
	}

	/**
	 * Execute a single template in an existing context using a {@see URL} of the template to execution
	 *
	 * @param templateURL A URL location to execution
	 * @param context     The context to execute the template in
	 *
	 */
	public void executeTemplate( URL templateURL, IBoxContext context ) {
		String path;
		try {
			path = ( new File( templateURL.toURI() ) ).getPath();
		} catch ( URISyntaxException e ) {
			throw new MissingIncludeException( "Invalid template path to execute.", "", templateURL.toString(), e );
		}
		executeTemplate( path, context );
	}

	/**
	 * Execute a single template in its own context using a {@see URL} of the template to execution
	 *
	 * @param templateURL A URL location to execution
	 *
	 */
	public void executeTemplate( URL templateURL ) {
		executeTemplate( templateURL, runtimeContext );
	}

	/**
	 * Execute a single template in its own context using an already-loaded template runnable
	 *
	 * @param template A template to execute
	 *
	 */
	public void executeTemplate( BoxTemplate template ) {
		executeTemplate( template, runtimeContext );
	}

	/**
	 * Execute a single template in an existing context using an already-loaded template runnable
	 *
	 * @param template A template to execute
	 * @param context  The context to execute the template in
	 *
	 */
	public void executeTemplate( BoxTemplate template, IBoxContext context ) {
		// Debugging Timers
		timerUtil.start( "execute-" + template.hashCode() );
		instance.logger.atDebug().log( "Executing template [{}]", template.getRunnablePath() );

		IBoxContext scriptingContext = ensureContextWithVariables( context );

		try {
			// Fire!!!
			template.invoke( scriptingContext );
		} finally {
			scriptingContext.flushBuffer( false );

			// Debugging Timer
			instance.logger.atDebug().log(
			    "Executed template [{}] in [{}] ms",
			    template.getRunnablePath(),
			    timerUtil.stopAndGetMillis( "execute-" + template.hashCode() )
			);
		}
	}

	/**
	 * Execute a single statement
	 *
	 * @param source A string of the statement to execute
	 *
	 */
	public Object executeStatement( String source ) {
		return executeStatement( source, runtimeContext );
	}

	/**
	 * Execute a single statement in a specific context
	 *
	 * @param source  A string of the statement to execute
	 * @param context The context to execute the source in
	 *
	 */
	public Object executeStatement( String source, IBoxContext context ) {
		BoxScript scriptRunnable = RunnableLoader.getInstance().loadStatement( source );
		// Debugging Timers
		timerUtil.start( "execute-" + source.hashCode() );
		instance.logger.atDebug().log( "Executing source " );

		IBoxContext scriptingContext = ensureContextWithVariables( context );
		try {
			// Fire!!!
			return scriptRunnable.invoke( scriptingContext );
		} finally {
			scriptingContext.flushBuffer( false );
			// Debugging Timer
			instance.logger.atDebug().log(
			    "Executed source  [{}] ms",
			    timerUtil.stopAndGetMillis( "execute-" + source.hashCode() )
			);
		}

	}

	/**
	 * Execute a source string
	 *
	 * @param source A string of source to execute
	 *
	 */
	public void executeSource( String source ) {
		executeSource( source, runtimeContext );
	}

	/**
	 * Execute a source string
	 *
	 * @param source  A string of source to execute
	 * @param context The context to execute the source in
	 *
	 */
	public void executeSource( String source, IBoxContext context ) {
		BoxScript scriptRunnable = RunnableLoader.getInstance().loadSource( source );
		// Debugging Timers
		timerUtil.start( "execute-" + source.hashCode() );
		instance.logger.atDebug().log( "Executing source " );

		IBoxContext scriptingContext = ensureContextWithVariables( context );
		try {
			// Fire!!!
			scriptRunnable.invoke( scriptingContext );
		} finally {
			scriptingContext.flushBuffer( false );

			// Debugging Timer
			instance.logger.atDebug().log(
			    "Executed source  [{}] ms",
			    timerUtil.stopAndGetMillis( "execute-" + source.hashCode() )
			);
		}
	}

	/**
	 * Parse source string and print AST as JSON
	 *
	 * @param source A string of source to parse and print AST for
	 *
	 */
	public void printSourceAST( String source ) {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result;
		try {
			result = parser.parse( source, BoxScriptType.CFSCRIPT );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}

		result.getIssues().forEach( System.out::println );
		if ( !result.isCorrect() ) {
			throw new BoxRuntimeException( "Error compiling source. " + result.getIssues().get( 0 ).toString() );
		}

		System.out.println( result.getRoot().toJSON().toString() );
	}

	/**
	 * Execute a source strings from an input stream
	 *
	 * @param sourceStream An input stream to read
	 */
	public void executeSource( InputStream sourceStream ) {
		executeSource( sourceStream, runtimeContext );
	}

	/**
	 * Execute a source strings from an input stream
	 *
	 * @param sourceStream An input stream to read
	 * @param context      The context to execute the source in
	 */
	public void executeSource( InputStream sourceStream, IBoxContext context ) {
		IBoxContext		scriptingContext	= ensureContextWithVariables( context );
		BufferedReader	reader				= new BufferedReader( new InputStreamReader( sourceStream ) );
		String			source;

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

				// Debugging Timers
				timerUtil.start( "execute-" + source.hashCode() );
				instance.logger.atDebug().log( "Executing source " );

				try {

					BoxScript	scriptRunnable	= RunnableLoader.getInstance().loadStatement( source );

					// Fire!!!
					Object		result			= scriptRunnable.invoke( scriptingContext );
					System.out.println( result );
				} catch ( Exception e ) {
					e.printStackTrace();
				} finally {
					// Debugging Timer
					instance.logger.atDebug().log(
					    "Executed source  [{}] ms",
					    timerUtil.stopAndGetMillis( "execute-" + source.hashCode() )
					);
				}

				if ( !quiet ) {
					System.out.print( "BoxLang> " );
				}
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading source stream", e );
		}

	}

	/**
	 * Check the given context to see if it has a variables scope. If not, create a new scripting
	 * context that has a variables scope and return that with the original context as the parent.
	 *
	 * @param context The context to check
	 *
	 * @return The context with a variables scope
	 */
	private IBoxContext ensureContextWithVariables( IBoxContext context ) {
		try {
			context.getScopeNearby( VariablesScope.name );
			return context;
		} catch ( ScopeNotFoundException e ) {
			return new ScriptingBoxContext( context );
		}
	}

	public IBoxContext getRuntimeContext() {
		return runtimeContext;
	}
}
