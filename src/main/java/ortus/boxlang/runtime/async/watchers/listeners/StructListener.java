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
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;

/**
 * A listener backed by a BoxLang {@link IStruct} of named handler functions.
 * <p>
 * Keys recognized in the struct:
 * <ul>
 * <li>{@code onCreate} — invoked for CREATED events</li>
 * <li>{@code onModify} — invoked for MODIFIED events</li>
 * <li>{@code onDelete} — invoked for DELETED events</li>
 * <li>{@code onOverflow} — invoked for OVERFLOW events</li>
 * <li>{@code onEvent} — invoked for all event kinds (alternative to per-kind handlers)</li>
 * <li>{@code onError} — invoked when another handler throws</li>
 * </ul>
 * All keys are optional — unmatched events are silently ignored.
 * </p>
 */
public class StructListener implements IWatcherListener {

	/**
	 * The struct containing handler functions. Expected keys: onCreate, onModify, onDelete, onOverflow, onEvent, onError. All optional.
	 */
	private final IStruct handlers;

	/**
	 * Construct from a struct of named BoxLang functions.
	 *
	 * @param handlers struct mapping handler keys to Function values
	 */
	public StructListener( IStruct handlers ) {
		this.handlers = handlers;
	}

	/**
	 * {@inheritDoc}
	 * Dispatches to the matching per-kind handler if present.
	 * If no per-kind handler is found, the {@code onEvent} handler is invoked if present.
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext ctx ) {
		// @formatter:off
		Key handlerKey = switch ( event.getKind() ) {
			case CREATED -> Key.onCreate;
			case MODIFIED -> Key.onModify;
			case DELETED -> Key.onDelete;
			case OVERFLOW -> Key.onOverflow;
		};
		// @formatter:on

		// Try the per-kind handler first
		Object	raw			= handlers.get( handlerKey );
		// Verify it's a Function before invoking
		if ( raw instanceof Function targetFunction ) {
			ThreadBoxContext.runInContext(
			    ctx.getBoxContext(),
			    true,
			    threadCtx -> threadCtx.invokeFunction(
			        targetFunction,
			        new Object[] { event.toStruct() }
			    )
			);
		}

		// Verify if we have a global onEvent handler and invoke it if so
		// This allows for a catch-all handler that receives all event kinds, even if specific handlers are defined for some kinds
		Object globalRaw = handlers.get( Key.onEvent );
		if ( globalRaw instanceof Function globalFunction ) {
			ThreadBoxContext.runInContext(
			    ctx.getBoxContext(),
			    true,
			    threadCtx -> threadCtx.invokeFunction(
			        globalFunction,
			        new Object[] { event.toStruct() }
			    )
			);
		}
	}

	/**
	 * {@inheritDoc}
	 * Invokes the {@code onError} handler if present.
	 */
	@Override
	public void onError( Exception exception, WatcherContext ctx ) {
		Object raw = handlers.get( Key.onError );
		if ( raw instanceof Function targetFunction ) {
			ThreadBoxContext.runInContext(
			    ctx.getBoxContext(),
			    true,
			    threadCtx -> threadCtx.invokeFunction(
			        targetFunction,
			        new Object[] { exception }
			    )
			);
		}
	}

}
