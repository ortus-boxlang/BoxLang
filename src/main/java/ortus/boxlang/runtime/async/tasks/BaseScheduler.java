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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.async.executors.BoxScheduledExecutor;
import ortus.boxlang.runtime.config.segments.ExecutorConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.ClusterService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The Async Scheduler is in charge of registering scheduled tasks, starting them, monitoring them and shutting them down if needed.
 *
 * Each scheduler is bound to an scheduled executor class.
 * The scheduled executor will be named <code>{name}-scheduler</code>
 */
public class BaseScheduler implements IScheduler {

	/**
	 * The global executor used when a bound global executor does not exist
	 */
	public static final String					DEFAULT_GLOBAL_EXECUTOR	= "scheduled-tasks";

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
	protected LinkedHashMap<String, TaskRecord>	tasks					= new LinkedHashMap<>( 20 );

	/**
	 * The Scheduled Executor we are bound to
	 */
	protected BoxExecutor						executor;

	/**
	 * The name of a global (shared) executor this scheduler runs on instead of building a private pool.
	 * Null means the scheduler builds and owns a private pool named <code>{name}-scheduler</code>.
	 */
	protected String							globalExecutorName		= null;

	/**
	 * True when the started scheduler runs on a shared global executor it must never shut down
	 */
	protected boolean							sharedExecutor			= false;

	/**
	 * Fallback global executor name used when the requested one is unusable. Visible for testing.
	 */
	protected String							fallbackExecutorName	= DEFAULT_GLOBAL_EXECUTOR;

	/**
	 * The timezone for the scheduler and the tasks it creates and manages
	 */
	protected ZoneId							timezone				= ZoneId.systemDefault();

	/**
	 * The name of the cache to use for server fixation/clustering locks for the tasks this scheduler
	 * creates. Defaults to the runtime's {@code scheduler.cacheName} config setting.
	 */
	protected String							cacheName;

	/**
	 * The async service we are bound to
	 */
	protected AsyncService						asyncService;

	/**
	 * Is the scheduler started?
	 */
	protected Boolean							started					= false;

	/**
	 * True from the moment a shutdown begins until the scheduler starts up again. While set, no new
	 * tasks may be registered and no task may schedule a new execution, so nothing can be submitted
	 * into an executor that is being torn down.
	 */
	protected volatile boolean					shuttingDown			= false;

	/**
	 * When the scheduler was started
	 */
	protected Instant							startedAt;

	/**
	 * The name of this scheduler
	 */
	protected String							name;

	/**
	 * Logger
	 */
	protected final BoxLangLogger				logger;

	/**
	 * The request context that started this scheduler
	 */
	protected IBoxContext						context;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Zero arg constructor with basic defaults:
	 * Auto-generated name, system default timezone and the default async service
	 */
	public BaseScheduler() {
		this( "boxlang-scheduler-" + RandomStringUtils.secure().nextAlphanumeric( 10 ) );
		this.context = BoxRuntime.getInstance().getRuntimeContext();
	}

	/**
	 * Create a new scheduler with a name and the default system timezone.
	 *
	 * @param name The name of the scheduler
	 */
	public BaseScheduler( String name ) {
		this( name, ZoneId.systemDefault() );
	}

	/**
	 * Create a new scheduler with a name and the default system timezone.
	 *
	 * @param name    The name of the scheduler
	 * @param context The context that started this scheduler
	 */
	public BaseScheduler( String name, IBoxContext context ) {
		this( name, ZoneId.systemDefault(), context );
	}

	/**
	 * Create a new scheduler with a name and a specific timezone
	 *
	 * @param name     The name of the scheduler
	 * @param timezone The timezone for the scheduler and the tasks it creates and manages
	 */
	public BaseScheduler( String name, ZoneId timezone ) {
		this( name, timezone, BoxRuntime.getInstance().getRuntimeContext() );
	}

	/**
	 * Create a new scheduler with a name and a specific timezone
	 *
	 * @param name     The name of the scheduler
	 * @param timezone The timezone for the scheduler and the tasks it creates and manages
	 * @param context  The context that started this scheduler
	 */
	public BaseScheduler( String name, ZoneId timezone, IBoxContext context ) {
		this.name			= name;
		this.timezone		= timezone;
		this.asyncService	= BoxRuntime.getInstance().getAsyncService();
		this.logger			= BoxRuntime.getInstance().getLoggingService().SCHEDULER_LOGGER;
		this.context		= context;
		this.cacheName		= BoxRuntime.getInstance().getConfiguration().scheduler.cacheName;

		// Log it
		this.logger.info( "Created scheduler [{}] with a [{}] timezone", name, timezone.getId() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Configurator Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set the scheduler context manually
	 */
	public BaseScheduler setContext( IBoxContext context ) {
		this.context = context;
		return this;
	}

	/**
	 * Get the scheduler context
	 */
	public IBoxContext getContext() {
		return this.context;
	}

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
		// A shutdown in progress means nothing may be added to a pool that is being torn down
		if ( this.shuttingDown ) {
			throw new BoxRuntimeException(
			    "Scheduler [" + this.name + "] is shutting down and cannot register task [" + name + "]"
			);
		}
		// Create task with custom name
		var oTask =
		    // Give me the task broda!
		    new ScheduledTask( name, group, null, this )
		        // Register ourselves in the task
		        .setScheduler( this )
		        // Set default timezone into the task
		        .setTimezone( this.getTimezone() );
		// Cluster coordination uses the single coordination cache owned by the cluster service, so
		// there is no per-scheduler cache to propagate

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
	 * The number of threads for this scheduler's executor: the thread count of the global executor named by
	 * {@code scheduler.executor} in boxlang.json, so it can be raised there when tasks queue up.
	 *
	 * @return The configured thread count, or null to use the default
	 */
	private Integer getConfiguredThreads() {
		var config = BoxRuntime.getInstance().getConfiguration();
		if ( config.executors.get( Key.of( config.scheduler.executor ) ) instanceof ExecutorConfig executorConfig && executorConfig.maxThreads > 0 ) {
			return executorConfig.maxThreads;
		}
		return null;
	}

	/**
	 * Bind this scheduler to a global (shared) executor, such as {@code scheduled-tasks}, instead of a private pool.
	 * The shared executor is never shut down by this scheduler. Must be called before startup.
	 *
	 * @param executorName The name of the global executor
	 *
	 * @return This scheduler
	 */
	public BaseScheduler useGlobalExecutor( String executorName ) {
		this.globalExecutorName = executorName;
		return this;
	}

	/**
	 * Override the fallback global executor name. Visible for testing.
	 *
	 * @param name The fallback executor name
	 *
	 * @return This scheduler
	 */
	public BaseScheduler setFallbackExecutorNameForTest( String name ) {
		this.fallbackExecutorName = name;
		return this;
	}

	/**
	 * Is there a global executor with this name that can schedule tasks?
	 */
	private boolean isUsableGlobalExecutor( String executorName ) {
		return this.asyncService.hasExecutor( executorName )
		    && this.asyncService.getExecutor( executorName ).executor() instanceof BoxScheduledExecutor;
	}

	/**
	 * Resolve the global executor this scheduler is bound to. An unknown or non-scheduled executor falls back to {@code scheduled-tasks}.
	 *
	 * @return The shared executor, or null if this scheduler is not bound to a global executor or no usable one exists
	 */
	private BoxExecutor resolveGlobalExecutor() {
		if ( this.globalExecutorName == null ) {
			return null;
		}
		if ( isUsableGlobalExecutor( this.globalExecutorName ) ) {
			return this.asyncService.getExecutor( this.globalExecutorName );
		}
		this.logger.warn( "Scheduler [{}] global executor [{}] does not exist or is not a scheduled executor, falling back to [{}]", this.name,
		    this.globalExecutorName, this.fallbackExecutorName );
		if ( isUsableGlobalExecutor( this.fallbackExecutorName ) ) {
			return this.asyncService.getExecutor( this.fallbackExecutorName );
		}
		this.logger.warn( "Scheduler [{}] fallback executor [{}] is not usable either, using a private pool", this.name, this.fallbackExecutorName );
		return null;
	}

	/**
	 * Startup this scheduler and all of it's scheduled tasks
	 */
	public synchronized BaseScheduler startup() {
		if ( !this.started ) {
			// A (re)start clears the shutdown gate, so the scheduler can accept work again
			this.shuttingDown = false;
			// ...and so must its tasks: the submission gate is per task, and leaving it closed would make
			// every restart a scheduler that never runs anything again.
			this.tasks.values().forEach( record -> record.task.resumeAcceptingSubmissions() );
			// Run on the shared global executor if bound to one, else build out a private executor for this scheduler
			this.executor		= resolveGlobalExecutor();
			this.sharedExecutor	= this.executor != null;
			if ( this.executor == null ) {
				this.executor = asyncService.newScheduledExecutor( name + "-scheduler", getConfiguredThreads() );
			}

			// Iterate over tasks and send them off for scheduling
			this.tasks.entrySet()
			    .parallelStream()
			    .forEachOrdered( entry -> startupTask( entry.getKey() ) );

			// Mark scheduler as started
			this.started	= true;
			this.startedAt	= Instant.now();

			// callback
			this.onStartup();

			// Log it
			this.logger.info( "Scheduler [{}] has started!", this.name );
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
	public synchronized BaseScheduler restart( boolean force, long timeout ) {
		this.logger.info( "+ Restarting scheduler [{}] with force: {} and timeout: {}", this.name, force, timeout );
		// Shutdown first
		shutdown( force, timeout );
		// Re-open the gate before configuring. shutdown() closes it, and configure() registers the tasks - the
		// gate is otherwise only reopened by startup(), which runs after configure(), so restart() would throw
		// on its own scheduler's task registration.
		this.shuttingDown = false;
		// Clear tasks
		clearTasks();
		// Configure again
		configure();
		// Startup
		startup();
		this.logger.info( "+ Scheduler [{}] has been restarted!", this.name );
		return this;
	}

	/**
	 * Clear all tasks from the scheduler. Usually done by a restart
	 *
	 * @return The scheduler object
	 */
	public synchronized BaseScheduler clearTasks() {
		this.tasks.clear();
		return this;
	}

	/**
	 * Reject a clustered task whose period cannot accommodate the claim protocol.
	 * <p>
	 * The floor comes from the configured timings ({@code verifyRounds x settleMillis} plus the claim
	 * delay, plus a margin), and is computed as if peers exist - a task must not validate against
	 * today's peer count and then start failing the moment a second node joins.
	 *
	 * @param taskRecord The task record being started
	 *
	 * @throws BoxRuntimeException When the task is clustered and its period is below the floor
	 */
	private void validateClusterPeriod( TaskRecord taskRecord ) {
		ClusterService cluster = BoxRuntime.getInstance().getClusterService();

		// A task that explicitly opts into clustering on a server that is not clustered would otherwise run on
		// every node - the opposite of what it asked for - so refuse it with a message that says what to do.
		if ( Boolean.TRUE.equals( taskRecord.task.getCluster() )
		    && ( cluster == null || cluster.getConfig() == null || !cluster.getConfig().enabled ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "Task [%s] in scheduler [%s] is marked as clustered, but clustering is not enabled on this node. "
			            + "Enable it with cluster.enabled=true and set cluster.cacheName to a cache backed by a store every "
			            + "node can see, or set the task's cluster=false.",
			        taskRecord.name,
			        this.name
			    )
			);
		}
		if ( cluster == null || cluster.getConfig() == null ) {
			return;
		}
		if ( !TaskClusterClaim.isClustered( taskRecord.task, cluster ) ) {
			return;
		}

		var		config		= cluster.getConfig();
		long	floor		= TaskClusterClaim.minimumPeriodSeconds();
		long	periodSec	= TaskClusterClaim.periodSeconds( taskRecord.task );

		// Cron and calendar tasks are polled, not period-bound, so only explicit periods are checked
		if ( periodSec <= 0 || periodSec >= floor ) {
			return;
		}

		throw new BoxRuntimeException(
		    String.format(
		        "Task [%s] in scheduler [%s] is clustered with a period of %ds, below the minimum of %ds for cluster mode [%s]. "
		            + "Lower the task period, or set the task's cluster=false. "
		            + "The period is not silently clamped because that would change the claim geometry.",
		        taskRecord.name,
		        this.name,
		        periodSec,
		        floor,
		        TaskClusterClaim.modeName(),
		        TaskClusterClaim.isBalanced() ? "balanced pushback" : "eager delay"
		    )
		);
	}

	/**
	 * Startup manullay the passed in task
	 *
	 * @param task The task to startup
	 *
	 * @return The task record
	 */
	public TaskRecord startupTask( ScheduledTask task ) {
		return startupTask( task.getName() );
	}

	/**
	 * Startup a specific task by name
	 *
	 * @param taskName The name of the task
	 *
	 * @return The task record
	 */
	public TaskRecord startupTask( String taskName ) {
		// Get the task record
		var taskRecord = getTaskRecord( taskName );

		// Nothing may be submitted once a shutdown has begun
		if ( this.shuttingDown ) {
			this.logger.warn( "- Scheduler ({}) is shutting down, not scheduling task ({})", this.name, taskName );
			return taskRecord;
		}

		// A clustered task whose period is below the cluster floor cannot service a claim. Reject it
		// loudly rather than silently clamping the protocol timings, which would change the geometry
		// the node computed.
		validateClusterPeriod( taskRecord );

		// Verify we can start it up the task or not
		if ( taskRecord.task.isDisabled() ) {
			taskRecord.disabled = true;
			this.logger.warn(
			    "- Scheduler ({}) skipping task ({}) as it is disabled.",
			    this.name,
			    taskName
			);
			return taskRecord;
		} else {
			// Log scheduling startup
			this.logger.info(
			    "- Scheduler ({}) scheduling task ({})...",
			    this.name,
			    taskName
			);
		}

		// Verify that the task record: scheduledAt is null
		if ( taskRecord.scheduledAt != null ) {
			this.logger.warn(
			    "- Scheduler ({}) skipping task ({}) as it has already been scheduled.",
			    this.name,
			    taskName
			);
			return taskRecord;
		}

		// Send it off for scheduling
		try {
			ScheduledFuture<?> future = taskRecord.task.start();
			if ( future == null ) {
				// The task refused the submission, because a shutdown began underneath us
				this.logger.warn( "- Scheduler ({}) did not schedule task ({}) as it is shutting down", this.name, taskName );
				return taskRecord;
			}
			taskRecord.future		= future;
			taskRecord.scheduledAt	= LocalDateTime.now( this.timezone );
			this.logger.debug(
			    "√ Task ({}) scheduled successfully.",
			    taskName
			);
		} catch ( Exception e ) {
			this.logger.error(
			    "X Error scheduling task ({}}) => {}",
			    this.name + "." + taskName,
			    e.getMessage()
			);
			e.printStackTrace();
			// Mark it as an error
			taskRecord.error		= true;
			taskRecord.errorMessage	= e.getMessage();
			taskRecord.stacktrace	= Arrays.toString( e.getStackTrace() );
		}

		return taskRecord;
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 *
	 * @param force   If true, it forces all shutdowns this is usually true when doing reinits
	 * @param timeout The timeout in seconds to wait for the shutdown of all tasks, defaults to 30 or whatever you set using the setShutdownTimeout()
	 *                method
	 */
	public BaseScheduler shutdown( boolean force, long timeout ) {
		// If started, then we can shutdown
		if ( !this.started ) {
			this.logger.info( "Scheduler [{}] has not been started yet. Skipping shutdown.", this.name );
			return this;
		}

		// Refuse any new work from this instant, before anything is cancelled or interrupted
		beginShutdown();

		// callback
		this.onShutdown();

		// Cancel the tasks this scheduler scheduled, they may run on a shared or per-task executor that must keep running
		this.tasks.values().forEach( record -> {
			if ( record.future != null ) {
				record.future.cancel( force );
			}
		} );

		if ( force ) {
			// Precise, and works whichever pool the task runs on
			interruptRunningTasks();
		}

		if ( this.sharedExecutor ) {
			// Shared executor: it keeps running for everyone else, but shutdown still honours its timeout
			// for this scheduler's runs - wait, then interrupt, then wait once more before giving up.
			if ( !force ) {
				awaitRunningTasks( timeout );
			}
		} else {
			// Private executor: shutdown and await termination or kill it now! A shared global executor is never shut down.
			if ( force || timeout <= 0 ) {
				// Zero grace means "kill it dead": no waiting, no interrupting, just stop it
				this.executor.scheduledExecutor().shutdownNow();
			} else {
				this.executor.shutdownAndAwaitTermination( timeout, TimeUnit.SECONDS );
			}
			// Remove executor
			this.asyncService.deleteExecutor( this.name + "-scheduler" );
		}
		// Remove task records
		clearTasks();

		// Mark it as stopped
		this.started	= false;
		this.startedAt	= null;

		// Log it
		this.logger.info( "Scheduler [{}] has been shutdown!", this.name );
		return this;
	}

	/**
	 * Wait for the executions of this scheduler's tasks that are in flight, up to the timeout. Used instead of an executor
	 * termination wait when the executor is shared.
	 */
	/**
	 * Stop accepting new work for this scheduler: no new tasks may be registered and no task may
	 * schedule a new execution from this point until the scheduler starts up again.
	 */
	private void beginShutdown() {
		this.shuttingDown = true;
		this.tasks.values().forEach( record -> record.task.stopAcceptingSubmissions() );
	}

	/**
	 * Enforce the shutdown timeout for this scheduler's in-flight runs.
	 * <p>
	 * Three phases, in order:
	 * <ol>
	 * <li><b>Wait</b> - give every run the chance to finish on its own.</li>
	 * <li><b>Interrupt</b> - if the wait expires, interrupt exactly this scheduler's runs. Per task,
	 * because the executor may be shared with other schedulers whose work must not be touched.</li>
	 * <li><b>Wait again, briefly</b> - give the interrupted runs a moment to unwind and record their
	 * outcome, then let shutdown proceed.</li>
	 * </ol>
	 * Only this scheduler's tasks are affected; the shared executor itself is left running and is
	 * stopped by {@code AsyncService} when the runtime shuts down.
	 */
	private void awaitRunningTasks( long timeout ) {
		long interruptGrace = interruptGraceSeconds();

		// Phase 1: wait for a clean finish. A zero grace means "do not wait at all".
		if ( timeout > 0 && awaitAllTasksRunning( timeout ) ) {
			return;
		}

		this.logger.warn(
		    "Scheduler [{}] shutdown: [{}] task(s) still running after [{}s], interrupting them",
		    this.name,
		    this.tasks.values().stream().filter( record -> record.task.getRunningCount() > 0 ).count(),
		    timeout
		);

		// Phase 2: interrupt this scheduler's runs
		interruptRunningTasks();

		// Phase 3: give them a bounded chance to unwind. A zero interrupt grace goes straight on to the
		// forced shutdown, which is how "kill it dead" is expressed.
		if ( interruptGrace <= 0 ) {
			return;
		}
		if ( !awaitAllTasksRunning( interruptGrace ) ) {
			this.tasks.values()
			    .stream()
			    .filter( record -> record.task.getRunningCount() > 0 )
			    .forEach( record -> this.logger.warn(
			        "Scheduler [{}] shutdown: task [{}] is still running [{}s] after being interrupted",
			        this.name,
			        record.name,
			        interruptGrace
			    ) );
		}
	}

	/**
	 * How long to wait after interrupting, from configuration. Zero means do not wait: go straight to
	 * the forced shutdown.
	 *
	 * @return The interrupt grace in seconds
	 */
	private long interruptGraceSeconds() {
		return BoxRuntime.getInstance().getConfiguration().scheduler.shutdownInterruptGraceSeconds;
	}

	/**
	 * Wait until no task in this scheduler is running.
	 *
	 * @return true when everything finished within the timeout
	 */
	private boolean awaitAllTasksRunning( long timeout ) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos( timeout );
		while ( this.tasks.values().stream().anyMatch( record -> record.task.getRunningCount() > 0 ) ) {
			if ( System.nanoTime() >= deadline ) {
				return false;
			}
			try {
				Thread.sleep( 25 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return this.tasks.values().stream().noneMatch( record -> record.task.getRunningCount() > 0 );
			}
		}
		return true;
	}

	/**
	 * Interrupt every in-flight run of every task in this scheduler.
	 */
	private void interruptRunningTasks() {
		this.tasks.values().forEach( record -> record.task.interruptRunningTasks() );
	}

	/**
	 * The shutdown timeout used when none is given: the configured grace, so operators can dial it.
	 *
	 * @return The configured graceful shutdown timeout in seconds
	 */
	public long getShutdownGraceSeconds() {
		return BoxRuntime.getInstance().getConfiguration().scheduler.shutdownGraceSeconds;
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * using the default timeout
	 *
	 * @param force If true, it forces all shutdowns this is usually true when doing reinits
	 *
	 * @return The scheduler object
	 */
	public BaseScheduler shutdown( boolean force ) {
		return shutdown( force, BoxRuntime.getInstance().getSchedulerService().getShutdownGraceSeconds() );
	}

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * We do not force and we use the default timeout
	 *
	 * @return The scheduler object
	 */
	public BaseScheduler shutdown() {
		return shutdown( false, BoxRuntime.getInstance().getSchedulerService().getShutdownGraceSeconds() );
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
		this.logger.info( "Shutting down scheduler [{}]", this.name );
	}

	/**
	 * Called after the scheduler has registered all schedules
	 */
	public void onStartup() {
		this.logger.info( "Starting up scheduler [{}]", this.name );
	}

	/**
	 * Called whenever ANY task fails
	 *
	 * @param task      The task that got executed
	 * @param exception The exception object
	 */
	public void onAnyTaskError( ScheduledTask task, Exception exception ) {
		this.logger.error(
		    "Task [{}.{}] has failed with {}",
		    getSchedulerName(),
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
		this.logger.info( "Task [{}.{}] has succeeded", getSchedulerName(), task.getName() );
	}

	/**
	 * Called before ANY task runs
	 *
	 * @param task The task about to be executed
	 */
	public void beforeAnyTask( ScheduledTask task ) {
		this.logger.debug( "Task [{}.{}] is about to run", getSchedulerName(), task.getName() );
	}

	/**
	 * Called after ANY task runs
	 *
	 * @param task   The task that got executed
	 *
	 * @param result The result (if any) that the task produced
	 */
	public void afterAnyTask( ScheduledTask task, Optional<?> result ) {
		this.logger.debug(
		    "Task [{}.{}] has run with result [{}]",
		    getSchedulerName(),
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
	 * Every task record this scheduler holds, for reporting and load accounting.
	 *
	 * @return The task records
	 */
	public java.util.Collection<TaskRecord> getTaskRecords() {
		return java.util.List.copyOf( this.tasks.values() );
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
	public BaseScheduler removeTask( String name ) {
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
		return this.tasks;
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
	 * Is it running: Alias to hasStarted()
	 *
	 * @return true if running, false if not
	 */
	public Boolean isRunning() {
		return this.started;
	}

	/**
	 * Get when it was started. If not started, then it will return null
	 */
	public Instant getStartedAt() {
		return this.startedAt;
	}

	/**
	 * Get the scheduler name
	 *
	 * @return the name
	 */
	public String getSchedulerName() {
		return this.name;
	}

	/**
	 * Set the scheduler name
	 *
	 * @param name the name to set
	 */
	public BaseScheduler setSchedulerName( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Get the timezone
	 *
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
	public BaseScheduler setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
		return this;
	}

	/**
	 * Set the scheduler's timezone as a string, we will convert it to a ZoneId object
	 *
	 * @param timezone the timezone to set as a string
	 */
	public BaseScheduler setTimezone( String timezone ) {
		return setTimezone( ZoneId.of( timezone ) );
	}

	/**
	 * Set the default timezone into the task
	 *
	 * @return Scheduler
	 */
	public BaseScheduler setDefaultTimezone() {
		this.timezone = ZoneId.systemDefault();
		return this;
	}

	/**
	 * Signal this scheduler to stop, without waiting for anything.
	 * <p>
	 * Used by runtime shutdown, where every pool is signalled first and then all of them are drained
	 * concurrently: waiting here as well would serialise the scheduler's drain behind the executor
	 * drain for no benefit, since both are waiting on the same runs.
	 *
	 * @return This scheduler
	 */
	public BaseScheduler signalShutdown() {
		if ( !this.started ) {
			return this;
		}
		beginShutdown();
		this.tasks.values().forEach( record -> {
			if ( record.future != null ) {
				record.future.cancel( false );
			}
		} );
		return this;
	}

	/**
	 * Finish this scheduler's shutdown after its pool has been drained. Clears the task records and
	 * marks the scheduler stopped, without waiting for anything.
	 *
	 * @return This scheduler
	 */
	public BaseScheduler completeShutdown() {
		clearTasks();
		this.started	= false;
		this.startedAt	= null;
		this.logger.info( "Scheduler [{}] has been shutdown!", this.name );
		return this;
	}

	/**
	 * Is this scheduler shutting down? Visible so callers and tests can see the gate.
	 *
	 * @return true once a shutdown has begun and before the scheduler starts again
	 */
	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

	/**
	 * Get the name of the coordination cache.
	 * <p>
	 * There is exactly one coordination cache for the deployment, owned by the cluster service, so this
	 * reads it from there rather than keeping a per-scheduler copy.
	 *
	 * @return the coordination cache name
	 */
	public String getCacheName() {
		ClusterService clusterService = BoxRuntime.getInstance().getClusterService();
		if ( clusterService != null && clusterService.getConfig() != null ) {
			return clusterService.getCacheName();
		}
		return this.cacheName;
	}

	/**
	 * Set the name of the coordination cache.
	 *
	 * @param cacheName the cache name to set
	 *
	 * @return the scheduler object
	 *
	 * @deprecated There is one coordination cache for the deployment, configured as
	 *             {@code cluster.cacheName}. Per-scheduler cache selection is ignored.
	 */
	@Deprecated
	public BaseScheduler setCacheName( String cacheName ) {
		this.cacheName = cacheName;
		this.logger.warn(
		    "Scheduler [{}] set a per-scheduler cache name [{}], which is ignored: cluster coordination uses the single [cluster.cacheName]",
		    this.name,
		    cacheName
		);
		return this;
	}

	/**
	 * Get the Aysnc Service
	 *
	 * @return the asyncService
	 */
	public AsyncService getAsyncService() {
		return this.asyncService;
	}

	/**
	 * Set the exectutor record for this scheduler
	 *
	 * @param executor the executor to set
	 *
	 * @return the scheduler object
	 */
	public BaseScheduler setExecutor( BoxExecutor executor ) {
		this.executor = executor;
		return this;
	}

	/**
	 * Get the executor record
	 *
	 * @return the executor record
	 */
	public BoxExecutor getExecutor() {
		return this.executor;
	}

	/**
	 * Get the logger
	 */
	public BoxLangLogger getLogger() {
		return this.logger;
	}

}
