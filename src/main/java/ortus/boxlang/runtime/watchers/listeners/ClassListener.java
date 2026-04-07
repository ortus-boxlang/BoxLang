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
package ortus.boxlang.runtime.watchers.listeners;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.watchers.WatcherContext;
import ortus.boxlang.runtime.watchers.WatcherEvent;

/**
 * A listener that resolves a BoxLang class by name and delegates events to its
 * {@code onEvent(event)} and {@code onError(message, exception)} methods.
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
	 * {@inheritDoc}
	 * Invokes {@code instance.onEvent(eventStruct)} via {@link DynamicObject}.
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext ctx ) {
		this.instance.dereferenceAndInvoke( this.context, Key.onEvent, new Object[] { event.toStruct() }, false );
	}

	/**
	 * {@inheritDoc}
	 * Invokes {@code instance.onError(message, exception)} if the method exists;
	 * silently ignores missing-method errors so the WatcherInstance fallback can log.
	 */
	@Override
	public void onError( Exception exception, WatcherContext ctx ) {
		try {
			this.instance.dereferenceAndInvoke( this.context, Key.onError, new Object[] { exception.getMessage(), exception }, false );
		} catch ( Exception ignored ) {
			// WatcherInstance logs to watcher.log
		}
	}

}
