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

import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of any function execution in BoxLang
 * It encapsulates the arguments scope and local scope and has a reference to the function being invoked.
 * This context is extended for use with both UDFs and Closures as well
 */
public class FunctionBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	protected IScope	argumentsScope;

	/**
	 * The local scope
	 */
	protected IScope	localScope;

	/**
	 * The Function being invoked with this context
	 */
	protected Function	function;

	/**
	 * The Function name being invoked with this context. Note this may or may not be the name the function was declared as.
	 */
	protected Key		functionCalledName;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public FunctionBoxContext( IBoxContext parent, Function function ) {
		this( parent, function, function.getName() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 *
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName ) {
		this( parent, function, functionCalledName, new ArgumentsScope() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The function being invoked with this context
	 * @param argumentsScope The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName, ArgumentsScope argumentsScope ) {
		super( parent );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for FunctionBoxContext" );
		}
		if ( function == null ) {
			throw new BoxRuntimeException( "function cannot be null for FunctionBoxContext" );
		}
		this.localScope			= new LocalScope();
		this.argumentsScope		= argumentsScope;
		this.function			= function;
		this.functionCalledName	= functionCalledName;
	}

	/**
	 * Returns the function being invoked with this context
	 */
	public Function getFunction() {
		return function;
	}

	/**
	 * Search for a variable in "nearby" scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 * @param shallow      Whether to search only the "nearby" scopes or all scopes
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		Object result = localScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ) );
		}

		result = argumentsScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ) );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		if ( shallow ) {
			return null;
		}

		if ( isInClass() ) {
			// A function executing in a class can see the class variables
			IScope classVariablesScope = getThisClass().getBottomClass().getVariablesScope();

			result = classVariablesScope.getRaw( key );
			// Null means not found
			if ( result != null ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( classVariablesScope, Struct.unWrapNull( result ) );
			}

			// A component cannot see nearby scopes above it
			return parent.scopeFind( key, defaultScope );
		} else {
			// A UDF is "transparent" and can see everything in the parent scope as a "local" observer
			return parent.scopeFindNearby( key, defaultScope );
		}

	}

	/**
	 * Search for a variable in scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.scopeFind( key, defaultScope );
	}

	/**
	 * Look for a scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScope( name );
	}

	/**
	 * Look for a "nearby" scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( localScope.getName() ) ) {
			return localScope;
		}
		if ( name.equals( argumentsScope.getName() ) ) {
			return argumentsScope;
		}

		if ( shallow ) {
			return null;
		}

		if ( isInClass() ) {
			if ( name.equals( VariablesScope.name ) ) {
				return getThisClass().getBottomClass().getVariablesScope();
			}
			// We don't have a check for "this" here because this.foo transpiles to a direct reference to the class itself

			// A component cannot see nearby scopes above it
			return parent.getScope( name );
		} else {
			// The FunctionBoxContext has no "global" scopes, so just defer to parent
			return parent.getScopeNearby( name );
		}
	}

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not called from a function
	 */
	@Override
	public Key findClosestFunctionName() {
		return functionCalledName;
	};

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		// DIFFERENT FROM CFML ENGINES! Same as Lucee's "local mode"
		return localScope;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	@Override
	public IBoxContext getFunctionParentContext() {
		// If a function is executed inside another function, it uses the parent since there is nothing a function can "see" from inside it's calling function
		return getParent();
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 * 
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public boolean isInClass() {
		return templates.peek() instanceof IClassRunnable;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 * 
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public IClassRunnable getThisClass() {
		return ( IClassRunnable ) templates.peek();
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 * 
	 * @param force true, flush even if output is disabled
	 * 
	 * @return This context
	 */
	public IBoxContext flushBuffer( boolean force ) {
		// direct flushing ignored if we can't output
		if ( force || getFunction().canOutput( this ) ) {
			super.flushBuffer( force );
		}
		return this;
	}
}
