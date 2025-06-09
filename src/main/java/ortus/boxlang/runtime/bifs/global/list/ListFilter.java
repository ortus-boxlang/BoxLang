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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listFilter" )

public class ListFilter extends BIF {

	/**
	 * Constructor
	 */
	public ListFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.list ),
		    new Argument( true, "function:Predicate", Key.filter ),
		    new Argument( false, Argument.STRING, Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, Argument.BOOLEAN, Key.includeEmptyFields, false ),
		    new Argument( false, Argument.BOOLEAN, Key.multiCharacterDelimiter, true ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.INTEGER, Key.maxThreads )
		};
	}

	/**
	 * Filters a delimted list and returns the values from the callback test
	 * This BIF will invoke the callback function for each entry in the list, passing the entry as a string.
	 * <ul>
	 * <li>If the callback returns true, the entry will be included in the new list.</li>
	 * <li>If the callback returns false, the entry will be excluded from the new list.</li>
	 * <li>If the callback requires strict arguments, it will only receive the entry as a string.</li>
	 * <li>If the callback does not require strict arguments, it will receive the entry as a string, the index (0-based), and the original list as a string.</li>
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
	 * @argument.list string list to filter entries from
	 *
	 * @argument.filter function closure filter test. You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is multi-character
	 *
	 * @argument.parallel Whether to run the filter in parallel. Defaults to false. If true, the filter will be run in parallel using a ForkJoinPool.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running the filter in parallel. If not passed it will use the default number of threads for the ForkJoinPool.
	 *                      If parallel is false, this argument is ignored.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.asString(
		    ListUtil.filter(
		        // Convert the list to an array to filter it
		        ListUtil.asList(
		            arguments.getAsString( Key.list ),
		            arguments.getAsString( Key.delimiter ),
		            arguments.getAsBoolean( Key.includeEmptyFields ),
		            arguments.getAsBoolean( Key.multiCharacterDelimiter )
		        ),
		        arguments.getAsFunction( Key.filter ),
		        context,
		        arguments.getAsBoolean( Key.parallel ),
		        arguments.getAsInteger( Key.maxThreads )
		    ),
		    arguments.getAsString( Key.delimiter )
		);
	}

}
