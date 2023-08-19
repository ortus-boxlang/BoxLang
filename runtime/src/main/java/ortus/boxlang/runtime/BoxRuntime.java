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
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.logging.SLF4JConfigurator;
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
	 * The timer utility class
	 */
	private static final Timer	timerUtil	= new Timer();

	/**
	 * Singleton instance
	 */
	private static BoxRuntime	instance;

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( BoxRuntime.class );

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant				startTime;

	/**
	 * Debug mode
	 */
	private Boolean				debugMode	= false;

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
	 * Check if the runtime has been started
	 *
	 * @return true if the runtime has been started
	 */
	public static Boolean hasStarted() {
		return instance != null;
	}

	/**
	 * Get the start time of the runtime, null if not started
	 *
	 * @return the runtime start time
	 */
	public static Instant getStartTime() {
		return ( hasStarted() ? instance.startTime : null );
	}

	/**
	 * Verifies if the runtime is in debug mode
	 *
	 * @return true if the runtime is in debug mode
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
	public static synchronized BoxRuntime startup( Boolean debugMode ) {
		// If we have already started, just return the instance
		if ( instance != null ) {
			return getInstance();
		}
		// Internal timer
		timerUtil.start( "startup" );

		// Startup logging
		SLF4JConfigurator.configure( debugMode );

		// We can now log the startup
		logger.atInfo().log( "+ Starting up BoxLang Runtime" + ( debugMode ? " in debug mode" : "" ) );

		// Create singleton instance
		instance			= new BoxRuntime();
		instance.startTime	= Instant.now();
		instance.debugMode	= debugMode;

		// Runtime Started
		logger.atInfo().log(
		        "+ BoxLang Runtime Started at [{}] in [{}]",
		        Instant.now(),
		        timerUtil.stop( "startup" )
		);
		return instance;
	}

	/**
	 * Shut down the runtime
	 */
	public static synchronized void shutdown() {
		logger.atInfo().log( "Shutting down BoxLang Runtime" );
		instance = null;
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
		IBoxContext context = new TemplateBoxContext( templatePath );

		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BoxPiler.parse( templatePath ).invoke( context );

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
	public static void executeTemplate( URL templatePath ) throws Throwable {

		// Build out the execution context for this execution and bind it to the incoming template
		IBoxContext context = new TemplateBoxContext( templatePath.getPath() );

		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BoxPiler.parse( templatePath.getPath() ).invoke( context );
	}

}
