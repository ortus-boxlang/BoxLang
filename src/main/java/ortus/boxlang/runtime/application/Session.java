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
import java.util.concurrent.atomic.AtomicBoolean;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
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
	 */
	public static final String	ID_CONCATENATOR		= "_";

	/**
	 * The URL token format
	 * MOVE TO COMPAT MODULE
	 */
	public static final String	URL_TOKEN_FORMAT	= "CFID=%s";

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
	private final AtomicBoolean	isNew				= new AtomicBoolean( true );

	/**
	 * The application name linked to
	 */
	private Key					applicationName		= null;

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

		DateTime	timeNow	= new DateTime();
		String		bxid	= this.applicationName + ID_CONCATENATOR + ID;

		// Initialize the session scope
		this.sessionScope.put( Key.jsessionID, ID.getName() );
		this.sessionScope.put( Key.sessionId, this.applicationName + ID_CONCATENATOR + ID );
		this.sessionScope.put( Key.timeCreated, timeNow );
		this.sessionScope.put( Key.lastVisit, timeNow );

		// Move these to the COMPAT module
		this.sessionScope.put( Key.urlToken, String.format( URL_TOKEN_FORMAT, bxid ) );
		this.sessionScope.put( Key.cfid, ID.getName() );
		this.sessionScope.put( Key.cftoken, 0 );

		// Announce it's creation
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce( BoxEvent.ON_SESSION_CREATED, Struct.of(
		        Key.session, this
		    ) );
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
		if ( !this.isNew.get() ) {
			updateLastVisit();
			return this;
		}

		// Announce it's start
		BaseApplicationListener listener = context.getParentOfType( RequestBoxContext.class ).getApplicationListener();
		listener.onSessionStart( context, new Object[] { this.ID } );
		this.isNew.set( false );

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
	 * Shutdown the session
	 *
	 * @param listener The listener that is shutting down the session
	 */
	public void shutdown( BaseApplicationListener listener ) {
		// Announce it's destruction
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce( BoxEvent.ON_SESSION_DESTROYED, Struct.of(
		        Key.session, this
		    ) );

		// Any buffer output in this context will be discarded
		listener.onSessionEnd(
		    new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() ),
		    new Object[] { sessionScope, listener.getApplication().getApplicationScope() }
		);

		// Clear the session scope
		this.sessionScope.clear();
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
		    "isNew", this.isNew.get(),
		    Key.applicationName, this.applicationName
		);
	}

}
