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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;

/**
 * I represent a Session
 */
public class Session {

	/**
	 * The unique ID of this session
	 */
	private Key				ID;

	/**
	 * The scope for this session
	 */
	private SessionScope	sessionScope;

	/**
	 * Flag for when session has been started
	 */
	private boolean			isNew	= true;

	/**
	 * Constructor
	 */
	public Session( Key ID ) {
		this.ID			= ID;
		sessionScope	= new SessionScope();
	}

	/**
	 * Start the session if not already started
	 * 
	 * @param context The context
	 */
	public Session start( IBoxContext context ) {
		if ( !isNew ) {
			return this;
		}
		context.getParentOfType( RequestBoxContext.class ).getApplicationListener().onSessionStart( context, new Object[] {} );
		this.isNew = false;
		return this;
	}

	/**
	 * Get the ID of this session
	 * 
	 * @return The ID
	 */
	public Key getID() {
		return ID;
	}

	/**
	 * Get the scope for this session
	 * 
	 * @return The scope
	 */
	public SessionScope getSessionScope() {
		return sessionScope;
	}

	public void shutdown() {
		// TODO: onSessionEnd
	}
}