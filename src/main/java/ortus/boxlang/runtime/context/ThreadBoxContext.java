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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents any code running inside of a thread. It can still have a grandparent
 * of a given request context, but it provides encapsulation for things like JDBC connections, which
 * cannot be shared with the parent thread.
 */
public class ThreadBoxContext extends BaseBoxContext implements IJDBCCapableContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The parent's variables scope
	 */
	protected IScope			variablesScope		= null;

	/**
	 * The JDBC connection manager, which tracks transaction state/context and
	 * allows a thread or request to retrieve connections.
	 */
	private ConnectionManager	connectionManager	= null;

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
	public ThreadBoxContext( IBoxContext parent ) {
		super( parent );
		registerShutdownListener( ( context ) -> this.doShutdown() );
	}

	/**
	 * Run a consumer with a given context. If we're running in parallel, we will create a new ThreadBoxContext
	 * and set it as the current context, cleaning up any JDBC connections when done.
	 * If this is not in parallel, then we just run the consumer in the current context.
	 * The parallel flag is for convenience since many BIFs allow for parallel execution to be toggled
	 * on and off, this will make for simpler code paths. When parallel is false, this method
	 * is basically a no-op.
	 *
	 * @param parent   The parent context to use for the ThreadBoxContext
	 * @param parallel If true, run in a new ThreadBoxContext, otherwise run in the current context
	 * @param runnable The runnable to execute
	 *
	 * @return The result of the runnable
	 */
	public static Object runInContext( IBoxContext parent, boolean parallel, java.util.function.Function<IBoxContext, Object> runnable ) {

		ClassLoader			oldClassLoader	= Thread.currentThread().getContextClassLoader();
		RequestBoxContext	requestContext	= parent.getRequestContext();
		if ( requestContext != null ) {
			Thread.currentThread().setContextClassLoader( requestContext.getRequestClassLoader() );
		} else {
			Thread.currentThread().setContextClassLoader( BoxRuntime.getInstance().getRuntimeLoader() );
		}

		if ( parallel ) {
			ThreadBoxContext context = new ThreadBoxContext( parent );
			if ( requestContext != null ) {
				requestContext.registerDependentThread();
			}
			try {
				RequestBoxContext.setCurrent( context );
				return runnable.apply( context );
			} finally {
				RequestBoxContext.removeCurrent();
				context.shutdown();
				Thread.currentThread().setContextClassLoader( oldClassLoader );
				if ( requestContext != null ) {
					requestContext.unregisterDependentThread();
				}
			}
		} else {
			try {
				return runnable.apply( parent );
			} finally {
				Thread.currentThread().setContextClassLoader( oldClassLoader );
			}
		}
	}

	/**
	 * Run a consumer with a given context. We will create a new ThreadBoxContext
	 * and set it as the current context, cleaning up any JDBC connections when done.
	 *
	 * @param parent   The parent context to use for the ThreadBoxContext
	 * @param runnable The runnable to execute
	 */
	public static Object runInContext( IBoxContext parent, java.util.function.Function<IBoxContext, Object> runnable ) {
		return runInContext( parent, true, runnable );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, getVariablesScope() );
		}
		return scopes;
	}

	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( nearby && key.equals( VariablesScope.name ) ) {
			return true;
		}
		return super.isKeyVisibleScope( key, false, false );
	}

	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		if ( key.equals( VariablesScope.name ) ) {
			return new ScopeSearchResult( getVariablesScope(), getVariablesScope(), key, true );
		}

		if ( !isKeyVisibleScope( key ) ) {
			Object result = getVariablesScope().getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// A thread has special permission to "see" the variables scope from its parent,
				// even though it's not "nearby" to any other scopes
				return new ScopeSearchResult( getVariablesScope(), Struct.unWrapNull( result ), key );
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

	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		return parent.scopeFind( key, defaultScope, forAssign );
	}

	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		return this.parent.getScope( name );
	}

	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {

		if ( name.equals( VariablesScope.name ) ) {
			return getVariablesScope();
		}

		return getScope( name );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return getVariablesScope();
	}

	@Override
	public void registerUDF( UDF udf, boolean override ) {
		registerUDF( getVariablesScope(), udf, override );
	}

	/**
	 * Lazy initializer for variables scope
	 * this.variablesScope may be null if not used.
	 */
	public IScope getVariablesScope() {
		if ( this.variablesScope == null ) {
			synchronized ( this ) {
				if ( this.variablesScope == null ) {
					this.variablesScope = parent.getScopeNearby( VariablesScope.name, false );
				}
			}
		}
		return this.variablesScope;
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

	/**
	 * Internal method to do the actual shutdown
	 */
	private void doShutdown() {
		shutdownConnections();

		// Wipe out this stuff to help GC
		this.variablesScope = null;

	}

}
