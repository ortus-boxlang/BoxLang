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

/**
 * A listener that delegates filesystem events to a single BoxLang {@link Function}
 * (closure, lambda, or UDF).
 * <p>
 * The function receives the event as a single positional argument: an {@link ortus.boxlang.runtime.types.IStruct}
 * produced by {@link WatcherEvent#toStruct()}.
 * </p>
 * <p>
 * An optional {@code onError} function can be supplied; if absent, errors propagate to
 * the WatcherInstance's built-in logging fallback.
 * </p>
 */
public class ClosureListener implements IWatcherListener {

	private final Function	onEventFn;

	/** Optional — invoked when onEvent throws. May be null. */
	private final Function	onErrorFn;

	/**
	 * Construct with an event handler only.
	 *
	 * @param onEventFn the BoxLang function to invoke for each event
	 */
	public ClosureListener( Function onEventFn ) {
		this( onEventFn, null );
	}

	/**
	 * Construct with both an event handler and an error handler.
	 *
	 * @param onEventFn the BoxLang function to invoke for each event
	 * @param onErrorFn the BoxLang function to invoke on error (may be null)
	 */
	public ClosureListener( Function onEventFn, Function onErrorFn ) {
		this.onEventFn	= onEventFn;
		this.onErrorFn	= onErrorFn;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Invokes the BoxLang function with the event struct as its first argument.
	 * </p>
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext ctx ) {
		FunctionBoxContext functionContext = Function.generateFunctionContext(
		    onEventFn,
		    ctx.getBoxContext(),
		    Key.onEvent,
		    new Object[] { event.toStruct() },
		    null,
		    null,
		    null
		);
		onEventFn.invoke( functionContext );
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Invokes the optional error function if one was supplied.
	 * </p>
	 */
	@Override
	public void onError( Exception exception, WatcherContext ctx ) {
		if ( onErrorFn != null ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    onErrorFn,
			    ctx.getBoxContext(),
			    Key.onError,
			    new Object[] { exception.getMessage(), exception },
			    null,
			    null,
			    null
			);
			onErrorFn.invoke( functionContext );
		}
	}

}
