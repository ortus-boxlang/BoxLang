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
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayEvery extends BIF {

	/**
	 * Constructor
	 */
	public ArrayEvery() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, "function:Predicate", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Used to iterate over an array and test whether <strong>every</strong> item meets the test callback.
	 * The function will be passed 3 arguments: the value, the index, and the array.
	 * You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 * The function should return true if the item meets the test, and false otherwise.
	 * <p>
	 * <strong>Note:</strong> This operation is a short-circuit operation, meaning it will stop iterating as soon as it finds the first item that does not meet the test condition.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the filter will be executed in parallel using a ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the filter in parallel, and destroy it after the operation is complete.
	 * This allows for efficient processing of large arrays, especially when the test function is computationally expensive or the array is large.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to test against the callback.
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array. You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 *
	 * @argument.parallel Whether to run the filter in parallel. Defaults to false. If true, the filter will be run in parallel using a ForkJoinPool.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running the filter in parallel. If not passed it will use the default number of threads for the ForkJoinPool.
	 *                      If parallel is false, this argument is ignored. If a boolean is provided it will be assigned to the virtual argument instead.
	 * 
	 * @argument.virtual If true, the function will be invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object maxThreads = arguments.get( Key.maxThreads );

		if ( maxThreads instanceof Boolean castBoolean ) {
			// If maxThreads is a boolean, we assign it to virtual
			arguments.put( Key.virtual, castBoolean );
			maxThreads = null;
		}

		CastAttempt<Integer> maxThreadsAttempt = IntegerCaster.attempt( maxThreads );

		return ListUtil.every(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    maxThreadsAttempt.getOrDefault( null ),
		    arguments.getAsBoolean( Key.virtual )
		);

	}
}
