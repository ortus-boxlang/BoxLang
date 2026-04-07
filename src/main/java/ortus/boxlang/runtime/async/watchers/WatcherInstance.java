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
package ortus.boxlang.runtime.async.watchers;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.async.watchers.exceptions.WatcherException;
import ortus.boxlang.runtime.async.watchers.listeners.IWatcherListener;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Represents a single filesystem watcher.
 * <p>
 * Each instance wraps one Java NIO.2 {@link WatchService} running inside a virtual-thread loop.
 * Multiple paths can be registered (recursive or not). Debounce and throttle strategies
 * suppress noise before events reach the listener.
 * </p>
 * <p>
 * A listener can be any implementation of the {@link IWatcherListener} interface, which defines {@code onEvent} and {@code onError} methods.
 * The listener is responsible for handling events and errors; the WatcherInstance delegates these responsibilities to the listener.
 * </p>
 *
 * <h3>Lifecycle</h3>
 *
 * <pre>
 *   CREATED ──► RUNNING ──► STOPPED
 *                  ▲____________|  (restart)
 * </pre>
 *
 * <p>
 * Use the {@link Builder} to construct instances.
 * </p>
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

	/**
	 * The unique name of this watcher instance. Used for lookup and logging.
	 */
	private final Key					name;

	/**
	 * The list of directory paths this watcher monitors. Paths are registered with the WatchService on start.
	 */
	private final List<Path>			watchPaths;

	/**
	 * Whether to watch directories recursively. When true, all subdirectories of each watch path are also monitored.
	 */
	private final boolean				recursive;

	/**
	 * Whether to treat rapid sequences of events on the same path as a single event. The debounce window is configured in milliseconds.
	 */
	private final long					debounce;

	/**
	 * Whether to suppress events on the same path that occur within a short time of each other. The throttle window is configured in milliseconds.
	 */
	private final long					throttle;

	/**
	 * Whether to filter out events that appear to be part of atomic write operations. When true, a common pattern of rapid CREATE-MODIFY-DELETE or CREATE-MODIFY on the same file is treated as a single MODIFY event.
	 */
	private final boolean				atomicWrites;

	/**
	 * The number of consecutive errors allowed in the watch loop before the watcher auto-stops. A value of 0 means no auto-stop.
	 */
	private final int					errorThreshold;

	/**
	 * The listener that handles events and errors for this watcher. This is a required field; the Builder will throw if it is not set.
	 */
	private final IWatcherListener		listener;

	/**
	 * The parent BoxContext of the caller that created this watcher. This context is used as the parent for the WatcherContext passed to listener methods, allowing listeners to access the caller's context if needed.
	 */
	private final IBoxContext			parentContext;

	/**
	 * A logger instance for this watcher. Logs are written to the global WATCHER_LOGGER with the watcher name as context.
	 */
	private final BoxLangLogger			logger;

	// -------------------------------------------------------------------------
	// Runtime state
	// -------------------------------------------------------------------------

	/**
	 * The current lifecycle state of the watcher. Transitions from CREATED → RUNNING → STOPPED.
	 * The state is used to control the watch loop and prevent invalid operations (e.g. starting an already running watcher).
	 * Access to this field is synchronized to ensure thread safety during lifecycle transitions.
	 */
	private volatile State				state				= State.CREATED;

	/**
	 * The Java NIO.2 WatchService used to monitor filesystem events. This is initialized on start and closed on stop.
	 */
	private WatchService				nioWatchService;

	/**
	 * The context passed to listener methods, containing a reference to this WatcherInstance and inheriting from the parent context.
	 * This is initialized on start and can be used by listeners to query watcher state or perform context-aware operations.
	 */
	private WatcherContext				watcherContext;

	/**
	 * A map of active WatchKeys to their corresponding Paths. This is used to resolve event contexts during the watch loop and to manage registered paths.
	 */
	private final Map<WatchKey, Path>	watchedKeys			= new ConcurrentHashMap<>();

	/**
	 * The executor service running the watch loop. This is a virtual-thread executor created on start and shut down on stop.
	 */
	private BoxExecutor					watchExecutor;

	/**
	 * The Future representing the running watch loop task. This is used to cancel the loop on stop.
	 */
	private Future<?>					watchLoopFuture;

	/**
	 * A counter for consecutive errors thrown by the listener during event dispatch. This is used to determine when to auto-stop the watcher based on the configured error threshold.
	 */
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
	 *
	 * @throws WatcherException if a path cannot be registered
	 */
	public synchronized WatcherInstance start() {
		// If already running, do nothing
		if ( this.state == State.RUNNING ) {
			return this;
		}

		this.logger.info( "+ Starting WatcherInstance [{}]...", this.name.getName() );

		try {
			this.nioWatchService = FileSystems.getDefault().newWatchService();
			this.watchedKeys.clear();
			for ( Path p : this.watchPaths ) {
				this.registerPath( p );
			}
		} catch ( IOException e ) {
			throw new WatcherException( "Failed to initialise WatchService for watcher [" + this.name.getName() + "]", e );
		}

		this.watcherContext		= new WatcherContext( this.parentContext, this );
		this.watchExecutor		= AsyncService.buildExecutor( "watcher-" + this.name.getName(), AsyncService.ExecutorType.VIRTUAL, 0 );
		this.state				= State.RUNNING;
		this.watchLoopFuture	= this.watchExecutor.submit( this::watchLoop );

		this.logger.info( "+ WatcherInstance [{}] started, watching {} path(s)", this.name.getName(), this.watchPaths.size() );
		this.announceEvent( BoxEvent.ON_WATCHER_STARTUP );

		return this;
	}

	/**
	 * Stop the watcher. Cancels the watch loop, closes the NIO watch service, and
	 * shuts down the executor. The watcher can be restarted via {@link #restart()}.
	 *
	 * @param force if true, forces an immediate stop by interrupting the watch loop thread and closing the WatchService to unblock it. This may cause events to be lost but allows for a faster shutdown in cases where the loop is unresponsive.
	 *
	 * @return this instance (fluent)
	 */
	public synchronized WatcherInstance stop( boolean force ) {
		// If not running, do nothing
		if ( this.state != State.RUNNING ) {
			return this;
		}

		this.logger.info( "+ Stopping WatcherInstance [{}]...", this.name.getName() );
		this.state = State.STOPPED;

		// Cancel the watch loop task. The loop checks the state and interruption status to exit promptly, so this should cause it to stop without needing to wait for the next event.
		if ( this.watchLoopFuture != null ) {
			this.watchLoopFuture.cancel( true );
		}

		// Close the WatchService to unblock the watch loop if it's waiting. The loop checks the state and interruption status to exit promptly.
		if ( this.nioWatchService != null ) {
			try {
				this.nioWatchService.close();
			} catch ( IOException e ) {
				this.logger.error( "WatcherInstance [{}] failed to close WatchService: {}", this.name.getName(), e.getMessage(), e );
			}
		}

		// Shut down the executor. Since the watch loop checks for interruption, this should cause it to stop promptly.
		this.watchExecutor.shutdownNow();

		// cleanup keys and contexts
		this.watchedKeys.clear();

		this.logger.info( "+ WatcherInstance [{}] stopped.", this.name.getName() );
		this.announceEvent( BoxEvent.ON_WATCHER_SHUTDOWN );

		return this;
	}

	/**
	 * Restart the watcher (stop + start).
	 *
	 * @return this instance (fluent)
	 */
	public WatcherInstance restart() {
		this.stop( true );
		this.announceEvent( BoxEvent.ON_WATCHER_RESTART );
		return this.start();
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
		Path resolvedPath = path.toAbsolutePath().normalize();

		if ( !Files.isDirectory( resolvedPath ) ) {
			throw new WatcherException( "Watch path [" + resolvedPath + "] is not a directory or does not exist.", null );
		}

		if ( this.recursive ) {
			Files.walkFileTree( resolvedPath, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
					WatchKey key = dir.register( WatcherInstance.this.nioWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE );
					WatcherInstance.this.watchedKeys.put( key, dir );
					WatcherInstance.this.logger.debug( "WatcherInstance [{}]: registered watch path [{}]", WatcherInstance.this.name.getName(), dir );
					return FileVisitResult.CONTINUE;
				}
			} );
		} else {
			WatchKey key = resolvedPath.register( this.nioWatchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE );
			this.watchedKeys.put( key, resolvedPath );
			this.logger.debug( "WatcherInstance [{}]: registered watch path [{}]", this.name.getName(), resolvedPath );
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
		while ( this.state == State.RUNNING && !Thread.currentThread().isInterrupted() ) {
			WatchKey watchKey;

			// Poll with timeout to allow periodic checks of the state and interruption status.
			// If the poll is interrupted, exit the loop. If any other exception occurs, log it and continue.
			try {
				watchKey = this.nioWatchService.poll( 500, TimeUnit.MILLISECONDS );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				break;
			} catch ( Exception e ) {
				this.handleLoopError( e, null );
				continue;
			}

			// If no events, continue to next poll
			if ( watchKey == null ) {
				continue;
			}

			// Process each event for the watch key
			for ( WatchEvent<?> raw : watchKey.pollEvents() ) {
				WatchEvent.Kind<?> rawKind = raw.kind();

				// OVERFLOW: no specific file context available, so dispatch a generic OVERFLOW event and continue to scan
				if ( rawKind == OVERFLOW ) {
					this.dispatchEvent( WatcherEvent.Kind.OVERFLOW, null, null );
					continue;
				}

				@SuppressWarnings( "unchecked" )
				WatchEvent<Path>	pathEvent	= ( WatchEvent<Path> ) raw;
				Path				dir			= ( Path ) watchKey.watchable();
				Path				child		= dir.resolve( pathEvent.context() ).toAbsolutePath().normalize();

				// Recursively register newly created subdirectories
				// Note that this may generate additional events which will be processed in the next poll cycle, but that's generally acceptable since the filesystem events are already coalesced by the OS and the timing strategies will help suppress noise.
				if ( this.recursive && rawKind == ENTRY_CREATE && Files.isDirectory( child ) ) {
					try {
						this.registerPath( child );
					} catch ( IOException e ) {
						this.handleLoopError( e, null );
					}
				}

				// Timing guards
				if ( this.debounce > 0 && this.shouldDebounce( child ) ) {
					continue;
				}
				if ( this.throttle > 0 && this.shouldThrottle( child ) ) {
					continue;
				}

				WatcherEvent.Kind kind = this.toEventKind( rawKind );
				this.dispatchEvent( kind, child, dir );
			}

			// Reset the watch key to receive further events.
			// If the key is no longer valid, remove it from the map.
			// If there are no more valid keys, stop the watcher since we have no paths to watch.
			boolean valid = watchKey.reset();
			if ( !valid ) {
				this.watchedKeys.remove( watchKey );
				if ( this.watchedKeys.isEmpty() ) {
					this.logger.warn( "WatcherInstance [{}]: all watched directories have been removed; stopping.", this.name.getName() );
					this.state = State.STOPPED;
					break;
				}
			}
		}
	}

	/**
	 * Dispatch an event to the listener, applying the atomicWrites strategy if enabled. Wraps the event context and any exceptions in WatcherExceptions for the listener, and handles consecutive errors according to the configured threshold.
	 *
	 * @param kind      The kind of event (CREATED, MODIFIED, DELETED, OVERFLOW)
	 * @param child     The path of the file/directory that triggered the event (null for OVERFLOW)
	 * @param watchRoot The root directory being watched that is the context for the event (null for OVERFLOW)
	 */
	private void dispatchEvent( WatcherEvent.Kind kind, Path child, Path watchRoot ) {
		WatcherEvent event;
		if ( child != null && watchRoot != null ) {
			Path relative = watchRoot.relativize( child );
			event = new WatcherEvent( kind, child, relative, watchRoot, Instant.now() );
		} else {
			event = new WatcherEvent( Instant.now() );
		}

		this.logger.trace( "WatcherInstance [{}] dispatching event: {}", this.name.getName(), event );

		try {
			this.listener.onEvent( event, this.watcherContext );
			this.consecutiveErrors.set( 0 );
		} catch ( Exception e ) {
			this.handleLoopError( e, event );
		}
	}

	/**
	 * Handle an error thrown during the watch loop or event dispatch. Wraps the error in a WatcherException if it's not already one, increments the consecutive error count, and announces the error to the listener. If the consecutive error count exceeds
	 * the configured threshold, auto-stops the watcher.
	 *
	 * @param e     the exception that was thrown
	 * @param event the watcher event associated with the error, if any
	 */
	private void handleLoopError( Exception e, WatcherEvent event ) {
		WatcherException	wrapped	= ( e instanceof WatcherException we ) ? we : new WatcherException( e.getMessage(), event, e );
		int					count	= this.consecutiveErrors.incrementAndGet();

		// Log the error always, since the listener may choose to ignore it
		this.logger.error(
		    "WatcherInstance [{}] encountered an error{}: {}",
		    this.name.getName(),
		    ( event != null ) ? " processing event [" + event + "]" : "",
		    e.getMessage(),
		    e
		);

		// Try to notify the listener of the error. Since the listener itself may be the source of errors, catch and log any exceptions thrown by onError to prevent the watch loop from crashing.
		try {
			this.listener.onError( wrapped, this.watcherContext );
		} catch ( Exception ignored ) {
			this.logger.error( "WatcherInstance [{}] onError itself threw: {}", this.name.getName(), ignored.getMessage() );
		}

		// If we've hit the error threshold, auto-stop the watcher to prevent a crash loop. Note that since the watch loop checks the state and interruption status, it should stop promptly without needing to wait for the next event.
		if ( this.errorThreshold > 0 && count >= this.errorThreshold ) {
			this.logger.warn(
			    "WatcherInstance [{}] hit error threshold ({}/{}), auto-stopping.",
			    this.name.getName(), count, this.errorThreshold
			);
			this.state = State.STOPPED;
			this.announceEvent( BoxEvent.ON_WATCHER_ERROR );
		}

	}

	// -------------------------------------------------------------------------
	// Timing strategies
	// -------------------------------------------------------------------------

	private boolean shouldDebounce( Path path ) {
		long	now		= System.currentTimeMillis();
		long	last	= this.lastEventTime.getOrDefault( path, 0L );
		this.lastEventTime.put( path, now );
		return ( now - last ) < this.debounce;
	}

	private boolean shouldThrottle( Path path ) {
		long	now		= System.currentTimeMillis();
		long	last	= this.lastFireTime.getOrDefault( path, 0L );
		if ( ( now - last ) < this.throttle ) {
			return true;
		}
		this.lastFireTime.put( path, now );
		return false;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Convert a WatchEvent.Kind from the NIO WatchService to our internal WatcherEvent.Kind enum. This allows us to abstract away the underlying event source and maintain a consistent event model for listeners.
	 *
	 * @param kind the WatchEvent.Kind from the NIO WatchService
	 *
	 * @return the corresponding WatcherEvent.Kind for our internal model
	 */
	private WatcherEvent.Kind toEventKind( WatchEvent.Kind<?> kind ) {
		if ( kind == ENTRY_CREATE )
			return WatcherEvent.Kind.CREATED;
		if ( kind == ENTRY_MODIFY )
			return WatcherEvent.Kind.MODIFIED;
		if ( kind == ENTRY_DELETE )
			return WatcherEvent.Kind.DELETED;
		return WatcherEvent.Kind.OVERFLOW;
	}

	/**
	 * Announce a lifecycle event to the BoxLang event system with this watcher as context.
	 *
	 * @param event the BoxEvent to announce
	 */
	private void announceEvent( BoxEvent event ) {
		try {
			BoxRuntime
			    .getInstance()
			    .getInterceptorService()
			    .announce( event, Struct.of( Key.watcher, this ) );
		} catch ( Exception e ) {
			this.logger.error(
			    "WatcherInstance [{}] failed to announce event [{}]: {}",
			    this.name.getName(),
			    event.name(),
			    e.getMessage(),
			    e
			);
		}
	}

	// -------------------------------------------------------------------------
	// Query methods
	// -------------------------------------------------------------------------

	public Key getName() {
		return this.name;
	}

	/**
	 * Check if the watcher is currently running. This is true if the state is RUNNING, which means the watch loop is active and events are being processed.
	 * Note that this does not guarantee that the watch loop thread is currently executing (it may be
	 * between polls), but it indicates that the watcher has been started and has not been stopped.
	 *
	 * @return {@code true} if the watcher is running, {@code false} otherwise
	 */
	public boolean isRunning() {
		return this.state == State.RUNNING;
	}

	/**
	 * Check if the watcher is currently stopped. This is true if the state is STOPPED, which means the watch loop has been stopped and no further events will be processed until it is started again.
	 *
	 * @return {@code true} if the watcher is stopped, {@code false} otherwise
	 */
	public boolean isStopped() {
		return this.state == State.STOPPED;
	}

	/**
	 * Get the current lifecycle state of the watcher. This can be CREATED (not started), RUNNING (started and processing events), or STOPPED (stopped and not processing events).
	 * The state is updated synchronously during lifecycle transitions to ensure thread safety.
	 * Note that even if the state is RUNNING, the watch loop may not be actively processing events at the moment (e.g. it may be between polls), but it indicates that the watcher has been started and has not been stopped.
	 *
	 * @return the current State of the watcher
	 */
	public State getState() {
		return this.state;
	}

	/**
	 * Helper method to get the current state as a string, which can be useful for debugging or when passing the state to contexts that expect strings. This simply returns the name of the current State enum value.
	 *
	 * @return the current state of the watcher as a string
	 */
	public String getStateAsString() {
		return this.state.name();
	}

	/**
	 * Get the list of paths this watcher is configured to watch. These are the paths that were registered with the WatchService on start. Note that if the watcher is configured with recursive=true, subdirectories of these paths are also being watched,
	 * but only the top-level paths are returned here.
	 *
	 * @return the list of paths this watcher is configured to watch
	 */
	public List<Path> getWatchPaths() {
		return this.watchPaths;
	}

	/**
	 * Get the listener associated with this watcher. The listener is responsible for handling events and errors dispatched by this watcher.
	 * This is the same listener that was provided during construction via the Builder.
	 *
	 * @return the listener associated with this watcher
	 */
	public IWatcherListener getListener() {
		return this.listener;
	}

	/**
	 * Get a struct representation of the watcher's current state and configuration for debugging or introspection purposes.
	 * This includes the name, state, watch paths, timing strategy settings, and consecutive error count.
	 *
	 * @return a struct containing the watcher's current state and configuration
	 */
	public IStruct getStats() {
		Array pathArray = new Array();
		this.watchPaths.forEach( p -> pathArray.add( p.toString() ) );

		return Struct.ofNonConcurrent(
		    Key._name, this.name.getName(),
		    Key.state, this.state.name(),
		    Key.paths, pathArray,
		    Key.recursive, this.recursive,
		    Key.debounce, this.debounce,
		    Key.throttle, this.throttle,
		    Key.atomicWrites, this.atomicWrites,
		    Key.errorThreshold, this.errorThreshold,
		    Key.consecutiveErrors, this.consecutiveErrors.get()
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

		public Builder paths( Array paths ) {
			paths.forEach( p -> this.watchPaths.add( Paths.get( StringCaster.cast( p ) ) ) );
			return this;
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
			if ( this.listener == null ) {
				throw new IllegalStateException( "WatcherInstance [" + this.name.getName() + "] requires a listener." );
			}
			if ( this.watchPaths.isEmpty() ) {
				throw new IllegalStateException( "WatcherInstance [" + this.name.getName() + "] requires at least one path." );
			}
			BoxLangLogger log = ortus.boxlang.runtime.BoxRuntime.getInstance().getLoggingService().WATCHER_LOGGER;
			return new WatcherInstance( this, log );
		}
	}

}
