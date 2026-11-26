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
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.types.util.ListUtil.ParallelSettings;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Merges together two arrays one index at a time" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayZip extends BIF {

	/**
	 * Constructor
	 */
	public ArrayZip() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array1 ),
		    new Argument( true, Argument.ARRAY, Key.array2 ),
		    new Argument( false, Argument.FUNCTION, Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Merges two arrays of the same length together, one index at a time. Each element of the result is a two-element array
	 * <code>[ array1Value, array2Value ]</code>. If a callback is given, it is invoked with each pair and its return value is used instead.
	 * The callback receives four arguments: the value from array1, the value from array2, the one-based index and the result array.
	 * An exception is thrown if the arrays do not have the same length.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If <code>parallel</code> is true and no <code>maxThreads</code> is passed, the work runs in parallel using the common ForkJoinPool.
	 * If <code>maxThreads</code> is passed, a new ForkJoinPool with that many threads is created for the call and destroyed afterwards, so it
	 * may be less efficient for repeated calls.
	 *
	 * <pre>
	 * arrayZip( [ 1, 2 ], [ "a", "b" ] ) // [ [ 1, "a" ], [ 2, "b" ] ]
	 * arrayZip( [ 1, 2 ], [ 10, 20 ], ( a, b ) -> a + b ) // [ 11, 22 ]
	 * </pre>
	 *
	 * Also available as the <code>zip()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array1 The first array to zip.
	 *
	 * @argument.array2 The second array to zip. Must have the same length as array1.
	 *
	 * @argument.callback An optional function that receives the pair of values (and index and result array) and returns the value to use for
	 *                    that position.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If not passed, the default ForkJoinPool thread count
	 *                      is used. Ignored if parallel is false. If a boolean is passed, it is assigned to the virtual argument instead.
	 *
	 * @argument.virtual If true, the function is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return An array of two-element arrays, or an array of the callback results if a callback was given.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	arr1	= arguments.getAsArray( Key.array1 );
		Array	arr2	= arguments.getAsArray( Key.array2 );

		if ( arr1.size() != arr2.size() ) {
			throw new BoxRuntimeException(
			    "The two arrays do not have the same length.  array1 length: [" + arr1.size() + "]. array2 length: [" + arr2.size() + "]" );
		}

		Array results = Array.of();
		for ( int i = 0; i < arr1.size(); i++ ) {
			Object	val1	= arr1.get( i );
			Object	val2	= arr2.get( i );
			results.add( Array.of( val1, val2 ) );
		}

		Function callback = arguments.getAsFunction( Key.callback );
		if ( callback == null ) {
			return results;
		}

		ParallelSettings						settings	= ListUtil.resolveParallelSettings( arguments );
		Boolean									parallel	= arguments.getAsBoolean( Key.parallel );

		// Build the mapper based on the callback
		// If the callback requires strict arguments, we only pass the item (Usually
		// Java Predicates)
		// Otherwise we pass the item, the index, and the array itself
		java.util.function.IntFunction<Object>	mapper;
		if ( callback.requiresStrictArguments() ) {
			mapper = idx -> ThreadBoxContext.runInContext( context, parallel,
			    ctx -> results.copyData(
			        idx,
			        ctx.invokeFunction(
			            callback,
			            new Object[] {
			                results.size() > idx ? ( ( Array ) results.get( idx ) ).get( 0 ) : null,
			                results.size() > idx ? ( ( Array ) results.get( idx ) ).get( 1 ) : null
			            }
			        )
			    )
			);
		} else {
			mapper = idx -> ThreadBoxContext.runInContext( context, parallel,
			    ctx -> results.copyData(
			        idx,
			        ctx.invokeFunction(
			            callback,
			            new Object[] {
			                results.size() > idx ? ( ( Array ) results.get( idx ) ).get( 0 ) : null,
			                results.size() > idx ? ( ( Array ) results.get( idx ) ).get( 1 ) : null,
			                idx + 1,
			                results
			            }
			        )
			    )
			);
		}
		return ListUtil.map(
		    results,
		    mapper,
		    arguments.getAsBoolean( Key.parallel ),
		    settings.maxThreads(),
		    settings.virtual()
		);
	}
}
