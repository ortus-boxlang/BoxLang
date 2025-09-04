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
package ortus.boxlang.runtime.async.executors;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * BoxLang Executor class that wraps ExecutorService instances with enhanced health monitoring,
 * detailed statistics, and activity tracking functionality.
 */
public class BoxExecutor {

	// Record-like fields to maintain compatibility
	private final ExecutorService							executor;
	private final String									name;
	private final ExecutorType								type;
	private final Integer									maxThreads;

	// Enhanced tracking fields
	private final LocalDateTime								created;
	private volatile LocalDateTime							lastActivity;
	private final AtomicLong								taskSubmissionCount;

	// Health monitoring thresholds
	private final Map<String, Map<String, Object>>			healthThresholds;
	private static final Map<String, Map<String, Object>>	FEATURES	= initializeFeatures();

	/**
	 * Constructor
	 *
	 * @param executor   The executor service
	 * @param name       The name of the executor
	 * @param type       The executor type
	 * @param maxThreads The max threads, if applicable
	 */
	public BoxExecutor(
	    ExecutorService executor,
	    String name,
	    ExecutorType type,
	    Integer maxThreads ) {
		this.executor				= executor;
		this.name					= name;
		this.type					= type;
		this.maxThreads				= maxThreads;
		this.created				= LocalDateTime.now();
		this.lastActivity			= this.created;
		this.taskSubmissionCount	= new AtomicLong( 0 );

		// Initialize health thresholds
		this.healthThresholds		= initializeHealthThresholds();
	}

	// Record-like accessors for compatibility
	public ExecutorService executor() {
		return executor;
	}

	public String name() {
		return name;
	}

	public ExecutorType type() {
		return type;
	}

	public Integer maxThreads() {
		return maxThreads;
	}

	/**
	 * Initialize health monitoring thresholds
	 */
	private Map<String, Map<String, Object>> initializeHealthThresholds() {
		Map<String, Map<String, Object>>	thresholds		= new HashMap<>();

		Map<String, Object>					poolUtilization	= new HashMap<>();
		poolUtilization.put( "degraded", 75 );
		poolUtilization.put( "critical", 95 );
		thresholds.put( "poolUtilization", poolUtilization );

		Map<String, Object> threadUtilization = new HashMap<>();
		threadUtilization.put( "degraded", 75 );
		threadUtilization.put( "critical", 95 );
		thresholds.put( "threadUtilization", threadUtilization );

		Map<String, Object> queueUtilization = new HashMap<>();
		queueUtilization.put( "degraded", 70 );
		queueUtilization.put( "critical", 95 );
		thresholds.put( "queueUtilization", queueUtilization );

		Map<String, Object> taskCompletionRate = new HashMap<>();
		taskCompletionRate.put( "degraded", 50 );
		taskCompletionRate.put( "critical", 25 );
		thresholds.put( "taskCompletionRate", taskCompletionRate );

		Map<String, Object> inactivity = new HashMap<>();
		inactivity.put( "value", 30 );
		thresholds.put( "inactivityMinutes", inactivity );

		Map<String, Object> minimumTasks = new HashMap<>();
		minimumTasks.put( "value", 10 );
		thresholds.put( "minimumTasksForCompletion", minimumTasks );

		return thresholds;
	}

	/**
	 * Initialize feature detection based on executor types
	 */
	private static Map<String, Map<String, Object>> initializeFeatures() {
		Map<String, Map<String, Object>>	features			= new HashMap<>();

		// ScheduledThreadPoolExecutor features
		Map<String, Object>					scheduledFeatures	= new HashMap<>();
		scheduledFeatures.put( "pool", true );
		scheduledFeatures.put( "taskMethods", true );
		scheduledFeatures.put( "isTerminating", true );
		scheduledFeatures.put( "queue", true );
		features.put( "ScheduledThreadPoolExecutor", scheduledFeatures );

		// ThreadPoolExecutor features
		Map<String, Object> threadPoolFeatures = new HashMap<>();
		threadPoolFeatures.put( "pool", true );
		threadPoolFeatures.put( "taskMethods", true );
		threadPoolFeatures.put( "isTerminating", true );
		threadPoolFeatures.put( "queue", true );
		features.put( "ThreadPoolExecutor", threadPoolFeatures );

		// ForkJoinPool features
		Map<String, Object> forkJoinFeatures = new HashMap<>();
		forkJoinFeatures.put( "pool", false );
		forkJoinFeatures.put( "taskMethods", false );
		forkJoinFeatures.put( "isTerminating", true );
		forkJoinFeatures.put( "queue", false );
		features.put( "ForkJoinPool", forkJoinFeatures );

		// ThreadPerTaskExecutor features (Virtual)
		Map<String, Object> virtualFeatures = new HashMap<>();
		virtualFeatures.put( "pool", false );
		virtualFeatures.put( "taskMethods", false );
		virtualFeatures.put( "isTerminating", false );
		virtualFeatures.put( "queue", false );
		features.put( "ThreadPerTaskExecutor", virtualFeatures );

		return features;
	}

	/**
	 * Check if the executor has a specific feature
	 */
	private boolean hasFeature( String feature ) {
		String				classType		= this.executor.getClass().getSimpleName();
		Map<String, Object>	typeFeatures	= FEATURES.get( classType );
		if ( typeFeatures == null ) {
			return false;
		}
		return Boolean.TRUE.equals( typeFeatures.get( feature ) );
	}

	/**
	 * Get the executor service casted as a {@link BoxScheduledExecutor}
	 *
	 * @return The executor service
	 */
	public BoxScheduledExecutor scheduledExecutor() {
		return ( BoxScheduledExecutor ) this.executor;
	}

	/**
	 * Get the executor logger
	 */
	public BoxLangLogger getLogger() {
		return BoxRuntime.getInstance().getLoggingService().ASYNC_LOGGER;
	}

	/**
	 * Check if executor is healthy (simple boolean check)
	 * Uses current stats to determine health
	 *
	 * @return boolean True if status is "healthy" or "idle"
	 */
	public boolean isHealthy() {
		IStruct	stats			= getStats();
		String	healthStatus	= ( String ) stats.get( "healthStatus" );
		return "healthy".equalsIgnoreCase( healthStatus ) || "idle".equalsIgnoreCase( healthStatus );
	}

	/**
	 * Returns true if all tasks have completed following shut down.
	 */
	public boolean isTerminated() {
		return this.executor.isTerminated();
	}

	/**
	 * Returns true if this executor is in the process of terminating after shutdown() or shutdownNow() but has
	 * not completely terminated.
	 */
	public boolean isTerminating() {
		if ( hasFeature( "isTerminating" ) ) {
			if ( this.executor instanceof ThreadPoolExecutor ) {
				return ( ( ThreadPoolExecutor ) this.executor ).isTerminating();
			} else if ( this.executor instanceof ForkJoinPool ) {
				return ( ( ForkJoinPool ) this.executor ).isTerminating();
			}
		}
		return false;
	}

	/**
	 * Returns true if this executor has been shut down.
	 */
	public boolean isShutdown() {
		return this.executor.isShutdown();
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or
	 * the current thread is interrupted, whichever happens first.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit to use
	 *
	 * @return true if all tasks have completed following shut down
	 */
	public boolean awaitTermination( Long timeout, TimeUnit unit ) {
		if ( this.executor == null )
			return true;
		timeout	= timeout == null ? AsyncService.DEFAULT_TIMEOUT : timeout;
		unit	= unit == null ? TimeUnit.SECONDS : unit;

		try {
			return this.executor.awaitTermination( timeout, unit );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
	 *
	 * @return This executor for chaining
	 */
	public BoxExecutor shutdown() {
		if ( this.executor != null ) {
			this.executor.shutdown();
		}
		return this;
	}

	/**
	 * Attempts to stop all actively executing tasks, halts the processing of
	 * waiting tasks, and returns a list of the tasks that were awaiting execution.
	 *
	 * @return list of tasks that never commenced execution
	 */
	public List<Runnable> shutdownNow() {
		if ( this.executor == null ) {
			return new ArrayList<>();
		}
		return this.executor.shutdownNow();
	}

	/**
	 * Returns the task queue used by this executor.
	 * If the executor has no queue, an empty queue is returned for compatibility.
	 *
	 * @return A queue that holds the tasks submitted to this executor.
	 */
	public BlockingQueue<Runnable> getQueue() {
		if ( hasFeature( "queue" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getQueue();
		}
		return new java.util.concurrent.LinkedBlockingQueue<>();
	}

	/**
	 * Returns the approximate number of threads that are actively executing tasks.
	 */
	public int getActiveCount() {
		if ( hasFeature( "taskMethods" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getActiveCount();
		}
		return 0;
	}

	/**
	 * Returns the approximate total number of tasks that have ever been scheduled for execution.
	 */
	public long getTaskCount() {
		if ( hasFeature( "taskMethods" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getTaskCount();
		}
		return 0;
	}

	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 */
	public long getCompletedTaskCount() {
		if ( hasFeature( "taskMethods" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getCompletedTaskCount();
		}
		return 0;
	}

	/**
	 * Returns the core number of threads.
	 */
	public int getCorePoolSize() {
		if ( hasFeature( "pool" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getCorePoolSize();
		}
		return 0;
	}

	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 */
	public int getLargestPoolSize() {
		if ( hasFeature( "pool" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getLargestPoolSize();
		}
		return 0;
	}

	/**
	 * Returns the maximum allowed number of threads.
	 */
	public int getMaximumPoolSize() {
		if ( hasFeature( "pool" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getMaximumPoolSize();
		}
		return 0;
	}

	/**
	 * Returns the current number of threads in the pool.
	 */
	public int getPoolSize() {
		if ( hasFeature( "pool" ) && this.executor instanceof ThreadPoolExecutor ) {
			return ( ( ThreadPoolExecutor ) this.executor ).getPoolSize();
		} else if ( this.executor instanceof ForkJoinPool ) {
			return ( ( ForkJoinPool ) this.executor ).getPoolSize();
		}
		return 0;
	}

	/**
	 * Calls the `shutdown` of the executor - which is non blocking
	 */
	public void shutdownQuiet() {
		if ( executor == null )
			return;
		getLogger().trace( "Executor ({}) shutting down quiet", this.name );
		this.executor.shutdown();
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or
	 * the current thread is interrupted, whichever happens first.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 */
	public void shutdownAndAwaitTermination( Long timeout, TimeUnit unit ) {
		if ( executor == null )
			return;
		timeout	= timeout == null ? AsyncService.DEFAULT_TIMEOUT : timeout;
		unit	= unit == null ? TimeUnit.SECONDS : unit;

		// Disable new tasks from being submitted
		this.executor.shutdown();
		try {
			getLogger().trace( "Executor ({}) shutdown executed, waiting for tasks to finalize...", this.name );

			// Wait for tasks to terminate
			if ( !this.executor.awaitTermination( timeout, unit ) ) {
				getLogger().warn( "Executor tasks did not shutdown, forcibly shutting down executor ({})...", this.name );

				// Cancel all tasks forcibly
				List<Runnable> taskList = this.executor.shutdownNow();

				getLogger().trace( "Tasks waiting execution on executor ({}) -> tasks({})", this.name, taskList.size() );

				// Wait again now forcibly
				if ( !this.executor.awaitTermination( timeout, unit ) ) {
					getLogger().error( "Executor ({}) did not terminate even gracefully :(", this.name );
				}
			} else {
				getLogger().trace( "Executor ({}) shutdown complete", this.name );
			}
		}
		// Catch if exceptions or interrupted
		catch ( InterruptedException e ) {
			getLogger().error( "Executor ({}) shutdown interrupted or exception thrown ({}) :)", this.name, e.getMessage() );
			// force it down!
			this.executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Build out a new scheduled task bound to this executor
	 * Calling this method is not the mean that the task will be executed.
	 * It just builds out a record for the task. It will be your job to call the {@code start()} method on it to start the task.
	 *
	 * This can be used to create a task and then schedule it to run at a later time and not
	 * bound to a Scheduler.
	 *
	 * @param name The name of the task
	 *
	 * @return The new task bound to this executor
	 */
	public ScheduledTask newTask( String name ) {
		return new ScheduledTask( name, this );
	}

	/**
	 * Build out a new scheduled task bound to this executor
	 * Calling this method is not the mean that the task will be executed.
	 * It just builds out a record for the task. It will be your job to call the {@code start()} method on it to start the task.
	 *
	 * This can be used to create a task and then schedule it to run at a later time and not
	 * bound to a Scheduler.
	 *
	 * The name will be auto-generated
	 *
	 * @return The new task bound to this executor
	 */
	public ScheduledTask newTask() {
		var name = "Task-" + System.currentTimeMillis();
		return new ScheduledTask( name, this );
	}

	/**
	 * Our very own stats struct map to give you a holistic view of the executor
	 * with enhanced health monitoring and detailed metrics
	 *
	 * @return The stats struct with comprehensive information
	 */
	public IStruct getStats() {
		LocalDateTime	now						= LocalDateTime.now();
		long			uptimeSeconds			= ChronoUnit.SECONDS.between( this.created, now );
		long			lastActivitySecondsAgo	= ChronoUnit.SECONDS.between( this.lastActivity, now );
		long			lastActivityMinutesAgo	= ChronoUnit.MINUTES.between( this.lastActivity, now );

		// Base stats structure
		Struct			stats					= new Struct();

		// Basic information
		stats.put( "created", this.created.toString() );
		stats.put( "features", FEATURES.get( this.executor.getClass().getSimpleName() ) );
		stats.put( "lastActivity", this.lastActivity.toString() );
		stats.put( "lastActivityMinutesAgo", Long.valueOf( lastActivityMinutesAgo ) );
		stats.put( "lastActivitySecondsAgo", Long.valueOf( lastActivitySecondsAgo ) );
		stats.put( "name", this.name );
		stats.put( "thresholds", this.healthThresholds );
		stats.put( "type", this.executor.getClass().getName() );
		stats.put( "uptimeSeconds", uptimeSeconds );
		stats.put( "uptimeDays", ChronoUnit.DAYS.between( this.created, now ) );

		// Task submission metrics
		long taskSubmissionCount = this.taskSubmissionCount.get();
		stats.put( "taskSubmissionCount", taskSubmissionCount );
		stats.put( "averageTasksPerSecond", uptimeSeconds > 0 ? ( double ) taskSubmissionCount / uptimeSeconds : 0.0 );
		stats.put( "averageTasksPerMinute", uptimeSeconds > 0 ? ( ( double ) taskSubmissionCount / uptimeSeconds ) * 60 : 0.0 );

		// States
		stats.put( "isShutdown", isShutdown() );
		stats.put( "isTerminated", isTerminated() );
		stats.put( "isTerminating", isTerminating() );

		// Initialize pool and task stats with defaults
		stats.put( "corePoolSize", 0 );
		stats.put( "largestPoolSize", 0 );
		stats.put( "maximumPoolSize", 0 );
		stats.put( "poolSize", 0 );
		stats.put( "poolUtilization", 0.0 );
		stats.put( "activeCount", 0 );
		stats.put( "completedTaskCount", 0L );
		stats.put( "taskCount", 0L );
		stats.put( "taskCompletionRate", 0.0 );
		stats.put( "threadsUtilization", 0.0 );

		// Initialize queue stats with defaults
		stats.put( "queueCapacity", 0 );
		stats.put( "queueIsEmpty", true );
		stats.put( "queueIsFull", false );
		stats.put( "queueRemainingCapacity", 0 );
		stats.put( "queueSize", 0 );
		stats.put( "queueType", "N/A" );
		stats.put( "queueUtilization", 0.0 );

		switch ( this.type ) {
			case SINGLE :
			case VIRTUAL : {
				stats.put( "activeCount", 0 );
				stats.put( "completedTaskCount", 0L );
				stats.put( "corePoolSize", 1 );
				stats.put( "largestPoolSize", 1 );
				stats.put( "maximumPoolSize", 1 );
				stats.put( "maxThreads", this.maxThreads );
				stats.put( "poolSize", 1 );
				stats.put( "taskCount", 1L );
				break;
			}
			case CACHED :
			case FIXED :
			case SCHEDULED :
				if ( this.executor instanceof ThreadPoolExecutor ) {
					ThreadPoolExecutor	thisExecutor	= ( ThreadPoolExecutor ) this.executor;

					// Pool stats
					int					corePoolSize	= thisExecutor.getCorePoolSize();
					int					maximumPoolSize	= thisExecutor.getMaximumPoolSize();
					int					poolSize		= thisExecutor.getPoolSize();
					int					activeCount		= thisExecutor.getActiveCount();

					stats.put( "activeCount", activeCount );
					stats.put( "completedTaskCount", thisExecutor.getCompletedTaskCount() );
					stats.put( "corePoolSize", corePoolSize );
					stats.put( "largestPoolSize", thisExecutor.getLargestPoolSize() );
					stats.put( "maximumPoolSize", maximumPoolSize );
					stats.put( "maxThreads", this.maxThreads );
					stats.put( "poolSize", poolSize );
					stats.put( "taskCount", thisExecutor.getTaskCount() );

					// Calculate utilization percentages
					if ( maximumPoolSize > 0 ) {
						stats.put( "poolUtilization", ( double ) poolSize / maximumPoolSize * 100.0 );
					}
					if ( poolSize > 0 ) {
						stats.put( "threadsUtilization", ( double ) activeCount / poolSize * 100.0 );
					}

					// Task completion rate
					long taskCount = thisExecutor.getTaskCount();
					if ( taskCount > 0 ) {
						stats.put( "taskCompletionRate", ( double ) thisExecutor.getCompletedTaskCount() / taskCount * 100.0 );
					}

					// Queue information
					BlockingQueue<Runnable>	queue				= thisExecutor.getQueue();
					int						queueSize			= queue.size();
					int						remainingCapacity	= queue.remainingCapacity();
					int						queueCapacity		= queueSize + remainingCapacity;

					stats.put( "queueCapacity", queueCapacity );
					stats.put( "queueIsEmpty", queue.isEmpty() );
					stats.put( "queueIsFull", remainingCapacity == 0 && queueCapacity > 0 );
					stats.put( "queueRemainingCapacity", remainingCapacity );
					stats.put( "queueSize", queueSize );
					stats.put( "queueType", queue.getClass().getSimpleName() );
					if ( queueCapacity > 0 ) {
						stats.put( "queueUtilization", ( double ) queueSize / queueCapacity * 100.0 );
					}
				}
				break;
			case WORK_STEALING :
			case FORK_JOIN :
				if ( this.executor instanceof ForkJoinPool ) {
					ForkJoinPool pool = ( ForkJoinPool ) this.executor;
					stats.put( "activeCount", Integer.valueOf( pool.getActiveThreadCount() ) );
					stats.put( "completedTaskCount", 0L ); // Not available for ForkJoinPool
					stats.put( "corePoolSize", pool.getPoolSize() );
					stats.put( "largestPoolSize", pool.getPoolSize() );
					stats.put( "maximumPoolSize", pool.getPoolSize() );
					stats.put( "maxThreads", this.maxThreads );
					stats.put( "poolSize", pool.getPoolSize() );
					stats.put( "taskCount", Long.valueOf( pool.getRunningThreadCount() ) );
					stats.put( "queuedTaskCount", pool.getQueuedTaskCount() );
					stats.put( "queuedSubmissionTaskCount", pool.getQueuedSubmissionCount() );
					stats.put( "hasQueuedSubmissions", pool.hasQueuedSubmissions() );
					stats.put( "stealCount", pool.getStealCount() );
					// ForkJoinPool doesn't have a traditional queue, use queued task count
					stats.put( "queueSize", Integer.valueOf( ( int ) pool.getQueuedTaskCount() ) );
					stats.put( "queueIsFull", false ); // ForkJoinPool typically doesn't have a fixed capacity
					stats.put( "queueUtilization", 0.0 ); // Not applicable for ForkJoinPool
					stats.put( "poolUtilization", pool.getPoolSize() > 0 ? ( double ) pool.getActiveThreadCount() / pool.getPoolSize() * 100.0 : 0.0 );
					stats.put( "threadsUtilization", pool.getPoolSize() > 0 ? ( double ) pool.getActiveThreadCount() / pool.getPoolSize() * 100.0 : 0.0 );
					stats.put( "taskCompletionRate", 0.0 ); // Not easily calculable for ForkJoinPool
				}
				break;
			default :
				// Keep defaults for unknown types
				break;
		}

		// Add health status (this must come last after all stats are calculated)
		String	healthStatus	= getHealthStatus( stats );
		IStruct	healthReport	= getHealthReport( stats, healthStatus );
		stats.put( "healthStatus", healthStatus );
		stats.put( "healthReport", healthReport );

		return stats;
	}

	/**
	 * Get the health status of the executor based on various metrics
	 *
	 * @param stats The current stats of the executor
	 *
	 * @return Health status string: "healthy", "degraded", "critical", "idle", "shutdown", "terminated", "draining"
	 */
	private String getHealthStatus( IStruct stats ) {
		// Shutdown/termination states (highest priority)
		if ( Boolean.TRUE.equals( stats.get( "isTerminated" ) ) )
			return "terminated";
		if ( Boolean.TRUE.equals( stats.get( "isShutdown" ) ) )
			return "shutdown";
		if ( Boolean.TRUE.equals( stats.get( "isTerminating" ) ) )
			return "draining";

		Map<String, Object>	poolThreshold				= ( Map<String, Object> ) this.healthThresholds.get( "poolUtilization" );
		Map<String, Object>	threadThreshold				= ( Map<String, Object> ) this.healthThresholds.get( "threadUtilization" );
		Map<String, Object>	queueThreshold				= ( Map<String, Object> ) this.healthThresholds.get( "queueUtilization" );
		Map<String, Object>	taskThreshold				= ( Map<String, Object> ) this.healthThresholds.get( "taskCompletionRate" );

		int					poolCritical				= ( Integer ) poolThreshold.get( "critical" );
		int					threadCritical				= ( Integer ) threadThreshold.get( "critical" );
		int					queueCritical				= ( Integer ) queueThreshold.get( "critical" );
		int					taskCritical				= ( Integer ) taskThreshold.get( "critical" );
		int					poolDegraded				= ( Integer ) poolThreshold.get( "degraded" );
		int					threadDegraded				= ( Integer ) threadThreshold.get( "degraded" );
		int					queueDegraded				= ( Integer ) queueThreshold.get( "degraded" );
		int					taskDegraded				= ( Integer ) taskThreshold.get( "degraded" );

		Double				poolUtilization				= ( Double ) stats.get( "poolUtilization" );
		Double				threadUtilization			= ( Double ) stats.get( "threadsUtilization" );
		Double				queueUtilization			= ( Double ) stats.get( "queueUtilization" );
		Double				taskCompletionRate			= ( Double ) stats.get( "taskCompletionRate" );
		Boolean				queueIsFull					= ( Boolean ) stats.get( "queueIsFull" );
		Long				lastActivityMinutesAgo		= ( Long ) stats.get( "lastActivityMinutesAgo" );
		Integer				activeCount					= ( Integer ) stats.get( "activeCount" );
		Integer				queueSize					= ( Integer ) stats.get( "queueSize" );
		Long				taskCount					= ( Long ) stats.get( "taskCount" );

		int					inactivityThreshold			= ( Integer ) ( ( Map<String, Object> ) this.healthThresholds.get( "inactivityMinutes" ) )
		    .get( "value" );
		int					minimumTasksForCompletion	= ( Integer ) ( ( Map<String, Object> ) this.healthThresholds.get( "minimumTasksForCompletion" ) )
		    .get( "value" );

		// Critical issues
		if ( Boolean.TRUE.equals( queueIsFull ) ||
		    poolUtilization > poolCritical ||
		    threadUtilization > threadCritical ||
		    queueUtilization > queueCritical ||
		    ( taskCount >= minimumTasksForCompletion && taskCompletionRate < taskCritical ) ) {
			return "critical";
		}

		// Check for idle state
		if ( lastActivityMinutesAgo > inactivityThreshold &&
		    activeCount == 0 &&
		    queueSize == 0 &&
		    !Boolean.TRUE.equals( stats.get( "isShutdown" ) ) &&
		    !Boolean.TRUE.equals( stats.get( "isTerminating" ) ) ) {
			return "idle";
		}

		// Degraded issues
		if ( poolUtilization > poolDegraded ||
		    threadUtilization > threadDegraded ||
		    queueUtilization > queueDegraded ||
		    ( taskCount >= minimumTasksForCompletion && taskCompletionRate < taskDegraded ) ) {
			return "degraded";
		}

		return "healthy";
	}

	/**
	 * Get a comprehensive health report with detailed analysis, issues, and recommendations
	 *
	 * @param stats        The current stats
	 * @param healthStatus The health status
	 *
	 * @return Detailed health report structure
	 */
	private IStruct getHealthReport( IStruct stats, String healthStatus ) {
		Struct				report					= new Struct();
		Array				issues					= new Array();
		Array				recommendations			= new Array();
		Array				alerts					= new Array();
		Array				insights				= new Array();

		Double				poolUtilization			= ( Double ) stats.get( "poolUtilization" );
		Double				threadUtilization		= ( Double ) stats.get( "threadsUtilization" );
		Double				queueUtilization		= ( Double ) stats.get( "queueUtilization" );
		Double				taskCompletionRate		= ( Double ) stats.get( "taskCompletionRate" );
		Double				averageTasksPerSecond	= ( Double ) stats.get( "averageTasksPerSecond" );
		Boolean				queueIsFull				= ( Boolean ) stats.get( "queueIsFull" );
		Long				uptimeDays				= ( Long ) stats.get( "uptimeDays" );
		Integer				poolSize				= ( Integer ) stats.get( "poolSize" );

		// Analyze pool utilization
		Map<String, Object>	poolThreshold			= ( Map<String, Object> ) this.healthThresholds.get( "poolUtilization" );
		int					poolCritical			= ( Integer ) poolThreshold.get( "critical" );
		int					poolDegraded			= ( Integer ) poolThreshold.get( "degraded" );

		if ( poolUtilization > poolCritical ) {
			issues.add( String.format( "Critical pool utilization: %.1f%%", poolUtilization ) );
			recommendations.add( "Immediately increase maximum pool size or reduce workload" );
			alerts.add( Struct.of( "level", "critical", "metric", "poolUtilization", "value", poolUtilization ) );
		} else if ( poolUtilization > poolDegraded ) {
			issues.add( String.format( "High pool utilization: %.1f%%", poolUtilization ) );
			recommendations.add( "Consider increasing maximum pool size" );
			alerts.add( Struct.of( "level", "warning", "metric", "poolUtilization", "value", poolUtilization ) );
		}

		// Analyze queue health
		if ( Boolean.TRUE.equals( queueIsFull ) ) {
			Integer	queueSize		= ( Integer ) stats.get( "queueSize" );
			Integer	queueCapacity	= ( Integer ) stats.get( "queueCapacity" );
			issues.add( String.format( "Queue is full: %d/%d capacity", queueSize, queueCapacity ) );
			recommendations.add( "Queue rejecting tasks - increase capacity or improve processing speed" );
			alerts.add( Struct.of( "level", "critical", "metric", "queueFull", "value", true ) );
		}

		// Performance insights
		if ( averageTasksPerSecond > 0 ) {
			insights.add( String.format( "Processing rate: %.2f tasks/second", averageTasksPerSecond ) );
		}
		if ( uptimeDays > 0 ) {
			insights.add( String.format( "Uptime: %d days", uptimeDays ) );
		}

		// Resource efficiency analysis
		if ( poolSize > 0 && averageTasksPerSecond > 0 ) {
			double tasksPerThread = averageTasksPerSecond / poolSize;
			insights.add( String.format( "Efficiency: %.2f tasks/second per thread", tasksPerThread ) );
		}

		report.put( "status", healthStatus );
		report.put( "summary",
		    issues.size() == 0 ? "Executor operating normally" : String.format( "%d issue%s detected", issues.size(), issues.size() == 1 ? "" : "s" ) );
		report.put( "issues", issues );
		report.put( "recommendations", recommendations );
		report.put( "alerts", alerts );
		report.put( "insights", insights );
		report.put( "lastChecked", LocalDateTime.now().toString() );

		return report;
	}

	/**
	 * Submit proxy to the executor with activity tracking
	 *
	 * @param runnable The runnable to submit
	 *
	 * @return The future
	 */
	public Future<?> submit( Runnable runnable ) {
		this.lastActivity = LocalDateTime.now();
		this.taskSubmissionCount.incrementAndGet();
		return this.executor.submit( runnable );
	}

	/**
	 * Submit proxy to the executor with activity tracking
	 *
	 * @param callable The callable to submit
	 *
	 * @return The future
	 */
	public Future<?> submit( Callable<?> callable ) {
		this.lastActivity = LocalDateTime.now();
		this.taskSubmissionCount.incrementAndGet();
		return this.executor.submit( callable );
	}

	/**
	 * Method to submit a Callable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( Callable<? extends Object> fn ) {
		try {
			return this.executor.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);
		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}

	/**
	 * Method to submit a Callable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( ForkJoinTask<? extends Object> fn ) {
		try {
			ForkJoinPool exec = ( ForkJoinPool ) this.executor;
			return exec.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);

		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}

	/**
	 * Method to submit a runnable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( Runnable fn ) {
		try {
			return this.executor.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);
		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}
}
