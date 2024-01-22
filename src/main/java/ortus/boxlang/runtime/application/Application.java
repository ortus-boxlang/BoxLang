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
package ortus.boxlang.runtime.application;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * I represent an Application in BoxLang
 */
public class Application {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The name of this application. Unique per runtime
	 */
	private Key					name;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant				startTime;

	/**
	 * The scope for this application
	 */
	private ApplicationScope	applicationScope;

	/**
	 * The sessions for this application
	 * TODO: timeout sessions
	 */
	private Map<Key, Session>	sessions	= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The name of the application
	 */
	public Application( Key name ) {
		this.name				= name;
		this.applicationScope	= new ApplicationScope();
		this.startTime			= Instant.now();

		startup();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a session by ID for this application, creating if neccessary
	 *
	 * @param ID The ID of the session
	 *
	 * @return The session
	 */
	public Session getSession( Key ID ) {
		return sessions.computeIfAbsent( ID, key -> new Session( ID ) );
	}

	/**
	 * Get the scope for this application
	 *
	 * @return The scope
	 */
	public ApplicationScope getApplicationScope() {
		return this.applicationScope;
	}

	/**
	 * Get the name of this application
	 *
	 * @return The name
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * Get the start time of the application
	 *
	 * @return the application start time, or null if not started
	 */
	public Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * Startup this application
	 */
	public void startup() {
		// TODO: onApplicationStart
	}

	/**
	 * Shutdown this application
	 */
	public void shutdown() {
		// TODO: onApplicationEnd

		sessions.values().parallelStream().forEach( Session::shutdown );
	}
}
