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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.BoxScheduler;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Start the scheduler or a specific scheduled task" )
public class SchedulerStart extends BIF {

	/**
	 * Constructor
	 */
	public SchedulerStart() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.className, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.STRING, Key._name ),
		    new Argument( false, Argument.BOOLEAN, Key.force, true )
		};
	}

	/**
	 * Create, register and start a scheduler with the given instantiation class path.
	 * <p>
	 * The className would be the same as instantiang the class via <code>new {className}</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.className The className to the scheduler class to be instantiated: Example: "models.myapp.MyScheduler"
	 *
	 * @argument.name The name of the scheduler to start, which overrides whatever is in the class.
	 *
	 * @argument.force If true, will force start the scheduler. Default is true
	 *
	 * @return The scheduler object.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	className		= arguments.getAsString( Key.className );
		String	schedulerName	= arguments.getAsString( Key._name );
		boolean	force			= arguments.getAsBoolean( Key.force );

		return startScheduler( context, className, schedulerName, force );
	}

	/**
	 * This method will create the incoming BoxLang scheduler class, register it and start it up in the scheduler service.
	 *
	 * @param context       The context in which the BIF is being invoked.
	 * @param className     The className to the scheduler class to be instantiated: Example: "models.myapp.MyScheduler"
	 * @param schedulerName The name of the scheduler to start, which overrides whatever is in the class.
	 * @param force         If true, will force start the scheduler. Default is true
	 *
	 * @return The scheduler object.
	 */
	public static IScheduler startScheduler( IBoxContext context, String className, String schedulerName, boolean force ) {
		// Create the scheduler class
		IClassRunnable target = ( IClassRunnable ) BoxRuntime.getInstance()
		    .getFunctionService()
		    .getGlobalFunction( Key.createObject )
		    .invoke( context, new Object[] { className }, false, Key.createObject );

		// Now wrap it in a scheduler proxy
		IScheduler scheduler = new BoxScheduler( target, context );

		// Configure the scheduler for the first time
		scheduler.configure();

		// Do we have a name override?
		if ( schedulerName != null && !schedulerName.isEmpty() ) {
			scheduler.setSchedulerName( schedulerName );
		}

		// Register and send for scheduling
		return BoxRuntime.getInstance()
		    .getSchedulerService()
		    .registerAndStartScheduler( scheduler, force );
	}

}
