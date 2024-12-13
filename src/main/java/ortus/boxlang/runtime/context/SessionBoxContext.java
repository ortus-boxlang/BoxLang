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
package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.application.Session;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This class represents the context of a session in the BoxLang runtime
 * It is a child of the RuntimeBoxContext and has access to the session scope
 * and the parent context and its scopes
 */
public class SessionBoxContext extends BaseBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
	 */
	protected Session	session;

	/**
	 * The session scope for this application
	 */
	protected IScope	sessionScope;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param session The session for this context
	 */
	public SessionBoxContext( Session session ) {
		super( null );
		this.session		= session;
		this.sessionScope	= session.getSessionScope();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the session for this context
	 *
	 * @return The session for this context
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * Update the session for this context with a new session
	 *
	 * @param session The new session to use
	 *
	 * @return This context
	 */
	public SessionBoxContext updateSession( Session session ) {
		this.session		= session;
		this.sessionScope	= session.getSessionScope();
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( SessionScope.name, sessionScope );
		return scopes;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		// There are no near-by scopes in the session context. Everything is global here.

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		if ( key.equals( sessionScope.getName() ) ) {
			return new ScopeSearchResult( sessionScope, sessionScope, key, true );
		}

		return parent.scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		// Check the scopes I know about
		if ( name.equals( sessionScope.getName() ) ) {
			return sessionScope;
		}

		return parent.getScope( name );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {

		if ( shallow ) {
			return null;
		}

		// The RuntimeBoxContext has no "nearby" scopes
		return getScope( name );
	}

}
