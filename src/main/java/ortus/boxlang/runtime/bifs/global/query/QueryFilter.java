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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.QueryUtil;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryFilter extends BIF {

	/**
	 * Constructor
	 */
	public QueryFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.QUERY, Key.query ),
		    new Argument( true, "function:Predicate", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.INTEGER, Key.maxThreads )
		};
	}

	/**
	 * Filters query rows specified in filter criteria
	 * This BIF will invoke the callback function for each row in the query, passing the row as a struct.
	 * <ul>
	 * <li>If the callback returns true, the row will be included in the new query.</li>
	 * <li>If the callback returns false, the row will be excluded from the new query.</li>
	 * <li>If the callback requires strict arguments, it will only receive the row as a struct.</li>
	 * <li>If the callback does not require strict arguments, it will receive the row as a struct, the row number (1-based), and the query itself.</li>
	 * </ul>
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the filter will be executed in parallel using a ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the filter in parallel, and destroy it after the operation is complete.
	 * Please note that this may not be the most efficient way to filter, as it will create a new ForkJoinPool for each invocation of the BIF. You may want to consider using a shared ForkJoinPool for better performance.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to get filtered
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the query row as a struct, the row number, the query. You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 *
	 * @argument.parallel Whether to run the filter in parallel. Defaults to false. If true, the filter will be run in parallel using a ForkJoinPool.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running the filter in parallel. If not passed it will use the default number of threads for the ForkJoinPool.
	 *                      If parallel is false, this argument is ignored.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return QueryUtil.filter(
		    arguments.getAsQuery( Key.query ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads )
		);
	}
}
