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
package ortus.boxlang.runtime.bifs.global.async;

import java.util.concurrent.TimeUnit;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF
public class ExecutorShutdown extends BIF {

	/**
	 * Constructor
	 */
	public ExecutorShutdown() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._NAME ),
		    new Argument( true, Argument.BOOLEAN, Key.force, false ),
		    new Argument( true, Argument.NUMERIC, Key.timeout, 0 )
		};
	}

	/**
	 * Shuts down an executor by name. By default
	 * the executors are shutdown gracefully. However, if you want to force the shutdown
	 * you can set the force argument to true. If you want to wait for the executor to shutdown
	 * you can set the timeout argument to the number of milliseconds to wait.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the executor to shutdown
	 *
	 * @argument.force Whether to force the shutdown, default is false
	 *
	 * @argument.timeout The number of milliseconds to wait for the executor to shutdown
	 *
	 * @throws KeyNotFoundException If the executor is not found.
	 */
	@Override
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	name	= arguments.getAsString( Key._NAME );
		Boolean	force	= arguments.getAsBoolean( Key.force );
		Long	timeout	= LongCaster.cast( arguments.get( Key.timeout ) );

		asyncService.shutdownExecutor( name, force, timeout, TimeUnit.MILLISECONDS );

		return true;
	}

}
