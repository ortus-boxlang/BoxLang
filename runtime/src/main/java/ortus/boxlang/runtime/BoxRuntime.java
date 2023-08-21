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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.logging.LoggingConfigurator;
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
	private static final String[]	RUNTIME_EVENTS	= {
	        "onRuntimeStart",
	        "onRuntimeShutdown",
	        "onRuntimeConfigurationLoad",
	        "preTemplateInvoke",
	        "postTemplateInvoke"
	};

	/**
	 * The timer utility class
	 */
	private static final Timer		timerUtil		= new Timer();

	/**
	 * Singleton instance
	 */
	private static BoxRuntime		instance;

	/**
	 * Logger
	 */
	private static final Logger		logger			= LoggerFactory.getLogger( BoxRuntime.class );

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant					startTime;

	/**
	 * Debug mode
	 */
	private Boolean					debugMode		= false;

	/**
	 * --------------------------------------------------------------------------
	 * Services
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The interceptor service
	 */
	private InterceptorService		interceptorService;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static constructor
	 */
	private BoxRuntime() {
		// Initialization code, if needed
	}

	/**
	 * Get the singleton instance. This can be null if the runtime has not been started yet.
	 *
	 * @return BoxRuntime
	 *
	 * @throws RuntimeException if the runtime has not been started
	 */
	public static BoxRuntime getInstance() {
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the interceptor service
	 *
	 * @return {@link InterceptorService} or null if the runtime has not started
	 */
	public InterceptorService getInterceptorService() {
		return ( hasStarted() ? instance.interceptorService : null );
	}

	/**
	 * Check if the runtime has been started
	 *
	 * @return true if the runtime has been started
	 */
	public static Boolean hasStarted() {
		return instance != null;
	}

	/**
	 * Get the start time of the runtime
	 *
	 * @return the runtime start time, or null if not started
	 */
	public static Instant getStartTime() {
		return ( hasStarted() ? instance.startTime : null );
	}

	/**
	 * Verifies if the runtime is in debug mode
	 *
	 * @return true if the runtime is in debug mode, or null if not started
	 */
	public static Boolean inDebugMode() {
		return ( hasStarted() ? instance.debugMode : null );
	}

	/**
	 * Start up then BoxLang runtime
	 *
	 * @param debugMode If true, enables debug mode
	 *
	 * @return The runtime instance
	 */
	public static synchronized BoxRuntime startup( Boolean debugMode ) throws RuntimeException {
		// If we have already started, just return the instance
		if ( instance != null ) {
			return getInstance();
		}
		// Internal timer
		timerUtil.start( "startup" );

		// Startup logging
		LoggingConfigurator.configure( debugMode );

		// We can now log the startup
		logger.atInfo().log( "+ Starting up BoxLang Runtime" + ( debugMode ? " in debug mode" : "" ) );

		// Create Runtime Instance
		instance					= new BoxRuntime();
		instance.startTime			= Instant.now();
		instance.debugMode			= debugMode;

		// Create Services
		instance.interceptorService	= InterceptorService.getInstance( RUNTIME_EVENTS );

		// Announce Startup to Services
		InterceptorService.onStartup();

		// Runtime Started
		logger.atInfo().log(
		        "+ BoxLang Runtime Started at [{}] in [{}]",
		        Instant.now(),
		        timerUtil.stop( "startup" )
		);

		// Announce it baby!
		InterceptorService.announce( "onRuntimeStart", new Struct() );

		return instance;
	}

	/**
	 * Shut down the runtime
	 */
	public static synchronized void shutdown() {
		logger.atInfo().log( "Shutting down BoxLang Runtime..." );

		// Announce it globally!
		InterceptorService.announce( "onRuntimeShutdown", new Struct() );

		// Shutdown the services
		InterceptorService.onShutdown();

		// Shutdown the runtime
		instance = null;

		// Shutdown logging
		logger.info( "+ BoxLang Runtime Shutdown" );
	}

	/**
	 * Execute a single template in its own context
	 *
	 * @param templatePath The path to the template to execute
	 *
	 * @throws Throwable if the template cannot be executed
	 */
	public static void executeTemplate( String templatePath ) throws Throwable {
		// Debugging Timers
		timerUtil.start( "execute-" + templatePath.hashCode() );
		logger.atDebug().log( "Executing template [{}]", templatePath );

		// Build out the execution context for this execution and bind it to the incoming template
		IBoxContext		context			= new TemplateBoxContext( templatePath );

		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BaseTemplate	targetTemplate	= BoxPiler.parse( templatePath );

		// Announcements
		Struct			data			= new Struct();
		data.put( "context", context );
		data.put( "template", targetTemplate );
		data.put( "templatePath", templatePath );
		InterceptorService.announce( "preTemplateInvoke", data );

		// Fire!!!
		targetTemplate.invoke( context );

		// Announce
		InterceptorService.announce( "postTemplateInvoke", data );

		// Debugging Timer
		logger.atDebug().log(
		        "Executed template [{}] in [{}] ms",
		        templatePath,
		        timerUtil.stopAndGetMillis( "execute-" + templatePath.hashCode() )
		);
	}

	/**
	 * Execute a single template in its own context using a {@see URL} of the template to execution
	 *
	 * @param templateURL A URL location to execution
	 *
	 * @throws Throwable if the template cannot be executed
	 */
	public static void executeTemplate( URL templateURL ) throws Throwable {
		executeTemplate( templateURL.getPath() );
	}

}
