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

import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF( description = "Apply a function to all elements asynchronously" )
public class AsyncAllApply extends BIF {

	/**
	 * Constructor
	 */
	public AsyncAllApply() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.items ),
		    new Argument( true, Argument.FUNCTION, Key.mapper ),
		    new Argument( false, Argument.FUNCTION, Key.errorHandler ),
		    new Argument( false, Argument.ANY, Key.executor ),
		    new Argument( false, Argument.LONG, Key.timeout, 0 ),
		    new Argument( false, Argument.ANY, Key.timeUnit, TimeUnit.SECONDS )
		};
	}

	/**
	 * Applies a function to every item of an array or struct in parallel and waits for all of the results.
	 * The <code>mapper</code> receives the item and must return a result. For an array the result is an array of results in the original order.
	 * For a struct, the mapper receives a struct with the <code>key</code> and <code>value</code> of each entry and must return a struct
	 * containing a
	 * <code>value</code> key; the result is a struct with the original keys and those values.
	 * <p>
	 * If the mapper throws an exception and an <code>errorHandler</code> was given, the handler receives the exception and its return value is
	 * used as the result for that item.
	 * Without an error handler, a struct describing the exception is used as the result for that item.
	 * <p>
	 * Unlike {@link AsyncAll} this BIF is blocking: it returns the final results directly, not a future.
	 * If the optional <code>timeout</code> (greater than 0) elapses before all items complete, an exception is thrown.
	 * The allowed time units are: <code>DAYS</code>, <code>HOURS</code>, <code>MINUTES</code>, <code>SECONDS</code>, <code>MILLISECONDS</code>,
	 * <code>MICROSECONDS</code>, <code>NANOSECONDS</code>.
	 * 
	 * <pre>
	 * squares = asyncAllApply( [ 1, 2, 3 ], ( n ) -> n * n )
	 * doubled = asyncAllApply( { a: 10, b: 20 }, ( item ) -> { value: item.value * 2 } )
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.items The items to apply the function to. Can be an array or a struct.
	 *
	 * @argument.mapper The function to apply to each item. It receives the item as an argument and must return a result (a struct with a value
	 *                  key when the items are a struct).
	 *
	 * @argument.errorHandler Optional function to handle errors thrown by the mapper. It receives the exception and its return value is used as
	 *                        the item result.
	 *
	 * @argument.executor Optional executor (name or BoxExecutor object) to run the operations on. If not provided, the common ForkJoinPool is
	 *                    used.
	 *
	 * @argument.timeout Optional maximum time to wait for all the items to complete. Defaults to 0, which waits indefinitely.
	 *
	 * @argument.timeUnit The time unit of the timeout. Defaults to seconds. Allowed values are: DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
	 *                    MICROSECONDS, NANOSECONDS.
	 *
	 * @throws KeyNotFoundException If the executor name passed is not valid.
	 *
	 * @throws BoxRuntimeException  If the items are not an array or struct, or the timeout elapses.
	 *
	 * @return An array (or struct, if the items were a struct) of the results.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object		executor		= arguments.get( Key.executor );
		BoxExecutor	executorRecord	= this.asyncService.getRecordOrNull( executor );

		return BoxFuture.allApply(
		    context,
		    arguments.get( Key.items ),
		    arguments.getAsFunction( Key.mapper ),
		    arguments.getAsFunction( Key.errorHandler ),
		    arguments.getAsLong( Key.timeout ),
		    arguments.get( Key.timeUnit ),
		    executorRecord != null ? executorRecord : this.asyncService.getCommonForkJoinPool()
		);

	}

}
