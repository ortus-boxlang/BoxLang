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
package ortus.boxlang.runtime.watchers;

import ortus.boxlang.runtime.context.IBoxContext;

/**
 * Carries the execution context required to invoke BoxLang listeners from inside a
 * {@link WatcherInstance} watch loop.
 * <p>
 * Created once at {@link WatcherInstance#start()} and reused for every event dispatch.
 * The {@link IBoxContext} is either the runtime context (for config-driven watchers)
 * or the calling BIF context (for BIF-created watchers).
 * </p>
 */
public class WatcherContext {

	private final IBoxContext		boxContext;
	private final WatcherInstance	watcher;

	/**
	 * Construct a watcher context.
	 *
	 * @param boxContext the BoxLang execution context for closure/function invocation
	 * @param watcher   the owning WatcherInstance
	 */
	public WatcherContext( IBoxContext boxContext, WatcherInstance watcher ) {
		this.boxContext	= boxContext;
		this.watcher	= watcher;
	}

	/**
	 * Get the BoxLang execution context.
	 * Required by all listener implementations to invoke closures, lambdas, and class methods.
	 *
	 * @return the IBoxContext
	 */
	public IBoxContext getBoxContext() {
		return boxContext;
	}

	/**
	 * Get the owning WatcherInstance.
	 *
	 * @return the WatcherInstance
	 */
	public WatcherInstance getWatcher() {
		return watcher;
	}

}
