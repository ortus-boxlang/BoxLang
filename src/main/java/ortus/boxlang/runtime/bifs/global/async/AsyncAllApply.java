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
	 * This function can accept an array of items or a struct of items and apply a function
	 * to each of the item's in parallel. The `mapper` argument receives the appropriate item
	 * and must return a result.
	 * <p>
	 * The `errorHandler` is optional and will be called if the mapper function throws an exception.
	 * The error handler receives the exception and the item that caused it, allowing you to handle errors gracefully.
	 * <p>
	 * The result is a future that will return an array of results, or a struct of results if the input was a struct.
	 * <p>
	 * The `executor` argument is optional and allows you to specify a custom executor for the
	 * asynchronous operations. If not provided, the common fork-join pool will be used.
	 * <p>
	 * The `timeout` and `timeUnit` arguments allow you to specify a timeout for the operation.
	 * If the operation does not complete within the specified timeout, it will throw a TimeoutException.
	 * The allowed time units are: `DAYS`, `HOURS`, `MINUTES`, `SECONDS`, `MILLISECONDS`, `MICROSECONDS`, `NANOSECONDS`.
	 * <p>
	 * Example usage:
	 *
	 * <pre>
	 * // Array
	 * allApply( items, ( item ) => item.getMemento() )
	 * // Struct: The result object is a struct of `key` and `value`
	 * allApply( data, ( item ) => item.key &amp; item.value.toString() )
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.items The items to apply the function to. Can be an array or a struct.
	 *
	 * @argument.mapper The function to apply to each item. It receives the item as an argument and must return a result.
	 *
	 * @argument.errorHandler Optional function to handle errors. It receives the exception and the item that caused it.
	 *
	 * @argument.executor Optional executor to use for the asynchronous operations. If not provided, the common fork-join pool will be used.
	 *
	 * @argument.timeout Optional timeout for the operation. If the operation does not complete within this time, it will throw a TimeoutException.
	 *
	 * @argument.timeUnit Optional time unit for the timeout. Defaults to seconds. Allowed values are: `DAYS`, `HOURS`, `MINUTES`, `SECONDS`, `MILLISECONDS`, `MICROSECONDS`, `NANOSECONDS`.
	 *
	 *
	 * @throws KeyNotFoundException If the executor name passed is not valid.
	 *
	 * @return An array or struct of the results
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
