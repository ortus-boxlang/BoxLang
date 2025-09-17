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

import ortus.boxlang.runtime.async.RequestThreadManager;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.scopes.AttributesScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents a thread started by the bx:thread component.
 */
public class ThreadComponentBoxContext extends BaseBoxContext implements IJDBCCapableContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The thread local scope
	 */
	protected IScope				localScope;

	/**
	 * The thread attributes scope
	 */
	protected IScope				attributesScope;

	/**
	 * The parent's variables scope
	 */
	protected IScope				variablesScope;

	/**
	 * The Thread
	 */
	protected Thread				thread;

	/**
	 * The BoxLang name of the thread as registered in the thread manager.
	 */
	protected Key					threadName;

	/**
	 * A shortcut to the request thread manager stored in one of our ancestor
	 * contexts
	 */
	private RequestThreadManager	threadManager;

	/**
	 * The JDBC connection manager, which tracks transaction state/context and
	 * allows a thread or request to retrieve connections.
	 */
	private ConnectionManager		connectionManager	= null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded function instance and parent
	 * context
	 *
	 * @param parent        The parent context
	 * @param threadManager The thread manager
	 * @param threadName    The name of the thread
	 */
	public ThreadComponentBoxContext( IBoxContext parent, RequestThreadManager threadManager, Key threadName, IStruct attributes ) {
		super( parent );
		this.threadManager		= threadManager;
		this.threadName			= threadName;
		// Connection manager is lazy-initialized
		this.localScope			= new LocalScope();
		this.attributesScope	= new AttributesScope( attributes );

		this.variablesScope		= parent.getScopeNearby( VariablesScope.name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set the thread
	 *
	 * @return THis context
	 */
	public ThreadComponentBoxContext setThread( Thread thread ) {
		this.thread = thread;
		return this;
	}

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( LocalScope.name, localScope );
			scopes.getAsStruct( Key.contextual ).put( Key.thread, threadManager.getThreadMeta( threadName ) );
			scopes.getAsStruct( Key.contextual ).put( Key.attributes, attributesScope );

			// A thread has special permission to "see" the variables and this scope from its parent,
			// even though it's not "nearby" to any other scopes
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );

			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() ) {
				scopes.getAsStruct( Key.contextual ).put( ThisScope.name, fbc.getThisClass().getBottomClass().getThisScope() );
			}
			if ( getParent() instanceof ClassBoxContext cbc ) {
				scopes.getAsStruct( Key.contextual ).put( ThisScope.name, cbc.getThisScope() );
			}
			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass().getSuper() != null ) {
				scopes.getAsStruct( Key.contextual ).put( Key._super, fbc.getThisClass().getSuper().getVariablesScope() );
			}
			if ( getParent() instanceof ClassBoxContext cbc && cbc.getThisClass().getSuper() != null ) {
				scopes.getAsStruct( Key.contextual ).put( Key._super, cbc.getThisClass().getSuper().getVariablesScope() );
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
			if ( key.equals( LocalScope.name ) || key.equals( Key.thread ) || key.equals( Key.attributes ) || key.equals( VariablesScope.name ) ) {
				return true;
			}

			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() && key.equals( ThisScope.name ) ) {
				return true;
			}
			if ( getParent() instanceof ClassBoxContext && key.equals( ThisScope.name ) ) {
				return true;
			}
			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass().getSuper() != null && key.equals( Key._super ) ) {
				return true;
			}
			if ( getParent() instanceof ClassBoxContext cbc && cbc.getThisClass().getSuper() != null && key.equals( Key._super ) ) {
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

		// Look in the local scope first
		if ( key.equals( localScope.getName() ) ) {
			return new ScopeSearchResult( localScope, localScope, key, true );
		}

		if ( key.equals( AttributesScope.name ) ) {
			return new ScopeSearchResult( attributesScope, attributesScope, key, true );
		}

		if ( key.equals( VariablesScope.name ) ) {
			return new ScopeSearchResult( variablesScope, variablesScope, key, true );
		}

		if ( !isKeyVisibleScope( key ) ) {
			Object result = localScope.getRaw( key );
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
			}

			// attributesScope
			result = attributesScope.getRaw( key );
			if ( isDefined( result, forAssign ) ) {
				return new ScopeSearchResult( attributesScope, Struct.unWrapNull( result ), key );
			}

			result = variablesScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// A thread has special permission to "see" the variables scope from its parent,
				// even though it's not "nearby" to any other scopes
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

		return scopeFind( key, defaultScope, forAssign );

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
		IStruct				threadMeta	= threadManager.getThreadMeta( threadName );
		ScopeSearchResult	parentSearchResult;

		// access thread.foo inside a thread
		if ( key.equals( Key.thread ) ) {
			return new ScopeSearchResult( threadMeta, threadMeta, key, true );
		}

		// access threadName.foo inside a thread
		if ( key.equals( threadName ) ) {
			return new ScopeSearchResult( threadMeta, threadMeta, key, true );
		}

		// If we're inside a function, we can see the function's this and super scopes
		if ( getParent() instanceof FunctionBoxContext fbc ) {
			parentSearchResult = fbc.scopeFindThis( key );
			if ( parentSearchResult != null ) {
				return parentSearchResult;
			}
			parentSearchResult = fbc.scopeFindSuper( key );
			if ( parentSearchResult != null ) {
				return parentSearchResult;
			}
		}

		// If we're inside a class (pseudoconstructor), we can see the class's this and super scopes
		if ( getParent() instanceof ClassBoxContext cbc ) {
			parentSearchResult = cbc.scopeFindThis( key );
			if ( parentSearchResult != null ) {
				return parentSearchResult;
			}
			parentSearchResult = cbc.scopeFindSuper( key );
			if ( parentSearchResult != null ) {
				return parentSearchResult;
			}
		}
		if ( !isKeyVisibleScope( key ) ) {
			Object result = threadMeta.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				return new ScopeSearchResult( threadMeta, Struct.unWrapNull( result ), key );
			}
		}

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
		return this.parent.getScope( name );
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
			return this.localScope;
		}

		if ( name.equals( AttributesScope.name ) ) {
			return this.attributesScope;
		}

		if ( name.equals( VariablesScope.name ) ) {
			// A thread has special permission to "see" the variables scope from its parent,
			// even though it's not "nearby" to any other scopes
			return this.variablesScope;
		}

		if ( name.equals( ThisScope.name ) ) {
			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() ) {
				return fbc.getThisClass().getBottomClass().getThisScope();
			}
			if ( getParent() instanceof ClassBoxContext cbc ) {
				return cbc.getThisScope();
			}
		}

		if ( name.equals( Key._super ) ) {
			if ( getParent() instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass().getSuper() != null ) {
				return fbc.getThisClass().getSuper().getVariablesScope();
			}
			if ( getParent() instanceof ClassBoxContext cbc && cbc.getThisClass().getSuper() != null ) {
				return cbc.getThisClass().getSuper().getVariablesScope();
			}
		}

		if ( shallow ) {
			return null;
		}

		// A custom tag cannot see nearby scopes above it
		return this.parent.getScope( name );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return this.localScope;
	}

	@Override
	public void registerUDF( UDF udf, boolean override ) {
		registerUDF( this.variablesScope, udf, override );
	}

	/**
	 * Get the thread
	 *
	 * @return The thread
	 */
	public Thread getThread() {
		return this.thread;
	}

	/**
	 * Get the ConnectionManager, which is the central point for managing database
	 * connections and transactions.
	 */
	public ConnectionManager getConnectionManager() {
		if ( this.connectionManager == null ) {
			synchronized ( this ) {
				if ( this.connectionManager == null ) {
					this.connectionManager = new ConnectionManager( this );
				}
			}
		}
		return this.connectionManager;
	}

	/**
	 * Shutdown the ConnectionManager and release any resources.
	 */
	public void shutdownConnections() {
		if ( this.connectionManager != null ) {
			this.connectionManager.shutdown();
			this.connectionManager = null;
		}
	}

	@Override
	public void shutdown() {
		shutdownConnections();
	}

}
