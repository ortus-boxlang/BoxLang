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
package ortus.boxlang.runtime.bifs.global.system;

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Clear system-level caches" )
public class SystemCacheClear extends BIF {

	/**
	 * Valid caches
	 */
	private static final String[]	VALID_CACHES	= new String[] {
	    "all",
	    "cfc",
	    "class",
	    "component",
	    "ct",
	    "customtag",
	    "function",
	    "http",
	    "object",
	    "page",
	    "query",
	    "tag",
	    "template"
	};

	private static final String		DEFAULT_CACHE	= "all";

	/**
	 * Constructor
	 */
	public SystemCacheClear() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.cacheName, DEFAULT_CACHE, Set.of( Validator.valueOneOf( VALID_CACHES ) ) )
		};
	}

	/**
	 * Clears many of the caches in the runtime. By default with no arguments, it will clear all caches.
	 *
	 * The following caches can be cleared:
	 *
	 * <ul>
	 * <li><code>all</code> - Clear everything</li>
	 * <li><code>page</code> - Clear the compiled class pools</li>
	 * <li><code>class</code> - Clear the class path resolvers</li>
	 * <li><code>template</code> - Clear all the templates cached using the bx:cache component</li>
	 * <li><code>query</code> - Clears the cache storing queries</li>
	 * <li><code>object</code> - Clear the default cache region</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	cacheName	= arguments.getAsString( Key.cacheName ).toLowerCase();
		boolean	clearAll	= cacheName.equals( DEFAULT_CACHE );

		// Page Pool region
		if ( clearAll || cacheName.equals( "page" ) ) {
			RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		}

		// Class resolvers: component, cfc, left for compat
		if ( clearAll || cacheName.equals( "component" ) || cacheName.equals( "cfc" ) || cacheName.equals( "class" ) ) {
			runtime.getClassLocator().clear();
		}

		// HTTP Clients
		if ( clearAll || cacheName.equals( "http" ) ) {
			runtime.getHttpService().clearAllClients();
		}

		// Query Caching
		if ( clearAll || cacheName.equals( "query" ) ) {
			runtime.getCacheService().getDefaultCache().clearAll(
			    new WildcardFilter( PendingQuery.CACHE_PREFIX + "*", true )
			);
		}

		// Specific caches
		if ( clearAll || cacheName.equals( "object" ) || cacheName.equals( "template" ) ) {
			runtime.getCacheService().getDefaultCache().clearAll();
		}

		return null;
	}
}
