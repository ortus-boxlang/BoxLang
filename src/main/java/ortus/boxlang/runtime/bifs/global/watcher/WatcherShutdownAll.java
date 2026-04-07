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
package ortus.boxlang.runtime.bifs.global.watcher;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

/**
 * Stop all watchers and remove them from the registry.
 */
@BoxBIF( description = "Stop all filesystem watchers and remove them from the registry." )
public class WatcherShutdownAll extends BIF {

	public WatcherShutdownAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.BOOLEAN, Key.of( "force" ), false )
		};
	}

	/**
	 * Stops all watchers and removes them from the watcher registry.
	 *
	 * This is the destructive bulk operation for watcher lifecycle management.
	 * After this call, all previous watcher registrations are cleared and must be
	 * recreated before use.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @argument.force Whether the shutdown should be treated as a forced shutdown.
	 *
	 * @return {@code null}. This BIF performs side effects only.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		runtime.getWatcherService().shutdownAll( BooleanCaster.cast( arguments.get( Key.of( "force" ) ) ) );
		return null;
	}

}
