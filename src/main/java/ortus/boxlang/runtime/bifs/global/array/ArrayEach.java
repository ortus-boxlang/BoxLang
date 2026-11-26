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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.ListUtil.ParallelSettings;

@BoxBIF( description = "Execute a callback function for each element in an array" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayEach extends BIF {

	/**
	 * Constructor
	 */
	public ArrayEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, "function:Consumer", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.ordered, false ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Iterates over an array and invokes the callback once for each element. Nothing is returned.
	 * The callback receives three arguments: the value, the one-based index, and the array. A Java Consumer may also be passed, in which case it
	 * only receives the value.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If <code>parallel</code> is true and no <code>maxThreads</code> is passed, the iteration runs in parallel using the common ForkJoinPool.
	 * If <code>maxThreads</code> is passed, a new ForkJoinPool with that many threads is created for the call and destroyed afterwards, which
	 * can be less efficient for repeated calls.
	 * Also available as the <code>each()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to iterate over.
	 *
	 * @argument.callback The function to invoke for each item. It receives the value, the one-based index and the array. A Java Consumer is also
	 *                    accepted and only receives the value.
	 *
	 * @argument.parallel Whether to run the iteration in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If not passed, the default ForkJoinPool thread count
	 *                      is used. Ignored if parallel is false. If a boolean is passed, it is assigned to the virtual argument instead.
	 *
	 * @argument.ordered Whether parallel operations should execute and maintain the order of the array. Defaults to false. Ignored if parallel
	 *                   is false.
	 *
	 * @argument.virtual If true, the function is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return Nothing (null).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ParallelSettings settings = ListUtil.resolveParallelSettings( arguments );
		ListUtil.each(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    settings.maxThreads(),
		    arguments.getAsBoolean( Key.ordered ),
		    settings.virtual(),
		    null
		);
		return null;
	}
}
