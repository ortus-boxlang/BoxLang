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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class SystemCacheClear extends BIF {

	/**
	 * Valid caches
	 */
	private static final String[]	VALID_CACHES	= new String[] {
	    "all", "template", "page", "component", "cfc", "class", "customtag", "ct", "query", "object", "tag", "function"
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
	 * <li><code>template</code> or <code>page</code> - Clear the compiled class pools</li>
	 * <li><code>class</code> - Clear the class path resolvers</li>
	 * <li><code>query</code> - Clear the default cache region</li>
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
		if ( clearAll || cacheName.equals( "template" ) || cacheName.equals( "page" ) ) {
			RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		}

		// Class resolvers: component, cfc, left for compat
		if ( clearAll || cacheName.equals( "component" ) || cacheName.equals( "cfc" ) || cacheName.equals( "class" ) ) {
			runtime.getClassLocator().clear();
		}

		// All of these are stored in the "default" cache region
		if ( clearAll || cacheName.equals( "query" ) || cacheName.equals( "object" ) ) {
			runtime.getCacheService().getDefaultCache().clearAll();
		}

		return null;
	}
}
