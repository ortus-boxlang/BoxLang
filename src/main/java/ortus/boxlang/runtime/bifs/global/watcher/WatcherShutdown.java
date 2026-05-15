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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

/**
 * Stop and unregister a watcher by name.
 */
@BoxBIF( description = "Stop and unregister a filesystem watcher by name." )
public class WatcherShutdown extends BIF {

	public WatcherShutdown() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._name, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Stops and removes a registered watcher from the watcher registry.
	 *
	 * This is the destructive single-watcher lifecycle operation. After this call,
	 * the watcher is no longer registered and must be recreated before use.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @argument.name The unique watcher name to shut down.
	 *
	 * @return {@code true} if the watcher was removed; otherwise {@code false}.
	 */
	@Override
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return this.runtime
		    .getWatcherService()
		    .removeWatcher( Key.of( arguments.getAsString( Key._name ) ) );
	}

}
