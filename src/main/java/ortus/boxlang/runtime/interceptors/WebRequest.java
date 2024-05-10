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
package ortus.boxlang.runtime.interceptors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.Component.ComponentBody;
import ortus.boxlang.runtime.components.cache.Cache;
import ortus.boxlang.runtime.components.cache.Cache.CacheAction;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.web.WebRequestBoxContext;

/**
 * Web request based interceptions
 * TODO: Move this to web runtime when broken out
 */
public class WebRequest {

	/**
	 * The runtime instance
	 */
	protected BoxRuntime		runtime				= BoxRuntime.getInstance();

	/**
	 * The component service helper
	 */
	protected ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

	/**
	 * Listens for the file component actions around web uploaads
	 *
	 * @param data The data to be intercepted
	 */
	@InterceptionPoint
	public void onFileComponentAction( IStruct data ) {
		IStruct	arguments	= data.getAsStruct( Key.arguments );
		Key		action		= Key.of( arguments.getAsString( Key.action ) );

		if ( action.equals( Key.upload ) ) {
			throw new BoxRuntimeException( "The file action [" + action.getName() + "] is yet implemented in the web runtime" );
		} else if ( action.equals( Key.uploadAll ) ) {
			throw new BoxRuntimeException( "The file action [" + action.getName() + "] is not yet implemented in the web runtime" );
		}
	}

	/**
	 * Listens for the file component actions around web uploaads
	 *
	 * @param data The data to be intercepted
	 */
	@InterceptionPoint
	public void onComponentInvocation( IStruct data ) {
		Component component = ( Component ) data.get( Key.component );
		if ( component == null || data.get( Key.result ) != null ) {
			return;
		}
		if ( component instanceof Cache ) {
			IBoxContext			context		= ( IBoxContext ) data.get( Key.context );
			IStruct				attributes	= data.getAsStruct( Key.attributes );
			ComponentBody		body		= ( ComponentBody ) data.get( Key.body );
			Cache.CacheAction	cacheAction	= Cache.CacheAction.fromString( attributes.getAsString( Key.action ) );
			Double				timespan	= attributes.getAsDouble( Key.timespan );

			if ( context.getParentOfType( WebRequestBoxContext.class ) == null ) {
				throw new BoxRuntimeException(
				    String.format( "The specified cache action [%s] is is not valid in a non-web runtime", cacheAction.toString().toLowerCase() ) );
			} else {
				String cacheDirective = null;
				if ( cacheAction.equals( CacheAction.SERVERCACHE ) ) {
					cacheDirective = timespan == null ? "server" : "s-max-age=" + DoubleCaster.cast( timespan * Cache.secondsInDay ).intValue();
				} else if ( cacheAction.equals( CacheAction.CLIENTCACHE ) ) {
					cacheDirective = timespan == null ? "private" : "max-age=" + DoubleCaster.cast( timespan * Cache.secondsInDay ).intValue();
				}
				if ( cacheDirective != null ) {
					componentService.getComponent( Key.header ).invoke(
					    context,
					    Struct.of(
					        Key._NAME, "Cache-Control",
					        Key.value, cacheDirective
					    ),
					    body
					);
					data.put( Key.result, cacheDirective );
				}
			}
		}

	}

}
