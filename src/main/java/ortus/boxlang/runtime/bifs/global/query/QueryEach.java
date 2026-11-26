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
package ortus.boxlang.runtime.bifs.global.query;

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
import ortus.boxlang.runtime.types.util.QueryUtil;

@BoxBIF( description = "Execute a callback function for each row in a query" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryEach extends BIF {

	/*
	 * queryEach(query, function(row [, currentRow] [, query] ){} [, parallel] [, maxThreads]) → returns void
	 */

	/**
	 * Constructor
	 */
	public QueryEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.QUERY, Key.query ),
		    new Argument( true, "function:Consumer", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.ordered, false ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Iterates over query rows and passes each row per iteration to a callback function.
	 * This function is used to perform an action for each row in the query.
	 * It does not return a value, but rather allows you to perform side effects such as printing or modifying data.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the filter will be executed in parallel using a ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the filter in parallel, and destroy it after the operation is complete.
	 * Please note that this may not be the most efficient way to iterate, as it will create a new ForkJoinPool for each invocation of the BIF. You may want to consider using a shared ForkJoinPool for better performance.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to iterate over.
	 *
	 * @argument.callback The function to invoke for each row. It receives the row as a struct, the one-based row number, and the
	 *                    query. You can alternatively pass a Java Consumer, which receives only the row.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If omitted, the default ForkJoinPool
	 *                      parallelism is used. Ignored if parallel is false. If a boolean is passed it is used as the virtual argument instead.
	 *
	 * @argument.ordered Accepted for consistency with other each functions but currently ignored: when running in parallel the rows
	 *                   are always processed in order.
	 *
	 * @argument.virtual Whether to use virtual threads when running in parallel. Defaults to false. Ignored if parallel is false.
	 *
	 * @return Nothing (null). The callback is executed for its side effects.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object maxThreads = arguments.get( Key.maxThreads );
		if ( maxThreads instanceof Boolean castBoolean ) {
			// If maxThreads is a boolean, we assign it to virtual
			arguments.put( Key.virtual, castBoolean );
			maxThreads = null;
		}

		CastAttempt<Integer> maxThreadsAttempt = IntegerCaster.attempt( maxThreads );
		QueryUtil.each(
		    arguments.getAsQuery( Key.query ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    maxThreadsAttempt.getOrDefault( 0 ),
		    true,
		    arguments.getAsBoolean( Key.virtual )
		);

		return null;
	}
}
