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
		this( null, application );
	}

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent      The parent context
	 * @param application The application to bind to this context
	 */
	public ApplicationBoxContext( IBoxContext parent, Application application ) {
		super( parent );
		updateApplication( application );
	}

	/**
	 * --------------------------------------------------------------------------
	 * App Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the application linked to this context
	 *
	 * @return The application
	 */
	public Application getApplication() {
		return this.application;
	}

	/**
	 * Get the application scope linked to this context
	 *
	 * @return The application scope
	 */
	public IScope getApplicationScope() {
		return this.applicationScope;
	}

	/**
	 * Set the application details into this context
	 *
	 * @param application The application
	 */
	public void updateApplication( Application application ) {
		this.application		= application;
		this.applicationScope	= application.getApplicationScope();
		try {
			this.applicationScope.put( Key.applicationName, application.getName().getName() );
		} catch ( Throwable e ) {
			System.err.println( "error application scope null, app name is: " + application.getName() );
			// This should never happen, adding debugging for now.
			e.printStackTrace();
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Context Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the visible scopes for this context
	 * This will return the scopes that are visible to this context
	 *
	 * @param scopes  The scopes to add the visible scopes to
	 * @param nearby  If true, only return scopes that are nearby
	 * @param shallow If true, only return the scopes that are directly visible
	 *
	 * @return The scopes that are visible to this context
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( ApplicationScope.name, applicationScope );
		return scopes;
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 * 
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 * 
	 * @return True if the key is visible in the current context, else false
	 */
	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( key.equals( ApplicationScope.name ) ) {
			return true;
		}
		return super.isKeyVisibleScope( key, false, false );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 * @param shallow      If true, only search the current scope
	 *
	 * @return The value of the key if found
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		// There are no near-by scopes in the application context. Everything is global here.

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 *
	 * @return The value of the key if found
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		if ( key.equals( applicationScope.getName() ) ) {
			return new ScopeSearchResult( applicationScope, applicationScope, key, true );
		}
		return parent.scopeFind( key, defaultScope, forAssign );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 */
	@Override
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
	 * @param name    The name of the scope to get
	 * @param shallow If true, only return the scopes that are directly visible
	 *
	 * @return The requested scope
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
