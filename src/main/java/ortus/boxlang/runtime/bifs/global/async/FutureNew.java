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

import java.util.concurrent.CompletableFuture;

import ortus.boxlang.runtime.async.BoxFuture;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
public class FutureNew extends BIF {

	/**
	 * Constructor
	 */
	public FutureNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.ANY, Key.value ),
		    new Argument( false, Argument.ANY, Key.executor )
		};
	}

	/**
	 * Create a new BoxFuture object.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value If passed, the value to set on the BoxFuture object as completed or it can be a lambda/closure
	 *                 that will provide the value and it will be executed asynchronously, or it can be a native Java CompletableFuture
	 *
	 * @argument.executor The executor to use for the BoxFuture object. By default, the BoxFuture object will use the
	 *                    default executor (ForkJoinPool.commonPool()). This can be the name of a named executor or a
	 *                    custom executor object.
	 *
	 * @return The newly created BoxFuture object.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	value		= arguments.get( Key.value );
		Object	executor	= arguments.get( Key.executor );

		// If not value, return an incomplete future.
		if ( value == null ) {
			return new BoxFuture<>();
		}

		// If it's a CompletableFuture, wrap it
		if ( value instanceof CompletableFuture ) {
			return BoxFuture.ofCompletableFuture( ( CompletableFuture<?> ) value );
		}

		// If the value is NOT a function then it is a value to be set on the future.
		if ( ! ( value instanceof Function ) ) {
			return BoxFuture.ofValue( value );
		}

		// If the value is a function then it is a lambda/closure that will provide the value.
		Function supplier = ( Function ) value;
		// Is the executor a string, then it is the name of a named executor.
		if ( executor != null ) {
			if ( executor instanceof String castedExecutor ) {
				ExecutorRecord executorRecord = asyncService.getExecutor( castedExecutor );
				return BoxFuture.ofFunction( context, supplier, executorRecord.executor() );
			}

			// If the executor is an executor object then use it.
			if ( executor instanceof ExecutorRecord castedExecutor ) {
				return BoxFuture.ofFunction( context, supplier, castedExecutor.executor() );
			}
		}

		// If no executor is provided then use the default executor.
		return BoxFuture.ofFunction( context, supplier );
	}

}
