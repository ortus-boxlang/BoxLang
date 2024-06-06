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

import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents an Application in BoxLang
 */
public class ApplicationBoxContext extends BaseBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The actual BoxLang application
	 */
	protected Application	application;

	/**
	 * The application scope for this application
	 */
	protected IScope		applicationScope;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param application The application to bind to this context
	 */
	public ApplicationBoxContext( Application application ) {
		super( null );
		updateApplication( application );
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( ApplicationScope.name, applicationScope );
		return scopes;
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		// There are no near-by scopes in the application context. Everything is global here.

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		if ( key.equals( applicationScope.getName() ) ) {
			return new ScopeSearchResult( applicationScope, applicationScope, key, true );
		}
		return parent.scopeFind( key, defaultScope );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @return The requested scope
	 */
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		// Check the scopes I know about
		if ( name.equals( applicationScope.getName() ) ) {
			return applicationScope;
		}

		return parent.getScope( name );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {

		if ( shallow ) {
			return null;
		}

		// The RuntimeBoxContext has no "nearby" scopes
		return getScope( name );
	}

	public Application getApplication() {
		return application;
	}

	/**
	 * Set the application details into this context
	 *
	 * @param application The application
	 */
	public void updateApplication( Application application ) {
		this.application		= application;
		this.applicationScope	= application.getApplicationScope();
		this.applicationScope.put( Key.applicationName, application.getName() );
	}

}
