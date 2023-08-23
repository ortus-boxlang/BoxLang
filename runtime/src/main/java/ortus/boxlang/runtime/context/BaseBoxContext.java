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

import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.scopes.*;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of a template execution in BoxLang
 */
public class BaseBoxContext implements IBoxContext {

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
	protected IBoxContext parent;

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param template The template that this execution context is bound to
	 * @param parent   The parent context
	 */
	public BaseBoxContext( IBoxContext parent ) {
		this.parent = parent;
	}

	/**
	 * Creates a new execution context with a bounded execution template
	 *
	 * @param templatePath The template that this execution context is bound to
	 */
	public BaseBoxContext() {
		this( null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

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
	 * Invoke a function call such as foo(). Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] args ) {
		// Check for registered BIF

		ScopeSearchResult result = scopeFindNearby( name, null );
		return null;
		// TODO: UDFs!
		// if( result.value() instanceof ??? ) {
		// Invoke BIF
		// }
	}

	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		throw new UnsupportedOperationException( "Unimplemented method 'getScope'" );
	}

	@Override
	public IScope getScopeNearby( Key name ) throws ScopeNotFoundException {
		throw new UnsupportedOperationException( "Unimplemented method 'getScopeNearby'" );
	}

	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		throw new UnsupportedOperationException( "Unimplemented method 'scopeFind'" );
	}

	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope ) {
		throw new UnsupportedOperationException( "Unimplemented method 'scopeFindNearby'" );
	}

}
