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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Return all registered watchers as a struct of name → WatcherInstance.
 */
@BoxBIF( description = "Return all registered filesystem watchers as a struct keyed by name." )
public class WatcherGetAll extends BIF {

	public WatcherGetAll() {
		super();
	}

	/**
	 * Retrieves all registered watchers as a struct keyed by watcher name.
	 *
	 * The returned struct is a snapshot of the service registry at invocation time,
	 * where each key is the watcher name and each value is the corresponding
	 * {@link ortus.boxlang.runtime.async.watchers.WatcherInstance}.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF. This BIF does not accept user arguments.
	 *
	 * @return A struct of watcher name to watcher instance mappings. Returns an empty struct when no watchers are registered.
	 */
	@Override
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Struct result = new Struct();
		this.runtime
		    .getWatcherService()
		    .getWatchers()
		    .forEach( ( k, v ) -> result.put( k, v ) );
		return result;
	}

}
