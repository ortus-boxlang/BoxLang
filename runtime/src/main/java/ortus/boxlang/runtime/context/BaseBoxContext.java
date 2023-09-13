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
import java.util.Stack;

import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.UDF;
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
	protected IBoxContext			parent;

	protected Stack<BaseTemplate>	templates	= new Stack<BaseTemplate>();

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
	 * Push a template to the stack
	 *
	 * @param templatePath The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( BaseTemplate template ) {
		this.templates.push( template );
		return this;
	}

	/**
	 * Pop a template from the stack
	 *
	 * @return The template that this execution context is bound to
	 */
	public BaseTemplate popTemplate() {
		return this.templates.pop();
	}

	/**
	 * Has the execution context been bound to a template?
	 *
	 * @return True if bound, else false
	 */
	public boolean hasTemplates() {
		return !this.templates.empty();
	}

	/**
	 * Finds the closest template
	 *
	 * @return The template instance if found, null if this code is not called from a template
	 */
	public BaseTemplate findClosestTemplate() {
		// If this context has templates, grab the first
		if ( hasTemplates() ) {
			return this.templates.peek();
		}

		// Otherwise, if we have a parent, as them
		if ( hasParent() ) {
			return getParent().findClosestTemplate();
		}

		// There are none to be found!
		return null;
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
	 * Invoke a function call such as foo() using positional args. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		Function function = findFunction( name );
		return invokeFunction( function, function.createArgumentsScope( positionalArguments ) );
	}

	/**
	 * Invoke a function call such as foo() using named args. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		Function function = findFunction( name );
		return invokeFunction( function, function.createArgumentsScope( namedArguments ) );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using positional args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Object[] positionalArguments ) {
		return invokeFunction( function, function.createArgumentsScope( positionalArguments ) );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Map<Key, Object> namedArguments ) {
		return invokeFunction( function, function.createArgumentsScope( namedArguments ) );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, ArgumentsScope argumentsScope ) {
		FunctionBoxContext fContext = new FunctionBoxContext( getFunctionParentContext(), function, argumentsScope );
		return function.invoke( fContext );
	}

	/**
	 * Find a function in the corrent context. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @param name The name of the function to find
	 *
	 * @return The function instance
	 */
	private Function findFunction( Key name ) {
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

	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {
		throw new UnsupportedOperationException( "Unimplemented method 'scopeFindNearby'" );
	}

	public void regsiterUDF( UDF udf ) {
		throw new UnsupportedOperationException( "This context cannot register a function" );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope() {
		throw new UnsupportedOperationException( "Unimplemented method 'getDefaultAssignmentScope'" );
	}

	/**
	 * Finds the closest function call
	 *
	 * @return The Function instance if found, null if this code is not called from a function
	 */
	public Function findClosestFunction() {
		IBoxContext context = this;
		// Climb the context tree until we find a function
		while ( context != null ) {
			if ( context instanceof FunctionBoxContext ) {
				return ( ( FunctionBoxContext ) context ).getFunction();
			}
			context = context.getParent();
		}
		return null;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	public IBoxContext getFunctionParentContext() {
		return this;
	}

	/**
	 * Try to get the requested key from an unkonwn scope but overriding the parent to check if not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 * @throws KeyNotFoundException If the key was not found in any scope
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope ) {
		return scopeFindNearby( key, defaultScope, false );
	}

}
