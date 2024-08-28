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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class SystemCacheClear extends BIF {

	/**
	 * Constructor
	 */
	public SystemCacheClear() {
		super();
		this.declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.cacheName )
		};
	}

	/**
	 * For Lucee compat right now. We'll see if we need to do anything else.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	@SuppressWarnings( "null" )
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String cacheName = arguments.getAsString( Key.cacheName );
		cacheName = cacheName == null ? null : cacheName.toLowerCase();
		boolean clearAll = cacheName == null || cacheName.equals( "all" );
		if ( clearAll || cacheName.equals( "template" ) || cacheName.equals( "page" ) ) {
			RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		}
		if ( clearAll || cacheName.equals( "component" ) || cacheName.equals( "cfc" ) || cacheName.equals( "class" ) ) {
			// TODO: The BoxResolver maintains the cache internally
		}
		if ( clearAll || cacheName.equals( "query" ) || cacheName.equals( "object" ) ) {
			// TODO: Will we have this?
		}
		if ( clearAll || cacheName.equals( "tag" ) ) {
			// I don't think this appiles to BoxLang as tags aren't an object which are transient
		}
		if ( clearAll || cacheName.equals( "function" ) ) {
			// TODO: Will we have this?
		}
		if ( clearAll || cacheName.equals( "customtag" ) || cacheName.equals( "ct" ) ) {
			// TODO: Will we have this?
		}
		return null;
	}
}
