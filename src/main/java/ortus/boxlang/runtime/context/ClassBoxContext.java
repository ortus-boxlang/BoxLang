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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of any function execution in BoxLang
 * It encapsulates the arguments scope and local scope and has a reference to the function being invoked.
 * This context is extended for use with both UDFs and Closures as well
 */
public class ClassBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	protected IScope			variablesScope;

	/**
	 * The local scope
	 */
	protected IScope			thisScope;

	/**
	 * The local scope
	 */
	protected IClassRunnable	thisClass;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public ClassBoxContext( IBoxContext parent, IClassRunnable thisClass ) {
		super( parent );
		this.variablesScope	= thisClass.getVariablesScope();
		this.thisScope		= thisClass.getThisScope();
		this.thisClass		= thisClass;

		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for ClassBoxContext" );
		}
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

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		Object result = variablesScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ) );
		}

		if ( shallow ) {
			return null;
		}

		// A component cannot see nearby scopes above it
		return parent.scopeFind( key, defaultScope );

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
		if ( name.equals( variablesScope.getName() ) ) {
			return variablesScope;
		}

		if ( shallow ) {
			return null;
		}

		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScope( name );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return variablesScope;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	@Override
	public IBoxContext getFunctionParentContext() {
		return this;
	}

	public void registerUDF( UDF udf ) {
		variablesScope.put( udf.getName(), udf );
		if ( udf.getAccess() == UDF.Access.PUBLIC ) {
			thisScope.put( udf.getName(), udf );
		}
	}

	public IClassRunnable getThisClass() {
		return thisClass;
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
		if ( force || getThisClass().canOutput() ) {
			super.flushBuffer( force );
		}
		return this;
	}
}
