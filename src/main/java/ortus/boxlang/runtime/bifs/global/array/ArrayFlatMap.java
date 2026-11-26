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
import ortus.boxlang.runtime.types.util.ListUtil.ParallelSettings;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Create a new array by transforming each element using a callback function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFlatMap extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFlatMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, "function:Function", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Maps each element of an array through a callback and flattens the result one level, returning a new array.
	 * The callback receives three arguments: the value, the one-based index and the array, and normally returns an array. A Java Function may
	 * also be passed, in which case it only receives the value.
	 * Callback results that are not arrays are kept as single elements.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If <code>parallel</code> is true and no <code>maxThreads</code> is passed, the work runs in parallel using the common ForkJoinPool.
	 * If <code>maxThreads</code> is passed, a new ForkJoinPool with that many threads is created for the call and destroyed afterwards, so it
	 * may be less efficient for repeated calls.
	 * <pre>
	 * values = [ 1, 2, 3 ]
	 * values.flatMap( ( value ) => [ value, value * 10 ] ) // [ 1, 10, 2, 20, 3, 30 ]
	 * </pre>
	 * Also available as the <code>flatMap()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to transform.
	 *
	 * @argument.callback The function to invoke for each item. It receives the value, the one-based index and the array. A Java Function is also
	 *                    accepted and only receives the value.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If not passed, the default ForkJoinPool thread count
	 *                      is used. Ignored if parallel is false. If a boolean is passed, it is assigned to the virtual argument instead.
	 *
	 * @argument.virtual If true, the function is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return A new, flattened array of the callback results.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ParallelSettings settings = ListUtil.resolveParallelSettings( arguments );

		return ListUtil.flatten(
		    ListUtil.map(
		        arguments.getAsArray( Key.array ),
		        arguments.getAsFunction( Key.callback ),
		        context,
		        arguments.getAsBoolean( Key.parallel ),
		        settings.maxThreads(),
		        settings.virtual()
		    ),
		    1
		);
	}
}
