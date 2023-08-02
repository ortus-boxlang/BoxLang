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

import java.time.Instant;
import java.util.Optional;

import ortus.boxlang.runtime.Bootstrap.RuntimeOptions;

/**
 * Represents the top level runtime container for box lang. Config, global scopes, mappings, threadpools, etc all go here.
 * All threads, requests, invocations, etc share this.
 */
public class BoxRuntime {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static BoxRuntime instance;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant startTime;

	/**
	 * The runtime options
	 */
	private RuntimeOptions options;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static constructor
	 */
	private BoxRuntime() {
		// Initialization code, if needed
	}

	/**
	 * Get the start time of the runtime, null if not started
	 *
	 * @return the runtime start time
	 */
	public static Optional<Instant> getStartTime() {
		return ( instance == null ) ? Optional.empty() : Optional.ofNullable( instance.startTime );
	}

	/**
	 * Get the runtime options
	 * 
	 * @return the runtime options
	 */
	public static Optional<RuntimeOptions> getOptions() {
		return ( instance == null ) ? Optional.empty() : Optional.ofNullable( instance.options );
	}

	/**
	 * Get the singleton instance.
	 *
	 * @return BoxRuntime
	 *
	 * @throws RuntimeException if the runtime has not been started
	 */
	public static BoxRuntime getInstance() {
		return instance;
	}

	/**
	 * Check if the runtime has been started
	 *
	 * @return true if the runtime has been started
	 */
	public static Boolean isStarted() {
		return instance != null;
	}

	/**
	 * Start up the runtime
	 *
	 * @param options The runtime options
	 *
	 * @return The runtime instance
	 */
	public static synchronized BoxRuntime startup( RuntimeOptions options ) {
		if ( instance != null ) {
			return getInstance();
		}
		System.out.println( "Starting up BoxLang Runtime" );

		instance			= new BoxRuntime();
		instance.startTime	= Instant.now();
		instance.options	= options;

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

}
