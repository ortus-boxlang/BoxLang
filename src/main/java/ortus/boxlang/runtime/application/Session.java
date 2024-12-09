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

import java.io.Serializable;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I represent a Session. This will be stored in a BoxLang cache
 * and will be used to store session data.
 */
public class Session implements Serializable {

	/**
	 * The concatenator for session IDs
	 * We leverage the `:` as it's a standard in many distributed caches like Redis and Couchbase to denote namespaces
	 */
	public static final String	ID_CONCATENATOR	= ":";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The unique ID of this session
	 */
	private Key					ID;

	/**
	 * The scope for this session
	 */
	private SessionScope		sessionScope;

	/**
	 * Flag for when session has been started
	 */
	private boolean				isNew			= true;

	/**
	 * The application name linked to
	 */
	private Key					applicationName	= null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param ID          The ID of this session
	 * @param application The application that this session belongs to
	 */
	public Session( Key ID, Application application ) {
		this.ID					= ID;
		this.applicationName	= application.getName();
		this.sessionScope		= new SessionScope();
		DateTime timeNow = new DateTime();

		// Initialize the session scope
		this.sessionScope.put( Key.jsessionID, ID.getName() );
		this.sessionScope.put( Key.sessionId, this.applicationName + ID_CONCATENATOR + ID );
		this.sessionScope.put( Key.timeCreated, timeNow );
		this.sessionScope.put( Key.lastVisit, timeNow );

		// Announce it's creation
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce( BoxEvent.ON_SESSION_CREATED, Struct.of(
		        Key.session, this
		    ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a cache key for a session
	 *
	 * @param id              The ID of the session
	 * @param applicationName The application name
	 *
	 * @return The cache key
	 */
	public static String buildCacheKey( Key id, Key applicationName ) {
		return applicationName + ID_CONCATENATOR + id;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Session Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Update the last visit time
	 */
	public void updateLastVisit() {
		this.sessionScope.put( Key.lastVisit, new DateTime() );
	}

	/**
	 * Start the session if not already started. If it is already started, just update the last visit time
	 *
	 * @param context The context that is starting the session
	 *
	 * @return This session
	 */
	public Session start( IBoxContext context ) {
		// If the session has started, just return it and update it's last visit time
		if ( !isNew ) {
			updateLastVisit();
			return this;
		}
		synchronized ( this ) {
			// Double check lock
			if ( !isNew ) {
				return this;
			}
			// Set this now so onSessionStart() won't go all recursive doom into itself
			isNew = false;

			try {
				// Announce it's start
				BaseApplicationListener listener = context.getRequestContext().getApplicationListener();
				listener.onSessionStart( context, new Object[] { this.ID } );
			} catch ( Exception e ) {
				// If startup errored, flag the session as not intialized. The next thread can try again.
				// An error in your onSessionStart() will mean you can never get passed it, but I think that's actually desired as the
				// app likely relies on a complete and successful session start.
				isNew = true;
			}
		}
		return this;
	}

	/**
	 * Get the ID of this session
	 *
	 * @return The ID
	 */
	public Key getID() {
		return this.ID;
	}

	/**
	 * Get the scope for this session
	 *
	 * @return The scope
	 */
	public SessionScope getSessionScope() {
		return this.sessionScope;
	}

	/**
	 * Get the application that this session belongs to
	 *
	 * @return The application name
	 */
	public Key getApplicationName() {
		return this.applicationName;
	}

	/**
	 * Get the cache key for this session
	 */
	public String getCacheKey() {
		return buildCacheKey( this.ID, this.applicationName );
	}

	/**
	 * Shutdown the session
	 *
	 * @param listener The listener that is shutting down the session
	 */
	public void shutdown( BaseApplicationListener listener ) {
		// Announce it's destruction to the runtime first
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce( BoxEvent.ON_SESSION_DESTROYED, Struct.of(
		        Key.session, this
		    ) );

		// Any buffer output in this context will be discarded
		// Create a temp request context with an application context with our application listener.
		// This will allow the application scope to be available as well as all settings from the original Application.bx
		listener.onSessionEnd(
		    new ScriptingRequestBoxContext(
		        new ApplicationBoxContext(
		            BoxRuntime.getInstance().getRuntimeContext(),
		            listener.getApplication()
		        ),
		        listener
		    ),
		    new Object[] {
		        // If the session scope is null, just pass an empty struct
		        sessionScope != null ? sessionScope : Struct.of(),
		        // Pass the application scope
		        listener.getApplication().getApplicationScope()
		    }
		);

		// Clear the session scope
		if ( this.sessionScope != null ) {
			this.sessionScope.clear();
		}
		this.sessionScope = null;
	}

	/**
	 * Convert to string
	 */
	@Override
	public String toString() {
		return "Session{" +
		    "ID=" + ID +
		    ", sessionScope=" + sessionScope +
		    ", isNew=" + isNew +
		    ", applicationName=" + applicationName +
		    '}';
	}

	/**
	 * Get the session state as a struct representation
	 */
	public IStruct asStruct() {
		return Struct.of(
		    Key.id, this.ID,
		    Key.scope, this.sessionScope,
		    "isNew", isNew,
		    Key.applicationName, this.applicationName
		);
	}

}
