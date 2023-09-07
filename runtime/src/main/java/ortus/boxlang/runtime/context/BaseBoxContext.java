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

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.UDF;
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
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		Function			function	= getFunction( name );
		FunctionBoxContext	fContext	= new FunctionBoxContext( this, function.createArgumentsScope( positionalArguments ) );
		return function.invoke( fContext );
	}

	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		Function			function	= getFunction( name );
		FunctionBoxContext	fContext	= new FunctionBoxContext( this, function.createArgumentsScope( namedArguments ) );
		return function.invoke( fContext );
	}

	public Function getFunction( Key name ) {
		// TODO: Check for registered BIF

		ScopeSearchResult result = scopeFindNearby( name, null );
		if ( result == null ) {
			throw new RuntimeException( "Function '" + name.toString() + "' not found" );
		}
		Object value = result.value();
		if ( value instanceof Function ) {
			return ( ( Function ) value );
		} else {
			throw new RuntimeException(
			    "Variable '" + name + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}
	}

	public IScope getScope( Key name ) throws ScopeNotFoundException {
		throw new UnsupportedOperationException( "Unimplemented method 'getScope'" );
	}

	public IScope getScopeNearby( Key name ) throws ScopeNotFoundException {
		throw new UnsupportedOperationException( "Unimplemented method 'getScopeNearby'" );
	}

	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		throw new UnsupportedOperationException( "Unimplemented method 'scopeFind'" );
	}

	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope ) {
		throw new UnsupportedOperationException( "Unimplemented method 'scopeFindNearby'" );
	}

	public void regsiterUDF( UDF udf ) {
		throw new UnsupportedOperationException( "This context cannot register a function" );
	}

}
