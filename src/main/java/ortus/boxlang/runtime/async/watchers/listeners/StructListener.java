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
import ortus.boxlang.runtime.context.FunctionBoxContext;
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
 * <li>{@code onError} — invoked when another handler throws</li>
 * </ul>
 * All keys are optional — unmatched events are silently ignored.
 * </p>
 */
public class StructListener implements IWatcherListener {

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
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext ctx ) {
		Key		handlerKey	= switch ( event.getKind() ) {
								case CREATED -> Key.onCreate;
								case MODIFIED -> Key.onModify;
								case DELETED -> Key.onDelete;
								case OVERFLOW -> Key.onOverflow;
							};

		Object	raw			= handlers.get( handlerKey );

		if ( raw instanceof Function targetFunction ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    targetFunction,
			    ctx.getBoxContext(),
			    handlerKey,
			    new Object[] { event.toStruct() },
			    null,
			    null,
			    null
			);
			targetFunction.invoke( functionContext );
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
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    targetFunction,
			    ctx.getBoxContext(),
			    Key.onError,
			    new Object[] { exception.getMessage(), exception },
			    null,
			    null,
			    null
			);
			targetFunction.invoke( functionContext );
		}
	}

}
