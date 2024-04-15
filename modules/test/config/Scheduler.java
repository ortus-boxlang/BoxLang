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
package config;

import java.util.Optional;

import ortus.boxlang.runtime.async.tasks.BaseScheduler;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;

public class Scheduler extends BaseScheduler {

	public Scheduler() {
		super( "ModuleScheduler" );
	}

	/**
	 * Declare the tasks for this scheduler
	 */
	@Override
	public void configure() {
		task( "PeriodicalTask" )
		    .call( () -> System.out.println( "++++++ >>>>>> Hello from MyModuleTask" ) )
		    .everySecond()
		    .onFailure( ( task, exception ) -> System.out.println( "MyModuleTask failed: " + exception.getMessage() ) )
		    .onSuccess( ( task, result ) -> System.out.println( "MyModuleTask succeeded: " + result ) );

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
		System.out.println( "[onShutdown] ==> Bye bye from the Module Scheduler!" );
	}

	/**
	 * Called after the scheduler has registered all schedules
	 */
	@Override
	public void onStartup() {
		System.out.println( "[onStartup] ==> The Module Scheduler is in da house!!!!!" );
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
		System.out.println( "[onAnyTaskError] ==> " + task.getName() + " task just went kabooooooom!" );
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
		System.out.println( "[onAnyTaskSuccess] ==>  " + task.getName() + " task completed!" );
	}

	/**
	 * Called before ANY task runs
	 *
	 * @task The task about to be executed
	 */
	@Override
	public void beforeAnyTask( ScheduledTask task ) {
		System.out.println( "[beforeAnyTask] ==> I am running before the task: " + task.getName() );
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
		System.out.println( "[afterAnyTask] ==> I am running after the task: " + task.getName() );
	}

}
