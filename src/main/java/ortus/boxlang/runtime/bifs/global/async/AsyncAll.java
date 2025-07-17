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
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF
public class AsyncAll extends BIF {

	/**
	 * Constructor
	 */
	public AsyncAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.futures ),
		    new Argument( false, Argument.ANY, Key.executor )
		};
	}

	/**
	 * This BIF accepts an infinite amount of future objects, closures or an array of future objects/closures
	 * in order to execute them in parallel. It will return back to you a future that will return back an array
	 * of results from every future that was executed. This way you can further attach processing and pipelining
	 * on the constructed array of values.
	 * <p>
	 * This means that the futures will be executed in parallel and the results will be returned in the order
	 * that they were passed in. This also means that this operation is non-blocking and will return immediately
	 * until you call get() on the future.
	 * <p>
	 * Each future can be a BoxFuture or a CompletableFuture or a BoxLang Function that will be treated as a future.
	 *
	 * <pre>
	 * results = all( [f1, f2, f3] ).get()
	 * all( [f1, f2, f3] ).then( (values) => logResults( values ) );
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.futures An array of BoxFuture objects to process in parallel
	 *
	 * @argument.executor The executor to use for the BoxFuture object. By default, the BoxFuture object will use the
	 *                    default executor (ForkJoinPool.commonPool()). This can be the name of a named executor or a
	 *                    custom executor record.
	 *
	 * @throws KeyNotFoundException If the executor name passed is not valid.
	 *
	 * @return A future that will return the results in an array
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array			futures			= arguments.getAsArray( Key.futures );
		Object			executor		= arguments.get( Key.executor );

		ExecutorRecord	executorRecord	= null;
		if ( executor != null ) {
			if ( executor instanceof String castedExecutor ) {
				executorRecord = asyncService.getExecutor( castedExecutor );
			} else if ( executor instanceof ExecutorRecord castedExecutor ) {
				executorRecord = castedExecutor;
			} else {
				throw new KeyNotFoundException( "Invalid executor type: " + executor.getClass().getName() );
			}
		}

		if ( executorRecord == null ) {
			return BoxFuture.all( context, futures );
		} else {
			return BoxFuture.all( context, futures, executorRecord );
		}
	}

}
