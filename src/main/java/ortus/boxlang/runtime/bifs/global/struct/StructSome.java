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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )
public class StructSome extends BIF {

	/**
	 * Constructor
	 */
	public StructSome() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( true, "function:BiPredicate", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads )
		};
	}

	/**
	 * Used to iterate over a struct and test whether <strong>ANY</strong> items meet the test callback.
	 * The function will be passed 3 arguments: the key, the value, and the struct.
	 * You can alternatively pass a Java BiPredicate which will only receive the first 2 args.
	 * The function should return true if the item meets the test, and false otherwise.
	 * <p>
	 * <strong>Note:</strong> This operation is a short-circuit operation, meaning it will stop iterating as soon as it finds the first item that meets the test condition.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If the <code>parallel</code> argument is set to true, and no <code>max_threads</code> are sent, the filter will be executed in parallel using a ForkJoinPool with parallel streams.
	 * If <code>max_threads</code> is specified, it will create a new ForkJoinPool with the specified number of threads to run the filter in parallel, and destroy it after the operation is complete.
	 * Please note that this may not be the most efficient way to iterate, as it will create a new ForkJoinPool for each invocation of the BIF. You may want to consider using a shared ForkJoinPool for better performance.
	 * <p>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct to test
	 *
	 * @argument.callback The function used to test. The function will be passed 3 arguments: the key, the value, the struct. You can alternatively pass a Java BiPredicate which will only receive the first 2 args.
	 *
	 * @argument.parallel Whether to run the filter in parallel. Defaults to false. If true, the filter will be run in parallel using a ForkJoinPool.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running the filter in parallel. If not passed it will use the default number of threads for the ForkJoinPool.
	 *                      If parallel is false, this argument is ignored.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return StructUtil.some(
		    arguments.getAsStruct( Key.struct ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads )
		);
	}

}
