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
package ortus.boxlang.runtime.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.WatcherConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.watchers.WatcherInstance;
import ortus.boxlang.runtime.watchers.listeners.ClassListener;

/**
 * Runtime service that manages all {@link WatcherInstance} objects.
 * <p>
 * Mirrors {@link SchedulerService} in architecture: a singleton service registered with
 * {@link BoxRuntime}, maintaining a {@link ConcurrentHashMap} of named watcher instances.
 * Config-driven watchers are loaded from {@code boxlang.json} at startup.
 * </p>
 */
public class WatcherService extends BaseService {

	// -------------------------------------------------------------------------
	// Private fields
	// -------------------------------------------------------------------------

	/**
	 * Registry of named watchers.
	 */
	private final Map<Key, WatcherInstance>	watchers	= new ConcurrentHashMap<>();

	/**
	 * Dedicated logger — writes to {@code watcher.log}.
	 */
	private BoxLangLogger					logger;

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	/**
	 * Construct the WatcherService. Called by BoxRuntime during startup.
	 *
	 * @param runtime the BoxRuntime singleton
	 */
	public WatcherService( BoxRuntime runtime ) {
		super( runtime, Key.watcherService );
	}

	// -------------------------------------------------------------------------
	// IService lifecycle
	// -------------------------------------------------------------------------

	/**
	 * Called when the runtime loads configuration. Assigns the watcher logger.
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger = runtime.getLoggingService().WATCHER_LOGGER;
	}

	/**
	 * Called when the runtime starts up. Loads config-driven watchers and starts them all.
	 */
	@Override
	public void onStartup() {
		timerUtil.start( "watcherservice-startup" );
		logger.info( "+ Starting up Watcher Service..." );

		registerGlobalWatchers();

		watchers.values()
		    .parallelStream()
		    .forEach( w -> {
			    try {
				    w.start();
			    } catch ( Exception e ) {
				    logger.error( "Failed to start watcher [{}]: {}", w.getName().getName(), e.getMessage() );
			    }
		    } );

		announce(
		    BoxEvent.ON_WATCHER_SERVICE_STARTUP,
		    Struct.of( Key.watcherService, this )
		);
		announce(
		    BoxEvent.ON_ALL_WATCHERS_STARTED,
		    Struct.of( Key.watchers, getWatcherNames() )
		);

		logger.info( "+ Watcher Service started in [{}] ms", timerUtil.stopAndGetMillis( "watcherservice-startup" ) );
	}

	/**
	 * Called when the runtime shuts down. Stops all watchers and clears the registry.
	 *
	 * @param force if true, forces an immediate shutdown
	 */
	@Override
	public void onShutdown( Boolean force ) {
		logger.info( "+ Shutting down Watcher Service..." );

		watchers.values()
		    .parallelStream()
		    .forEach( w -> {
			    try {
				    w.stop();
			    } catch ( Exception e ) {
				    logger.error( "Error stopping watcher [{}] during shutdown: {}", w.getName().getName(), e.getMessage() );
			    }
		    } );

		watchers.clear();

		announce(
		    BoxEvent.ON_WATCHER_SERVICE_SHUTDOWN,
		    Struct.of( Key.watcherService, this )
		);

		logger.info( "+ Watcher Service shut down." );
	}

	// -------------------------------------------------------------------------
	// Registry methods
	// -------------------------------------------------------------------------

	/**
	 * Register a watcher instance.
	 *
	 * @param watcher the watcher to register
	 * @param force   if true, replaces an existing watcher with the same name (stopping it first)
	 *
	 * @return the registered watcher
	 * @throws BoxRuntimeException if a watcher with the same name already exists and force is false
	 */
	public WatcherInstance register( WatcherInstance watcher, boolean force ) {
		Key key = watcher.getName();

		if ( watchers.containsKey( key ) ) {
			if ( force ) {
				watchers.get( key ).stop();
			} else {
				throw new BoxRuntimeException( "A watcher named [" + key.getName() + "] is already registered. Use force=true to replace it." );
			}
		}

		watchers.put( key, watcher );
		announce( BoxEvent.ON_WATCHER_REGISTRATION, Struct.of( Key.watcher, watcher ) );
		return watcher;
	}

	/**
	 * Register and immediately start a watcher.
	 *
	 * @param watcher the watcher to register and start
	 * @param force   replace an existing watcher if present
	 *
	 * @return the started watcher
	 */
	public WatcherInstance registerAndStart( WatcherInstance watcher, boolean force ) {
		register( watcher, force );
		watcher.start();
		return watcher;
	}

	/**
	 * Get a registered watcher by name.
	 *
	 * @param name the watcher name
	 *
	 * @return the watcher, or null if not found
	 */
	public WatcherInstance getWatcher( Key name ) {
		return watchers.get( name );
	}

	/**
	 * Get a registered watcher by name or throw.
	 *
	 * @param name the watcher name
	 *
	 * @return the watcher
	 * @throws BoxRuntimeException if not found
	 */
	public WatcherInstance getWatcherOrFail( Key name ) {
		WatcherInstance w = watchers.get( name );
		if ( w == null ) {
			throw new BoxRuntimeException( "No watcher named [" + name.getName() + "] is registered." );
		}
		return w;
	}

	/**
	 * Check whether a watcher is registered.
	 *
	 * @param name the watcher name
	 *
	 * @return true if registered
	 */
	public boolean hasWatcher( Key name ) {
		return watchers.containsKey( name );
	}

	/**
	 * Remove a registered watcher, stopping it first.
	 *
	 * @param name the watcher name
	 *
	 * @return true if found and removed, false if not present
	 */
	public boolean removeWatcher( Key name ) {
		WatcherInstance w = watchers.remove( name );
		if ( w != null ) {
			w.stop();
			announce( BoxEvent.ON_WATCHER_REMOVAL, Struct.of( Key.watcher, w ) );
			return true;
		}
		return false;
	}

	/**
	 * Get all registered watchers as an unmodifiable map view.
	 *
	 * @return map of Key → WatcherInstance
	 */
	public Map<Key, WatcherInstance> getWatchers() {
		return Map.copyOf( watchers );
	}

	/**
	 * Get an array of all registered watcher names.
	 *
	 * @return Array of name strings
	 */
	public Array getWatcherNames() {
		Array names = new Array();
		watchers.keySet().forEach( k -> names.add( k.getName() ) );
		return names;
	}

	/**
	 * Stop all running watchers without removing them from the registry.
	 */
	public void stopAll() {
		watchers.values().parallelStream().forEach( w -> {
			try {
				w.stop();
			} catch ( Exception e ) {
				logger.error( "Error stopping watcher [{}]: {}", w.getName().getName(), e.getMessage() );
			}
		} );
	}

	/**
	 * Stop all watchers and clear the registry.
	 *
	 * @param force if true, forces immediate shutdown
	 */
	public void shutdownAll( Boolean force ) {
		stopAll();
		watchers.clear();
	}

	/**
	 * Return the number of registered watchers.
	 *
	 * @return watcher count
	 */
	public int size() {
		return watchers.size();
	}

	// -------------------------------------------------------------------------
	// Config-driven startup
	// -------------------------------------------------------------------------

	/**
	 * Register all watchers defined in the {@code watcher.definitions} config block.
	 * Each definition must supply at minimum {@code paths} and {@code listener} (a class name string).
	 */
	private void registerGlobalWatchers() {
		WatcherConfig	config			= runtime.getConfiguration().watcher;
		IBoxContext		runtimeContext	= runtime.getRuntimeContext();

		if ( config.definitions == null || config.definitions.isEmpty() ) {
			return;
		}

		config.definitions.entrySet().forEach( entry -> {
			Key		watcherName	= entry.getKey();
			Object	raw			= entry.getValue();

			if ( ! ( raw instanceof IStruct def ) ) {
				logger.warn( "Watcher definition [{}] is not a struct; skipping.", watcherName.getName() );
				return;
			}

			// Required: paths
			if ( !def.containsKey( Key.paths ) ) {
				logger.warn( "Watcher definition [{}] is missing 'paths'; skipping.", watcherName.getName() );
				return;
			}

			// Required: listener (class name string)
			if ( !def.containsKey( Key.listener ) ) {
				logger.warn( "Watcher definition [{}] is missing 'listener'; skipping.", watcherName.getName() );
				return;
			}

			String listenerClass = StringCaster.cast( def.get( Key.listener ) );

			// Inherit defaults → override with per-definition values
			boolean	defRecursive		= def.containsKey( Key.recursive ) ? BooleanCaster.cast( def.get( Key.recursive ) ) : config.recursive;
			long	defDebounce			= def.containsKey( Key.debounce ) ? LongCaster.cast( def.get( Key.debounce ) ) : config.debounce;
			long	defThrottle			= def.containsKey( Key.throttle ) ? LongCaster.cast( def.get( Key.throttle ) ) : config.throttle;
			boolean	defAtomicWrites		= def.containsKey( Key.atomicWrites ) ? BooleanCaster.cast( def.get( Key.atomicWrites ) ) : config.atomicWrites;
			int		defErrorThreshold	= def.containsKey( Key.errorThreshold ) ? IntegerCaster.cast( def.get( Key.errorThreshold ) ) : config.errorThreshold;

			// Build path list
			WatcherInstance.Builder builder = WatcherInstance.builder( watcherName )
			    .recursive( defRecursive )
			    .debounce( defDebounce )
			    .throttle( defThrottle )
			    .atomicWrites( defAtomicWrites )
			    .errorThreshold( defErrorThreshold )
			    .parentContext( runtimeContext )
			    .listener( new ClassListener( listenerClass, runtimeContext ) );

			Object pathsRaw = def.get( Key.paths );
			if ( pathsRaw instanceof Array pathArray ) {
				pathArray.forEach( p -> builder.addPath( StringCaster.cast( p ) ) );
			} else {
				builder.addPath( StringCaster.cast( pathsRaw ) );
			}

			try {
				WatcherInstance watcher = builder.build();
				register( watcher, false );
				logger.info( "+ Registered config-driven watcher [{}]", watcherName.getName() );
			} catch ( Exception e ) {
				logger.error( "Failed to register watcher [{}]: {}", watcherName.getName(), e.getMessage() );
			}
		} );
	}

}
