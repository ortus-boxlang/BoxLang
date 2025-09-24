/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.scheduler;

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Restart a scheduled task" )
public class SchedulerRestart extends BIF {

	/**
	 * Constructor
	 */
	public SchedulerRestart() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._name, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.BOOLEAN, Key.force, false ),
		    new Argument( false, Argument.INTEGER, Key.timeout, SchedulerService.DEFAULT_SHUTDOWN_TIMEOUT )
		};
	}

	/**
	 * Restart a scheduler by name.
	 * <p>
	 * The default will do a graceful shutdown, but if the force argument is set to true,
	 * it will force shutdown the scheduler.
	 * <p>
	 * If the timeout argument is set, it will wait for the specified amount of time in seconds
	 * before forcing the shutdown. The default is 30 seconds.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the scheduler to restart.
	 *
	 * @argument.force If true, will force restart the scheduler. Default is false.
	 *
	 * @argument.timeout The timeout in seconds to wait for the scheduler to restart gracefully. Default is 30 seconds.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		schedulerName	= Key.of( arguments.get( Key._name ) );
		boolean	force			= arguments.getAsBoolean( Key.force );
		long	timeout			= LongCaster.cast( arguments.get( Key.timeout ) );

		// Scheduler Check
		if ( !this.schedulerService.hasScheduler( schedulerName ) ) {
			throw new IllegalArgumentException(
			    "Scheduler [" + schedulerName + "] not registered. Valid schedulers are: " + this.schedulerService.getSchedulerNames()
			);
		}

		// Shutdown the scheduler
		this.schedulerService.restartScheduler( schedulerName, force, timeout );

		return null;
	}

}
