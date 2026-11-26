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

import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Create a new executor service" )
public class ExecutorNew extends BIF {

	/**
	 * Constructor
	 */
	public ExecutorNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._NAME ),
		    new Argument( true, Argument.STRING, Key.type ),
		    new Argument( false, Argument.INTEGER, Key.maxThreads, AsyncService.DEFAULT_MAX_THREADS ),
		};
	}

	/**
	 * Creates and registers a new executor by name and type. If an executor with that name already exists, the existing executor is returned
	 * unchanged.
	 * The return value is a BoxExecutor object that can be used to interact with the executor.
	 * <p>
	 * Available types (case insensitive) are:
	 * <ul>
	 * <li><code>cached</code> - Creates a cached thread pool executor.</li>
	 * <li><code>fixed</code> - Creates a fixed thread pool executor.</li>
	 * <li><code>fork_join</code> - Creates a fork join pool executor.</li>
	 * <li><code>scheduled</code> - Creates a scheduled thread pool executor.</li>
	 * <li><code>single</code> - Creates a single thread executor.</li>
	 * <li><code>virtual</code> - Creates a virtual thread executor.</li>
	 * <li><code>work_stealing</code> - Creates a work stealing thread pool executor.</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the executor to create if not already created.
	 *
	 * @argument.type The type of executor to create. An invalid type throws an exception.
	 *
	 * @argument.maxThreads The maximum number of threads, for the types that use it. Defaults to 10.
	 *
	 * @return The new (or already registered) BoxExecutor.
	 */
	@Override
	public BoxExecutor _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	name		= arguments.getAsString( Key._NAME );
		String	type		= arguments.getAsString( Key.type ).toUpperCase();
		Integer	maxThreads	= IntegerCaster.cast( arguments.get( Key.maxThreads ) );

		return asyncService.newExecutor(
		    name,
		    AsyncService.ExecutorType.valueOf( type ),
		    maxThreads
		);
	}

}
