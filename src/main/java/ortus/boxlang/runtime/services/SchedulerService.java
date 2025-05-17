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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.BoxScheduler;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.context.IBoxContext;
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

}
