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

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the initialization of an interface, and is really only here for the registerUDF method
 */
public class InterfaceBoxContext extends BaseBoxContext {

	/**
	 * The interface instance
	 */
	protected BoxInterface	thisInterface;

	protected StaticScope	staticScope;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent        The parent context
	 * @param thisInterface The target interface
	 */
	public InterfaceBoxContext( IBoxContext parent, BoxInterface thisInterface ) {
		super( parent );
		this.thisInterface	= thisInterface;
		this.staticScope	= thisInterface.getStaticScope();

	}

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
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		// Static Scope
		if ( key.equals( StaticScope.name ) ) {
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		Object result = staticScope.getRaw( key );
		// Null means not found
		if ( isDefined( result, forAssign ) ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( staticScope, Struct.unWrapNull( result ), key );
		}

		if ( shallow ) {
			return null;
		}

		// A component cannot see nearby scopes above it
		return parent.scopeFind( key, defaultScope, forAssign );

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
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		// The interface context has no "global" scopes, so just defer to parent
		return parent.scopeFind( key, defaultScope, forAssign );
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
		// The interface context has no "global" scopes, so just defer to parent
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

		if ( name.equals( StaticScope.name ) ) {
			return staticScope;
		}

		if ( shallow ) {
			return null;
		}

		// The interface context has no "global" scopes, so just defer to parent
		return parent.getScope( name );
	}

	@Override
	public void registerUDF( UDF udf, boolean override ) {
		if ( udf.hasModifier( BoxMethodDeclarationModifier.STATIC ) ) {
			registerUDF( staticScope, udf, override );
		} else {
			if ( override || !thisInterface.getDefaultMethods().containsKey( udf.getName() ) ) {
				if ( thisInterface.getDefaultMethods().get( udf.getName() ) instanceof Function f && f.hasModifier( BoxMethodDeclarationModifier.FINAL ) ) {
					throw new BoxRuntimeException( "Cannot override final method " + udf.getName() );
				}
				thisInterface.getDefaultMethods().put( udf.getName(), udf );
			}
		}
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

}
