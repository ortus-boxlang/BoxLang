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

import org.slf4j.Logger;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This service manages all schedulers in the system.
 */
public class SchedulerService extends BaseService {

	/**
	 * Scheduler map registry
	 */
	private Map<Key, IScheduler>	schedulers	= new ConcurrentHashMap<>();

	/**
	 * Logger
	 */
	private Logger					logger;

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
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		this.logger = runtime.getLoggingService().getLogger( "scheduler" );
		BoxRuntime.timerUtil.start( "schedulerservice-startup" );
		this.logger.info( "+ Starting up Scheduler Service..." );

		// Register the Global Scheduler
		// This will look in the configuration for the global scheduler and start it up

		// Startup all the schedulers
		startupSchedulers();

		// Announce it
		announce(
		    BoxEvent.ON_SCHEDULER_SERVICE_STARTUP,
		    Struct.of( "schedulerService", this )
		);

		// Let it be known!
		this.logger.info( "+ Scheduler Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "schedulerservice-startup" ) );
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
		schedulers.values().parallelStream().forEach( scheduler -> shutdownScheduler( scheduler, false, 0L ) );
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
	public SchedulerService startupSchedulers() {
		this.schedulers.values()
		    // Stream it in parallel
		    .parallelStream()
		    // Only non started schedulers
		    .filter( scheduler -> !scheduler.hasStarted() )
		    // Start them up
		    .forEach( scheduler -> {
			    this.logger.info( "+ Starting up scheduler [{}] ...", scheduler.getSchedulerName() );
			    scheduler.startup();
			    // Announce it
			    announce(
			        BoxEvent.ON_SCHEDULER_STARTUP,
			        Struct.of( "scheduler", scheduler )
			    );
		    } );

		// Announce
		announce(
		    BoxEvent.ON_ALL_SCHEDULERS_STARTED,
		    Struct.of( "schedulers", this.schedulers )
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
	 * @throws BoxRuntimeException If a scheduler with the same name already exists
	 *
	 * @return The scheduler
	 */
	public IScheduler registerScheduler( IScheduler scheduler, Boolean force ) {
		if ( this.schedulers.containsKey( Key.of( scheduler.getSchedulerName() ) ) && !force ) {
			throw new BoxRuntimeException( "A scheduler with the name [" + scheduler.getSchedulerName() + "] already exists" );
		}
		this.schedulers.put( Key.of( scheduler.getSchedulerName() ), scheduler );
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
		// Register it
		registerScheduler( scheduler.setSchedulerName( name.getName() ), true );

		// Configure it
		scheduler.configure();

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
		return removeScheduler( name, false, 0L );
	}

	/**
	 * Clear all schedulers from the service. This will also shutdown all the schedulers
	 *
	 * @param force   If true, forces the shutdown of the scheduler
	 * @param timeout The timeout in milliseconds to wait for the scheduler to shutdown
	 */
	public void clearSchedulers( Boolean force, Long timeout ) {
		this.schedulers.values().parallelStream().forEach( scheduler -> {
			shutdownScheduler( scheduler, force, timeout );
		} );
		this.schedulers.clear();
	}

	/**
	 * Restart the scheduler if it exists
	 *
	 * @param name    The name of the scheduler
	 * @param force   If true, forces the shutdown of the scheduler
	 * @param timeout The timeout in milliseconds to wait for the scheduler to shutdown
	 */
	public boolean restartScheduler( Key name, boolean force, long timeout ) {
		IScheduler scheduler = this.schedulers.get( name );
		if ( scheduler != null ) {
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
			return true;
		}
		return false;
	}

	/**
	 * Restart the scheduler if it exists. Not forced
	 *
	 * @param name The name of the scheduler
	 */
	public boolean restartScheduler( Key name ) {
		return restartScheduler( name, false, 0 );
	}

	/**
	 * Shutdown the scheduler
	 *
	 * @param scheduler The scheduler to shutdown
	 * @param force     If true, forces the shutdown of the scheduler
	 * @param timeout   The timeout in milliseconds to wait for the scheduler to shutdown
	 */
	private void shutdownScheduler( IScheduler scheduler, Boolean force, Long timeout ) {
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

}
