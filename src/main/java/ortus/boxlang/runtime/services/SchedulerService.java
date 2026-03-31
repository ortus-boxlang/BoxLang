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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.BoxScheduler;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This service manages all schedulers in the system.
 */
public class SchedulerService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * PUBLIC CONSTANTS
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The default shutdown timeout for the scheduler service
	 */
	public static final long		DEFAULT_SHUTDOWN_TIMEOUT	= 30;

	/**
	 * The default scheduler name used by the bx:schedule component when none is specified.
	 */
	public static final String		DEFAULT_SCHEDULER_NAME		= "bxschedule";

	/**
	 * --------------------------------------------------------------------------
	 * Private Variables
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Scheduler map registry
	 */
	private Map<Key, IScheduler>	schedulers					= new ConcurrentHashMap<>();

	/**
	 * The logger for this service
	 */
	private BoxLangLogger			logger;

	/**
	 * Lock object for thread-safe tasks.json access.
	 */
	private final Object			tasksFileLock				= new Object();

	/**
	 * Cached AES encryption key used to protect credentials in tasks.json.
	 * Lazily initialised by {@link #getOrCreateEncryptionKey()}.
	 */
	private volatile String			tasksEncryptionKey			= null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The BoxRuntime
	 */
	public SchedulerService( BoxRuntime runtime ) {
		super( runtime, Key.schedulerService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger = runtime.getLoggingService().SCHEDULER_LOGGER;
	}

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		BoxRuntime.timerUtil.start( "schedulerservice-startup" );
		this.logger.info( "+ Starting up Scheduler Service..." );

		// Register the Global Scheduler
		// This will look in the configuration for the global scheduler and start it up
		registerGlobalSchedulers();

		// Load and register any persisted bxschedule tasks from tasks.json
		registerPersistedScheduleTasks();

		// Startup all the schedulers
		startupRegisteredSchedulers();

		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_SERVICE_STARTUP,
		    Struct.of( "schedulerService", this )
		);

		// Let it be known!
		this.logger.info( "+ Scheduler Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "schedulerservice-startup" ) );
	}

	/**
	 * Register all global schedulers found in the boxlang.json config
	 */
	public void registerGlobalSchedulers() {
		IBoxContext runtimeContext = runtime.getRuntimeContext();

		// Process the global schedulers
		runtime.getConfiguration().scheduler.schedulers
		    .stream()
		    .map( schedulerPath -> {
			    // Locate it
			    Path targetPath = Paths.get( schedulerPath.toString() ).normalize().toAbsolutePath();
			    // Compile the scheduler
			    Class<IBoxRunnable> targetSchedulerClass = RunnableLoader.getInstance()
			        .loadClass(
			            ResolvedFilePath.of( targetPath ),
			            runtimeContext
			        );
			    // Construct the scheduler
			    IClassRunnable targetScheduler = ( IClassRunnable ) DynamicObject.of( targetSchedulerClass )
			        .invokeConstructor( runtimeContext )
			        .getTargetInstance();
			    // Create the proxy
			    return new BoxScheduler( targetScheduler, runtimeContext );
		    } )
		    .forEach( target -> {
			    // Register the scheduler
			    registerScheduler( target, true );
		    } );
	}

	/**
	 * Load persisted scheduled tasks from the configured {@code tasksFile} and register them
	 * in their respective schedulers. This is called during startup before {@link #startupRegisteredSchedulers()}.
	 */
	public void registerPersistedScheduleTasks() {
		java.nio.file.Path tasksFile = getTasksFilePath();
		if ( !java.nio.file.Files.exists( tasksFile ) ) {
			return;
		}

		this.logger.info( "+ Loading persisted schedule tasks from [{}]", tasksFile );

		Array		tasks;
		IBoxContext	runtimeContext	= runtime.getRuntimeContext();
		try {
			tasks = loadTasksFromDisk();
		} catch ( Exception e ) {
			this.logger.error( "Failed to load persisted schedule tasks — startup will continue without them: {}", e.getMessage() );
			return;
		}

		for ( Object entry : tasks ) {
			if ( ! ( entry instanceof IStruct ) ) {
				continue;
			}
			IStruct	taskDef			= ( IStruct ) entry;

			String	taskName		= taskDef.getAsString( Key.task );

			// Get the scheduler name from the task definition
			Object	schedulerField	= taskDef.get( Key.scheduler );
			String	schedulerName	= schedulerField != null ? schedulerField.toString() : null;
			if ( schedulerName == null || schedulerName.isBlank() ) {
				schedulerName = DEFAULT_SCHEDULER_NAME;
			}
			if ( taskName == null || taskName.isBlank() ) {
				this.logger.warn( "Skipping persisted task with no name" );
				continue;
			}

			boolean	paused			= BooleanCaster.cast( taskDef.getOrDefault( Key.paused, false ) );

			// Get or create the named scheduler (register only — startup happens later via startupRegisteredSchedulers)
			Key		schedulerKey	= Key.of( schedulerName );
			if ( !hasScheduler( schedulerKey ) ) {
				ortus.boxlang.runtime.async.tasks.BaseScheduler s = new ortus.boxlang.runtime.async.tasks.BaseScheduler( schedulerName, runtimeContext );
				registerScheduler( s, false );
			}
			ortus.boxlang.runtime.async.tasks.BaseScheduler scheduler = ( ortus.boxlang.runtime.async.tasks.BaseScheduler ) getScheduler( schedulerKey );

			// Decrypt credentials before passing to the callable builder
			decryptTaskCredentials( taskDef );

			// Build callable and register the task
			Runnable										callable		= ortus.boxlang.runtime.components.async.Schedule.buildTaskCallable( runtimeContext,
			    taskDef );

			String											group			= taskDef.getAsString( Key.group );
			ortus.boxlang.runtime.async.tasks.ScheduledTask	scheduledTask	= scheduler.task( taskName, group != null ? group : "" ).call( callable );

			// Apply full configuration (identical to doUpdate — repeat, exclude, callbacks, metadata, scheduling)
			ortus.boxlang.runtime.components.async.Schedule.applyTaskConfiguration( scheduledTask, callable, taskDef, runtimeContext );

			// Restore paused state last so it overrides any enable set by configuration
			if ( paused ) {
				scheduledTask.disable();
			}

			this.logger.info( "  + Loaded persisted task [{}] in scheduler [{}]", taskName, schedulerName );
		}
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_SERVICE_SHUTDOWN,
		    Struct.of( "schedulerService", this )
		);
		// Call shutdown on each scheduler in parallel
		schedulers.values()
		    .parallelStream()
		    .forEach( scheduler -> shutdownScheduler( scheduler, false, DEFAULT_SHUTDOWN_TIMEOUT ) );
		// Log it
		this.logger.info( "+ Scheduler Service shutdown" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Startup all the registered schedulers
	 * This is called by the runtime when it starts up
	 */
	public SchedulerService startupRegisteredSchedulers() {
		this.schedulers.values()
		    // Stream it in parallel
		    .parallelStream()
		    // Start them up
		    .forEach( this::startupScheduler );

		// Announce
		announce(
		    BoxEvent.ON_ALL_SCHEDULERS_STARTED,
		    Struct.of( "schedulers", this.schedulers )
		);
		return this;
	}

	/**
	 * Start a scheduler if not started already
	 *
	 * @param scheduler The scheduler to start
	 *
	 * @return Scheduler
	 */
	public SchedulerService startupScheduler( IScheduler scheduler ) {

		if ( scheduler.hasStarted() ) {
			this.logger.debug(
			    "+ Scheduler [{}] already started, skipping startup again",
			    scheduler.getSchedulerName()
			);
			return this;
		}

		long start = System.currentTimeMillis();
		this.logger.debug( "+ Starting up scheduler [{}] ...", scheduler.getSchedulerName() );

		// Startup the scheduler
		scheduler.startup();

		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_STARTUP,
		    Struct.of( "scheduler", scheduler )
		);

		// Log it
		this.logger.info(
		    "+ Scheduler [{}] started in [{}]ms",
		    scheduler.getSchedulerName(),
		    ( System.currentTimeMillis() - start )
		);
		return this;
	}

	/**
	 * Get all schedulers
	 *
	 * @return The schedulers
	 */
	public Map<Key, IScheduler> getSchedulers() {
		return this.schedulers;
	}

	/**
	 * Get all the registered scheduler names as a BoxLang Array
	 */
	public Array getSchedulerNames() {
		return this.schedulers.keySet()
		    .stream()
		    .map( Key::getName )
		    .collect( BLCollector.toArray() );
	}

	/**
	 * Get a scheduler by name
	 *
	 * @param name The name of the scheduler
	 *
	 * @return The scheduler or null if not found
	 */
	public IScheduler getScheduler( Key name ) {
		return this.schedulers.get( name );
	}

	/**
	 * Get a scheduler or fail the call with an exception if it does not exist
	 *
	 * @param name The name of the scheduler
	 *
	 * @throws BoxRuntimeException If the scheduler is not found
	 *
	 * @return The scheduler
	 */
	public IScheduler getSchedulerOrFail( Key name ) {
		IScheduler scheduler = this.schedulers.get( name );
		if ( scheduler == null ) {
			throw new BoxRuntimeException( "Scheduler [" + name.getName() + "] not registered. Registered schedulers are: " + getSchedulerNames() );
		}
		return scheduler;
	}

	/**
	 * Get a single scheduler task stats
	 *
	 * @param name The name of the scheduler
	 *
	 * @throws BoxRuntimeException If the scheduler is not found
	 *
	 * @return The scheduler task stats
	 */
	public IStruct getSchedulerStats( Key name ) {
		return getSchedulerOrFail( name ).getTaskStats();
	}

	/**
	 * Get all scheduler task stats
	 *
	 * @return A struct with all the scheduler task stats
	 */
	public IStruct getSchedulerStats() {
		IStruct stats = new Struct();
		this.schedulers.values()
		    .stream()
		    .forEach( scheduler -> {
			    stats.put( scheduler.getSchedulerName(), scheduler.getTaskStats() );
		    } );
		return stats;
	}

	/**
	 * How many schedulers do we have registered
	 *
	 * @return The number of schedulers
	 */
	public int size() {
		return this.schedulers.size();
	}

	/**
	 * Do we have a scheduler by this name?
	 *
	 * @param name The name of the scheduler
	 */
	public boolean hasScheduler( Key name ) {
		return this.schedulers.containsKey( name );
	}

	/**
	 * Register a scheduler with the service
	 *
	 * @param scheduler The IScheduler to register
	 *
	 * @throws BoxRuntimeException If a scheduler with the same name already exists
	 *
	 * @return The scheduler
	 */
	public IScheduler registerScheduler( IScheduler scheduler ) {
		return registerScheduler( scheduler, false );
	}

	/**
	 * Register a scheduler with the service
	 *
	 * @param scheduler The IScheduler to register
	 * @param force     If true, forces the registration of the scheduler
	 *
	 * @throws BoxRuntimeException If a scheduler with the same name already exists but force is false
	 *
	 * @return The scheduler
	 */
	public IScheduler registerAndStartScheduler( IScheduler scheduler, Boolean force ) {
		// Register the scheduler
		registerScheduler( scheduler, force );

		// Start the scheduler
		startupScheduler( scheduler );

		return scheduler;
	}

	/**
	 * Register a scheduler with the service
	 *
	 * @param scheduler The IScheduler to register
	 * @param force     If true, forces the registration of the scheduler
	 *
	 * @throws BoxRuntimeException If a scheduler with the same name already exists but force is false
	 *
	 * @return The scheduler
	 */
	public IScheduler registerScheduler( IScheduler scheduler, Boolean force ) {
		Key schedulerName = scheduler.getSchedulerNameAsKey();

		if ( hasScheduler( schedulerName ) && !force ) {
			throw new BoxRuntimeException( "A scheduler with the name [" + scheduler.getSchedulerName() + "] already exists" );
		}

		// Remove the existing scheduler if force is true
		if ( force && hasScheduler( name ) ) {
			this.logger.info( "+ Replacing existing scheduler [{}]", scheduler.getSchedulerName() );
			removeScheduler( schedulerName, true, DEFAULT_SHUTDOWN_TIMEOUT );
		}

		// Register the scheduler
		this.schedulers.put( schedulerName, scheduler );
		this.logger.info( "+ Registered scheduler [{}]", scheduler.getSchedulerName() );

		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_REGISTRATION,
		    Struct.of( "scheduler", scheduler, "force", force )
		);

		return scheduler;
	}

	/**
	 * This method is used to load a scheduler into the service. If the scheduler already exists, it will be replaced.
	 * This is usually used from the ModuleService to load a module scheduler
	 *
	 * @param name      The name of the scheduler
	 * @param scheduler The IScheduler to load
	 *
	 * @return
	 */
	public IScheduler loadScheduler( Key name, IScheduler scheduler ) {
		// Configure it
		scheduler.configure();

		// Register it
		registerScheduler( scheduler.setSchedulerName( name.getName() ), true );

		// Return it
		return scheduler;
	}

	/**
	 * Remove a scheduler from the service. This will also shutdown the scheduler.
	 *
	 * @param name    The name of the scheduler
	 * @param force   If true, forces the shutdown of the scheduler
	 * @param timeout The timeout in milliseconds to wait for the scheduler to shutdown
	 *
	 * @return True if the scheduler was removed, false if it was not found
	 */
	public boolean removeScheduler( Key name, boolean force, long timeout ) {
		IScheduler scheduler = this.schedulers.remove( name );
		if ( scheduler != null ) {
			// Announce it
			announce(
			    BoxEvent.ON_SCHEDULER_REMOVAL,
			    Struct.of(
			        "scheduler", scheduler,
			        "force", force,
			        "timeout", timeout
			    )
			);
			shutdownScheduler( scheduler, force, timeout );
			return true;
		}
		return false;
	}

	/**
	 * Remove a scheduler from the service. This will also shutdown the scheduler gracefully
	 *
	 * @param name The name of the scheduler
	 *
	 * @return True if the scheduler was removed, false if it was not found
	 */
	public boolean removeScheduler( Key name ) {
		return removeScheduler( name, false, DEFAULT_SHUTDOWN_TIMEOUT );
	}

	/**
	 * Clear all schedulers from the service. This will also shutdown all the schedulers
	 *
	 * @param force   If true, forces the shutdown of the scheduler
	 * @param timeout The timeout in milliseconds to wait for the scheduler to shutdown
	 */
	public SchedulerService clearSchedulers( Boolean force, Long timeout ) {
		this.schedulers.values().parallelStream().forEach( scheduler -> {
			shutdownScheduler( scheduler, force, timeout );
		} );
		this.schedulers.clear();
		return this;
	}

	/**
	 * Restart the scheduler if it exists
	 *
	 * @param name    The name of the scheduler
	 * @param force   If true, forces the shutdown of the scheduler
	 * @param timeout The timeout in milliseconds to wait for the scheduler to shutdown
	 *
	 * @throws BoxRuntimeException If the scheduler is not found
	 */
	public SchedulerService restartScheduler( Key name, boolean force, long timeout ) {
		IScheduler scheduler = getSchedulerOrFail( name );
		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_RESTART,
		    Struct.of(
		        "scheduler", scheduler,
		        "force", force,
		        "timeout", timeout
		    )
		);

		scheduler.restart( force, timeout );

		return this;
	}

	/**
	 * Restart the scheduler if it exists. Not forced
	 *
	 * @param name The name of the scheduler
	 *
	 * @throws BoxRuntimeException If the scheduler is not found
	 */
	public SchedulerService restartScheduler( Key name ) {
		return restartScheduler( name, false, 0 );
	}

	/**
	 * Shutdown the scheduler
	 *
	 * @param scheduler The scheduler to shutdown
	 * @param force     If true, forces the shutdown of the scheduler
	 * @param timeout   The timeout in seconds to wait for the scheduler to shutdown
	 */
	public void shutdownScheduler( IScheduler scheduler, boolean force, long timeout ) {
		// Validate the Scheduler is not null
		Objects.requireNonNull( scheduler, "Scheduler cannot be null" );
		// Log it
		this.logger.info( "+ Shutting down scheduler [{}]", scheduler.getSchedulerName() );
		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_SHUTDOWN,
		    Struct.of(
		        "scheduler", scheduler,
		        "force", force,
		        "timeout", timeout
		    )
		);
		scheduler.shutdown( force, timeout );
	}

	/**
	 * Shutdown the scheduler
	 *
	 * @param scheduler The scheduler name to shutdown
	 * @param force     If true, forces the shutdown of the scheduler
	 * @param timeout   The timeout in seconds to wait for the scheduler to shutdown
	 */
	public void shutdownScheduler( Key scheduler, boolean force, long timeout ) {
		shutdownScheduler( getSchedulerOrFail( scheduler ), force, timeout );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Task Persistence Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the resolved {@link Path} for the tasks persistence file,
	 * taken from {@code scheduler.tasksFile} in boxlang.json.
	 */
	private Path getTasksFilePath() {
		return Paths.get( runtime.getConfiguration().scheduler.tasksFile );
	}

	/**
	 * Load the persisted task array from the configured {@code tasksFile}.
	 * Returns an empty array when the file does not yet exist.
	 * Throws when the file exists but cannot be read or parsed, so callers that
	 * would subsequently write back to disk do not overwrite a temporarily
	 * unreadable file with an empty array.
	 *
	 * @return The array of persisted task definition structs, or an empty array if the file does not exist.
	 *
	 * @throws BoxRuntimeException if the file exists but cannot be read or parsed.
	 */
	public Array loadTasksFromDisk() {
		Path tasksFile = getTasksFilePath();
		if ( !Files.exists( tasksFile ) ) {
			return new Array();
		}
		try {
			Object parsed = JSONUtil.fromJSON( tasksFile.toFile(), true );
			if ( parsed instanceof Array ) {
				return ( Array ) parsed;
			}
			// File exists but contains non-array JSON — treat as corrupt
			throw new BoxRuntimeException( "tasks.json exists but does not contain a JSON array; refusing to overwrite." );
		} catch ( BoxRuntimeException e ) {
			throw e;
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to read tasks.json: " + e.getMessage(), e );
		}
	}

	/**
	 * Save the task array to the configured {@code tasksFile}.
	 *
	 * @param tasks The array of task definition structs to persist.
	 */
	public void saveTasksToDisk( Array tasks ) {
		Path tasksFile = getTasksFilePath();
		try {
			String json = JSONUtil.getJSONBuilder( true ).asString( tasks );
			FileSystemUtil.write( tasksFile.toString(), json, "UTF-8", true );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to persist tasks to disk: " + e.getMessage(), e );
		}
	}

	/**
	 * Persist (upsert) a task definition to tasks.json based on its component attributes.
	 *
	 * @param attributes The component attribute struct containing the task definition fields.
	 */
	public void persistTask( IStruct attributes ) {
		synchronized ( tasksFileLock ) {
			Array	tasks		= loadTasksFromDisk();
			String	taskName	= attributes.getAsString( Key.task );
			String	scheduler	= attributes.getAsString( Key.scheduler );

			String	encKey		= getOrCreateEncryptionKey();
			IStruct	taskDef		= Struct.ofNonConcurrent(
			    "task", taskName,
			    "scheduler", scheduler,
			    "group", attributes.getAsString( Key.group ),
			    "url", attributes.getAsString( Key.URL ),
			    "interval", attributes.getAsString( Key.interval ),
			    "cronTime", attributes.getAsString( Key.cronTime ),
			    "startDate", attributes.getAsString( Key.startDate ),
			    "startTime", attributes.getAsString( Key.startTime ),
			    "endDate", attributes.getAsString( Key.endDate ),
			    "endTime", attributes.getAsString( Key.endTime ),
			    "repeat", attributes.getAsInteger( Key.repeat ),
			    "exclude", attributes.getAsString( Key.exclude ),
			    "port", attributes.getAsInteger( Key.port ),
			    "username", encryptCredential( attributes.getAsString( Key.username ), encKey ),
			    "password", encryptCredential( attributes.getAsString( Key.password ), encKey ),
			    "proxyServer", attributes.getAsString( Key.proxyServer ),
			    "proxyPort", attributes.getAsInteger( Key.proxyPort ),
			    "proxyUser", encryptCredential( attributes.getAsString( Key.proxyUser ), encKey ),
			    "proxyPassword", encryptCredential( attributes.getAsString( Key.proxyPassword ), encKey ),
			    "publish", BooleanCaster.cast( attributes.getOrDefault( Key.publish, false ) ),
			    "path", attributes.getAsString( Key.path ),
			    "file", attributes.getAsString( Key.file ),
			    "overwrite", BooleanCaster.cast( attributes.getOrDefault( Key.overwrite, true ) ),
			    "resolveURL", BooleanCaster.cast( attributes.getOrDefault( Key.resolveUrl, false ) ),
			    "retryCount", attributes.getAsInteger( Key.retryCount ),
			    "onException", attributes.getAsString( Key.onException ),
			    "oncomplete", attributes.getAsString( Key.onComplete ),
			    "eventhandler", attributes.getAsString( Key.eventHandler ),
			    "cluster", BooleanCaster.cast( attributes.getOrDefault( Key.cluster, false ) ),
			    "isDaily", BooleanCaster.cast( attributes.getOrDefault( Key.isDaily, false ) ),
			    "paused", false
			);

			// Upsert: remove existing entry with same task + scheduler
			tasks.removeIf( entry -> {
				if ( entry instanceof IStruct ) {
					IStruct existing = ( IStruct ) entry;
					return taskName.equals( existing.getAsString( Key.task ) )
					    && scheduler.equals( existing.getAsString( Key.scheduler ) );
				}
				return false;
			} );
			tasks.add( taskDef );

			saveTasksToDisk( tasks );
		}
	}

	/**
	 * Remove a task from tasks.json.
	 *
	 * @param taskName      The name of the task to remove.
	 * @param schedulerName The name of the scheduler the task belongs to.
	 */
	public void removeTaskFromDisk( String taskName, String schedulerName ) {
		synchronized ( tasksFileLock ) {
			Array tasks = loadTasksFromDisk();
			tasks.removeIf( entry -> {
				if ( entry instanceof IStruct ) {
					IStruct existing = ( IStruct ) entry;
					return taskName.equals( existing.getAsString( Key.task ) )
					    && schedulerName.equals( existing.getAsString( Key.scheduler ) );
				}
				return false;
			} );
			saveTasksToDisk( tasks );
		}
	}

	/**
	 * Update the paused flag for a single task in tasks.json.
	 *
	 * @param taskName      The name of the task.
	 * @param schedulerName The name of the scheduler the task belongs to.
	 * @param paused        The new paused state.
	 */
	public void updateTaskPausedState( String taskName, String schedulerName, boolean paused ) {
		synchronized ( tasksFileLock ) {
			Array tasks = loadTasksFromDisk();
			for ( Object entry : tasks ) {
				if ( entry instanceof IStruct ) {
					IStruct existing = ( IStruct ) entry;
					if ( taskName.equals( existing.getAsString( Key.task ) )
					    && schedulerName.equals( existing.getAsString( Key.scheduler ) ) ) {
						existing.put( Key.paused, paused );
						break;
					}
				}
			}
			saveTasksToDisk( tasks );
		}
	}

	/**
	 * Update the paused flag for all tasks in a given scheduler in tasks.json.
	 * Optionally filter by group.
	 *
	 * @param schedulerName The name of the scheduler.
	 * @param group         Optional group filter; pass null or blank to match all groups.
	 * @param paused        The new paused state.
	 */
	public void updateAllTasksPausedState( String schedulerName, String group, boolean paused ) {
		synchronized ( tasksFileLock ) {
			Array tasks = loadTasksFromDisk();
			for ( Object entry : tasks ) {
				if ( entry instanceof IStruct ) {
					IStruct existing = ( IStruct ) entry;
					if ( !schedulerName.equals( existing.getAsString( Key.scheduler ) ) ) {
						continue;
					}
					if ( group != null && !group.isBlank() && !group.equals( existing.getAsString( Key.group ) ) ) {
						continue;
					}
					existing.put( Key.paused, paused );
				}
			}
			saveTasksToDisk( tasks );
		}
	}

	/**
	 * Returns the AES encryption key used to protect task credentials in tasks.json.
	 * Reads the runtime seed from {@code ${boxLangHome}/config/.seed}, which BoxRuntime
	 * auto-generates on first startup — no separate key file needed.
	 *
	 * @return Base64-encoded AES key string.
	 */
	private String getOrCreateEncryptionKey() {
		if ( tasksEncryptionKey != null ) {
			return tasksEncryptionKey;
		}
		synchronized ( tasksFileLock ) {
			if ( tasksEncryptionKey != null ) {
				return tasksEncryptionKey;
			}
			Path seedPath = runtime.getRuntimeHome().resolve( "config/.seed" );
			try {
				tasksEncryptionKey = new String( Files.readAllBytes( seedPath ), java.nio.charset.StandardCharsets.UTF_8 ).trim();
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Failed to read runtime seed for task credential encryption: " + e.getMessage(), e );
			}
			return tasksEncryptionKey;
		}
	}

	/**
	 * Encrypts a credential value using the tasks AES key.
	 * Returns an empty string if the value is null or blank.
	 *
	 * @param value  The plaintext credential.
	 * @param encKey The AES key string from {@link #getOrCreateEncryptionKey()}.
	 *
	 * @return Encrypted Base64-encoded string, or empty string.
	 */
	private String encryptCredential( String value, String encKey ) {
		if ( value == null || value.isBlank() ) {
			return "";
		}
		return EncryptionUtil.encrypt( value, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM, encKey, EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING, null, null );
	}

	/**
	 * Decrypts the credential fields (username, password, proxyUser, proxyPassword) in a
	 * persisted task definition struct in-place, so callers receive plaintext values.
	 *
	 * @param taskDef The task definition struct loaded from disk.
	 */
	private void decryptTaskCredentials( IStruct taskDef ) {
		String encKey = getOrCreateEncryptionKey();
		for ( Key credKey : new Key[] { Key.username, Key.password, Key.proxyUser, Key.proxyPassword } ) {
			String encrypted = taskDef.getAsString( credKey );
			if ( encrypted != null && !encrypted.isBlank() ) {
				try {
					taskDef.put( credKey, ( String ) EncryptionUtil.decrypt( encrypted, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM, encKey,
					    EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING, null, null ) );
				} catch ( Exception e ) {
					this.logger.warn( "Failed to decrypt credential [{}] for task [{}] — using empty value: {}", credKey.getName(),
					    taskDef.getAsString( Key.task ), e.getMessage() );
					taskDef.put( credKey, "" );
				}
			}
		}
	}

}
