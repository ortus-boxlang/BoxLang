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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.watchers.exceptions.WatcherException;
import ortus.boxlang.runtime.watchers.listeners.IWatcherListener;

/**
 * Represents a single filesystem watcher.
 * <p>
 * Each instance wraps one Java NIO.2 {@link WatchService} running inside a virtual-thread loop.
 * Multiple paths can be registered (recursive or not). Debounce and throttle strategies
 * suppress noise before events reach the listener.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   CREATED ──► RUNNING ──► STOPPED
 *                  ▲____________|  (restart)
 * </pre>
 *
 * <p>Use the {@link Builder} to construct instances.</p>
 */
public class WatcherInstance {

	/**
	 * Watcher lifecycle states.
	 */
	public enum State {
		CREATED, RUNNING, STOPPED
	}

	// -------------------------------------------------------------------------
	// Config (immutable after construction)
	// -------------------------------------------------------------------------

	private final Key					name;
	private final List<Path>			watchPaths;
	private final boolean				recursive;
	private final long					debounce;
	private final long					throttle;
	private final boolean				atomicWrites;
	private final int					errorThreshold;
	private final IWatcherListener		listener;
	private final IBoxContext			parentContext;
	private final BoxLangLogger			logger;

	// -------------------------------------------------------------------------
	// Runtime state
	// -------------------------------------------------------------------------

	private volatile State				state				= State.CREATED;
	private WatchService				nioWatchService;
	private WatcherContext				watcherContext;
	private final Map<WatchKey, Path>	watchedKeys			= new ConcurrentHashMap<>();
	private ExecutorService				watchExecutor;
	private Future<?>					watchLoopFuture;
	private final AtomicInteger			consecutiveErrors	= new AtomicInteger( 0 );

	/** Debounce state: path → last event time (ms) */
	private final Map<Path, Long>		lastEventTime		= new ConcurrentHashMap<>();

	/** Throttle state: path → last fire time (ms) */
	private final Map<Path, Long>		lastFireTime		= new ConcurrentHashMap<>();

	// -------------------------------------------------------------------------
	// Constructor (package-private — use Builder)
	// -------------------------------------------------------------------------

	WatcherInstance( Builder builder, BoxLangLogger logger ) {
		this.name			= builder.name;
		this.watchPaths		= List.copyOf( builder.watchPaths );
		this.recursive		= builder.recursive;
		this.debounce		= builder.debounce;
		this.throttle		= builder.throttle;
		this.atomicWrites	= builder.atomicWrites;
		this.errorThreshold	= builder.errorThreshold;
		this.listener		= builder.listener;
		this.parentContext	= builder.parentContext;
		this.logger			= logger;
	}

	// -------------------------------------------------------------------------
	// Lifecycle
	// -------------------------------------------------------------------------

	/**
	 * Start the watcher. Registers all paths with a new {@link WatchService} and
	 * submits the watch loop to a virtual-thread executor.
	 *
	 * @return this instance (fluent)
	 * @throws WatcherException if a path cannot be registered
	 */
	public synchronized WatcherInstance start() {
		if ( state == State.RUNNING ) {
			return this;
		}

		logger.info( "+ Starting WatcherInstance [{}]...", name.getName() );

		try {
			this.nioWatchService	= FileSystems.getDefault().newWatchService();
			this.watchedKeys.clear();
			for ( Path p : watchPaths ) {
				registerPath( p );
			}
		} catch ( IOException e ) {
			throw new WatcherException( "Failed to initialise WatchService for watcher [" + name.getName() + "]", e );
		}

		this.watcherContext		= new WatcherContext( parentContext, this );
		this.watchExecutor		= AsyncService.buildExecutor( "watcher-" + name.getName(), AsyncService.ExecutorType.VIRTUAL, 0 ).executor();
		this.state				= State.RUNNING;
		this.watchLoopFuture	= watchExecutor.submit( this::watchLoop );

		logger.info( "+ WatcherInstance [{}] started, watching {} path(s)", name.getName(), watchPaths.size() );
		announceEvent( BoxEvent.ON_WATCHER_STARTUP );
		return this;
	}

	/**
	 * Stop the watcher. Cancels the watch loop, closes the NIO watch service, and
	 * shuts down the executor. The watcher can be restarted via {@link #restart()}.
	 *
	 * @return this instance (fluent)
	 */
	public synchronized WatcherInstance stop() {
		if ( state != State.RUNNING ) {
			return this;
		}

		logger.info( "+ Stopping WatcherInstance [{}]...", name.getName() );
		state = State.STOPPED;

		if ( watchLoopFuture != null ) {
			watchLoopFuture.cancel( true );
		}
		if ( nioWatchService != null ) {
			try {
				nioWatchService.close();
			} catch ( IOException ignored ) {
			}
		}
		if ( watchExecutor != null ) {
			watchExecutor.shutdownNow();
		}

		watchedKeys.clear();
		logger.info( "+ WatcherInstance [{}] stopped.", name.getName() );
		announceEvent( BoxEvent.ON_WATCHER_SHUTDOWN );
		return this;
	}

	/**
	 * Restart the watcher (stop + start).
	 *
	 * @return this instance (fluent)
	 */
	public WatcherInstance restart() {
		stop();
		announceEvent( BoxEvent.ON_WATCHER_RESTART );
		return start();
	}

	// -------------------------------------------------------------------------
	// Path management
	// -------------------------------------------------------------------------

	/**
	 * Register a single path (and its subdirectories when {@code recursive} is true)
	 * with the live watch service.
	 *
	 * @param path the directory path to watch
	 *
	 * @throws IOException if the path cannot be registered
	 */
	private void registerPath( Path path ) throws IOException {
		Path resolved = path.toAbsolutePath().normalize();

		if ( !Files.isDirectory( resolved ) ) {
			throw new WatcherException( "Watch path [" + resolved + "] is not a directory or does not exist.", null );
		}

		if ( recursive ) {
			Files.walkFileTree( resolved, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
					WatchKey key = dir.register( nioWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE );
					watchedKeys.put( key, dir );
					return FileVisitResult.CONTINUE;
				}
			} );
		} else {
			WatchKey key = resolved.register( nioWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE );
			watchedKeys.put( key, resolved );
		}
	}

	// -------------------------------------------------------------------------
	// Watch loop
	// -------------------------------------------------------------------------

	/**
	 * The main virtual-thread watch loop. Polls the {@link WatchService} and dispatches
	 * events through the configured timing strategy and listener.
	 */
	private void watchLoop() {
		while ( state == State.RUNNING && !Thread.currentThread().isInterrupted() ) {
			WatchKey watchKey;
			try {
				watchKey = nioWatchService.poll( 500, TimeUnit.MILLISECONDS );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				break;
			} catch ( Exception e ) {
				handleLoopError( e, null );
				continue;
			}

			if ( watchKey == null ) {
				continue;
			}

			for ( WatchEvent<?> raw : watchKey.pollEvents() ) {
				WatchEvent.Kind<?> rawKind = raw.kind();

				// OVERFLOW: no specific file context
				if ( rawKind == OVERFLOW ) {
					dispatchEvent( WatcherEvent.Kind.OVERFLOW, null, null );
					continue;
				}

				@SuppressWarnings( "unchecked" )
				WatchEvent<Path>	pathEvent	= ( WatchEvent<Path> ) raw;
				Path				dir			= ( Path ) watchKey.watchable();
				Path				child		= dir.resolve( pathEvent.context() ).toAbsolutePath().normalize();

				// Recursively register newly created subdirectories
				if ( recursive && rawKind == ENTRY_CREATE && Files.isDirectory( child ) ) {
					try {
						registerPath( child );
					} catch ( IOException e ) {
						handleLoopError( e, null );
					}
				}

				// Timing guards
				if ( debounce > 0 && shouldDebounce( child ) ) {
					continue;
				}
				if ( throttle > 0 && shouldThrottle( child ) ) {
					continue;
				}

				WatcherEvent.Kind kind = toEventKind( rawKind );
				dispatchEvent( kind, child, dir );
			}

			boolean valid = watchKey.reset();
			if ( !valid ) {
				watchedKeys.remove( watchKey );
				if ( watchedKeys.isEmpty() ) {
					logger.warn( "WatcherInstance [{}]: all watched directories have been removed; stopping.", name.getName() );
					state = State.STOPPED;
					break;
				}
			}
		}
	}

	private void dispatchEvent( WatcherEvent.Kind kind, Path child, Path watchRoot ) {
		WatcherEvent event;
		if ( child != null && watchRoot != null ) {
			Path relative = watchRoot.relativize( child );
			event = new WatcherEvent( kind, child, relative, watchRoot, Instant.now() );
		} else {
			event = new WatcherEvent( Instant.now() );
		}

		try {
			listener.onEvent( event, watcherContext );
			consecutiveErrors.set( 0 );
		} catch ( Exception e ) {
			handleLoopError( e, event );
		}
	}

	private void handleLoopError( Exception e, WatcherEvent event ) {
		WatcherException wrapped = ( e instanceof WatcherException we ) ? we : new WatcherException( e.getMessage(), event, e );
		int count = consecutiveErrors.incrementAndGet();

		try {
			listener.onError( wrapped, watcherContext );
		} catch ( Exception ignored ) {
			logger.error( "WatcherInstance [{}] onError itself threw: {}", name.getName(), ignored.getMessage() );
		}

		if ( errorThreshold > 0 && count >= errorThreshold ) {
			logger.warn(
			    "WatcherInstance [{}] hit error threshold ({}/{}), auto-stopping.",
			    name.getName(), count, errorThreshold
			);
			state = State.STOPPED;
			announceEvent( BoxEvent.ON_WATCHER_ERROR );
		}
	}

	// -------------------------------------------------------------------------
	// Timing strategies
	// -------------------------------------------------------------------------

	private boolean shouldDebounce( Path path ) {
		long now	= System.currentTimeMillis();
		long last	= lastEventTime.getOrDefault( path, 0L );
		lastEventTime.put( path, now );
		return ( now - last ) < debounce;
	}

	private boolean shouldThrottle( Path path ) {
		long now	= System.currentTimeMillis();
		long last	= lastFireTime.getOrDefault( path, 0L );
		if ( ( now - last ) < throttle ) {
			return true;
		}
		lastFireTime.put( path, now );
		return false;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private WatcherEvent.Kind toEventKind( WatchEvent.Kind<?> kind ) {
		if ( kind == ENTRY_CREATE ) return WatcherEvent.Kind.CREATED;
		if ( kind == ENTRY_MODIFY ) return WatcherEvent.Kind.MODIFIED;
		if ( kind == ENTRY_DELETE ) return WatcherEvent.Kind.DELETED;
		return WatcherEvent.Kind.OVERFLOW;
	}

	private void announceEvent( BoxEvent event ) {
		try {
			ortus.boxlang.runtime.BoxRuntime.getInstance()
			    .getInterceptorService()
			    .announce( event, Struct.of( Key.watcher, this ) );
		} catch ( Exception ignored ) {
		}
	}

	// -------------------------------------------------------------------------
	// Query methods
	// -------------------------------------------------------------------------

	public Key getName() {
		return name;
	}

	public boolean isRunning() {
		return state == State.RUNNING;
	}

	public State getState() {
		return state;
	}

	public List<Path> getWatchPaths() {
		return watchPaths;
	}

	public IWatcherListener getListener() {
		return listener;
	}

	public IStruct getStats() {
		Array pathArray = new Array();
		watchPaths.forEach( p -> pathArray.add( p.toString() ) );

		return Struct.ofNonConcurrent(
		    Key._name, name.getName(),
		    Key.of( "state" ), state.name(),
		    Key.paths, pathArray,
		    Key.recursive, recursive,
		    Key.debounce, debounce,
		    Key.throttle, throttle,
		    Key.atomicWrites, atomicWrites,
		    Key.errorThreshold, errorThreshold,
		    Key.of( "consecutiveErrors" ), consecutiveErrors.get()
		);
	}

	// -------------------------------------------------------------------------
	// Builder
	// -------------------------------------------------------------------------

	/**
	 * Create a builder for a new {@link WatcherInstance}.
	 *
	 * @param name the unique watcher name
	 *
	 * @return a new Builder
	 */
	public static Builder builder( Key name ) {
		return new Builder( name );
	}

	/**
	 * Fluent builder for {@link WatcherInstance}.
	 */
	public static class Builder {

		private final Key			name;
		private final List<Path>	watchPaths		= new ArrayList<>();
		private boolean				recursive		= true;
		private long				debounce		= 0L;
		private long				throttle		= 0L;
		private boolean				atomicWrites	= true;
		private int					errorThreshold	= 10;
		private IWatcherListener	listener;
		private IBoxContext			parentContext;

		private Builder( Key name ) {
			this.name = name;
		}

		public Builder paths( String... paths ) {
			for ( String p : paths ) {
				this.watchPaths.add( Paths.get( p ) );
			}
			return this;
		}

		public Builder paths( List<String> paths ) {
			paths.forEach( p -> this.watchPaths.add( Paths.get( p ) ) );
			return this;
		}

		public Builder addPath( String path ) {
			this.watchPaths.add( Paths.get( path ) );
			return this;
		}

		public Builder recursive( boolean recursive ) {
			this.recursive = recursive;
			return this;
		}

		public Builder debounce( long millis ) {
			this.debounce = millis;
			return this;
		}

		public Builder throttle( long millis ) {
			this.throttle = millis;
			return this;
		}

		public Builder atomicWrites( boolean atomicWrites ) {
			this.atomicWrites = atomicWrites;
			return this;
		}

		public Builder errorThreshold( int threshold ) {
			this.errorThreshold = threshold;
			return this;
		}

		public Builder listener( IWatcherListener listener ) {
			this.listener = listener;
			return this;
		}

		public Builder parentContext( IBoxContext context ) {
			this.parentContext = context;
			return this;
		}

		/**
		 * Build the WatcherInstance, using the WATCHER_LOGGER from the runtime's LoggingService.
		 *
		 * @return the configured (but not yet started) WatcherInstance
		 */
		public WatcherInstance build() {
			if ( listener == null ) {
				throw new IllegalStateException( "WatcherInstance [" + name.getName() + "] requires a listener." );
			}
			if ( watchPaths.isEmpty() ) {
				throw new IllegalStateException( "WatcherInstance [" + name.getName() + "] requires at least one path." );
			}
			BoxLangLogger log = ortus.boxlang.runtime.BoxRuntime.getInstance().getLoggingService().WATCHER_LOGGER;
			return new WatcherInstance( this, log );
		}
	}

}
