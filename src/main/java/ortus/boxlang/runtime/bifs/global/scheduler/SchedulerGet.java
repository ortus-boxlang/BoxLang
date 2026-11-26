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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Get information about a scheduled task" )
public class SchedulerGet extends BIF {

	/**
	 * Constructor
	 */
	public SchedulerGet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._name, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Get a registered scheduler by name.
	 * <p>
	 * Looks up the scheduler in the SchedulerService and returns the scheduler object itself, so you can interact with it directly (register tasks,
	 * get stats, etc.).
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the scheduler to get. Required and must not be empty.
	 *
	 * @throws IllegalArgumentException if no scheduler is registered with the given name.
	 *
	 * @return The scheduler object registered under the given name.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key schedulerName = Key.of( arguments.get( Key._name ) );
		if ( this.schedulerService.hasScheduler( schedulerName ) ) {
			return this.schedulerService.getScheduler( schedulerName );
		}

		throw new IllegalArgumentException( "Scheduler not found: " + schedulerName );
	}

}
