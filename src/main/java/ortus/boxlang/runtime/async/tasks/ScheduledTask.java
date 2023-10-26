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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.impl.duration.TimeUnit;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Struct;

/**
 * The ScheduledTask class is a Runnable that is used by the schedulers to execute tasks
 * in a more human and fluent approach.
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
	private TimeUnit							timeUnit			= TimeUnit.MILLISECOND;

	/**
	 * A handy boolean that is set when the task is annually scheduled
	 */
	private Boolean								annually			= false;

	/**
	 * The boolean value is used for debugging
	 */
	private Boolean								debug				= false;

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
	private String								startOnDateTime		= "";

	/**
	 * Constraint of when the task must not continue to execute
	 */
	private String								endOnDateTime		= "";

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
	// TODO: Later
	// private scheduler;

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
		// pub
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
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Set the human group name of this task.
	 *
	 * @param group the group to set
	 */
	public void setGroup( String group ) {
		this.group = group;
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
	public void setTask( DynamicObject task ) {
		this.task = task;
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
	public void setMethod( String method ) {
		this.method = method;
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
	public void setDelay( long delay ) {
		this.delay = delay;
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
	public void setDelayTimeUnit( TimeUnit delayTimeUnit ) {
		this.delayTimeUnit = delayTimeUnit;
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
	public void setPeriod( long period ) {
		this.period = period;
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
	public void setSpacedDelay( long spacedDelay ) {
		this.spacedDelay = spacedDelay;
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
	public void setTimeUnit( TimeUnit timeUnit ) {
		this.timeUnit = timeUnit;
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
	public void setAnnually( Boolean annually ) {
		this.annually = annually;
	}

	/**
	 * Get the boolean value used for debugging.
	 */
	public Boolean isDebug() {
		return debug;
	}

	/**
	 * Set the boolean value used for debugging.
	 */
	public void setDebug( Boolean debug ) {
		this.debug = debug;
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
	public void setDisabled( Boolean disabled ) {
		this.disabled = disabled;
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
	public void setWhenClosure( Predicate<Boolean> whenClosure ) {
		this.whenClosure = whenClosure;
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
	public void setDayOfTheMonth( int dayOfTheMonth ) {
		this.dayOfTheMonth = dayOfTheMonth;
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
	public void setDayOfTheWeek( int dayOfTheWeek ) {
		this.dayOfTheWeek = dayOfTheWeek;
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
	public void setWeekends( Boolean weekends ) {
		this.weekends = weekends;
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
	public void setWeekdays( Boolean weekdays ) {
		this.weekdays = weekdays;
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
	public void setFirstBusinessDay( Boolean firstBusinessDay ) {
		this.firstBusinessDay = firstBusinessDay;
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
	public void setLastBusinessDay( Boolean lastBusinessDay ) {
		this.lastBusinessDay = lastBusinessDay;
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
	public void setNoOverlaps( Boolean noOverlaps ) {
		this.noOverlaps = noOverlaps;
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
	public void setTaskTime( String taskTime ) {
		this.taskTime = taskTime;
	}

	/**
	 * Get the constraint of when the task can start execution.
	 */
	public String getStartOnDateTime() {
		return startOnDateTime;
	}

	/**
	 * Set the constraint of when the task can start execution.
	 */
	public void setStartOnDateTime( String startOnDateTime ) {
		this.startOnDateTime = startOnDateTime;
	}

	/**
	 * Get the constraint of when the task must not continue to execute.
	 */
	public String getEndOnDateTime() {
		return endOnDateTime;
	}

	/**
	 * Set the constraint of when the task must not continue to execute.
	 */
	public void setEndOnDateTime( String endOnDateTime ) {
		this.endOnDateTime = endOnDateTime;
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
	public void setStartTime( String startTime ) {
		this.startTime = startTime;
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
	public void setEndTime( String endTime ) {
		this.endTime = endTime;
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
	public void setScheduled( Boolean scheduled ) {
		this.scheduled = scheduled;
	}

	/**
	 * Get the timezone this task runs under. By default, we use the timezone defined in the schedulers.
	 */
	public ZoneId getTimezone() {
		return timezone;
	}

	/**
	 * Set the timezone this task runs under. By default, we use the timezone defined in the schedulers.
	 */
	public void setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
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
	public void setBeforeTask( Consumer<ScheduledTask> beforeTask ) {
		this.beforeTask = beforeTask;
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
	public void setAfterTask( Consumer<ScheduledTask> afterTask ) {
		this.afterTask = afterTask;
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
	public void setOnTaskSuccess( BiConsumer<ScheduledTask, Object> onTaskSuccess ) {
		this.onTaskSuccess = onTaskSuccess;
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
	public void setOnTaskFailure( BiConsumer<ScheduledTask, Object> onTaskFailure ) {
		this.onTaskFailure = onTaskFailure;
	}

}
