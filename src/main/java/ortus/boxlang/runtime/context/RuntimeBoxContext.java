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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of the entire BoxLang Runtime. The runtime is persistent once
 * started, and can be used to process one or more "requests" for execution. The "server" scope here is
 * global and will be shared by all requests.
 */
public class RuntimeBoxContext extends BaseBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private static final Logger	logger			= LoggerFactory.getLogger( ServerScope.class );

	/**
	 * The variables scope
	 */
	protected IScope			serverScope		= new ServerScope();

	/**
	 * Box Runtime
	 */
	private BoxRuntime			runtime			= BoxRuntime.getInstance();

	/**
	 * Runtime configuration
	 */
	private Configuration		runtimeConfig	= runtime.getConfiguration();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	public RuntimeBoxContext( IBoxContext parent ) {
		super( parent );
	}

	/**
	 * Creates a new execution context
	 */
	public RuntimeBoxContext() {
		this( null );
	}

	/**
	 * This is an optional method on contexts which require initialization outside of its constructor
	 * In this case we do, since we want modules to collaborate to the runtime context
	 */
	@Override
	public void startup() {
		// Initialize the server scope
		this.serverScope.initialize();

		// Announce we are done
		BoxRuntime.getInstance().announce(
		    BoxEvent.ON_RUNTIME_BOX_CONTEXT_STARTUP,
		    Struct.of(
		        "context", this,
		        "configuration", this.runtimeConfig,
		        "serverScope", this.serverScope
		    )
		);

		logger.debug( "Runtime Box Context started" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters + Setters
	 * --------------------------------------------------------------------------
	 */

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( ServerScope.name, this.serverScope );
		return scopes;
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		// There are no near-by scopes in the runtime context. Everything is global here.

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		if ( parent != null ) {
			return parent.scopeFind( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null, key );
		}
		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @return The requested scope
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		// Check the scopes I know about
		if ( name.equals( this.serverScope.getName() ) ) {
			return this.serverScope;
		}

		if ( parent != null ) {
			return parent.getScope( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {

		if ( shallow ) {
			return null;
		}

		// The RuntimeBoxContext has no "nearby" scopes
		return getScope( name );
	}

	@Override
	public void registerUDF( UDF udf, boolean override ) {
		// This will prolly be unreachable since all executing code will be wrapped by another scope
		if ( override || !this.serverScope.containsKey( udf.getName() ) ) {
			this.serverScope.put( udf.getName(), udf );
		}
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		// This will prolly be unreachable since all executing code will be wrapped by another scope
		return this.serverScope;
	}

	/**
	 * Get the contexual config struct. Each context has a chance to add in config of their
	 * own to the struct, or override existing config with a new struct of their own design.
	 * It depends on whether the context wants its changes to exist for the rest of the entire
	 * request or only for code that executes in the current context and below.
	 *
	 * @return A struct of configuration
	 */
	@Override
	public IStruct getConfig() {
		return this.runtimeConfig.asStruct();
	}

}
