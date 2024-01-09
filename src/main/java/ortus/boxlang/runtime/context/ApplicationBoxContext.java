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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of the entire BoxLang Runtime. The runtime is persistent once
 * started, and can be used to process one or more "requests" for execution. The "server" scope here is
 * global and will be shared by all requests.
 */
public class ApplicationBoxContext extends BaseBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
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
	 * @param parent The parent context
	 */
	public ApplicationBoxContext( Application application ) {
		super( null );
		this.application		= application;
		this.applicationScope	= application.getApplicationScope();
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

	public void registerUDF( UDF udf ) {
		// This will prolly be unreachable since all executing code will be wrapped by another scope
		applicationScope.put( udf.getName(), udf );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope() {
		// This will prolly be unreachable since all executing code will be wrapped by another scope
		return applicationScope;
	}

	/**
	 * Get the contexual config struct. Each context has a chance to add in config of their
	 * own to the struct, or override existing config with a new struct of their own design.
	 * It depends on whether the context wants its changes to exist for the rest of the entire
	 * request or only for code that executes in the current context and below.
	 * 
	 * @return A struct of configuration
	 */
	public Struct getConfig() {
		// TODO: Add in application config here
		return parent.getConfig();
	}

}
