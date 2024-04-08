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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

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
	 * Bit that determines if the application is running or not. Accesible by multiple threads
	 */
	private volatile boolean	started				= false;

	/**
	 * The scope for this application
	 */
	private ApplicationScope	applicationScope;

	/**
	 * The sessions for this application
	 * TODO: timeout sessions
	 */
	private Map<Key, Session>	sessions			= new ConcurrentHashMap<>();

	/**
	 * The listener that started this application (used for stopping it)
	 */
	private ApplicationListener	startingListener	= null;

	/**
	 * Logger
	 */
	private static final Logger	logger				= LoggerFactory.getLogger( Application.class );

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
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Start the application if not already started
	 *
	 * @param context The context
	 */
	public Application start( IBoxContext context ) {
		if ( started ) {
			return this;
		}
		synchronized ( this ) {
			if ( started ) {
				return this;
			}
			this.startTime	= Instant.now();
			this.started	= true;

			// Announce it
			BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationStart, Struct.of(
			    "application", this
			) );

			startingListener = context.getParentOfType( RequestBoxContext.class ).getApplicationListener();
			if ( startingListener != null ) {
				startingListener.onApplicationStart( context, new Object[] {} );
			}
		}
		logger.atDebug().log( "Application.start() - {}", this.name );
		return this;
	}

	/**
	 * Get a session by ID for this application, creating if neccessary if not found
	 *
	 * @param ID The ID of the session
	 *
	 * @return The session
	 */
	public Session getSession( Key ID ) {
		return sessions.computeIfAbsent( ID, key -> new Session( ID, this ) );
	}

	/**
	 * How many sessions are currently tracked
	 */
	public int getSessionCount() {
		return hasStarted() ? sessions.size() : 0;
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
	 * Check if the application is running
	 *
	 * @return True if the application is running
	 */
	public boolean hasStarted() {
		return this.started;
	}

	/**
	 * Restart this application
	 */
	public synchronized void restart( IBoxContext context ) {
		// Announce it
		BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationRestart, Struct.of(
		    "application", this
		) );
		shutdown();
		start( context );
	}

	/**
	 * Shutdown this application
	 */
	public synchronized void shutdown() {
		// If the app has already been shutdown, don't do it again4
		if ( !hasStarted() ) {
			logger.atDebug().log( "Can't shutdown application [{}] as it's already shutdown", this.name );
			return;
		}

		// Announce it
		BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationEnd, Struct.of(
		    "application", this
		) );

		// Shutdown all sessions
		if ( sessions != null ) {
			sessions.values().parallelStream().forEach( Session::shutdown );
		}

		if ( startingListener != null ) {
			// Any buffer output in this context will be discarded
			startingListener.onApplicationEnd( new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() ),
			    new Object[] { applicationScope } );
		}

		// Clear out the data
		this.sessions			= null;
		this.applicationScope	= null;
		this.startTime			= null;
		this.started			= false;

		logger.atDebug().log( "Application.shutdown() - {}", this.name );
	}
}
