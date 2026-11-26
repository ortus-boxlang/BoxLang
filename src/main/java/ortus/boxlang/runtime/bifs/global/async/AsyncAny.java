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

import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF( description = "Wait for any of the async operations to complete" )
public class AsyncAny extends BIF {

	/**
	 * Constructor
	 */
	public AsyncAny() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.futures ),
		    new Argument( false, Argument.ANY, Key.executor )
		};
	}

	/**
	 * Accepts an array of futures/closures/lambdas and executes them all in parallel,
	 * returning a BoxFuture that will contain the result of the first future that completes.
	 * The operation is non-blocking; call <code>get()</code> on the returned future to wait for the result.
	 *
	 * <pre>
	 * result = asyncAny( [ () -> slowCall(), () -> fastCall() ] ).get()
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.futures An array of BoxFuture objects, CompletableFutures or functions to process in parallel.
	 *
	 * @argument.executor The executor to use. This can be the name of a registered executor or a BoxExecutor object. By default the common
	 *                    ForkJoinPool is used. This is used to run any functions in the array.
	 *
	 * @throws KeyNotFoundException If the executor name passed is not valid.
	 *
	 * @return A BoxFuture that will contain the result of the first future to complete.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		futures			= arguments.getAsArray( Key.futures );
		Object		executor		= arguments.get( Key.executor );

		BoxExecutor	executorRecord	= this.asyncService.getRecordOrNull( executor );

		if ( executorRecord == null ) {
			return BoxFuture.any( context, futures );
		} else {
			return BoxFuture.any( context, futures, executorRecord );
		}

	}

}
