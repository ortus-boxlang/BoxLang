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
package ortus.boxlang.web;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.xnio.channels.StreamSinkChannel;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.web.scopes.CGIScope;
import ortus.boxlang.web.scopes.CookieScope;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.URLScope;

/**
 * This context represents the context of a web/HTTP site in BoxLang
 * There a variables and request scope present.
 */
public class WebRequestBoxContext extends RequestBoxContext {

	private static BoxRuntime		runtime			= BoxRuntime.getInstance();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
	 */
	protected IScope				variablesScope	= new VariablesScope();

	/**
	 * The request scope
	 */
	protected IScope				requestScope	= new RequestScope();

	/**
	 * The URL scope
	 */
	protected IScope				URLScope;

	/**
	 * The form scope
	 */
	protected IScope				formScope;

	/**
	 * The CGI scope
	 */
	protected IScope				CGIScope;

	/**
	 * The cookie scope
	 */
	protected IScope				cookieScope;

	/**
	 * The Undertow exchange for this request
	 */
	protected HttpServerExchange	exchange;

	/**
	 * Undertow response channel
	 */
	protected StreamSinkChannel		channel			= null;

	/**
	 * The request body can only be read once, so we cache it here
	 */
	protected byte[]				requestBody		= null;

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
	public WebRequestBoxContext( IBoxContext parent, HttpServerExchange exchange, URI template ) {
		super( parent );
		this.exchange	= exchange;
		URLScope		= new URLScope( exchange );
		formScope		= new FormScope( exchange );
		CGIScope		= new CGIScope( exchange );
		cookieScope		= new CookieScope( exchange );

	}

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	public WebRequestBoxContext( IBoxContext parent, HttpServerExchange exchange ) {
		this( parent, exchange, null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the session key for this request
	 *
	 * @return The session key
	 */
	public Key getSessionID() {
		// TODO: make this logic configurable
		Cookie	sessionCookie	= exchange.getRequestCookie( "jsessionid" );
		String	sessionID;
		if ( sessionCookie != null ) {
			sessionID = sessionCookie.getValue();
		} else {
			sessionID = UUID.randomUUID().toString();
			// TODO: secure, domain, etc
			exchange.setResponseCookie( new CookieImpl( "jsessionid", sessionID ) );
		}
		return Key.of( sessionID );
	}

	/**
	 * Invalidate a session
	 */
	public void resetSession() {
		synchronized ( this ) {
			exchange.setResponseCookie( new CookieImpl( "jsessionid", null ) );
			getApplicationListener().invalidateSession( getSessionID() );
		}
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		scopes.getAsStruct( Key.contextual ).put( ortus.boxlang.web.scopes.URLScope.name, URLScope );
		scopes.getAsStruct( Key.contextual ).put( FormScope.name, formScope );
		scopes.getAsStruct( Key.contextual ).put( ortus.boxlang.web.scopes.CGIScope.name, CGIScope );
		scopes.getAsStruct( Key.contextual ).put( CookieScope.name, cookieScope );
		scopes.getAsStruct( Key.contextual ).put( RequestScope.name, requestScope );
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
	 * cgi or server which are never encapsulated like variables is inside a CFC.
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		if ( key.equals( CGIScope.getName() ) ) {
			return new ScopeSearchResult( CGIScope, CGIScope, key, true );
		}
		if ( key.equals( URLScope.getName() ) ) {
			return new ScopeSearchResult( URLScope, URLScope, key, true );
		}
		if ( key.equals( formScope.getName() ) ) {
			return new ScopeSearchResult( formScope, formScope, key, true );
		}
		if ( key.equals( cookieScope.getName() ) ) {
			return new ScopeSearchResult( cookieScope, cookieScope, key, true );
		}
		Object result = CGIScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( CGIScope, Struct.unWrapNull( result ), key );
		}

		result = URLScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( URLScope, Struct.unWrapNull( result ), key );
		}

		result = formScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( formScope, Struct.unWrapNull( result ), key );
		}

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

		if ( name.equals( URLScope.getName() ) ) {
			return URLScope;
		}

		if ( name.equals( formScope.getName() ) ) {
			return formScope;
		}

		if ( name.equals( CGIScope.getName() ) ) {
			return CGIScope;
		}

		if ( name.equals( cookieScope.getName() ) ) {
			return cookieScope;
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

	@Override
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

	private synchronized StreamSinkChannel getReponseChannel() {
		if ( channel == null ) {
			channel = exchange.getResponseChannel();
		}
		return channel;

	}

	public void finalizeResponse() {
		try {
			getReponseChannel().shutdownWrites();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
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
		String output = "";
		// If there are extra buffers registered, we ignore flush requests since someone
		// out there is wanting to capture our buffer instead.
		if ( buffers.size() == 1 ) {
			StringBuffer buffer = getBuffer();
			synchronized ( buffer ) {
				output = buffer.toString();
				clearBuffer();
			}

		} else if ( force ) {
			for ( StringBuffer buf : buffers ) {
				synchronized ( buf ) {
					output.concat( buf.toString() );
					buf.setLength( 0 );
				}
			}
		}
		if ( !output.isEmpty() ) {
			ByteBuffer bBuffer = ByteBuffer.wrap( output.getBytes() );
			try {
				getReponseChannel().write( bBuffer );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			// This ends the exchange, so not what we want
			// exchange.getResponseSender().send( output );
		}
		return this;
	}

	/**
	 * Get the Undertow server exchange
	 *
	 * @return The exchange
	 */
	public HttpServerExchange getExchange() {
		return exchange;
	}

	/**
	 * Get the request body as a byte array
	 *
	 * @return The request body
	 */
	public byte[] getRequestBody() {
		if ( requestBody != null ) {
			return requestBody;
		}
		synchronized ( exchange ) {
			if ( requestBody != null ) {
				return requestBody;
			}
			requestBody = ortus.boxlang.runtime.util.FileSystemUtil.convertInputStreamToByteArray( exchange.getInputStream() );
		}

		return requestBody;
	}

}
