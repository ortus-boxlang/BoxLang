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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
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

	private static BoxRuntime	runtime			= BoxRuntime.getInstance();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
	 */
	protected IScope			variablesScope	= new VariablesScope();

	/**
	 * The request scope
	 */
	protected IScope			requestScope	= new RequestScope();

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
	public ScriptingRequestBoxContext( IBoxContext parent ) {
		super( parent );
	}

	/**
	 * Creates a new execution context
	 */
	public ScriptingRequestBoxContext() {
		this( runtime.getRuntimeContext() );
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
	 * Here is the order for bx templates
	 * (Not all yet implemented and some will be according to platform: WebContext, AndroidContext, IOSContext, etc)
	 *
	 * 1. Query (only in query loops)
	 * 2. Thread
	 * 3. Variables
	 * 4. CGI (should it exist in the core runtime?)
	 * 5. CFFILE
	 * 6. URL (Only for web runtime)
	 * 7. FORM (Only for web runtime)
	 * 8. COOKIE (Only for web runtime)
	 * 9. CLIENT (Only for web runtime)
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
		// If there are extra buffers registered, we ignore flush requests since someone
		// out there is wanting to capture our buffer instead.
		if ( hasParent() && buffers.size() == 1 ) {
			StringBuffer buffer = getBuffer();
			synchronized ( buffer ) {
				output = buffer.toString();
				clearBuffer();
			}
			// If a scripting context is our top-level context, we flush to the console.
			System.out.print( output );
		} else if ( force ) {
			for ( StringBuffer buf : buffers ) {
				synchronized ( buf ) {
					output = buf.toString();
					buf.setLength( 0 );
				}
				System.out.print( output );
			}
		}
		return this;
	}

}
