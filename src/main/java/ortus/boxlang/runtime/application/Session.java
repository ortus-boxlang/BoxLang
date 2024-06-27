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

import java.util.concurrent.atomic.AtomicBoolean;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.types.DateTime;

/**
 * I represent a Session
 */
public class Session {

	/**
	 * The concatenator for session IDs
	 */
	public static final String		ID_CONCATENATOR		= "_";

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The unique ID of this session
	 */
	private Key						ID;

	/**
	 * The scope for this session
	 */
	private SessionScope			sessionScope;

	/**
	 * Flag for when session has been started
	 */
	private final AtomicBoolean		isNew				= new AtomicBoolean( true );

	/**
	 * The listener that started this session (used for stopping it)
	 */
	private BaseApplicationListener	startingListener	= null;

	/**
	 * The application that this session belongs to
	 */
	private Application				application			= null;

	/**
	 * The URL token format
	 */
	private static final String		URL_TOKEN_FORMAT	= "CFID=%s";

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
		this.ID				= ID;
		this.application	= application;
		sessionScope		= new SessionScope();
		DateTime	timeNow	= new DateTime();
		String		cfid	= application.getName() + ID_CONCATENATOR + ID;
		sessionScope.put( Key.cfid, ID.getName() );
		sessionScope.put( Key.cftoken, 0 );
		sessionScope.put( Key.sessionId, application.getName() + ID_CONCATENATOR + ID );
		sessionScope.put( Key.timeCreated, timeNow );
		sessionScope.put( Key.lastVisit, timeNow );
		sessionScope.put( Key.urlToken, String.format( URL_TOKEN_FORMAT, cfid ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Session Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Start the session if not already started
	 *
	 * @param context The context
	 */
	public Session start( IBoxContext context ) {
		if ( !this.isNew.get() ) {
			return this;
		}
		this.startingListener = context.getParentOfType( RequestBoxContext.class ).getApplicationListener();
		this.startingListener.onSessionStart( context, new Object[] { this.ID } );
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
	 * @return The application
	 */
	public Application getApplication() {
		return this.application;
	}

	/**
	 * Shutdown the session
	 */
	public void shutdown() {
		// Any buffer output in this context will be discarded
		if ( this.startingListener != null ) {
			this.startingListener.onSessionEnd(
			    new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() ),
			    new Object[] { sessionScope, application.getApplicationScope() } );
		}
		this.sessionScope = null;
	}
}
