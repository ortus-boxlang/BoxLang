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

import java.net.URL;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.config.ConfigLoader;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RuntimeBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Struct;
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
	    "onScopeCreation",
	    "onConfigurationLoad"
	);

	/**
	 * The timer utility class
	 */
	private static final Timer	timerUtil		= new Timer();

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
	 * --------------------------------------------------------------------------
	 * Services
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The interceptor service
	 */
	private InterceptorService	interceptorService;

	/**
	 * The function service
	 */
	private FunctionService		functionService;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static constructor
	 *
	 * @param debugMode true if the runtime should be started in debug mode
	 */
	private BoxRuntime( Boolean debugMode ) {
		// Internal timer
		timerUtil.start( "startup" );

		// Startup logging
		LoggingConfigurator.configure( debugMode );
		// Attach logging now that it's configured
		this.logger = LoggerFactory.getLogger( BoxRuntime.class );

		// We can now log the startup
		this.logger.atInfo().log( "+ Starting up BoxLang Runtime" + ( debugMode ? " in debug mode" : " in production mode" ) );

		// Seed startup properties
		this.startTime			= Instant.now();
		this.debugMode			= debugMode;

		// Create Services
		this.interceptorService	= InterceptorService.getInstance( RUNTIME_EVENTS );

		// Load Core Configuration
		this.configuration		= ConfigLoader.getInstance().load();
		interceptorService.announce( "onConfigurationLoad", Struct.of( "config", this.configuration ) );

		// Create our runtime context that will be the granddaddy of all contexts that execute inside this runtime
		this.runtimeContext = new RuntimeBoxContext();

		// Announce Startup to Services only
		interceptorService.onStartup();

		// Runtime Started log it
		this.logger.atInfo().log(
		    "+ BoxLang Runtime Started at [{}] in [{}]",
		    Instant.now(),
		    timerUtil.stop( "startup" )
		);

		// Announce it baby! Runtime is up
		interceptorService.announce( "onRuntimeStart", new Struct() );
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @return BoxRuntime
	 *
	 */
	public static BoxRuntime getInstance( Boolean debugMode ) {
		if ( instance == null ) {
			instance = new BoxRuntime( debugMode );
		}
		return instance;
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @return BoxRuntime
	 */
	public static BoxRuntime getInstance() {
		return getInstance( false );
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
	 * Get the interceptor service
	 *
	 * @return {@link InterceptorService} or null if the runtime has not started
	 */
	public InterceptorService getInterceptorService() {
		return instance.interceptorService;
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
	 * Shut down the runtime
	 */
	public synchronized void shutdown() {
		instance.logger.atInfo().log( "Shutting down BoxLang Runtime..." );

		// Announce it globally!
		interceptorService.announce( "onRuntimeShutdown", new Struct() );

		// Shutdown the services
		interceptorService.onShutdown();

		// Shutdown logging
		instance.logger.info( "+ BoxLang Runtime has been shutdown" );

		// Shutdown the runtime
		instance = null;
	}

	/**
	 * Execute a single template in its own context
	 *
	 * @param templatePath The path to the template to execute
	 *
	 */
	public void executeTemplate( String templatePath ) {
		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BoxTemplate targetTemplate = BoxPiler.parse( templatePath );
		executeTemplate( targetTemplate );
	}

	/**
	 * Execute a single template in its own context using a {@see URL} of the template to execution
	 *
	 * @param templateURL A URL location to execution
	 *
	 */
	public void executeTemplate( URL templateURL ) {
		executeTemplate( templateURL.getPath() );
	}

	/**
	 * Execute a single template in its own context using a {@see URL} of the template to execution
	 *
	 * @param template A template to execute
	 *
	 */
	public void executeTemplate( BoxTemplate template ) {
		// Debugging Timers
		timerUtil.start( "execute-" + template.hashCode() );
		instance.logger.atDebug().log( "Executing template [{}]", template.getRunnablePath() );

		// Build out the execution context for this execution and bind it to the incoming template
		IBoxContext context = new ScriptingBoxContext( runtimeContext );

		// Fire!!!
		template.invoke( context );

		// Debugging Timer
		instance.logger.atDebug().log(
		    "Executed template [{}] in [{}] ms",
		    template.getRunnablePath(),
		    timerUtil.stopAndGetMillis( "execute-" + template.hashCode() )
		);

	}

}
