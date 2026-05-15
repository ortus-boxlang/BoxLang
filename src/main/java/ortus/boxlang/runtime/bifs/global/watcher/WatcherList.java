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
import ortus.boxlang.runtime.types.Array;

/**
 * Return an Array of all registered watcher names.
 */
@BoxBIF( description = "Return an Array of all registered filesystem watcher names." )
public class WatcherList extends BIF {

	public WatcherList() {
		super();
	}

	/**
	 * Lists the names of all watchers currently registered in the watcher service.
	 *
	 * This returns only watcher identifiers and does not include watcher metadata.
	 * Use {@code watcherGetAll()} when you need full watcher instances.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF. This BIF does not accept user arguments.
	 *
	 * @return An {@link ortus.boxlang.runtime.types.Array} of watcher names. Returns an empty array when no watchers are registered.
	 */
	@Override
	public Array _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return this.runtime.getWatcherService().getWatcherNames();
	}

}
