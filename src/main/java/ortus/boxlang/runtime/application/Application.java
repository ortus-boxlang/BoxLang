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

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * I represent an Applications
 */
public class Application {

	/**
	 * The name of this application. Unique per runtime
	 */
	private Key					name;

	/**
	 * The scope for this application
	 */
	private ApplicationScope	applicationScope;

	/**
	 * The sessions for this application
	 * TODO: timeout sessions
	 */
	private Map<Key, Session>	sessions	= new HashMap<Key, Session>();

	/**
	 * Constructor
	 */
	public Application( Key name ) {
		// TODO: onApplicationStart
		this.name			= name;
		applicationScope	= new ApplicationScope();
	}

	/**
	 * Get an application by name, creating if neccessary
	 * 
	 * @param name The name of the application
	 * 
	 * @return The application
	 */
	public Session getSession( Key ID ) {
		// TODO: Application settings
		Session thisSession = sessions.get( ID );
		if ( thisSession == null ) {
			// Consider lock on session name so onsessionstarts don't block across sessinos
			synchronized ( sessions ) {
				thisSession = sessions.get( ID );
				if ( thisSession == null ) {
					// TODO: onSessionStart
					thisSession = new Session( ID );
					sessions.put( name, thisSession );
				}
			}
		}
		return thisSession;
	}

	/**
	 * Get the scope for this application
	 * 
	 * @return The scope
	 */
	public ApplicationScope getApplicationScope() {
		return applicationScope;
	}

	/**
	 * Get the name of this application
	 * 
	 * @return The name
	 */
	public Key getName() {
		return name;
	}

	/**
	 * Shutdown this application
	 */
	public void shutdown() {
		// TODO: onApplicationEnd

		// loop over sessiona and shutdown
		for ( Session session : sessions.values() ) {
			session.shutdown();
		}
	}
}