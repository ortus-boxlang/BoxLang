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
 * Start a registered watcher by name.
 */
@BoxBIF( description = "Start a registered filesystem watcher by name." )
public class WatcherStart extends BIF {

	public WatcherStart() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._name, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Starts a registered watcher by name.
	 *
	 * If the watcher is already running, the underlying watcher implementation returns
	 * the same instance without duplicating execution loops.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @argument.name The unique watcher name to start.
	 *
	 * @return The started {@link ortus.boxlang.runtime.watchers.WatcherInstance}.
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.BoxRuntimeException If no watcher with the given name is registered.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return runtime.getWatcherService()
		    .getWatcherOrFail( Key.of( arguments.getAsString( Key._name ) ) )
		    .start();
	}

}
