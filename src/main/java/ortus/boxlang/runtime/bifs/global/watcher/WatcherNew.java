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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.watchers.WatcherInstance;
import ortus.boxlang.runtime.watchers.listeners.ClassListener;
import ortus.boxlang.runtime.watchers.listeners.ClosureListener;
import ortus.boxlang.runtime.watchers.listeners.IWatcherListener;
import ortus.boxlang.runtime.watchers.listeners.StructListener;

/**
 * Create and register a new {@link WatcherInstance} without starting it.
 * Call {@code watcherStart(name)} to begin watching.
 *
 * <p>The {@code listener} argument accepts three forms:
 * <ul>
 *   <li>A <strong>Function</strong> (closure/lambda) — invoked for every event</li>
 *   <li>An <strong>IStruct</strong> of named functions — keys: {@code onCreate}, {@code onModify}, {@code onDelete}, {@code onOverflow}, {@code onError}</li>
 *   <li>A <strong>String</strong> class name — the class is instantiated and its {@code onEvent} method is called</li>
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
		    new Argument( false, Argument.BOOLEAN, Key.of( "force" ), false )
		};
	}

	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Determine watcher name (auto-generate if not supplied)
		String	nameArg	= arguments.getAsString( Key._name );
		Key		name	= ( nameArg != null && !nameArg.isBlank() ) ? Key.of( nameArg ) : Key.of( "watcher-" + System.nanoTime() );

		// Resolve listener
		Object				listenerArg	= arguments.get( Key.listener );
		IWatcherListener	listener	= resolveListener( listenerArg, context );

		// Build the watcher
		WatcherInstance.Builder builder = WatcherInstance.builder( name )
		    .recursive( BooleanCaster.cast( arguments.get( Key.recursive ) ) )
		    .debounce( LongCaster.cast( arguments.get( Key.debounce ) ) )
		    .throttle( LongCaster.cast( arguments.get( Key.throttle ) ) )
		    .atomicWrites( BooleanCaster.cast( arguments.get( Key.atomicWrites ) ) )
		    .errorThreshold( IntegerCaster.cast( arguments.get( Key.errorThreshold ) ) )
		    .parentContext( context )
		    .listener( listener );

		// Add paths
		Object pathsArg = arguments.get( Key.paths );
		if ( pathsArg instanceof Array pathArray ) {
			pathArray.forEach( p -> builder.addPath( StringCaster.cast( p ) ) );
		} else {
			builder.addPath( StringCaster.cast( pathsArg ) );
		}

		WatcherInstance	watcher	= builder.build();
		boolean			force	= BooleanCaster.cast( arguments.get( Key.of( "force" ) ) );

		return runtime.getWatcherService().register( watcher, force );
	}

	private IWatcherListener resolveListener( Object listenerArg, IBoxContext context ) {
		if ( listenerArg instanceof Function fn ) {
			return new ClosureListener( fn );
		}
		if ( listenerArg instanceof IStruct structArg ) {
			return new StructListener( structArg );
		}
		if ( listenerArg instanceof String className ) {
			return new ClassListener( className, context );
		}
		// Try coercing to String
		String asString = StringCaster.cast( listenerArg );
		if ( asString != null && !asString.isBlank() ) {
			return new ClassListener( asString, context );
		}
		throw new BoxRuntimeException( "watcherNew: listener must be a Function, IStruct of functions, or a class name String." );
	}

}
