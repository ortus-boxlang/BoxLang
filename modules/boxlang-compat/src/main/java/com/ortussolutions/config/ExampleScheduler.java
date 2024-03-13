package com.ortussolutions.config;

import java.util.Optional;

import ortus.boxlang.runtime.async.tasks.BaseScheduler;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;

public class ExampleScheduler extends BaseScheduler {

	public ExampleScheduler() {
		super( "ExampleScheduler" );
	}

	/**
	 * Declare the tasks for this scheduler
	 */
	@Override
	public void configure() {
		task( "PeriodicalTask" )
		    .call( () -> System.out.println( "++++++ >>>>>> Hello from PeriodicalTask" ) )
		    .everySecond()
		    .onFailure( ( task, exception ) -> System.out.println( "PeriodicalTask failed: " + exception.getMessage() ) )
		    .onSuccess( ( task, result ) -> System.out.println( "PeriodicalTask succeeded: " + result ) );

		task( "OneOffTask" )
		    .call( () -> System.out.println( "++++++ >>>>>> Hello from OneOffTask" ) );

		xtask( "A Disabled Task" )
		    .call( () -> System.out.println( "Hello from A Disabled Task" ) )
		    .everySecond();
	}

	/**
	 * Called before the scheduler is going to be shutdown
	 */
	@Override
	public void onShutdown() {
		System.out.println( "[onShutdown] ==> The ExampleScheduler has been shutdown" );
	}

	/**
	 * Called after the scheduler has registered all schedules
	 */
	@Override
	public void onStartup() {
		System.out.println( "[onStartup] ==> The ExampleScheduler has been started" );
	}

	/**
	 * Called whenever ANY task fails
	 *
	 * @task The task that got executed
	 *
	 * @exception The ColdFusion exception object
	 */
	@Override
	public void onAnyTaskError( ScheduledTask task, Exception exception ) {
		System.out.println( "[onAnyTaskError] ==> " + task.getName() + " ran into an error: " + exception.getMessage() );
	}

	/**
	 * Called whenever ANY task succeeds
	 *
	 * @task The task that got executed
	 *
	 * @result The result (if any) that the task produced
	 */
	@Override
	public void onAnyTaskSuccess( ScheduledTask task, Optional<?> result ) {
		System.out.println( "[onAnyTaskSuccess] ==>  " + task.getName() + " task successful!" );
	}

	/**
	 * Called before ANY task runs
	 *
	 * @task The task about to be executed
	 */
	@Override
	public void beforeAnyTask( ScheduledTask task ) {
		System.out.println( "[beforeAnyTask] ==> Before task: " + task.getName() );
	}

	/**
	 * Called after ANY task runs
	 *
	 * @task The task that got executed
	 *
	 * @result The result (if any) that the task produced
	 */
	@Override
	public void afterAnyTask( ScheduledTask task, Optional<?> result ) {
		System.out.println( "[afterAnyTask] ==> After task: " + task.getName() );
	}

}
