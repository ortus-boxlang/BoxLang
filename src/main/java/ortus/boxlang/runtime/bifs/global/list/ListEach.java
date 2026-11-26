/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArrayEach;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.ListUtil.ParallelSettings;

@BoxBIF( description = "Execute a callback function for each item in a list" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listEach" )

public class ListEach extends ArrayEach {

	/**
	 * Constructor
	 */
	public ListEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.list ),
		    new Argument( true, "function:Consumer", Key.callback ),
		    new Argument( false, Argument.STRING, Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, Argument.BOOLEAN, Key.includeEmptyFields, false ),
		    new Argument( false, Argument.BOOLEAN, Key.multiCharacterDelimiter, false ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.ordered, false ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Used to iterate over a delimited list and run the function closure for each item in the list.
	 * This BIF is similar to the ArrayEach BIF, but operates on a delimited list instead of an array.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the filter will be executed in parallel using a ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the filter in parallel, and destroy it after the operation is complete.
	 * Please note that this may not be the most efficient way to iterate, as it will create a new ForkJoinPool for each invocation of the BIF. You may want to consider using a shared ForkJoinPool for better performance.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The delimited list to iterate over.
	 *
	 * @argument.callback The function to invoke for each element. It receives the value, the one-based index, and the original list
	 *                    string. You can alternatively pass a Java Consumer, which receives only the value.
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *                     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty elements (for example those created by consecutive delimiters) are kept. Defaults
	 *                              to false.
	 *
	 * @argument.multiCharacterDelimiter When true, the delimiter is treated as one multi-character separator instead of a set of
	 *                                   single-character delimiters. Defaults to false.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If omitted, the default ForkJoinPool
	 *                      parallelism is used. Ignored if parallel is false. If a boolean is passed it is assigned to the virtual argument instead.
	 *
	 * @argument.ordered Whether parallel execution should maintain element order. Defaults to false. Ignored if parallel is false.
	 *
	 * @argument.virtual If true, the callback is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return Nothing (null). The callback is executed for its side effects.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ParallelSettings settings = ListUtil.resolveParallelSettings( arguments );
		ListUtil.each(
		    ListUtil.asList(
		        arguments.getAsString( Key.list ),
		        arguments.getAsString( Key.delimiter ),
		        arguments.getAsBoolean( Key.includeEmptyFields ),
		        arguments.getAsBoolean( Key.multiCharacterDelimiter )
		    ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    settings.maxThreads(),
		    arguments.getAsBoolean( Key.ordered ),
		    settings.virtual(),
		    arguments.getAsString( Key.list )
		);
		return null;
	}

}
