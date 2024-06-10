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

import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * This context represents the context of any function execution in BoxLang
 * It encapsulates the arguments scope and local scope and has a reference to
 * the function being invoked.
 * This context is extended for use with both UDFs and Closures as well
 */
public class ThreadBoxContext extends BaseBoxContext implements IJDBCCapableContext {

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
	 * The parent's variables scope
	 */
	protected IScope				variablesScope;

	/**
	 * The parent's this scope
	 */
	protected IScope				thisScope;

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
	private ConnectionManager		connectionManager;

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
	public ThreadBoxContext( IBoxContext parent, RequestThreadManager threadManager, Key threadName ) {
		super( parent );
		this.threadManager		= threadManager;
		this.threadName			= threadName;
		this.connectionManager	= new ConnectionManager( this );
		localScope				= new LocalScope();

		variablesScope			= parent.getScopeNearby( VariablesScope.name );
		thisScope				= null;
		if ( parent instanceof FunctionBoxContext context ) {
			thisScope = context.getThisClass().getThisScope();
		} else if ( parent instanceof ClassBoxContext context ) {
			thisScope = context.getThisClass().getThisScope();
		}
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
	public ThreadBoxContext setThread( Thread thread ) {
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
			// A thread has special permission to "see" the variables and this scope from its parent,
			// even though it's not "nearby" to any other scopes
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
			if ( thisScope != null ) {
				scopes.getAsStruct( Key.contextual ).put( ThisScope.name, thisScope );
			}
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

		Object result = localScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
		}

		result = variablesScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// A thread has special permission to "see" the variables scope from its parent,
			// even though it's not "nearby" to any other scopes
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope );

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
		IStruct threadMeta = threadManager.getThreadMeta( threadName );

		// access thread.foo inside a thread
		if ( key.equals( Key.thread ) ) {
			return new ScopeSearchResult( threadMeta, threadMeta, key, true );
		}

		// access threadName.foo inside a thread
		if ( key.equals( threadName ) ) {
			return new ScopeSearchResult( threadMeta, threadMeta, key, true );
		}

		if ( thisScope != null && key.equals( ThisScope.name ) ) {
			return new ScopeSearchResult( thisScope, thisScope, key, true );
		}

		Object result = threadMeta.getRaw( key );
		// Null means not found
		if ( result != null ) {
			return new ScopeSearchResult( threadMeta, Struct.unWrapNull( result ), key );
		}

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

		if ( name.equals( VariablesScope.name ) ) {
			// A thread has special permission to "see" the variables scope from its parent,
			// even though it's not "nearby" to any other scopes
			return this.variablesScope;
		}

		if ( thisScope != null ) {
			if ( name.equals( ThisScope.name ) ) {
				// A thread has special permission to "see" the this scope from its parent,
				// even though it's not "nearby" to any other scopes
				return this.thisScope;
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
	public void registerUDF( UDF udf ) {
		this.variablesScope.put( udf.getName(), udf );
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
		return this.connectionManager;
	}

	/**
	 * Shutdown the ConnectionManager and release any resources.
	 */
	public void shutdownConnections() {
		this.connectionManager.shutdown();
	}
}
