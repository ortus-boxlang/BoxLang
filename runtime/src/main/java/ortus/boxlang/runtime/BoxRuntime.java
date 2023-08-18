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
import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.TemplateBoxContext;

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
	 * Singleton instance
	 */
	private static BoxRuntime	instance;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant				startTime;

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
	 * Get the start time of the runtime, null if not started
	 *
	 * @return the runtime start time
	 */
	public static Optional<Instant> getStartTime() {
		return ( instance == null ) ? Optional.empty() : Optional.ofNullable( instance.startTime );
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
	 * Start up the runtime
	 *
	 * @return The runtime instance
	 */
	public static synchronized BoxRuntime startup() {
		if ( instance != null ) {
			return getInstance();
		}
		System.out.println( "Starting up BoxLang Runtime" );

		instance			= new BoxRuntime();
		instance.startTime	= Instant.now();

		System.out.println( "BoxLang Runtime Started" );
		return instance;
	}

	/**
	 * Shut down the runtime
	 */
	public static synchronized void shutdown() {
		System.out.println( "Shutting down BoxLang Runtime" );
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

		// Build out the execution context for this execution and bind it to the incoming template
		IBoxContext context = new TemplateBoxContext( templatePath );

		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary
		BoxPiler.parse( templatePath ).invoke( context );
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
