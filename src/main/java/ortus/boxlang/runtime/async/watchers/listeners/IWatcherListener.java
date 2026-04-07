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
package ortus.boxlang.runtime.async.watchers.listeners;

import ortus.boxlang.runtime.async.watchers.WatcherContext;
import ortus.boxlang.runtime.async.watchers.WatcherEvent;

/**
 * Contract for all watcher listener implementations.
 * <p>
 * {@link #onEvent} is called for every filesystem event that passes the timing guards
 * (debounce / throttle). {@link #onError} has a default no-op implementation; the
 * {@link ortus.boxlang.runtime.async.watchers.WatcherInstance} falls back to logging when
 * neither the listener nor its {@code onError} override handles the exception.
 * </p>
 *
 * <p>
 * Implementations <strong>must not</strong> throw from {@code onEvent} — catch and
 * handle internally or let the {@code WatcherInstance} handle it via {@code onError}.
 * </p>
 */
public interface IWatcherListener {

	/**
	 * Invoked for each filesystem event that passes the configured timing strategy.
	 *
	 * @param event   the filesystem event
	 * @param context the watcher execution context (carries IBoxContext for closure invocation)
	 */
	void onEvent( WatcherEvent event, WatcherContext context );

	/**
	 * Invoked when {@link #onEvent} throws or when the watch loop encounters a loop-level error.
	 * <p>
	 * The {@code exception} is either the raw exception thrown by the listener or a
	 * {@link ortus.boxlang.runtime.async.watchers.exceptions.WatcherException} wrapping it with
	 * event context when available.
	 * </p>
	 * <p>
	 * Default implementation is a no-op; the WatcherInstance logs to {@code watcher.log}.
	 * </p>
	 *
	 * @param exception the error that occurred
	 * @param context   the watcher execution context
	 */
	default void onError( Exception exception, WatcherContext context ) {
		// no-op — WatcherInstance logs to watcher.log as fallback
	}

}
