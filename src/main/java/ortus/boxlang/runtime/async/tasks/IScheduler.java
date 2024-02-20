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

import java.time.ZoneId;
import java.util.Optional;

/**
 * All BoxLang schedulers must implement this interface and
 * inherit from the {@code Scheduler} class.
 *
 * This interface provides the basic methods and life-cycle
 * callbacks that all schedulers must implement.
 *
 * This is also important so a {@link java.util.ServiceLoader} can find the
 * concrete implementations of the {@code IScheduler} interface.
 *
 * @see BaseScheduler
 */
public interface IScheduler {

	/**
	 * --------------------------------------------------------------------------
	 * Configurator
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Usually where concrete implementations add their tasks and configs
	 */
	public void configure();

	/**
	 * --------------------------------------------------------------------------
	 * Startup/Shutdown Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Startup this scheduler and all of it's scheduled tasks
	 */
	public BaseScheduler startup();

	/**
	 * Has this scheduler been started?
	 *
	 * @return true if started, false if not
	 */
	public Boolean hasStarted();

	/**
	 * Restart the scheduler by shutting it down and starting it up again
	 *
	 * @param force   If true, it forces all shutdowns this is usually true when doing reinits
	 * @param timeout The timeout in seconds to wait for the shutdown of all tasks, defaults to 30 or whatever you set using the setShutdownTimeout()
	 *
	 * @return
	 */
	public BaseScheduler restart( boolean force, long timeout );

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 *
	 * @param force   If true, it forces all shutdowns this is usually true when doing reinits
	 * @param timeout The timeout in seconds to wait for the shutdown of all tasks, defaults to 30 or whatever you set using the setShutdownTimeout()
	 *                method
	 */
	public BaseScheduler shutdown( boolean force, long timeout );

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * using the default timeout
	 *
	 * @param force If true, it forces all shutdowns this is usually true when doing reinits
	 *
	 * @return The scheduler object
	 */
	public BaseScheduler shutdown( boolean force );

	/**
	 * Shutdown this scheduler by calling the executor to shutdown and disabling all tasks
	 * We do not force and we use the default timeout
	 *
	 * @return The scheduler object
	 */
	public BaseScheduler shutdown();

	/**
	 * --------------------------------------------------------------------------
	 * Life - Cycle Callbacks
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Called before the scheduler is going to be shutdown
	 */
	public void onShutdown();

	/**
	 * Called after the scheduler has registered all schedules
	 */
	public void onStartup();

	/**
	 * Called whenever ANY task fails
	 *
	 * @param task      The task that got executed
	 * @param exception The exception object
	 */
	public void onAnyTaskError( ScheduledTask task, Exception exception );

	/**
	 * Called whenever ANY task succeeds
	 *
	 * @param task   The task that got executed
	 * @param result The result (if any) that the task produced
	 */
	public void onAnyTaskSuccess( ScheduledTask task, Optional<?> result );

	/**
	 * Called before ANY task runs
	 *
	 * @param task The task about to be executed
	 */
	public void beforeAnyTask( ScheduledTask task );

	/**
	 * Called after ANY task runs
	 *
	 * @param task   The task that got executed
	 *
	 * @param result The result (if any) that the task produced
	 */
	public void afterAnyTask( ScheduledTask task, Optional<?> result );

	/**
	 * --------------------------------------------------------------------------
	 * Required methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the scheduler name
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Set the scheduler name
	 *
	 * @param name the name
	 *
	 * @return the scheduler object
	 */
	public BaseScheduler setName( String name );

	/**
	 * Get the scheduler timezone
	 *
	 * @return the timezone
	 */
	public ZoneId getTimezone();

	/**
	 * Set the scheduler's timezone
	 *
	 * @param timezone the timezone to set
	 * 
	 * @return the scheduler object
	 */
	public BaseScheduler setTimezone( ZoneId timezone );

}
