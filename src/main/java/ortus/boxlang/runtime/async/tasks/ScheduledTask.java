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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.management.InvalidAttributeValueException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.async.time.DateTimeHelper;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * The ScheduledTask class is a {@link Runnable} that is used by the schedulers to execute tasks
 * in a more human and fluent approach.
 *
 * A task can be represented either by a DynamicObject or a Java Lambda.
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
	private String								name;

	/**
	 * The human group name of this task
	 */
	private String								group;

	/**
	 * The task dynamic object that will be executed by the task
	 * Must implement the run() method or use the {@code method} property
	 */
	private DynamicObject						task;

	/**
	 * The method to execute in the DynamicObject, by default it is run()
	 */
	private String								method				= "run";

	/**
	 * The delay or time to wait before we execute the task in the scheduler
	 */
	private long								delay;

	/**
	 * The time unit string used when there is a delay requested for the task
	 */
	private TimeUnit							delayTimeUnit;

	/**
	 * A fixed time period of execution of the tasks in this schedule. It does not wait for tasks to finish,
	 * tasks are fired exactly at that time period.
	 */
	private long								period				= 0;

	/**
	 * The delay to use when using scheduleWithFixedDelay(), so tasks execute after this delay once completed
	 */
	private long								spacedDelay			= 0;

	/**
	 * The time unit used to schedule the task
	 */
	private TimeUnit							timeUnit			= TimeUnit.MILLISECONDS;

	/**
	 * A handy boolean that is set when the task is annually scheduled
	 */
	private Boolean								annually			= false;

	/**
	 * A handy boolean that disables the scheduling of this task
	 */
	private Boolean								disabled			= false;

	/**
	 * A lambda, that if registered, determines if this task will be sent for scheduling or not.
	 * It is both evaluated at scheduling and at runtime.
	 */
	private Predicate<Boolean>					whenClosure;

	/**
	 * Constraint of what day of the month we need to run on: 1-31
	 */
	private int									dayOfTheMonth		= 0;

	/**
	 * Constraint of what day of the week this runs on: 1-7
	 */
	private int									dayOfTheWeek		= 0;

	/**
	 * Constraint to run only on weekends
	 */
	private Boolean								weekends			= false;

	/**
	 * Constraint to run only on weekdays
	 */
	private Boolean								weekdays			= false;

	/**
	 * Constraint to run only on the first business day of the month
	 */
	private Boolean								firstBusinessDay	= false;

	/**
	 * Constraint to run only on the last business day of the month
	 */
	private Boolean								lastBusinessDay		= false;

	/**
	 * By default tasks execute in an interval frequency which can cause tasks to
	 * stack if they take longer than their periods ( fire immediately after completion ).
	 * With this boolean flag turned on, the schedulers don't kick off the
	 * intervals until the tasks finish executing. Meaning no stacking.
	 */
	private Boolean								noOverlaps			= false;

	/**
	 * Used by first and last business day constraints to
	 * log the time of day for use in setNextRunTime()
	 */
	private String								taskTime			= "";

	/**
	 * Constraint of when the task can start execution.
	 */
	private LocalDateTime						startOnDateTime		= null;

	/**
	 * Constraint of when the task must not continue to execute
	 */
	private LocalDateTime						endOnDateTime		= null;

	/**
	 * Constraint to limit the task to run after a specified time of day.
	 */
	private String								startTime			= "";

	/**
	 * Constraint to limit the task to run before a specified time of day.
	 */
	private String								endTime				= "";

	/**
	 * The boolean value that lets us know if this task has been scheduled
	 */
	private Boolean								scheduled			= false;

	/**
	 * This task can be assigned to a task scheduler or be executed on its own at runtime
	 */
	private Scheduler							scheduler			= null;

	/**
	 * A struct for the task that can be used to store any metadata
	 */
	private Struct								meta				= new Struct();

	/**
	 * The collection of stats for the task: { name, created, lastRun, nextRun, totalRuns, totalFailures, totalSuccess, lastResult, neverRun,
	 * lastExecutionTime }
	 */
	private Struct								stats				= Struct.of(
	    // Save name just in case
	    "name", "",
	    // When task got created
	    "created", LocalDateTime.now(),
	    // The last execution run timestamp
	    "lastRun", "",
	    // The next execution run timestamp
	    "nextRun", "",
	    // Total runs
	    "totalRuns", 0,
	    // Total faiulres
	    "totalFailures", 0,
	    // Total successful task executions
	    "totalSuccess", 0,
	    // How long the last execution took
	    "lastExecutionTime", 0,
	    // The latest result if any
	    "lastResult", "",
	    // If the task has never ran or not
	    "neverRun", true
	// Server Host
	// "inetHost", variables.util.discoverInetHost(),
	// Server IP
	// "localIp", variables.util.getServerIp()
	);

	/**
	 * The timezone this task runs under, by default we use the timezone defined in the schedulers
	 */
	private ZoneId								timezone			= ZoneId.systemDefault();

	/**
	 * The before task lambda
	 */
	private Consumer<ScheduledTask>				beforeTask;

	/**
	 * The after task lambda
	 */
	private Consumer<ScheduledTask>				afterTask;

	/**
	 * The task success lambda
	 */
	private BiConsumer<ScheduledTask, Object>	onTaskSuccess;

	/**
	 * The task failure lambda
	 */
	private BiConsumer<ScheduledTask, Object>	onTaskFailure;

	/**
	 * Logger
	 */
	private static final Logger					logger				= LoggerFactory.getLogger( ScheduledTask.class );

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	public ScheduledTask() {
		debugLog( "init" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runnable Interface
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void run() {
		throw new UnsupportedOperationException( "Unimplemented method 'run'" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Task Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method is used to register the callable DynamicObject on this scheduled task.
	 *
	 * @param task The DynamicObject to register
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( DynamicObject task ) {
		return call( task, "run" );
	}

	/**
	 * This method is used to register the callable DynamicObject on this scheduled task.
	 *
	 * @param task   The DynamicObject to register
	 * @param method The method to execute in the DynamicObject, by default it is run()
	 *
	 * @return The ScheduledTask instance
	 */
	public ScheduledTask call( DynamicObject task, String method ) {
		debugLog( "call" );
		setTask( task );
		setMethod( method == null ? "run" : method );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Restrictions
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register a when lambda that will be executed before the task is set to be registered.
	 * If the lambda returns true we schedule, else we disable it.
	 */
	public ScheduledTask when( Predicate<Boolean> target ) {
		debugLog( "when" );
		this.whenClosure = target;
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
		this.startTime = validateTime( time );
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
		this.endTime = validateTime( time );
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
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Validates an incoming string to adhere to HH:mm while allowing a user to simply enter an hour value
	 *
	 * @param time The time to validate
	 *
	 * @return The validated time
	 *
	 * @throws InvalidAttributeValueException
	 */
	private String validateTime( String time ) throws InvalidAttributeValueException {
		if ( !time.matches( "^([0-1][0-9]|[2][0-3]):[0-5][0-9]$" ) ) {

			// Do we have only hours?
			if ( time.contains( ":" ) ) {
				throw new InvalidAttributeValueException( "Invalid time representation (" + time + "). Time is represented in 24 hour minute format => HH:mm" );
			}

			return validateTime( time + ":00" );
		}

		return time;
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
	private void debugLog( String caller, Struct args ) {
		if ( logger.isDebugEnabled() ) {
			List<String> message = List.of(
			    "+ ScheduledTask",
			    "group: ", getGroup(),
			    "name: ", getName(),
			    "caller: ", caller
			);
			if ( args != null ) {
				message.add( "args: " + args.toString() );
			}
			logger.debug( message.toString() );
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
	 * Dumb Getters and Setters!
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
		return group;
	}

	/**
	 * Get the task dynamic object that will be executed by the task.
	 * Must implement the run() method or use the {@code method} property.
	 */
	public DynamicObject getTask() {
		return task;
	}

	/**
	 * Set the task dynamic object that will be executed by the task.
	 * Must implement the run() method or use the {@code method} property.
	 */
	public ScheduledTask setTask( DynamicObject task ) {
		this.task = task;
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
	 * Get the delay or time to wait before we execute the task in the scheduler.
	 */
	public long getDelay() {
		return delay;
	}

	/**
	 * Set the delay or time to wait before we execute the task in the scheduler.
	 */
	public ScheduledTask setDelay( long delay ) {
		this.delay = delay;
		return this;
	}

	/**
	 * Get the time unit string used when there is a delay requested for the task.
	 */
	public TimeUnit getDelayTimeUnit() {
		return delayTimeUnit;
	}

	/**
	 * Set the time unit string used when there is a delay requested for the task.
	 */
	public ScheduledTask setDelayTimeUnit( TimeUnit delayTimeUnit ) {
		this.delayTimeUnit = delayTimeUnit;
		return this;
	}

	/**
	 * Get a fixed time period of execution of the tasks in this schedule.
	 * It does not wait for tasks to finish; tasks are fired exactly at that time period.
	 */
	public long getPeriod() {
		return period;
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
		return spacedDelay;
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
		return timeUnit;
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
		return annually;
	}

	/**
	 * Set a handy boolean that is set when the task is annually scheduled.
	 */
	public ScheduledTask setAnnually( Boolean annually ) {
		this.annually = annually;
		return this;
	}

	/**
	 * Get a handy boolean that disables the scheduling of this task.
	 */
	public Boolean isDisabled() {
		return disabled;
	}

	/**
	 * Set a handy boolean that disables the scheduling of this task.
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
	public Predicate<Boolean> getWhenClosure() {
		return whenClosure;
	}

	/**
	 * Set a lambda that determines if this task will be sent for scheduling or not.
	 * It is both evaluated at scheduling and at runtime.
	 */
	public ScheduledTask setWhenClosure( Predicate<Boolean> whenClosure ) {
		this.whenClosure = whenClosure;
		return this;
	}

	/**
	 * Get the constraint of what day of the month we need to run on: 1-31.
	 */
	public int getDayOfTheMonth() {
		return dayOfTheMonth;
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
		return dayOfTheWeek;
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
	public Boolean isWeekends() {
		return weekends;
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
	public Boolean isWeekdays() {
		return weekdays;
	}

	/**
	 * Set the constraint to run only on weekdays.
	 */
	public ScheduledTask setWeekdays( Boolean weekdays ) {
		this.weekdays = weekdays;
		return this;
	}

	/**
	 * Get the constraint to run only on the first business day of the month.
	 */
	public Boolean isFirstBusinessDay() {
		return firstBusinessDay;
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
	public Boolean isLastBusinessDay() {
		return lastBusinessDay;
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
		return noOverlaps;
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
		return taskTime;
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
		return startOnDateTime;
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
		return endOnDateTime;
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
		return startTime;
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
		return endTime;
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
		return scheduled;
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
	public Struct getMeta() {
		return meta;
	}

	/**
	 * Set the Meta struct for the task that can be used to store any metadata.
	 *
	 * @param meta the meta to set
	 */
	public ScheduledTask setMeta( Struct meta ) {
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
		return timezone;
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
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Check if this task has a scheduler bound to it.
	 *
	 * @return true if it has a scheduler, false otherwise
	 */
	public Boolean hasScheduler() {
		return scheduler != null;
	}

	/**
	 * Set the bound scheduler for this task.
	 *
	 * @param scheduler the scheduler to set
	 */
	public ScheduledTask setScheduler( Scheduler scheduler ) {
		this.scheduler = scheduler;
		return this;
	}

	/**
	 * Get the before task lambda.
	 */
	public Consumer<ScheduledTask> getBeforeTask() {
		return beforeTask;
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
	public Consumer<ScheduledTask> getAfterTask() {
		return afterTask;
	}

	/**
	 * Set the after task lambda.
	 */
	public ScheduledTask setAfterTask( Consumer<ScheduledTask> afterTask ) {
		this.afterTask = afterTask;
		return this;
	}

	/**
	 * Get the task success lambda.
	 */
	public BiConsumer<ScheduledTask, Object> getOnTaskSuccess() {
		return onTaskSuccess;
	}

	/**
	 * Set the task success lambda.
	 */
	public ScheduledTask setOnTaskSuccess( BiConsumer<ScheduledTask, Object> onTaskSuccess ) {
		this.onTaskSuccess = onTaskSuccess;
		return this;
	}

	/**
	 * Get the task failure lambda.
	 */
	public BiConsumer<ScheduledTask, Object> getOnTaskFailure() {
		return onTaskFailure;
	}

	/**
	 * Set the task failure lambda.
	 */
	public ScheduledTask setOnTaskFailure( BiConsumer<ScheduledTask, Object> onTaskFailure ) {
		this.onTaskFailure = onTaskFailure;
		return this;
	}

}
