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
package ortus.boxlang.runtime.async.tasks;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The Async Scheduler is in charge of registering scheduled tasks, starting them, monitoring them and shutting them down if needed.
 *
 * Each scheduler is bound to an scheduled executor class. You can override the executor using the `setExecutor()` method if you so desire.
 * The scheduled executor will be named <code>{name}-scheduler</code>
 *
 * In a ColdBox context, you might have the global scheduler in charge of the global tasks and also 1 per module as well in HMVC fashion.
 * In a ColdBox context, this object will inherit from the ColdBox super type as well dynamically at runtime.
 *
 */
public class Scheduler implements IScheduler {

	/**
	 * --------------------------------------------------------------------------
	 * Protected Properties
	 * --------------------------------------------------------------------------
	 * All the properties are protected so they can be accessed by concrete implementations
	 * and also by the super class.
	 */

	/**
	 * An ordered struct of all the tasks this scheduler manages
	 */
	protected LinkedHashMap<String, TaskRecord>	tasks						= new LinkedHashMap<>( 20 );

	/**
	 * The Scheduled Executor we are bound to
	 */
	protected ExecutorRecord					executor;

	/**
	 * The timezone for the scheduler and the tasks it creates and manages
	 */
	protected ZoneId							timezone					= ZoneId.systemDefault();

	/**
	 * The async service we are bound to
	 */
	protected AsyncService						asyncService;

	/**
	 * Is the scheduler started?
	 */
	protected Boolean							started						= false;

	/**
	 * The name of this scheduler
	 */
	protected String							name;

	/**
	 * The default timeout to use when gracefully shutting down this scheduler. Default is 30 seconds.
	 */
	protected static final long					DEFAULT_SHUTDOWN_TIMEOUT	= 30;

	/**
	 * Logger
	 */
	protected static final Logger				logger						= LoggerFactory.getLogger( Scheduler.class );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new scheduler with a name and the default system timezone.
	 *
	 * @param name         The name of the scheduler
	 * @param asyncService The async service we are bound to
	 */
	public Scheduler( String name, AsyncService asyncService ) {
		this( name, ZoneId.systemDefault(), asyncService );
	}

	/**
	 * Create a new scheduler with a name and a specific timezone
	 *
	 * @param name         The name of the scheduler
	 * @param timezone     The timezone for the scheduler and the tasks it creates and manages
	 * @param asyncService The async service we are bound to
	 */
	public Scheduler( String name, ZoneId timezone, AsyncService asyncService ) {
		this.name			= name;
		this.timezone		= timezone;
		this.asyncService	= asyncService;
		// Log it
		logger.info( "Created scheduler [{}] with a [{}] timezone", name, timezone.getId() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Configurator
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Usually where concrete implementations add their tasks and configs
	 */
	public void configure() {
		// called externally
	}

	/**
	 * --------------------------------------------------------------------------
	 * Task Registration Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register a new task in this scheduler but disable it immediately. This is useful
	 * when debugging tasks and have the easy ability to disable them.
	 *
	 * @param name The name of this task
	 *
	 * @return The registered and disabled Scheduled Task
	 */
	public ScheduledTask xtask( String name ) {
		return xtask( name, "" );
	}

	/**
	 * Register a new task in this scheduler but disable it immediately. This is useful
	 * when debugging tasks and have the easy ability to disable them.
	 *
	 * @param name  The name of this task
	 * @param group The group of this task
	 *
	 * @return The registered and disabled Scheduled Task
	 */
	public ScheduledTask xtask( String name, String group ) {
		return task( name, group ).disable();
	}

	/**
	 * Register a new task in this scheduler that will be executed once the `startup()` is fired or manually
	 * via the run() method of the task. The group will be empty.
	 *
	 * @param name The name of this task
	 *
	 * @return a ScheduledTask object so you can work on the registration of the task
	 */
	public ScheduledTask task( String name ) {
		return task( name, "" );
	}

	/**
	 * Register a new task in this scheduler that will be executed once the `startup()` is fired or manually
	 * via the run() method of the task.
	 *
	 * @param name The name of this task
	 *
	 * @return a ScheduledTask object so you can work on the registration of the task
	 */
	public ScheduledTask task( String name, String group ) {
		// Create task with custom name
		var oTask =
		    // Give me the task broda!
		    new ScheduledTask( name, group, null, this )
		        // Register ourselves in the task
		        .setScheduler( this )
		        // Set default timezone into the task
		        .setTimezone( this.getTimezone() );

		// Register the task by name
		this.tasks.put( name, new TaskRecord( name, group, oTask ) );

		return oTask;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Startup/Shutdown Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Startup this scheduler and all of it's scheduled tasks
	 */
	public synchronized Scheduler startup() {
		if ( !this.started ) {
			// Build out an executor for this scheduler
			this.executor = asyncService.newScheduledExecutor( name + "-scheduler" );

			// Iterate over tasks and send them off for scheduling
			this.tasks.entrySet()
			    .parallelStream()
			    .forEachOrdered( entry -> startupTask( entry.getKey(), entry.getValue() ) );

			// Mark scheduler as started
			this.started = true;

			// callback
			this.onStartup();

			// Log it
			logger.info( "Scheduler [{}] has started!", this.name );
		}

		return this;
	}

	/**
	 * Restart the scheduler by shutting it down and starting it up again
	 *
	 * @param force   If true, it forces all shutdowns this is usually true when doing reinits
	 * @param timeout The timeout in seconds to wait for the shutdown of all tasks, defaults to 30 or whatever you set using the setShutdownTimeout()
	 *
	 * @return
	 */
	public synchronized Scheduler restart( boolean force, long timeout ) {
		logger.info( "+ Restarting scheduler [{}] with force: {} and timeout: {}", this.name, force, timeout );
		// Shutdown first
		shutdown( force, timeout );
		// Clear tasks
		clearTasks();
		// Configure again
		configure();
		// Startup
		startup();
		logger.info( "+ Scheduler [{}] has been restarted!", this.name );
		return this;
	}

	/**
	 * Clear all tasks from the scheduler. Usually done by a restart
	 *
	 * @return The scheduler object
	 */
	public synchronized Scheduler clearTasks() {
		this.tasks.clear();
		return this;
	}

	/**
	 * Startup a specific task by name
	 *
	 * @param taskName   The name of the task
	 * @param taskRecord The task record object
	 */
	private void startupTask( String taskName, TaskRecord taskRecord ) {
		// Verify we can start it up the task or not
		if ( taskRecord.task.isDisabled() ) {
			taskRecord.disabled = true;
			logger.atWarn().log(
			    "- Scheduler ({}) skipping task ({}) as it is disabled.",
			    this.name,
			    taskName
			);
			// Continue iteration
			return;
		} else {
			// Log scheduling startup
			logger.info(
			    "- Scheduler ({}) scheduling task ({})...",
			    this.name,
			    taskName
			);
		}

		// Send it off for scheduling
		try {
			taskRecord.future		= taskRecord.task.start();
			taskRecord.scheduledAt	= LocalDateTime.now( this.timezone );
			logger.atInfo().log(
			    "√ Task ({}) scheduled successfully.",
			    taskName
			);
		} catch ( Exception e ) {
			logger.error(
			    "X Error scheduling task ({}}) => {}",
			    this.name + "." + taskName,
			    e.getMessage()
			);
			taskRecord.error		= true;
			taskRecord.errorMessage	= e.getMessage();
			taskRecord.stacktrace	= Arrays.toString( e.getStackTrace() );
		}
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 *
	 * @param force   If true, it forces all shutdowns this is usually true when doing reinits
	 * @param timeout The timeout in seconds to wait for the shutdown of all tasks, defaults to 30 or whatever you set using the setShutdownTimeout()
	 *                method
	 */
	public Scheduler shutdown( boolean force, long timeout ) {
		// callback
		this.onShutdown();

		// shutdown executor and await termination or kill it now!
		if ( force ) {
			this.executor.scheduledExecutor().shutdownNow();
		} else {
			this.executor.shutdownAndAwaitTermination( timeout, TimeUnit.SECONDS );
		}

		// Remove executor
		this.asyncService.deleteExecutor( this.name + "-scheduler" );
		// Mark it as stopped
		this.started = false;
		// Log it
		logger.info( "Scheduler [{}] has been shutdown!", this.name );
		return this;
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * using the default timeout
	 *
	 * @param force If true, it forces all shutdowns this is usually true when doing reinits
	 *
	 * @return The scheduler object
	 */
	public Scheduler shutdown( boolean force ) {
		return shutdown( force, DEFAULT_SHUTDOWN_TIMEOUT );
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * We do not force and we use the default timeout
	 *
	 * @return The scheduler object
	 */
	public Scheduler shutdown() {
		return shutdown( false, DEFAULT_SHUTDOWN_TIMEOUT );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life - Cycle Callbacks
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Called before the scheduler is going to be shutdown
	 */
	public void onShutdown() {
		logger.info( "Shutting down scheduler [{}]", this.name );
	}

	/**
	 * Called after the scheduler has registered all schedules
	 */
	public void onStartup() {
		logger.info( "Starting up scheduler [{}]", this.name );
	}

	/**
	 * Called whenever ANY task fails
	 *
	 * @param task      The task that got executed
	 * @param exception The exception object
	 */
	public void onAnyTaskError( ScheduledTask task, Exception exception ) {
		logger.error(
		    "Task [{}.{}] has failed with {}",
		    getName(),
		    task.getName(),
		    exception.getMessage(),
		    exception
		);
	}

	/**
	 * Called whenever ANY task succeeds
	 *
	 * @param task   The task that got executed
	 * @param result The result (if any) that the task produced
	 */
	public void onAnyTaskSuccess( ScheduledTask task, Optional<?> result ) {
		logger.info( "Task [{}.{}] has succeeded", getName(), task.getName() );
	}

	/**
	 * Called before ANY task runs
	 *
	 * @param task The task about to be executed
	 */
	public void beforeAnyTask( ScheduledTask task ) {
		logger.atDebug().log( "Task [{}.{}] is about to run", getName(), task.getName() );
	}

	/**
	 * Called after ANY task runs
	 *
	 * @param task   The task that got executed
	 *
	 * @param result The result (if any) that the task produced
	 */
	public void afterAnyTask( ScheduledTask task, Optional<?> result ) {
		logger.atDebug().log(
		    "Task [{}.{}] has run with result []",
		    getName(),
		    task.getName(),
		    result.isPresent() ? result.get() : "no result"
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Builds out a report for all the registered tasks in this scheduler.
	 * The key is the task name and the value is a struct with the task stats.
	 *
	 * @return A struct with the report: {@code { taskName: { stats } }}
	 */
	public IStruct getTaskStats() {
		return this.tasks
		    .entrySet()
		    .parallelStream()
		    .collect(
		        Collectors.toMap(
		            // key
		            entry -> Key.of( entry.getKey() ),
		            // value
		            entry -> entry.getValue().task.getStats(),
		            // merge function
		            ( existing, replacement ) -> existing,
		            // map type
		            Struct::new
		        )
		    );
	}

	/**
	 * Get an array of all the tasks managed by this scheduler
	 */
	public List<String> getRegisteredTasks() {
		return this.tasks
		    .keySet()
		    .stream()
		    .sorted()
		    .collect( Collectors.toCollection( ArrayList::new ) );
	}

	/**
	 * Check if a task is registered in this scheduler
	 *
	 * @param name The name of the task
	 *
	 * @return true if registered, false if not
	 */
	public boolean hasTask( String name ) {
		return this.tasks.containsKey( name );
	}

	/**
	 * Get's a task record from the collection by name
	 *
	 * @param name The name of the task
	 *
	 * @return The task record object
	 */
	public TaskRecord getTaskRecord( String name ) {
		if ( hasTask( name ) ) {
			return this.tasks.get( name );
		}
		throw new BoxRuntimeException(
		    String.format(
		        "No task found with the name: (%s). Registered tasks are (%s)",
		        name,
		        getRegisteredTasks()
		    )
		);
	}

	/**
	 * Removes a task from the scheduler.
	 * It tries to cancel the task first and then removes it from the scheduler.
	 * if the task has a future, then it will try to cancel it with interrupt.
	 *
	 * @param name The name of the task
	 *
	 * @return The scheduler object
	 */
	public Scheduler removeTask( String name ) {
		var taskRecord = getTaskRecord( name );

		// Check if the task has been registered so we can cancel it
		if ( taskRecord.future != null ) {
			taskRecord.future.cancel( true );
		}

		// Delete it
		this.tasks.remove( name );

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the registered tasks in this scheduler
	 *
	 * @return the tasks
	 */
	public Map<String, TaskRecord> getTasks() {
		return tasks;
	}

	/**
	 * Has this scheduler been started?
	 *
	 * @return true if started, false if not
	 */
	public Boolean hasStarted() {
		return this.started;
	}

	/**
	 * Get the scheduler name
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set the scheduler name
	 *
	 * @param name the name to set
	 */
	public Scheduler setName( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * @return the timezone
	 */
	public ZoneId getTimezone() {
		return this.timezone;
	}

	/**
	 * Set the scheduler's timezone
	 *
	 * @param timezone the timezone to set
	 */
	public Scheduler setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
		return this;
	}

	/**
	 * Set the scheduler's timezone as a string, we will convert it to a ZoneId object
	 *
	 * @param timezone the timezone to set as a string
	 */
	public Scheduler setTimezone( String timezone ) {
		return setTimezone( ZoneId.of( timezone ) );
	}

	/**
	 * Set the default timezone into the task
	 *
	 * @return Scheduler
	 */
	public Scheduler setDefaultTimezone() {
		this.timezone = ZoneId.systemDefault();
		return this;
	}

	/**
	 * Set the async service
	 *
	 * @param asyncService the asyncService to set
	 */
	public Scheduler setAsyncService( AsyncService asyncService ) {
		this.asyncService = asyncService;
		return this;
	}

	/**
	 * Get the Aysnc Service
	 *
	 * @return the asyncService
	 */
	public AsyncService getAsyncService() {
		return asyncService;
	}

	/**
	 * Get the executor record
	 *
	 * @return the executor record
	 */
	public ExecutorRecord getExecutor() {
		return executor;
	}

}
