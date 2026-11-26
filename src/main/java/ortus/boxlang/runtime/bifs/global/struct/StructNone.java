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
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Test whether no items in struct match the callback function" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructNone extends StructSome {

	/**
	 * Constructor
	 */
	public StructNone() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRUCT_LOOSE, Key.struct ),
		    new Argument( true, "function:BiPredicate", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Used to iterate over a struct and test whether <strong>NONE</strong> of the items meet the test callback.
	 * This is the opposite of {@link StructSome}.
	 * <p>
	 * The function will be passed 3 arguments: the key, the value, and the struct.
	 * You can alternatively pass a Java BiPredicate which will only receive the first 2 args (key and value).
	 * The function should return true if the item meets the test, and false otherwise.
	 * <p>
	 * <strong>Note:</strong> This operation is a short-circuit operation, meaning it will stop iterating as soon as it finds the first item that
	 * meets the test condition.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the test will be executed in parallel using a
	 * ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the test in parallel,
	 * and destroy it after the operation is complete.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct to test.
	 *
	 * @argument.callback The function used to test. The function will be passed 3 arguments: the key, the value, the struct. You can alternatively
	 *                    pass a Java BiPredicate which will only receive the first 2 args.
	 *
	 * @argument.parallel Whether to run the test in parallel. Defaults to false. If true, the test will be run in parallel using a ForkJoinPool.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If not passed the common ForkJoinPool is used. Ignored if
	 *                      parallel is false. If a boolean is provided it is assigned to the virtual argument instead.
	 *
	 * @argument.virtual Whether to use a virtual thread for each task when running in parallel. Defaults to false. Ignored if parallel is false.
	 *
	 * @return true if the callback returned false for every entry (or the struct is empty), false if it returned true for any entry.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return !BooleanCaster.cast( super._invoke( context, arguments ) );
	}

}
