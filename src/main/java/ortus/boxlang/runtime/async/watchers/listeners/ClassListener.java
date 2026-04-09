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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.watchers.WatcherContext;
import ortus.boxlang.runtime.async.watchers.WatcherEvent;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;

/**
 * A listener that resolves a BoxLang class by name and delegates events to the different methods on that class.
 * The only mandatory method is {@code onEvent}.
 * <p>
 * Methods allowed are:
 * <ul>
 * <li>{@code onCreate} — invoked for CREATED events</li>
 * <li>{@code onModify} — invoked for MODIFIED events</li>
 * <li>{@code onDelete} — invoked for DELETED events</li>
 * <li>{@code onOverflow} — invoked for OVERFLOW events</li>
 * <li>{@code onEvent} — invoked for all event kinds (alternative to per-kind handlers)</li>
 * <li>{@code onError} — invoked when another handler throws</li>
 * </ul>
 * </p>
 * <p>
 * Class resolution uses the exact pattern from
 * {@code SchedulerStart.startScheduler()}: calling the global {@code createObject}
 * BIF via {@link ortus.boxlang.runtime.services.FunctionService#getGlobalFunction},
 * which honours all BoxLang class-path lookups, module classes, and mappings.
 * </p>
 * <p>
 * The listener instance is created once and reused for all events.
 * </p>
 */
public class ClassListener implements IWatcherListener {

	private final IClassRunnable	instance;
	private final IBoxContext		context;

	/**
	 * Resolve and instantiate the given class name.
	 *
	 * @param className the fully-qualified BoxLang class name (e.g. {@code "models.MyListener"})
	 * @param context   the IBoxContext used for class resolution and method invocation
	 */
	public ClassListener( String className, IBoxContext context ) {
		this.context	= context;
		this.instance	= ( IClassRunnable ) BoxRuntime.getInstance()
		    .getFunctionService()
		    .getGlobalFunction( Key.createObject )
		    .invoke( context, new Object[] { className }, false, Key.createObject );
	}

	/**
	 * Wrap an already-instantiated {@link IClassRunnable} as a listener.
	 * The instance is used as-is; no class resolution is performed.
	 *
	 * @param instance the pre-instantiated BoxLang class instance
	 * @param context  the IBoxContext used for method invocation
	 */
	public ClassListener( IClassRunnable instance, IBoxContext context ) {
		this.context	= context;
		this.instance	= instance;
	}

	/**
	 * {@inheritDoc}
	 * Invokes {@code instance.onEvent(eventStruct)} via {@link DynamicObject}.
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext ctx ) {
		// @formatter:off
		Key methodKey = switch ( event.getKind() ) {
			case CREATED -> Key.onCreate;
			case MODIFIED -> Key.onModify;
			case DELETED -> Key.onDelete;
			case OVERFLOW -> Key.onOverflow;
		};
		// @formatter:on

		// Verify if the class has the method, if so, call it.
		if ( this.instance.getThisScope().containsKey( methodKey ) ) {
			ThreadBoxContext.runInContext(
			    ctx.getBoxContext(),
			    true,
			    threadCtx -> this.instance.dereferenceAndInvoke( threadCtx, methodKey, new Object[] { event.toStruct() }, false )
			);
		}

		// Send to the generic onEvent handler if it exists and wasn't already handled by a specific method
		// This is a mandatory signature for ClassListener, so we can skip the existence check
		ThreadBoxContext.runInContext(
		    ctx.getBoxContext(),
		    true,
		    threadCtx -> this.instance.dereferenceAndInvoke( threadCtx, Key.onEvent, new Object[] { event.toStruct() }, false )
		);
	}

	/**
	 * {@inheritDoc}
	 * Invokes {@code instance.onError(message, exception)} if the method exists;
	 * silently ignores missing-method errors so the WatcherInstance fallback can log.
	 */
	@Override
	public void onError( Exception exception, WatcherContext ctx ) {
		if ( this.instance.getThisScope().containsKey( Key.onError ) ) {
			ThreadBoxContext.runInContext(
			    ctx.getBoxContext(),
			    true,
			    threadCtx -> this.instance.dereferenceAndInvoke( threadCtx, Key.onError, new Object[] { exception }, false )
			);
		}
	}

}
