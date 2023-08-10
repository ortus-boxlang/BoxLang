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

import ortus.boxlang.runtime.scopes.*;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * This represents the most basic execution context. It will usually be sub-classed for more specific contexts.
 * TODO: Should we make it abstract?
 */
public class BaseContext {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * A context has a variables scope
	 */
	protected IScope	variablesScope;

	/**
	 * The context has a unique name
	 */
	protected String	name;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context
	 */
	public BaseContext() {
	}

	/**
	 * Creates a new execution context
	 */
	public BaseContext( String name ) {
		this.name = name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name of the context
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the variables scope of the context
	 *
	 * @return The variables scope
	 */
	public IScope getVariablesScope() {
		return this.variablesScope;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 * @throws KeyNotFoundException If the key was not found in any scope
	 */
	public Object scopeFind( Key key ) {

		// In Variables scope?
		if ( getVariablesScope().containsKey( key ) ) {
			return getVariablesScope().get( key );
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		        String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

}
