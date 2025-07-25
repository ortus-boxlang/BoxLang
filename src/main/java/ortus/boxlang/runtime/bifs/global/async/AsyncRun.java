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

import java.util.concurrent.Executor;

import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF
@BoxBIF( alias = "RunAsync" )
public class AsyncRun extends BIF {

	/**
	 * Constructor
	 */
	public AsyncRun() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.FUNCTION, Key.callback ),
		    new Argument( false, Argument.ANY, Key.executor )
		};
	}

	/**
	 * Executes the given code asynchronously and returns to you a BoxFuture object which inherits from CompletableFuture.
	 * This way you can create fluent asynchronous code that can be chained and composed.
	 *
	 * @see https://docs.oracle.com/en%2Fjava%2Fjavase%2F22%2Fdocs%2Fapi%2F%2F/java.base/java/util/concurrent/CompletableFuture.html
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.callback The code to execute asynchronously, this can be a closure
	 *                    or lambda.
	 *
	 * @argument.executor The executor to use for the asynchronous execution. This
	 *                    can be an instance of an Executor class, or the name of a
	 *                    registered executor in the AsyncService.
	 *
	 * @throws KeyNotFoundException If the executor name passed is not valid.
	 *
	 * @return A BoxFuture object that you can use to interact with the
	 *         asynchronously executed code.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the callback
		Function	callback	= arguments.getAsFunction( Key.callback );
		// Get the executor
		Object		executor	= arguments.get( Key.executor );

		// Run the code asynchronously in the fork/join pool if no executor is provided
		if ( executor == null ) {
			return BoxFuture.ofFunction( context, callback );
		}

		// Check if the executor is a string, if so, and then check the async service
		// for the executor
		// If the executor is not found, throw an exception
		if ( executor instanceof String castedExecutor ) {
			return BoxFuture.ofFunction( context, callback, asyncService.getExecutor( castedExecutor ).executor() );
		}

		// Check if the executor is an instance of an Executor class
		if ( executor instanceof Executor castedExecutor ) {
			return BoxFuture.ofFunction( context, callback, castedExecutor );
		}

		throw new BoxRuntimeException( "Invalid executor type " + executor.getClass().getName() );
	}

}
