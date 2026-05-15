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

import ortus.boxlang.runtime.async.watchers.WatcherInstance;
import ortus.boxlang.runtime.async.watchers.listeners.ClassListener;
import ortus.boxlang.runtime.async.watchers.listeners.ClosureListener;
import ortus.boxlang.runtime.async.watchers.listeners.IWatcherListener;
import ortus.boxlang.runtime.async.watchers.listeners.StructListener;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Create and register a new {@link WatcherInstance} without starting it.
 * Call {@code watcherStart(name)} to begin watching.
 *
 * <p>
 * The {@code listener} argument accepts three forms:
 * <ul>
 * <li>A <strong>Function</strong> (closure/lambda) — invoked for every event</li>
 * <li>An <strong>IStruct</strong> of named functions — keys: {@code onCreate}, {@code onModify}, {@code onDelete}, {@code onOverflow}, {@code onError}</li>
 * <li>A <strong>String</strong> class name — the class is instantiated and its {@code onEvent} method is called</li>
 * </ul>
 */
@BoxBIF( description = "Create and register a new filesystem watcher. Returns the WatcherInstance." )
public class WatcherNew extends BIF {

	public WatcherNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key._name ),
		    new Argument( true, Argument.ANY, Key.paths ),
		    new Argument( true, Argument.ANY, Key.listener ),
		    new Argument( false, Argument.BOOLEAN, Key.recursive, true ),
		    new Argument( false, Argument.LONG, Key.debounce, 0L ),
		    new Argument( false, Argument.LONG, Key.throttle, 0L ),
		    new Argument( false, Argument.BOOLEAN, Key.atomicWrites, true ),
		    new Argument( false, Argument.LONG, Key.delay, 0L ),
		    new Argument( false, Argument.INTEGER, Key.errorThreshold, 10 ),
		    new Argument( false, Argument.BOOLEAN, Key.force, false )
		};
	}

	/**
	 * Creates and registers a new watcher instance in the watcher service.
	 *
	 * The watcher is registered but not started. Call {@code watcherStart( name )} to
	 * begin processing filesystem events or {@code start()} on the returned WatcherInstance.
	 *
	 * You can register three types of listeners, which BoxLang will automatically wrap into an IWatcherListener implementation:
	 * <ul>
	 * <li>{@link Function}: wrapped as a {@code ClosureListener}. Can only handle the {@code onEvent} method.</li>
	 * <li>{@link IStruct}: wrapped as a {@code StructListener}. Can handle {@code onEvent} and {@code onError} methods.</li>
	 * <li>{@link String}: treated as a class name and wrapped as a {@code ClassListener}. The class must implement {@code IWatcherListener}.</li>
	 * </ul>
	 *
	 * If {@code name} is omitted or blank, a unique watcher name is auto-generated.
	 *
	 * @param context   The BoxContext of the caller.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @argument.name Optional watcher name. If blank, an auto-generated name is used.
	 *
	 * @argument.paths A directory path string or an array of directory path strings to watch.
	 *
	 * @argument.listener Listener definition: function, struct listener map, or listener class name string.
	 *
	 * @argument.recursive Whether to watch subdirectories recursively.
	 *
	 * @argument.debounce Debounce window in milliseconds.
	 *
	 * @argument.throttle Throttle window in milliseconds.
	 *
	 * @argument.atomicWrites Whether atomic write filtering is enabled.
	 *
	 * @argument.delay Startup delay in milliseconds.
	 *
	 * @argument.errorThreshold Consecutive listener errors before auto-stop.
	 *
	 * @argument.force Whether to replace an existing watcher with the same name.
	 *
	 * @return The registered {@link WatcherInstance}.
	 *
	 * @throws BoxRuntimeException If listener resolution fails or the watcher cannot be registered.
	 */
	@Override
	public WatcherInstance _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Determine watcher name (auto-generate if not supplied)
		String				nameArg		= arguments.getAsString( Key._name );
		Key					name		= ( nameArg != null && !nameArg.isBlank() ) ? Key.of( nameArg ) : Key.of( "watcher-" + System.nanoTime() );

		// Resolve listener
		Object				listenerArg	= arguments.get( Key.listener );
		IWatcherListener	listener	= resolveListener( listenerArg, context );

		// If the incoming paths is a string, convert it to a single-element array for easier processing
		Object				pathsArg	= arguments.get( Key.paths );
		Array				pathArray	= new Array();
		if ( pathsArg instanceof String singlePath ) {
			pathArray.add( singlePath );
			arguments.put( Key.paths, pathArray );
		} else if ( pathsArg instanceof Array arr ) {
			pathArray = arr;
		} else {
			throw new BoxRuntimeException( "watcherNew: 'paths' argument must be a string or an array of strings." );
		}

		// Build the watcher
		WatcherInstance watcher = WatcherInstance.builder( name )
		    .paths( pathArray )
		    .recursive( BooleanCaster.cast( arguments.get( Key.recursive ) ) )
		    .debounce( LongCaster.cast( arguments.get( Key.debounce ) ) )
		    .throttle( LongCaster.cast( arguments.get( Key.throttle ) ) )
		    .atomicWrites( BooleanCaster.cast( arguments.get( Key.atomicWrites ) ) )
		    .errorThreshold( IntegerCaster.cast( arguments.get( Key.errorThreshold ) ) )
		    .parentContext( context )
		    .listener( listener )
		    .build();

		return this.runtime
		    .getWatcherService()
		    .register(
		        watcher,
		        BooleanCaster.cast( arguments.get( Key.force ) )
		    );
	}

	/**
	 * Resolves the listener argument into an IWatcherListener implementation.
	 *
	 * @param listenerArg The listener argument, which can be a Function, IStruct, or String.
	 * @param context     The BoxContext, required for ClassListener instantiation.
	 *
	 * @throws BoxRuntimeException If the listenerArg is not a supported type or if class instantiation fails.
	 *
	 * @return An IWatcherListener instance corresponding to the input argument.
	 */
	private IWatcherListener resolveListener( Object listenerArg, IBoxContext context ) {
		// A Function becomes a ClosureListener; an IStruct becomes a StructListener; a String becomes a ClassListener

		if ( listenerArg instanceof Function fn ) {
			return new ClosureListener( fn );
		}

		if ( listenerArg instanceof IStruct structArg ) {
			return new StructListener( structArg );
		}

		if ( listenerArg instanceof IClassRunnable icr ) {
			return new ClassListener( icr, context );
		}

		if ( listenerArg instanceof String className ) {
			return new ClassListener( className, context );
		}

		throw new BoxRuntimeException( "watcherNew: listener must be a Function, IStruct of functions, or a class name String." );
	}

}
