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

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the static constructor of a box class
 */
public class StaticClassBoxContext extends BaseBoxContext {

	/**
	 * The static scope
	 */
	protected IScope		staticScope;

	/**
	 * The class in which this function is executing, if any
	 */
	protected DynamicObject	staticBoxClass	= null;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent    The parent context
	 * @param thisClass The function instance
	 */
	public StaticClassBoxContext( IBoxContext parent, DynamicObject staticBoxClass, StaticScope staticScope ) {
		super( parent );
		this.staticBoxClass	= staticBoxClass;
		this.staticScope	= staticScope;

		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for StaticClassBoxContext" );
		}
	}

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( StaticScope.name, staticScope );

		}
		return scopes;
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

		if ( key.equals( ThisScope.name ) ) {
			throw new BoxRuntimeException( "Cannot access this scope in a static context" );
		}

		if ( key.equals( StaticScope.name ) ) {
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}

		if ( key.equals( Key._super ) ) {
			throw new BoxRuntimeException( "Cannot access super scope in a static context" );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
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
		if ( name.equals( VariablesScope.name ) ) {
			// This will prevent unscoped lookups of a varible named static.variables FWIW, but it seems any such code would be a mistake
			throw new BoxRuntimeException( "Cannot access variables scope in a static context" );
		}

		if ( name.equals( StaticScope.name ) ) {
			return staticScope;
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
		return staticScope;
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

	@Override
	public void registerUDF( UDF udf ) {
		staticScope.put( udf.getName(), udf );
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	@Override
	public IBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		super.flushBuffer( force );
		return this;
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @return Whether the function can output
	 */
	@Override
	public Boolean canOutput() {
		return BoxClassSupport.canOutput( this, staticBoxClass );
	}
}
