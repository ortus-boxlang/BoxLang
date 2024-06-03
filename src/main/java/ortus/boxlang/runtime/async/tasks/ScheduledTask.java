/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.async.tasks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.management.InvalidAttributeValueException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.DateTimeHelper;
import ortus.boxlang.runtime.types.util.StringUtil;
import ortus.boxlang.runtime.util.Timer;

/**
 * The ScheduledTask class is a {@link Runnable} that is used by the schedulers to execute tasks
 * in a more human and fluent approach.
 *
 * A task can be represented either by a {@link DynamicObject} or a Java {@link Callable} Lambda.
 *
 * You can use this class to create scheduled tasks in a human and friendly way!
 */
public class ScheduledTask implements Runnable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The human name of this task
	 */
	private String									name;

	/**
	 * The human group name of this task
	 */
	private String									group;

	/**
	 * The executor to use for this task
	 */
	private ExecutorRecord							executor			= null;

	/**
	 * The task as a {@link DynamicObject} or a {@link java.util.concurrent.Callable} Lambda that will be executed by the task
	 * Must implement the run() method or use the {@code method} property
	 */
	private Object									task;

	/**
	 * The method to execute in the DynamicObject, by default it is run()
	 */
	private String									method				= "run";

	/**
	 * The delay or time to wait before we execute the task in the scheduler
	 */
	private long									initialDelay		= 0L;

	/**
	 * The time unit string used when there is a delay requested for the task
	 */
	private TimeUnit								initialDelayTimeUnit;

	/**
	 * A fixed time period of execution of the tasks in this schedule. It does not wait for tasks to finish,
	 * tasks are fired exactly at that time period.
	 */
	private long									period				= 0L;

	/**
	 * The delay to use when using scheduleWithFixedDelay(), so tasks execute after this delay once completed
	 */
	private long									spacedDelay			= 0L;

	/**
	 * The time unit used to schedule the task
	 */
	private TimeUnit								timeUnit			= TimeUnit.MILLISECONDS;

	/**
	 * A handy boolean that is set when the task is annually scheduled
	 */
	private Boolean									annually			= false;

	/**
	 * A handy boolean that disables the scheduling of this task
	 */
	private Boolean									disabled			= false;

	/**
	 * A lambda, that if registered, determines if this task will be sent for scheduling or not.
	 * It is both evaluated at scheduling and at runtime.
	 */
	private Predicate<ScheduledTask>				whenPredicate;

	/**
	 * Constraint of what day of the month we need to run on: 1-31
	 */
	private int										dayOfTheMonth		= 0;

	/**
	 * Constraint of what day of the week this runs on: 1-7
	 */
	private int										dayOfTheWeek		= 0;

	/**
	 * Constraint to run only on weekends
	 */
	private Boolean									weekends			= false;

	/**
	 * Constraint to run only on weekdays
	 */
	private Boolean									weekdays			= false;

	/**
	 * Constraint to run only on the first business day of the month
	 */
	private Boolean									firstBusinessDay	= false;

	/**
	 * Constraint to run only on the last business day of the month
	 */
	private Boolean									lastBusinessDay		= false;

	/**
	 * By default tasks execute in an interval frequency which can cause tasks to
	 * stack if they take longer than their periods ( fire immediately after completion ).
	 * With this boolean flag turned on, the schedulers don't kick off the
	 * intervals until the tasks finish executing. Meaning no stacking.
	 */
	private Boolean									noOverlaps			= false;

	/**
	 * Used by first and last business day constraints to
	 * log the time of day for use in setNextRunTime()
	 */
	private String									taskTime			= "";

	/**
	 * Constraint of when the task can start execution.
	 */
	private LocalDateTime							startOnDateTime		= null;

	/**
	 * Constraint of when the task must not continue to execute
	 */
	private LocalDateTime							endOnDateTime		= null;

	/**
	 * Constraint to limit the task to run after a specified time of day.
	 */
	private String									startTime			= "";

	/**
	 * Constraint to limit the task to run before a specified time of day.
	 */
	private String									endTime				= "";

	/**
	 * The boolean value that lets us know if this task has been scheduled
	 */
	private Boolean									scheduled			= false;

	/**
	 * This task can be assigned to a task scheduler or be executed on its own at runtime
	 */
	private BaseScheduler							scheduler			= null;

	/**
	 * A struct for the task that can be used to store any metadata
	 */
	private IStruct									meta				= new Struct();

	/**
	 * The collection of stats for the task: { name, created, lastRun, nextRun, totalRuns, totalFailures, totalSuccess, lastResult, neverRun,
	 * lastExecutionTime }
	 */
	private IStruct									stats;

	/**
	 * The timezone this task runs under, by default we use the timezone defined in the schedulers
	 */
	private ZoneId									timezone			= ZoneId.systemDefault();

	/**
	 * The before task lambda
	 */
	private Consumer<ScheduledTask>					beforeTask;

	/**
	 * The after task lambda
	 */
	private BiConsumer<ScheduledTask, Optional<?>>	afterTask;

	/**
	 * The task success lambda
	 */
	private BiConsumer<ScheduledTask, Optional<?>>	onTaskSuccess;

	/**
	 * The task failure lambda
	 */
	private BiConsumer<ScheduledTask, Exception>	onTaskFailure;

	/**
	 * Logger
	 */
	private static final Logger						logger				= LoggerFactory.getLogger( ScheduledTask.class );

	/**
	 * BoxLang Timer utility
	 */
	private final Timer								timer				= new Timer();

	/**
	 * Interceptor Service
	 */
	private InterceptorService						interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new ScheduledTask with a name and a named group and it's accompanying executor
	 *
	 * @param name      The name of the task
	 * @param group     The group of the task
	 * @param executor  The executor we are bound to
	 * @param scheduler The scheduler we are bound to
	 */
	public ScheduledTask( String name, String group, ExecutorRecord executor, BaseScheduler scheduler ) {
		// Seed it
		this.name		= name;
		this.group		= group;
		this.executor	= executor;
		this.scheduler	= scheduler;

		// Init the stats
		this.stats		= Struct.of(
		    // Save name just in case
		    "name", name,
		    // Save group just in case
		    "group", group,
		    // When task got created
		    "created", LocalDateTime.now(),
		    // The last execution run timestamp
		    "lastRun", null,
		    // The next execution run timestamp
		    "nextRun", null,
		    // Total runs
		    "totalRuns", new AtomicInteger( 0 ),
		    // Total failures
		    "totalFailures", new AtomicInteger( 0 ),
		    // Total successful task executions
		    "totalSuccess", new AtomicInteger( 0 ),
		    // How long the last execution took
		    "lastExecutionTime", new AtomicLong( 0 ),
		    // The latest result if any
		    "lastResult", Optional.empty(),
		    // If the task has never ran or not
		    "neverRun", true
		// Server Host
		// "inetHost", util.discoverInetHost(),
		// Server IP
		// "localIp", variables.util.getServerIp()
		);

		debugLog( "constructor", Struct.of( "name", name, "group", group ) );
	}

	/**
	 * Creates a new ScheduledTask with a name and the default "empty" group
	 *
	 * @param name     The name of the task
	 * @param executor The executor we are bound to
	 */
	public ScheduledTask( String name, ExecutorRecord executor ) {
		this( name, "", executor, null );
	}

	/**
	 * Creates a new ScheduledTask with a name and the default "empty" group and a scheduler
	 *
	 * @param name      The name of the task
	 * @param scheduler The scheduler we are bound to
	 */
	public ScheduledTask( String name, BaseScheduler scheduler ) {
		this( name, "", null, scheduler );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Startup and Runnable Proxy
	 * --------------------------------------------------------------------------
	 * These are the methods that are used to start the task and execute it.
	 */

	/**
	 * This method verifies if the running task is constrained to run on specific valid constraints:
	 *
	 * - when
	 * - dayOfTheMonth
	 * - dayOfTheWeek
	 * - firstBusinessDay
	 * - lastBusinessDay
	 * - weekdays
	 * - weekends
	 * - startOnDateTime
	 * - endOnDateTime
	 * - startTime and/or endTime
	 *
	 * This method is called by the `run()` method at runtime to determine if the task can be ran at that point in time
	 */

	/**
	 * This is the runnable proxy method that executes your code by the executors
	 */
	@Override
	public void run() {
		run( false );
	}

	/**
	 * This is a convenience method to execute the task manually if needed by forcing execution.
	 *
	 * @param force Whether to force execution or not. It ignores if the task is disabled or constrained
	 */
	public void run( Boolean force ) {
		debugLog( String.format( "run( force: %b )", force ) );
		String timerLabel = "task-" + System.currentTimeMillis();
		timer.start( timerLabel );

		// If disabled or paused
		if ( !force && isDisabled() ) {
			setNextRunTime();
			return;
		}

		// Check for constraints of execution
		if ( !force && isConstrained() ) {
			setNextRunTime();
			return;
		}

		// Mark the task as it will run now for the first time
		this.stats.put( "neverRun", false );
		try {
			// Before Interceptors : From global to local
			this.interceptorService.announce(
			    BoxEvent.SCHEDULER_BEFORE_ANY_TASK,
			    Struct.of( "task", this )
			);
			if ( hasScheduler() ) {
				getScheduler().beforeAnyTask( this );
			}
			if ( beforeTask != null ) {
				beforeTask.accept( this );
			}

			// Execution by type
			switch ( task ) {
				case DynamicObject castedTask -> {
					this.stats.put( "lastResult", Optional.ofNullable( castedTask.invoke( method ) ) );
				}
				case Callable<?> castedTask -> {
					this.stats.put( "lastResult", Optional.ofNullable( castedTask.call() ) );
				}
				case Runnable castedTask -> {
					castedTask.run();
					this.stats.put( "lastResult", Optional.empty() );
				}
				case Function castedTask -> {
					castedTask.invoke(
					    Function.generateFunctionContext(
					        castedTask, // the function
					        BoxRuntime.getInstance().getRuntimeContext(), // we use the runtime context
					        castedTask.getName(), // the function name
					        new Object[] {}, // no args
					        null // No class, lambda/closure
					    )
					);
				}
				default -> {
					throw new IllegalArgumentException( "Task is not a DynamicObject or a Callable or a Runnable" );
				}
			}

			// Get the last result
			var result = ( Optional<?> ) this.stats.get( "lastResult" );

			// After Interceptors : From local to global
			if ( afterTask != null ) {
				afterTask.accept( this, result );
			}
			if ( hasScheduler() ) {
				getScheduler().afterAnyTask( this, result );
			}
			this.interceptorService.announce(
			    BoxEvent.SCHEDULER_AFTER_ANY_TASK,
			    Struct.of( "task", this, "result", result )
			);

			// Store successes and call success interceptor : From global to local
			( ( AtomicInteger ) this.stats.get( "totalSuccess" ) ).incrementAndGet();
			if ( onTaskSuccess != null ) {
				onTaskSuccess.accept( this, result );
			}
			if ( hasScheduler() ) {
				getScheduler().onAnyTaskSuccess( this, result );
			}
			this.interceptorService.announce(
			    BoxEvent.SCHEDULER_ON_ANY_TASK_SUCCESS,
			    Struct.of( "task", this, "result", result )
			);

		} catch ( Exception e ) {
			// store failures
			( ( AtomicInteger ) this.stats.get( "totalFailures" ) ).incrementAndGet();
			logger.error( "Error running task ({}) failed: {}", name, e.getMessage() );
			logger.error( "Stacktrace for ({}) : {}", name, e.getStackTrace() );

			// Try to execute the error handlers. Try try try just in case.
			try {
				// Life Cycle onTaskFailure call : From global to local
				if ( onTaskFailure != null ) {
					onTaskFailure.accept( this, e );
				}
				// If we have a scheduler attached, called the schedulers life-cycle
				if ( hasScheduler() ) {
					getScheduler().onAnyTaskError( this, e );
				}
				this.interceptorService.announce(
				    BoxEvent.SCHEDULER_ON_ANY_TASK_ERROR,
				    Struct.of( "task", this, "exception", e )
				);

				// After Tasks Interceptor with the exception as the last result : From global to local
				if ( afterTask != null ) {
					afterTask.accept( this, Optional.of( e ) );
				}
				if ( hasScheduler() ) {
					getScheduler().afterAnyTask( this, Optional.of( e ) );
				}
				this.interceptorService.announce(
				    BoxEvent.SCHEDULER_AFTER_ANY_TASK,
				    Struct.of( "task", this, "result", Optional.of( e ) )
				);
			} catch ( Exception afterException ) {
				// Log it, so it doesn't go to ether and executor doesn't die.
				logger.error(
				    "Error running task ({}) after/error handlers : {}",
				    name,
				    afterException.getMessage()
				);
				logger.error(
				    "Stacktrace for task ({}) after/error handlers : {}",
				    name,
				    afterException.getStackTrace()
				);
			}
		} finally {
			// Store finalization stats
			this.stats.put( "lastRun", getNow() );
			( ( AtomicLong ) this.stats.get( "lastExecutionTime" ) ).set( timer.stopAndGetMillis( timerLabel ) );
			( ( AtomicInteger ) this.stats.get( "totalRuns" ) ).incrementAndGet();
			// Call internal cleanups event
			cleanupTaskRun();
			// set next run time based on timeUnit and period
			setNextRunTime();
		}
	}

	/**
	 * Get the last result of the task
	 *
	 * @return The last result of the task as an Optional
	 */
	public Optional<?> getLastResult() {
		return ( Optional<?> ) this.stats.get( "lastResult" );
	}

	/**
	 * This method registers the task into the executor and sends it for execution and scheduling.
	 * This will not register the task for execution if the disabled flag or the constraints allow it.
	 *
	 * @return A ScheduledFuture from where you can monitor the task, an empty ScheduledFuture if the task was not registered
	 */
	public ScheduledFuture<?> start() {
		// If we have overlaps and the spaced delay is 0 then grab it from the period
		if ( this.noOverlaps && this.spacedDelay == 0 ) {
			this.spacedDelay = this.period;
		}

		// If we have a delay and a delayTimeUnit, then we need to compare to our
		// current timeUnit and convert to support the delay
		// ( only if our time unit is seconds , if not we disable the delay )
		// for the previous issue where the delay and/or setting would replace
		// the time setting of the task based on the order presented
		if ( this.initialDelay > 0 &&
		    this.initialDelayTimeUnit != null &&
		    !this.initialDelayTimeUnit.equals( this.timeUnit ) ) {
			if ( this.timeUnit != TimeUnit.SECONDS ) {
				this.initialDelay = 0;
				// reset the initial nextRunTime
				this.stats.put( "nextRun", null );
			} else {
				this.initialDelay = DateTimeHelper.timeUnitToSeconds( this.initialDelay, this.initialDelayTimeUnit );
			}
		}

		// Log it
		debugLog(
		    "start",
		    Struct.of(
		        "initialDelay", this.initialDelay,
		        "delayTimeUnit", this.initialDelayTimeUnit,
		        "period", this.period,
		        "spacedDelay", this.spacedDelay,
		        "timeUnit", this.timeUnit,
		        "type", this.spacedDelay > 0 ? "scheduleWithFixedDelay" : this.period > 0 ? "scheduleAtFixedRate" : "runOnce"
		    )
		);

		try {
			// Startup a spaced frequency task: no overlaps
			if ( this.spacedDelay > 0 ) {
				return getExecutor().scheduledExecutor().scheduleWithFixedDelay(
				    this,
				    this.initialDelay,
				    this.spacedDelay,
				    this.timeUnit
				);
			}

			// Startup a task with a frequency period
			if ( period > 0 ) {
				return getExecutor().scheduledExecutor().scheduleAtFixedRate(
				    this,
				    this.initialDelay,
				    this.period,
				    this.timeUnit
				);
			}

			// Start off a one-off task
			return getExecutor().scheduledExecutor().schedule(
			    this,
			    this.initialDelay,
			    this.timeUnit
			);
		} finally {
			this.scheduled = true;
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life - Cycle Methods
	 * --------------------------------------------------------------------------
	 * You can register lambda callbacks to execute before and after the task is executed
	 * and also register a lambda to execute when the task is successful or fails.
	 */

	/**
	 * Store the closure to execute before the task is executed
	 *
	 * @param target The closure to execute
	 */
	public ScheduledTask before( Consumer<ScheduledTask> target ) {
		debugLog( "before" );
		beforeTask = target;
		return this;
	}

	/**
	 * Store the closure to execute after the task is executed
	 *
	 * @param target The closure to execute
	 */
	public ScheduledTask after( BiConsumer<ScheduledTask, Optional<?>> target ) {
		debugLog( "after" );
		afterTask = target;
		return this;
	}

	/**
	 * Store the closure to execute after the task is executed successfully
	 *
	 * @param target The closure to execute
	 */
	public ScheduledTask onSuccess( BiConsumer<ScheduledTask, Optional<?>> target ) {
		debugLog( "onSuccess" );
		onTaskSuccess = target;
		return this;
	}

	/**
	 * Store the closure to execute after the task is executed successfully
	 *
	 * @param target The closure to execute
	 */
	public ScheduledTask onFailure( BiConsumer<ScheduledTask, Exception> target ) {
		debugLog( "onFailure" );
		onTaskFailure = target;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Task Registration Methods
	 * --------------------------------------------------------------------------
	 * A task can be represented either by many approved types:
	 * - DynamicObject
	 * - Java Lambda (Callable, Runnable)
	 * - BoxLang Function
	 * - BoxLang Object + method
	 * Here is where you can register the task to be executed.
	 */

	/**
	 * This method is used to register the callable DynamicObject on this scheduled task.
	 *
	 * @param task The DynamicObject to register with a run() method
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( DynamicObject task ) {
		return call( task, "run" );
	}

	/**
	 * This method is used to register the callable DynamicObject/Callable lambda on this scheduled task.
	 *
	 * @param task   The DynamicObject/Functional Lambda to register
	 * @param method The method to execute in the DynamicObject/Functional Lambda, by default it is run()
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( DynamicObject task, String method ) {
		return call( task, method );
	}

	/**
	 * This method is used to register the callable Callable Lambda on this scheduled task.
	 *
	 * @param task The Callable Lambda to register
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( Callable<?> task ) {
		return call( task, null );
	}

	/**
	 * This method is used to register the Runnable Lambda on this scheduled task.
	 *
	 * @param task The {@link Runnable} Lambda to register
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( Runnable task ) {
		return call( task, null );
	}

	/**
	 * This method is used to register any executable as a scheduled task.
	 *
	 * @param task The object to register as an executable task
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( Object task ) {
		return call( task, null );
	}

	/**
	 * This method is used to register any object that is either:
	 * - DynamicObject
	 * - IReferenceable
	 * - Callable
	 * - Runnable
	 * as a scheduled task.
	 *
	 * @param task   The object to register as an executable task
	 * @param method The method to execute in the object, by default it is run()
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( Object task, String method ) {
		debugLog( "call" );

		// If the task is an IReferenceable then wrap them in a DynamicObject
		if ( task instanceof IReferenceable castedTask ) {
			task = DynamicObject.of( castedTask );
		}

		// Store them up!
		setTask( task );
		setMethod( method == null ? "run" : method );

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Restrictions and Constraints
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method verifies if the running task is constrained to run on specific valid constraints:
	 *
	 * - when
	 * - dayOfTheMonth
	 * - dayOfTheWeek
	 * - firstBusinessDay
	 * - lastBusinessDay
	 * - weekdays
	 * - weekends
	 * - startOnDateTime
	 * - endOnDateTime
	 * - startTime and/or endTime
	 *
	 * This method is called by the `run()` method at runtime to determine if the task can be ran at that point in time
	 */
	public boolean isConstrained() {
		debugLog( "isConstrained" );

		var now = getNow();

		// When lambda that dictates if the task can be scheduled/ran: true => yes, false => no
		if ( whenPredicate != null && whenPredicate.test( this ) == false ) {
			return true;
		}

		// Do we have a day of the month constraint? and the same as the running date/time? Else skip it
		// If the day assigned is greater than the days in the month, then we let it thru
		// as the user intended to run it at the end of the month
		if ( dayOfTheMonth > 0 &&
		    now.getDayOfMonth() != dayOfTheMonth &&
		    dayOfTheMonth <= DateTimeHelper.daysInMonth( now ) ) {
			return true;
		}

		// Do we have day of the week?
		if ( dayOfTheWeek > 0 &&
		    now.getDayOfWeek().getValue() != dayOfTheWeek ) {
			return true;
		}

		// Do we have a first business day constraint
		if ( firstBusinessDay &&
		    now.getDayOfMonth() != DateTimeHelper.getFirstBusinessDayOfTheMonth( getTimezone() ).getDayOfMonth() ) {
			return true;
		}

		// Do we have a last business day constraint
		if ( lastBusinessDay &&
		    now.getDayOfMonth() != DateTimeHelper.getLastBusinessDayOfTheMonth( getTimezone() ).getDayOfMonth() ) {
			return true;
		}

		// Do we have weekdays?
		if ( weekdays &&
		    now.getDayOfWeek().getValue() > 5 ) {
			return true;
		}

		// Do we have weekends?
		if ( weekends &&
		    now.getDayOfWeek().getValue() <= 5 ) {
			return true;
		}

		// Do we have a start on constraint
		if ( startOnDateTime != null &&
		    now.isBefore( startOnDateTime ) ) {
			return true;
		}

		// Do we have an end on constraint
		if ( endOnDateTime != null &&
		    now.isAfter( endOnDateTime ) ) {
			return true;
		}

		// Do we have we have a start time and / or end time constraint
		if ( startTime.length() > 0 || endTime.length() > 0 ) {
			LocalDateTime	targetStartTime	= DateTimeHelper.parse( now + "T" + ( startTime.length() > 0 ? startTime : "00:00:00" ) );
			LocalDateTime	targetEndTime	= DateTimeHelper.parse( now + "T" + ( endTime.length() > 0 ? endTime : "23:59:59" ) );

			if ( now.isBefore( targetStartTime ) || now.isAfter( targetEndTime ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Register a when lambda that will be executed to verify if the task can run.
	 * If the lambda returns true we execute it, else we don't.
	 *
	 * @param target The lambda to execute
	 */
	public ScheduledTask when( Predicate<ScheduledTask> target ) {
		debugLog( "when" );
		this.whenPredicate = target;
		return this;
	}

	/**
	 * Set when this task should start execution on. By default it starts automatically.
	 *
	 * @param date The date when this task should start execution on => yyyy-mm-dd format is preferred.
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask startOn( String date, String time ) {
		debugLog( "startOn", Struct.of( "date", date, "time", time ) );

		this.startOnDateTime = DateTimeHelper.parse( date + "T" + time );

		return this;
	}

	/**
	 * Set when this task should start execution on. By default it starts automatically.
	 *
	 * @param date The date when this task should start execution on => yyyy-mm-dd format is preferred.
	 */
	public ScheduledTask startOn( String date ) {
		return startOn( date, "00:00" );
	}

	/**
	 * Sets a daily start time restriction for this task.
	 *
	 * @param time The specific time using 24 hour format => HH:mm
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask startOnTime( String time ) throws InvalidAttributeValueException {
		debugLog( "startOnTime" );
		// Validate time format
		this.startTime = DateTimeHelper.validateTime( time );
		return this;
	}

	/**
	 * Set when this task should stop execution on. By default it never ends.
	 *
	 * @param date The date when this task should stop execution on => yyyy-mm-dd format is preferred.
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask endOn( String date, String time ) {
		debugLog( "endOn", Struct.of( "date", date, "time", time ) );

		this.endOnDateTime = DateTimeHelper.parse( date + "T" + time );

		return this;
	}

	/**
	 * Set when this task should stop execution on. By default it never ends.
	 *
	 * @param date The date when this task should stop execution on => yyyy-mm-dd format is preferred.
	 */
	public ScheduledTask endOn( String date ) {
		return endOn( date, "00:00" );
	}

	/**
	 * Sets a daily end time restriction for this task.
	 *
	 * @param time The specific time using 24 hour format => HH:mm
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask endOnTime( String time ) throws InvalidAttributeValueException {
		debugLog( "endOnTime" );
		// Validate time format
		this.endTime = DateTimeHelper.validateTime( time );
		return this;
	}

	/**
	 * Sets a daily time range restriction where this task can run on.
	 *
	 * @param startTime The specific time using 24 hour format => HH:mm
	 * @param endTime   The specific time using 24 hour format => HH:mm
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask between( String startTime, String endTime ) throws InvalidAttributeValueException {
		debugLog( "between" );
		startOnTime( startTime );
		endOnTime( endTime );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Frequency Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set an initial delay in the running of the task that will be registered with this schedule
	 *
	 * @param delay      The delay that will be used before executing the task
	 * @param timeUnit   The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The
	 *                   default is milliseconds
	 * @param overwrites Boolean to overwrite delay and delayTimeUnit even if value is already set, this is helpful if the delay is set later in the
	 *                   chain when creating the task - defaults to false
	 */
	public ScheduledTask delay(
	    long delay,
	    TimeUnit timeUnit,
	    Boolean overwrites ) {

		debugLog(
		    "delay",
		    Struct.of(
		        "delay", delay,
		        "timeUnit", timeUnit,
		        "overwrites", overwrites
		    )
		);

		if ( overwrites || this.initialDelay == 0 ) {
			this.initialDelay			= delay;
			this.initialDelayTimeUnit	= timeUnit;
		}

		if ( this.initialDelay > 0 ) {
			setNextRunTime();
		}

		return this;
	}

	/**
	 * Set an initial delay in the running of the task that will be registered with this schedule in milliseconds
	 *
	 * @param delay    The delay that will be used before executing the task
	 * @param timeunit The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask delay( long delay, TimeUnit timeunit ) {
		return delay( delay, timeunit, false );
	}

	/**
	 * Set an initial delay in the running of the task that will be registered with this schedule in milliseconds
	 *
	 * @param delay The delay that will be used before executing the task
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask delay( long delay ) {
		return delay( delay, TimeUnit.MILLISECONDS, false );
	}

	/**
	 * Run the task every custom spaced delay of execution, meaning no overlaps
	 *
	 * @param spacedDelay The delay that will be used before executing the task with no overlaps
	 * @param timeUnit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds.
	 *                    The default is milliseconds
	 */
	public ScheduledTask spacedDelay( long spacedDelay, TimeUnit timeUnit ) {
		debugLog(
		    "spacedDelay",
		    Struct.of( "spacedDelay", spacedDelay, "timeUnit", timeUnit )
		);
		this.spacedDelay	= spacedDelay;
		this.timeUnit		= timeUnit;
		return this;
	}

	public ScheduledTask spacedDelay( long spacedDelay ) {
		return spacedDelay( spacedDelay, TimeUnit.MILLISECONDS );
	}

	/**
	 * Calling this method prevents task frequencies to overlap. By default all tasks are executed with an
	 * interval but could potentially overlap if they take longer to execute than the period.
	 */
	public ScheduledTask withNoOverlaps() {
		debugLog( "withNoOverlaps" );
		this.noOverlaps = true;
		return this;
	}

	/**
	 * BoxLang proxy
	 *
	 * @param period   The period of execution
	 * @param timeunit The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask every( Double period, String timeUnit ) {
		timeUnit = StringUtil.pluralize( timeUnit ).toUpperCase();
		return every( period.longValue(), TimeUnit.valueOf( timeUnit ) );
	}

	/**
	 * Run the task every custom period of execution
	 *
	 * @param period   The period of execution
	 * @param timeUnit The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 *                 is milliseconds
	 */
	public ScheduledTask every( long period, TimeUnit timeUnit ) {
		debugLog( "every", Struct.of( "period", period, "timeUnit", timeUnit ) );

		this.period		= period;
		this.timeUnit	= timeUnit;

		setNextRunTime();

		return this;
	}

	/**
	 * Run the task every custom period of execution
	 * The default time unit is milliseconds
	 *
	 * @param period The period of execution
	 */
	public ScheduledTask every( long period ) {
		return every( period, TimeUnit.MILLISECONDS );
	}

	/**
	 * Run the task every second from the time it get's scheduled
	 */
	public ScheduledTask everySecond() {
		debugLog( "everySecond" );
		return every( 1, TimeUnit.SECONDS );
	}

	/**
	 * Run the task every minute from the time it get's scheduled
	 */
	public ScheduledTask everyMinute() {
		debugLog( "everyMinute" );
		return every( 1, TimeUnit.MINUTES );
	}

	/**
	 * Run the task every hour from the time it get's scheduled
	 */
	public ScheduledTask everyHour() {
		debugLog( "everyHour" );
		return every( 1, TimeUnit.HOURS );
	}

	/**
	 * Set the period to be hourly at a specific minute mark and 00 seconds
	 *
	 * @param minutes The minutes past the hour mark
	 */
	public ScheduledTask everyHourAt( int minutes ) {
		debugLog( "everyHourAt", Struct.of( "minutes", minutes ) );

		// Get times
		var	now		= getNow();
		var	nextRun	= now.withMinute( minutes ).withSecond( 0 );

		// If we passed it, then move to the next hour
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusHours( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun, TimeUnit.HOURS, 1 );

		return this;
	}

	/**
	 * Daily task that runs at midnight
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask everyDay() throws InvalidAttributeValueException {
		debugLog( "everyDay" );
		return everyDayAt( "00:00" );
	}

	/**
	 * Run the task daily with a specific time in 24 hour format: HH:mm
	 * We will always add 0 seconds for you.
	 *
	 * @param time The specific time using 24 hour format => HH:mm
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyDayAt( String time ) throws InvalidAttributeValueException {
		debugLog( "everyDayAt", Struct.of( "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next day
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusDays( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		return this;
	}

	/**
	 * Run the every Sunday at midnight
	 *
	 * @return The ScheduledTask instance
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyWeek() throws InvalidAttributeValueException {
		return everyWeekOn( 7 );
	}

	/**
	 * Run the task weekly on the given day of the week at midnight
	 *
	 * @param dayOfWeek The day of the week from 1 (Monday) -> 7 (Sunday)
	 *
	 * @return The ScheduledTask instance
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyWeekOn( int dayOfWeek ) throws InvalidAttributeValueException {
		return everyWeekOn( dayOfWeek, "00:00" );
	}

	/**
	 * Run the task weekly on the given day of the week and time
	 *
	 * @param dayOfWeek The day of the week from 1 (Monday) -> 7 (Sunday)
	 * @param time      The specific time using 24 hour format => HH:mm, defaults to midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask everyWeekOn( int dayOfWeek, String time ) throws InvalidAttributeValueException {
		debugLog( "everyWeekOn", Struct.of( "dayOfWeek", dayOfWeek, "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    // Given day
		    .with( ChronoField.DAY_OF_WEEK, dayOfWeek )
		    // Given time
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next week
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusWeeks( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun, TimeUnit.DAYS, 7 );

		// set constraints
		this.dayOfTheWeek = dayOfWeek;

		return this;
	}

	/**
	 * Run the task on the first day of every month at midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask everyMonth() throws InvalidAttributeValueException {
		debugLog( "everyMonth" );
		return everyMonthOn( 1 );
	}

	/**
	 * Run the task every month on a specific day at midnight
	 *
	 * @param day Which day of the month
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask everyMonthOn( int day ) throws InvalidAttributeValueException {
		return everyMonthOn( day, "00:00" );
	}

	/**
	 * Run the task every month on a specific day and time
	 *
	 * @param day  Which day of the month
	 * @param time The specific time using 24 hour format => HH:mm, defaults to midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask everyMonthOn( int day, String time ) throws InvalidAttributeValueException {
		debugLog( "everyMonthOn", Struct.of( "day", day, "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    // First day of the month
		    .with( ChronoField.DAY_OF_MONTH, day )
		    // Specific Time
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next month
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusMonths( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		// Set constraints
		this.dayOfTheMonth = day;

		return this;
	}

	/**
	 * Run the task on the first Monday of every month at midnight
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onFirstBusinessDayOfTheMonth() throws InvalidAttributeValueException {
		return onFirstBusinessDayOfTheMonth( "00:00" );
	}

	/**
	 * Run the task on the first Monday of every month
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onFirstBusinessDayOfTheMonth( String time ) throws InvalidAttributeValueException {
		debugLog( "onFirstBusinessDayOfTheMonth", Struct.of( "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= DateTimeHelper.getFirstBusinessDayOfTheMonth( time, false, getTimezone() );

		// If we passed it, then move to the first business day of next month
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = DateTimeHelper.getFirstBusinessDayOfTheMonth( time, true, getTimezone() );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		// Set constraints
		this.firstBusinessDay	= true;
		this.taskTime			= time;

		return this;
	}

	/**
	 * Run the task on the last business day of the month at midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onLastBusinessDayOfTheMonth() throws InvalidAttributeValueException {
		return onLastBusinessDayOfTheMonth( "00:00" );
	}

	/**
	 * Run the task on the last business day of the month
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onLastBusinessDayOfTheMonth( String time ) throws InvalidAttributeValueException {
		debugLog( "onLastBusinessDayOfTheMonth", Struct.of( "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= DateTimeHelper.getLastBusinessDayOfTheMonth( time, false, getTimezone() );

		// If we passed it, then move to the last business day of next month
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = DateTimeHelper.getLastBusinessDayOfTheMonth( time, true, getTimezone() );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		// Set constraints
		this.lastBusinessDay	= true;
		this.taskTime			= time;

		return this;
	}

	/**
	 * Run the task on the first day of the year at midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyYear() throws InvalidAttributeValueException {
		debugLog( "everyYear" );
		return everyYearOn( 1, 1, "00:00" );
	}

	/**
	 * Set the period to be weekly at a specific time at a specific day of the week
	 *
	 * @param month The month in numeric format 1-12
	 * @param day   Which day of the month
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyYearOn( int month, int day ) throws InvalidAttributeValueException {
		return everyYearOn( month, day, "00:00" );
	}

	/**
	 * Set the period to be weekly at a specific time at a specific day of the week
	 *
	 * @param month The month in numeric format 1-12
	 * @param day   Which day of the month
	 * @param time  The specific time using 24 hour format => HH:mm, defaults to 00:00
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask everyYearOn( int month, int day, String time ) throws InvalidAttributeValueException {
		debugLog( "everyYearOn", Struct.of( "month", month, "day", day, "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    // Specific month
		    .with( ChronoField.MONTH_OF_YEAR, month )
		    // Specific day of the month
		    .with( ChronoField.DAY_OF_MONTH, day )
		    // Midnight
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next year
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusYears( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun, TimeUnit.DAYS, 365 );
		// Set constraints
		this.annually = true;

		return this;
	}

	/**
	 * Run the task on saturday and sundays at midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onWeekends() throws InvalidAttributeValueException {
		return onWeekends( "00:00" );
	}

	/**
	 * Run the task on saturday and sundays
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onWeekends( String time ) throws InvalidAttributeValueException {
		debugLog( "onWeekends", Struct.of( "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next day
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusDays( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		// Set constraints
		this.weekends	= true;
		this.weekdays	= false;

		return this;
	}

	/**
	 * Set the period to be from Monday - Friday at midnight
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onWeekdays() throws InvalidAttributeValueException {
		return onWeekdays( "00:00" );
	}

	/**
	 * Set the period to be from Monday - Friday
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 *
	 * @throws InvalidAttributeValueException When the time format is invalid
	 */
	public ScheduledTask onWeekdays( String time ) throws InvalidAttributeValueException {
		debugLog( "onWeekdays", Struct.of( "time", time ) );

		// Validate time format
		time = DateTimeHelper.validateTime( time );

		// Get times
		var	now		= getNow();
		var	nextRun	= now
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// If we passed it, then move to the next day
		if ( now.compareTo( nextRun ) > 0 ) {
			nextRun = nextRun.plusDays( 1 );
		}

		// Set the initial delay, period, and time unit
		setInitialDelayPeriodAndTimeUnit( now, nextRun );

		// Set constraints
		this.weekdays	= true;
		this.weekends	= false;

		return this;
	}

	/**
	 * Set the period to be on Mondays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onMondays() throws InvalidAttributeValueException {
		return everyWeekOn( 1 );
	}

	/**
	 * Set the period to be on Mondays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onMondays( String time ) throws InvalidAttributeValueException {
		debugLog( "onMondays", Struct.of( "time", time ) );
		return everyWeekOn( 1, time );
	}

	/**
	 * Set the period to be on Tuesdays
	 */
	public ScheduledTask onTuesdays() throws InvalidAttributeValueException {
		return everyWeekOn( 2 );
	}

	/**
	 * Set the period to be on Tuesdays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onTuesdays( String time ) throws InvalidAttributeValueException {
		debugLog( "onTuesdays", Struct.of( "time", time ) );
		return everyWeekOn( 2, time );
	}

	/**
	 * Set the period to be on Wednesdays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onWednesdays() throws InvalidAttributeValueException {
		return everyWeekOn( 3 );
	}

	/**
	 * Set the period to be on Wednesdays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onWednesdays( String time ) throws InvalidAttributeValueException {
		debugLog( "onWednesdays", Struct.of( "time", time ) );
		return everyWeekOn( 3, time );
	}

	/**
	 * Set the period to be on Thursdays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onThursdays() throws InvalidAttributeValueException {
		return everyWeekOn( 4 );
	}

	/**
	 * Set the period to be on Thursdays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onThursdays( String time ) throws InvalidAttributeValueException {
		debugLog( "onThursdays", Struct.of( "time", time ) );
		return everyWeekOn( 4, time );
	}

	/**
	 * Set the period to be on Fridays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onFridays() throws InvalidAttributeValueException {
		return everyWeekOn( 5 );
	}

	/**
	 * Set the period to be on Fridays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onFridays( String time ) throws InvalidAttributeValueException {
		debugLog( "onFridays", Struct.of( "time", time ) );
		return everyWeekOn( 5, time );
	}

	/**
	 * Set the period to be on Saturdays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onSaturdays() throws InvalidAttributeValueException {
		return everyWeekOn( 6 );
	}

	/**
	 * Set the period to be on Saturdays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onSaturdays( String time ) throws InvalidAttributeValueException {
		debugLog( "onSaturdays", Struct.of( "time", time ) );
		return everyWeekOn( 6, time );
	}

	/**
	 * Set the period to be on Sundays
	 *
	 * @throws InvalidAttributeValueException
	 */
	public ScheduledTask onSundays() throws InvalidAttributeValueException {
		return everyWeekOn( 7 );
	}

	/**
	 * Set the period to be on Sundays
	 *
	 * @throws InvalidAttributeValueException
	 *
	 * @param time The specific time using 24 hour format => HH:mm, defaults to 00:00
	 */
	public ScheduledTask onSundays( String time ) throws InvalidAttributeValueException {
		debugLog( "onSundays", Struct.of( "time", time ) );
		return everyWeekOn( 7, time );
	}

	/**
	 * --------------------------------------------------------------------------
	 * TimeUnit Methods
	 * --------------------------------------------------------------------------
	 * These methods are used to set the time unit of the interval or periods.
	 * Last one called wins!
	 */

	/**
	 * Set the time unit in days
	 */
	public ScheduledTask inDays() {
		debugLog( "inDays" );
		this.timeUnit = TimeUnit.DAYS;
		return this;
	}

	/**
	 * Set the time unit in hours
	 */
	public ScheduledTask inHours() {
		debugLog( "inHours" );
		this.timeUnit = TimeUnit.HOURS;
		return this;
	}

	/**
	 * Set the time unit in microseconds
	 */
	public ScheduledTask inMicroseconds() {
		debugLog( "inMicroseconds" );
		this.timeUnit = TimeUnit.MICROSECONDS;
		return this;
	}

	/**
	 * Set the time unit in milliseconds
	 */
	public ScheduledTask inMilliseconds() {
		debugLog( "inMilliseconds" );
		this.timeUnit = TimeUnit.MILLISECONDS;
		return this;
	}

	/**
	 * Set the time unit in minutes
	 */
	public ScheduledTask inMinutes() {
		debugLog( "inMinutes" );
		this.timeUnit = TimeUnit.MINUTES;
		return this;
	}

	/**
	 * Set the time unit in nanoseconds
	 */
	public ScheduledTask inNanoseconds() {
		debugLog( "inNanoseconds" );
		this.timeUnit = TimeUnit.NANOSECONDS;
		return this;
	}

	/**
	 * Set the time unit in seconds
	 */
	public ScheduledTask inSeconds() {
		debugLog( "inSeconds" );
		this.timeUnit = TimeUnit.SECONDS;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method is called to set the next run time of the task
	 * so it can process start/end times to calculate the next run on the passed initial date-time
	 *
	 * @param now The initial next run date time
	 *
	 * @return The calculated next run date time accouting start/end times
	 */
	private LocalDateTime startEndTimeNextRun( LocalDateTime now ) {
		LocalDateTime	sTime	= this.startTime.length() > 0
		    ? now
		        .withHour( Integer.valueOf( this.startTime.split( ":" )[ 0 ] ) )
		        .withMinute( Integer.valueOf( this.startTime.split( ":" )[ 1 ] ) )
		        .withSecond( 0 )
		    : now
		        .withHour( 0 )
		        .withMinute( 0 )
		        .withSecond( 0 );
		LocalDateTime	eTime	= this.endTime.length() > 0
		    ? now
		        .withHour( Integer.valueOf( this.endTime.split( ":" )[ 0 ] ) )
		        .withMinute( Integer.valueOf( this.endTime.split( ":" )[ 1 ] ) )
		        .withSecond( 0 )
		    : now
		        .withHour( 23 )
		        .withMinute( 59 )
		        .withSecond( 59 );

		if ( now.compareTo( sTime ) < 0 ) {
			return sTime;
		} else if ( now.compareTo( eTime ) > 0 ) {
			return sTime.plusDays( 1 );
		} else {
			return now;
		}
	}

	/**
	 * This method is called ALWAYS after a task runs, wether in failure or success but used internally for
	 * any type of cleanups
	 */
	public void cleanupTaskRun() {
		debugLog( "cleanupTaskRun" );
		// no cleanups for now
	}

	/**
	 * This method calculates the next run date according to initial and recurrent scenarios.
	 */
	private synchronized void setNextRunTime() {
		// Prep vars
		LocalDateTime	initialNextRun	= null;
		LocalDateTime	nextRun			= getNow();

		if ( this.stats.get( "nextRun" ) instanceof LocalDateTime castedNextRun ) {
			initialNextRun = castedNextRun;
		}

		// Debug
		debugLog(
		    "setNextRunTime-start",
		    Struct.of(
		        "delay", this.initialDelay,
		        "delayTimeUnit", this.initialDelayTimeUnit,
		        "period", this.period,
		        "spacedDelay", this.spacedDelay,
		        "timeUnit", this.timeUnit,
		        "startTime", this.startTime,
		        "endTime", this.endTime,
		        "firstBusinessDay", this.firstBusinessDay,
		        "lastBusinessDay", this.lastBusinessDay,
		        "taskTime", this.taskTime,
		        "initialNextRun", initialNextRun == null ? "null" : initialNextRun.toString()
		    )
		);

		// First and Last business days are special cases
		// It overrides the incoming next run date time to be the first or last business day of the month
		if ( this.firstBusinessDay ) {
			nextRun = DateTimeHelper.getFirstBusinessDayOfTheMonth( this.taskTime, true, getTimezone() );
		} else if ( this.lastBusinessDay ) {
			nextRun = DateTimeHelper.getLastBusinessDayOfTheMonth( this.taskTime, true, getTimezone() );
		}
		// If we have start/end times, we need to modify this
		else if ( this.startTime.length() > 0 || this.endTime.length() > 0 ) {
			nextRun = startEndTimeNextRun( nextRun );
		}

		// Add in the delay ONLY if it's the initial next run
		if ( this.initialDelay > 0 && initialNextRun == null ) {
			nextRun = DateTimeHelper.dateTimeAdd( nextRun, this.initialDelay, this.initialDelayTimeUnit );
		}
		// If we have run this task already and have already a task run
		// then calculate it via the period and timeUnit
		else if ( initialNextRun != null ) {
			// Calculate the amount of time to add to the next run time based on period or the spaced delay
			// Which is what the task operates on, either one.
			var amount = this.spacedDelay != 0 ? this.spacedDelay : this.period;
			// if overlaps are allowed task is immediately scheduled
			if ( this.spacedDelay == 0 && ( ( AtomicLong ) this.stats.get( "lastExecutionTime" ) ).get() / 1000 > this.period ) {
				amount = 0;
			}
			nextRun = DateTimeHelper.dateTimeAdd( nextRun, amount, this.timeUnit );
		}

		// Store it
		debugLog( "setNextRunTime-end", Struct.of( "nextRun", nextRun ) );
		this.stats.put( "nextRun", nextRun );
	}

	/**
	 * This method is called to set the initial delay period which
	 * calls setInitialNextRunTime, then sets the timeUnit to seconds
	 * and the period based on a value to convert to seconds.
	 *
	 * @param now              The current time to use for calculating the initial delay
	 * @param nextRun          The first run time to use for calculating the initial delay
	 * @param periodValue      The value to use when calculating the period to seconds
	 * @param periodMultiplier The multiplier to use when calculating the period to seconds
	 */
	private void setInitialDelayPeriodAndTimeUnit(
	    LocalDateTime now,
	    LocalDateTime nextRun,
	    TimeUnit periodValue,
	    int periodMultiplier ) {
		debugLog(
		    "setInitialDelayPeriodAndTimeUnit",
		    Struct.of(
		        "now", now,
		        "nextRun", nextRun,
		        "periodValue", periodValue,
		        "periodMultiplier", periodMultiplier
		    )
		);

		// Get the duration time for the next run and delay accordingly
		this.delay(
		    Duration
		        .between( now, nextRun )
		        .getSeconds(),
		    TimeUnit.SECONDS,
		    true
		);

		// Set the period to be every hour in seconds
		this.period		= periodValue.toSeconds( periodMultiplier );
		this.timeUnit	= TimeUnit.SECONDS;
	}

	/**
	 * Shortcut using the default of DAYS and 1 for the multiplier
	 *
	 * @param now     The current time to use for calculating the initial delay
	 * @param nextRun The first run time to use for calculating the initial delay
	 */
	private void setInitialDelayPeriodAndTimeUnit( LocalDateTime now, LocalDateTime nextRun ) {
		setInitialDelayPeriodAndTimeUnit( now, nextRun, TimeUnit.DAYS, 1 );
	}

	/**
	 * Get the date time now in the timezone of the task
	 *
	 * @return The date time now in the timezone of the task
	 */
	public LocalDateTime getNow() {
		return DateTimeHelper.now( getTimezone() );
	}

	/**
	 * Call this method periodically in a long-running task to check to see if the thread has been interrupted.
	 *
	 * @throws InterruptedException - When the thread has been interrupted
	 */
	public void checkInterrupted() throws InterruptedException {
		debugLog( "checkInterrupted" );
		if ( Thread.currentThread().isInterrupted() ) {
			Thread.interrupted();
			throw new InterruptedException( "Task Thread has been interrupted" );
		}
	}

	/**
	 * Debug output to the console, this is only used for hard-core debugging
	 *
	 * @param caller The name of the method calling this
	 * @param args   The arguments to output
	 */
	private void debugLog( String caller, IStruct args ) {
		if ( logger.isTraceEnabled() ) {
			List<String> message = List.of(
			    "+ ScheduledTask",
			    "group: ", getGroup(),
			    "name: ", getName(),
			    "caller: ", caller,
			    "args", args == null ? "<no args>" : args.toString()
			);

			logger.trace( message.toString() );
		}
	}

	/**
	 * Debug output to the console, this is only used for hard-core debugging
	 *
	 * @param caller The name of the method calling this
	 */
	private void debugLog( String caller ) {
		debugLog( caller, null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Glorious Getters and Setters!
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the human name of this task.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the human name of this task.
	 */
	public ScheduledTask setName( String name ) {
		this.name = name;
		return this;
	}

	/**
	 * Set the human group name of this task.
	 *
	 * @param group the group to set
	 */
	public ScheduledTask setGroup( String group ) {
		this.group = group;
		return this;
	}

	/**
	 * Get the human group name of this task.
	 *
	 * @return the group
	 */
	public String getGroup() {
		return this.group;
	}

	/**
	 * Get the task dynamic object that will be executed by the task.
	 * Must implement the run() method or use the {@code method} property.
	 */
	public Object getTask() {
		return this.task;
	}

	/**
	 * Set the task dynamic object that will be executed by the task.
	 * Must implement the run() method or use the {@code method} property.
	 *
	 * @param task the task to set
	 *
	 * @throws IllegalArgumentException When the task is not a DynamicObject or a Callable Lambda
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask setTask( Object task ) {
		if ( task instanceof DynamicObject || task instanceof Callable || task instanceof Runnable ) {
			this.task = task;
		} else {
			throw new IllegalArgumentException( "Task must be a DynamicObject or a Callable or Runnable Lambda" );
		}
		return this;
	}

	/**
	 * Get the method to execute in the DynamicObject, by default it is run().
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Set the method to execute in the DynamicObject, by default it is run().
	 */
	public ScheduledTask setMethod( String method ) {
		this.method = method;
		return this;
	}

	/**
	 * Get the task stats
	 *
	 * @return the stats
	 */
	public IStruct getStats() {
		return this.stats;
	}

	/**
	 * Get the delay or time to wait before we execute the task in the scheduler.
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

	/**
	 * Set the delay or time to wait before we execute the task in the scheduler.
	 */
	public ScheduledTask setInitialDelay( long delay ) {
		this.initialDelay = delay;
		return this;
	}

	/**
	 * Get the time unit string used when there is a delay requested for the task.
	 */
	public TimeUnit getInitialDelayTimeUnit() {
		return this.initialDelayTimeUnit;
	}

	/**
	 * Set the time unit string used when there is a delay requested for the task.
	 */
	public ScheduledTask setInitialDelayTimeUnit( TimeUnit delayTimeUnit ) {
		this.initialDelayTimeUnit = delayTimeUnit;
		return this;
	}

	/**
	 * Get a fixed time period of execution of the tasks in this schedule.
	 * It does not wait for tasks to finish; tasks are fired exactly at that time period.
	 */
	public long getPeriod() {
		return this.period;
	}

	/**
	 * Set a fixed time period of execution of the tasks in this schedule.
	 * It does not wait for tasks to finish; tasks are fired exactly at that time period.
	 */
	public ScheduledTask setPeriod( long period ) {
		this.period = period;
		return this;
	}

	/**
	 * Get the delay to use when using scheduleWithFixedDelay(), so tasks execute after this delay once completed.
	 */
	public long getSpacedDelay() {
		return this.spacedDelay;
	}

	/**
	 * Set the delay to use when using scheduleWithFixedDelay(), so tasks execute after this delay once completed.
	 */
	public ScheduledTask setSpacedDelay( long spacedDelay ) {
		this.spacedDelay = spacedDelay;
		return this;
	}

	/**
	 * Get the time unit used to schedule the task.
	 */
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * Set the time unit used to schedule the task.
	 */
	public ScheduledTask setTimeUnit( TimeUnit timeUnit ) {
		this.timeUnit = timeUnit;
		return this;
	}

	/**
	 * Get a handy boolean that is set when the task is annually scheduled.
	 */
	public Boolean isAnnually() {
		return this.annually;
	}

	/**
	 * Set a handy boolean that is set when the task is annually scheduled.
	 */
	public ScheduledTask setAnnually( Boolean annually ) {
		this.annually = annually;
		return this;
	}

	/**
	 * Is the task disabled
	 */
	public Boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Is the task enabled
	 */
	public Boolean isEnabled() {
		return !isDisabled();
	}

	/**
	 * Set a handy boolean that disables the scheduling of this task.
	 *
	 * @param disabled The disabled to set
	 */
	public ScheduledTask setDisabled( Boolean disabled ) {
		this.disabled = disabled;
		return this;
	}

	/**
	 * Disable this task, the human way
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask disable() {
		debugLog( "disable" );
		return setDisabled( true );
	}

	/**
	 * Enable this task, the human way
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask enable() {
		debugLog( "enable" );
		return setDisabled( false );
	}

	/**
	 * Get a lambda that determines if this task will be sent for scheduling or not.
	 * It is both evaluated at scheduling and at runtime.
	 */
	public Predicate<ScheduledTask> getWhenPredicate() {
		return this.whenPredicate;
	}

	/**
	 * Set a lambda that determines if this task will be sent for scheduling or not.
	 * It is both evaluated at scheduling and at runtime.
	 */
	public ScheduledTask setWhenPredicate( Predicate<ScheduledTask> whenPredicate ) {
		this.whenPredicate = whenPredicate;
		return this;
	}

	/**
	 * Get the constraint of what day of the month we need to run on: 1-31.
	 */
	public int getDayOfTheMonth() {
		return this.dayOfTheMonth;
	}

	/**
	 * Set the constraint of what day of the month we need to run on: 1-31.
	 */
	public ScheduledTask setDayOfTheMonth( int dayOfTheMonth ) {
		this.dayOfTheMonth = dayOfTheMonth;
		return this;
	}

	/**
	 * Get the constraint of what day of the week this runs on: 1-7.
	 */
	public int getDayOfTheWeek() {
		return this.dayOfTheWeek;
	}

	/**
	 * Set the constraint of what day of the week this runs on: 1-7.
	 */
	public ScheduledTask setDayOfTheWeek( int dayOfTheWeek ) {
		this.dayOfTheWeek = dayOfTheWeek;
		return this;
	}

	/**
	 * Get the constraint to run only on weekends.
	 */
	public Boolean getWeekends() {
		return this.weekends;
	}

	/**
	 * Set the constraint to run only on weekends.
	 */
	public ScheduledTask setWeekends( Boolean weekends ) {
		this.weekends = weekends;
		return this;
	}

	/**
	 * Get the constraint to run only on weekdays.
	 */
	public Boolean getWeekdays() {
		return this.weekdays;
	}

	/**
	 * Set the constraint to run only on weekdays.
	 */
	public ScheduledTask setWeekdays( Boolean weekdays ) {
		this.weekdays = weekdays;
		return this;
	}

	/**
	 * @return the firstBusinessDay
	 */
	public Boolean getFirstBusinessDay() {
		return this.firstBusinessDay;
	}

	/**
	 * Set the constraint to run only on the first business day of the month.
	 */
	public ScheduledTask setFirstBusinessDay( Boolean firstBusinessDay ) {
		this.firstBusinessDay = firstBusinessDay;
		return this;
	}

	/**
	 * Get the constraint to run only on the last business day of the month.
	 */
	public Boolean getLastBusinessDay() {
		return this.lastBusinessDay;
	}

	/**
	 * Set the constraint to run only on the last business day of the month.
	 */
	public ScheduledTask setLastBusinessDay( Boolean lastBusinessDay ) {
		this.lastBusinessDay = lastBusinessDay;
		return this;
	}

	/**
	 * Get a boolean flag that turns off task stacking.
	 * By default, tasks execute in an interval frequency which can cause tasks to
	 * stack if they take longer than their periods (fire immediately after completion).
	 * With this flag turned on, the schedulers don't kick off the intervals until the tasks finish executing, meaning no stacking.
	 */
	public Boolean isNoOverlaps() {
		return this.noOverlaps;
	}

	/**
	 * Get the NoOverlaps flag
	 *
	 * @return the noOverlaps
	 */
	public Boolean getNoOverlaps() {
		return this.noOverlaps;
	}

	/**
	 * Set a boolean flag that turns off task stacking.
	 * By default, tasks execute in an interval frequency which can cause tasks to
	 * stack if they take longer than their periods (fire immediately after completion).
	 * With this flag turned on, the schedulers don't kick off the intervals until the tasks finish executing, meaning no stacking.
	 */
	public ScheduledTask setNoOverlaps( Boolean noOverlaps ) {
		this.noOverlaps = noOverlaps;
		return this;
	}

	/**
	 * Get the time of day for use in setNextRunTime().
	 */
	public String getTaskTime() {
		return this.taskTime;
	}

	/**
	 * Set the time of day for use in setNextRunTime().
	 */
	public ScheduledTask setTaskTime( String taskTime ) {
		this.taskTime = taskTime;
		return this;
	}

	/**
	 * Get the constraint of when the task can start execution.
	 */
	public LocalDateTime getStartOnDateTime() {
		return this.startOnDateTime;
	}

	/**
	 * Set the constraint of when the task can start execution.
	 */
	public ScheduledTask setStartOnDateTime( LocalDateTime startOnDateTime ) {
		this.startOnDateTime = startOnDateTime;
		return this;
	}

	/**
	 * Get the constraint of when the task must not continue to execute.
	 */
	public LocalDateTime getEndOnDateTime() {
		return this.endOnDateTime;
	}

	/**
	 * Set the constraint of when the task must not continue to execute.
	 */
	public ScheduledTask setEndOnDateTime( LocalDateTime endOnDateTime ) {
		this.endOnDateTime = endOnDateTime;
		return this;
	}

	/**
	 * Get the constraint to limit the task to run after a specified time of day.
	 */
	public String getStartTime() {
		return this.startTime;
	}

	/**
	 * Set the constraint to limit the task to run after a specified time of day.
	 */
	public ScheduledTask setStartTime( String startTime ) {
		this.startTime = startTime;
		return this;
	}

	/**
	 * Get the constraint to limit the task to run before a specified time of day.
	 */
	public String getEndTime() {
		return this.endTime;
	}

	/**
	 * Set the constraint to limit the task to run before a specified time of day.
	 */
	public ScheduledTask setEndTime( String endTime ) {
		this.endTime = endTime;
		return this;
	}

	/**
	 * Get a boolean value that lets us know if this task has been scheduled.
	 */
	public Boolean isScheduled() {
		return this.scheduled;
	}

	/**
	 * Set a boolean value that lets us know if this task has been scheduled.
	 */
	public ScheduledTask setScheduled( Boolean scheduled ) {
		this.scheduled = scheduled;
		return this;
	}

	/**
	 * Get the Meta struct for the task that can be used to store any metadata.
	 *
	 * @return the meta
	 */
	public IStruct getMeta() {
		return this.meta;
	}

	/**
	 * Set the Meta struct for the task that can be used to store any metadata.
	 *
	 * @param meta the meta to set
	 */
	public ScheduledTask setMeta( IStruct meta ) {
		this.meta = meta;
		return this;
	}

	/**
	 * Set a meta key/value pair in the meta struct.
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask setMetaKey( String key, Object value ) {
		this.meta.put( Key.of( key ), value );
		return this;
	}

	/**
	 * Delete a meta key/value pair in the meta struct.
	 *
	 * @param key The key to delete
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask deleteMetaKey( String key ) {
		this.meta.remove( Key.of( key ) );
		return this;
	}

	/**
	 * Get the timezone this task runs under. By default, we use the timezone defined in the schedulers.
	 */
	public ZoneId getTimezone() {
		return this.timezone;
	}

	/**
	 * Set the timezone this task runs under. By default, we use the timezone defined in the schedulers.
	 *
	 * @param timezone The timezone to set as a ZoneId
	 */
	public ScheduledTask setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
		return this;
	}

	/**
	 * Set the timezone this task runs under using a timezone string representation
	 *
	 * @param timezone The timezone to set as a string
	 */
	public ScheduledTask setTimezone( String timezone ) {
		return setTimezone( ZoneId.of( timezone ) );
	}

	/**
	 * Get the bound scheduler for this task or null if none.
	 *
	 * @return the scheduler or null if not bound
	 */
	public BaseScheduler getScheduler() {
		return this.scheduler;
	}

	/**
	 * Check if this task has a scheduler bound to it.
	 *
	 * @return true if it has a scheduler, false otherwise
	 */
	public Boolean hasScheduler() {
		return this.scheduler != null;
	}

	/**
	 * Set the bound scheduler for this task.
	 *
	 * @param scheduler the scheduler to set
	 */
	public ScheduledTask setScheduler( BaseScheduler scheduler ) {
		this.scheduler = scheduler;
		return this;
	}

	/**
	 * Get the before task lambda.
	 */
	public Consumer<ScheduledTask> getBeforeTask() {
		return this.beforeTask;
	}

	/**
	 * Set the before task lambda.
	 */
	public ScheduledTask setBeforeTask( Consumer<ScheduledTask> beforeTask ) {
		this.beforeTask = beforeTask;
		return this;
	}

	/**
	 * Get the after task lambda.
	 */
	public BiConsumer<ScheduledTask, Optional<?>> getAfterTask() {
		return this.afterTask;
	}

	/**
	 * Set the after task lambda.
	 */
	public ScheduledTask setAfterTask( BiConsumer<ScheduledTask, Optional<?>> afterTask ) {
		this.afterTask = afterTask;
		return this;
	}

	/**
	 * Get the task success lambda.
	 */
	public BiConsumer<ScheduledTask, Optional<?>> getOnTaskSuccess() {
		return this.onTaskSuccess;
	}

	/**
	 * Set the task success lambda.
	 */
	public ScheduledTask setOnTaskSuccess( BiConsumer<ScheduledTask, Optional<?>> onTaskSuccess ) {
		this.onTaskSuccess = onTaskSuccess;
		return this;
	}

	/**
	 * Get the task failure lambda.
	 */
	public BiConsumer<ScheduledTask, Exception> getOnTaskFailure() {
		return this.onTaskFailure;
	}

	/**
	 * Set the task failure lambda.
	 */
	public ScheduledTask setOnTaskFailure( BiConsumer<ScheduledTask, Exception> onTaskFailure ) {
		this.onTaskFailure = onTaskFailure;
		return this;
	}

	/**
	 * Lazy getter for the executor
	 *
	 * @return The executor
	 */
	private ExecutorRecord getExecutor() {
		if ( this.executor == null ) {
			this.executor = this.scheduler.getExecutor();
		}
		return this.executor;
	}

}
