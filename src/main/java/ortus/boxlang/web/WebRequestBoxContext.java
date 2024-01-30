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

import io.undertow.server.HttpServerExchange;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
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
	protected IScope				FormScope;

	/**
	 * The CGI scope
	 */
	protected IScope				CGIScope;

	/**
	 * The cookie scope
	 */
	protected IScope				CookieScope;

	/**
	 * The Undertow exchange for this request
	 */
	protected HttpServerExchange	exchange;

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
	public WebRequestBoxContext( IBoxContext parent, HttpServerExchange exchange ) {
		super( parent );
		this.exchange	= exchange;
		URLScope		= new URLScope( exchange );
		FormScope		= new FormScope( exchange );
		CGIScope		= new CGIScope( exchange );
		CookieScope		= new CookieScope( exchange );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

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
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ) );
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
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		Object result = CGIScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( CGIScope, Struct.unWrapNull( result ) );
		}

		result = URLScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( URLScope, Struct.unWrapNull( result ) );
		}

		result = FormScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( FormScope, Struct.unWrapNull( result ) );
		}

		if ( parent != null ) {
			return parent.scopeFind( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null );
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
	public IScope getScope( Key name ) throws ScopeNotFoundException {

		if ( name.equals( requestScope.getName() ) ) {
			return requestScope;
		}

		if ( name.equals( URLScope.getName() ) ) {
			return URLScope;
		}

		if ( name.equals( FormScope.getName() ) ) {
			return FormScope;
		}

		if ( name.equals( CGIScope.getName() ) ) {
			return CGIScope;
		}

		if ( name.equals( CookieScope.getName() ) ) {
			return CookieScope;
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
	public IBoxContext flushBuffer( boolean force ) {
		String output;
		synchronized ( buffer ) {
			output = buffer.toString();
			clearBuffer();
		}

		exchange.getResponseSender().send( output );

		return this;
	}

}
