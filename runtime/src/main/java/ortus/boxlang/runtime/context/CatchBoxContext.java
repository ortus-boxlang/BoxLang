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

import java.util.Map;

import ortus.boxlang.runtime.scopes.*;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of a template execution in BoxLang
 */
public class CatchBoxContext implements IBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private IBoxContext	parent;

	/**
	 * The variables scope
	 */
	private IScope		variablesScope;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param templatePath The template that this execution context is bound to
	 * @param parent       The parent context
	 */
	public CatchBoxContext( IBoxContext parent, Key exceptionKey, Throwable exception ) {
		this.parent			= parent;
		this.variablesScope	= new ScopeWrapper(
		    parent.getScopeLocal( VariablesScope.name ),
		    Map.of( exceptionKey, exception )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A local lookup is used for the closest context to the executing code
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 * @throws KeyNotFoundException If the key was not found in any scope
	 */
	public Object scopeFindLocal( Key key ) {

		// In Variables scope? (thread-safe lookup and get)
		Object result = variablesScope.get( key );
		// Handle full null support
		if ( result != null ) {
			return result;
		}

		return scopeFind( key );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindLocal(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a CFC.
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 * @throws KeyNotFoundException If the key was not found in any scope
	 */
	public Object scopeFind( Key key ) {

		if ( parent != null ) {
			return parent.scopeFindLocal( key );
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	/**
	 * Returns the parent box context. Null if none.
	 *
	 * @return The parent box context. Null if none.
	 */
	public IBoxContext getParent() {
		return this.parent;
	}

	/**
	 * Verifies if a parent context is attached to this context
	 *
	 * @return True if a parent context is attached to this context, else false
	 */
	public Boolean hasParent() {
		return this.parent != null;
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @return The requested scope
	 */
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		if ( parent != null ) {
			return parent.getScopeLocal( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	public IScope getScopeLocal( Key name ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( VariablesScope.name ) ) {
			return variablesScope;
		}

		return getScope( name );

	}

}
