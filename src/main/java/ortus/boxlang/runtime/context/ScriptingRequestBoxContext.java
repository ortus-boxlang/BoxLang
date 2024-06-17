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

import java.net.URI;
import java.util.UUID;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of a scripting execution in BoxLang
 * There a variables and request scope present.
 *
 * The request scope may or may not belong here, but we're sort of using the scripting
 * context as the top level context for an execution request right now, so it make the
 * most sense here currently.
 *
 * There may or may NOT be a template defined.
 */
public class ScriptingRequestBoxContext extends RequestBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private static BoxRuntime	runtime			= BoxRuntime.getInstance();

	/**
	 * The variables scope
	 */
	protected IScope			variablesScope	= new VariablesScope();

	/**
	 * The request scope
	 */
	protected IScope			requestScope	= new RequestScope();

	/**
	 * The tied session ID to this context of execution, if any.
	 * By default we default a random key GUID
	 */
	private Key					sessionID		= new Key( UUID.randomUUID().toString() );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a parent context
	 *
	 * @param parent The parent context
	 */
	public ScriptingRequestBoxContext( IBoxContext parent ) {
		super( parent );
		loadApplicationDescriptor( null );
	}

	/**
	 * Creates a new execution context with a parent context, and template
	 *
	 * @param parent The parent context
	 */
	public ScriptingRequestBoxContext( IBoxContext parent, URI template ) {
		super( parent );
		loadApplicationDescriptor( template );
	}

	/**
	 * Creates a new execution context with a template
	 *
	 * @param template The template to use
	 */
	public ScriptingRequestBoxContext( URI template ) {
		super( null );
		loadApplicationDescriptor( template );
	}

	/**
	 * Creates a new execution context
	 */
	public ScriptingRequestBoxContext() {
		this( runtime.getRuntimeContext(), null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters + Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the session ID for this request
	 *
	 * @return The session ID
	 */
	public Key getSessionID() {
		return sessionID;
	}

	/**
	 * Invalidate a session
	 */
	public void resetSession() {
		this.sessionID = new Key( UUID.randomUUID().toString() );
		getApplicationListener().invalidateSession( this.sessionID );
	}

	/**
	 * The session ID can be set externally
	 *
	 * @param sessionID
	 */
	public void setSessionID( Key sessionID ) {
		this.sessionID = sessionID;
	}

	/**
	 * Get the visible scopes for this context
	 *
	 * @param scopes  The scopes to add to
	 * @param nearby  If nearby scopes should be included
	 * @param shallow If only the top level scopes should be included
	 *
	 * @return The scopes
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( !shallow ) {
			scopes.getAsStruct( Key.contextual ).put( RequestScope.name, requestScope );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
		}
		return super.getVisibleScopes( scopes, nearby, shallow );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A local lookup is used for the closest context to the executing code
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		// In Variables scope? (thread-safe lookup and get)
		Object result = variablesScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
		}

		if ( shallow ) {
			return null;
		}

		return scopeFind( key, defaultScope );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindNearby(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a class.
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		return super.scopeFind( key, defaultScope );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are local to an execution context
	 *
	 * @return The requested scope
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		if ( name.equals( requestScope.getName() ) ) {
			return requestScope;
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
		// Check the scopes I know about
		if ( name.equals( variablesScope.getName() ) ) {
			return variablesScope;
		}

		if ( shallow ) {
			return null;
		}

		return getScope( name );
	}

	public void registerUDF( UDF udf ) {
		variablesScope.put( udf.getName(), udf );
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
	 * Flush the buffer to the output stream
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
		String output;
		// If there are extra buffers registered, we ignore flush requests since someone
		// out there is wanting to capture our buffer instead.
		if ( hasParent() && buffers.size() == 1 ) {
			StringBuffer buffer = getBuffer();
			synchronized ( buffer ) {
				output = buffer.toString();
				clearBuffer();
			}

			// Announce it
			IStruct eventData = Struct.of(
			    Key.context, this,
			    Key.output, output
			);
			BoxRuntime.getInstance().getInterceptorService()
			    .announce( BoxEvent.ON_REQUEST_FLUSH_BUFFER, eventData );

			// If a scripting context is our top-level context, we flush to the console.
			getOut().print( eventData.getAsString( Key.output ) );
		} else if ( force ) {
			for ( StringBuffer buf : buffers ) {
				synchronized ( buf ) {
					output = buf.toString();
					buf.setLength( 0 );
				}

				// Announce it
				IStruct eventData = Struct.of(
				    Key.context, this,
				    Key.output, output
				);
				BoxRuntime.getInstance().getInterceptorService()
				    .announce( BoxEvent.ON_REQUEST_FLUSH_BUFFER, eventData );

				getOut().print( eventData.getAsString( Key.output ) );
			}
		}
		return this;
	}

}
