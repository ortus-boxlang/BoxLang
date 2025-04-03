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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class SchedulerStats extends BIF {

	/**
	 * Constructor
	 */
	public SchedulerStats() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key._name )
		};
	}

	/**
	 * Get the stats of all schedulers or a specific scheduler by name.
	 * <p>
	 * Each stats structure contains the following fields:
	 * <ul>
	 * <li>created</li>
	 * <li>lastExecutionTime</li>
	 * <li>lastResult</li>
	 * <li>lastRun</li>
	 * <li>name</li>
	 * <li>neverRun</li>
	 * <li>nextRun</li>
	 * <li>totalFailures</li>
	 * <li>totalRuns</li>
	 * <li>totalSuccess</li>
	 * <ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the scheduler to get stats on or if not passed for all schedulers
	 *
	 * @return A single stats or all stats data
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String schedulerName = arguments.getAsString( Key._name );
		if ( schedulerName == null ) {
			return this.schedulerService.getSchedulerStats();
		} else {
			return this.schedulerService.getSchedulerStats( Key.of( schedulerName ) );
		}
	}

}
