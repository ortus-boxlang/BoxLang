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

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This class represents the execution context for a custom tag
 * <p>
 * It has a variables scope and a this scope, if any
 */
public class CustomTagBoxContext extends BaseBoxContext {

	/**
	 * The variables scope
	 */
	protected IScope	variablesScope;

	/**
	 * The this scope, if any
	 */
	protected IScope	thisScope;

	/**
	 * The name of the executing tag
	 */
	private Key			tagName;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent  The parent context
	 * @param tagName The name of the executing tag
	 */
	public CustomTagBoxContext( IBoxContext parent, Key tagName ) {
		super( parent );
		this.tagName	= tagName;
		variablesScope	= new VariablesScope();
		thisScope		= null;

		if ( parent instanceof FunctionBoxContext context && context.isInClass() ) {
			thisScope = context.getThisClass().getBottomClass().getThisScope();
		} else if ( parent instanceof ClassBoxContext context ) {
			thisScope = context.getThisClass().getBottomClass().getThisScope();
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
			if ( thisScope != null ) {
				scopes.getAsStruct( Key.contextual ).put( ThisScope.name, thisScope );
			}
		}
		return scopes;
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 * 
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 * 
	 * @return True if the key is visible in the current context, else false
	 */
	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( nearby ) {
			if ( key.equals( VariablesScope.name ) ) {
				return true;
			}
			if ( thisScope != null && key.equals( ThisScope.name ) ) {
				return true;
			}
		}
		return super.isKeyVisibleScope( key, false, false );
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

		if ( !isKeyVisibleScope( key ) ) {
			Object result = variablesScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
			}

			// In query loop?
			var querySearch = queryFindNearby( key );
			if ( querySearch != null ) {
				return querySearch;
			}
		}

		if ( shallow ) {
			return null;
		}

		// A custom tag cannot see nearby scopes above it
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
		if ( thisScope != null && key.equals( ThisScope.name ) ) {
			return new ScopeSearchResult( thisScope, thisScope, key, true );
		}
		// The custom tag has no "global" scopes, so just defer to parent
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
		// The custom tag has no "global" scopes, so just defer to parent
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

		if ( thisScope != null && name.equals( ThisScope.name ) ) {
			// A thread has special permission to "see" the this scope from its parent,
			// even though it's not "nearby" to any other scopes
			return this.thisScope;
		}

		if ( shallow ) {
			return null;
		}

		// A custom tag cannot see nearby scopes above it
		return parent.getScope( name );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void registerUDF( UDF udf, boolean override ) {
		registerUDF( variablesScope, udf, override );
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
	 * Get the name of the executing tag in this context
	 */
	public Key getTagName() {
		return tagName;
	}

}
