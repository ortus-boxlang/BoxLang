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
	 * Returns a zipped array.
	 *
	 * <pre>
	 * arrayZip( [ 1, 2 ], [ "a", "b" ] );
	 * // [ [ 1, "a" ], [ 2, "b" ] ]
	 *
	 * arrayZip( [ 1, 2 ], [ 10, 20 ], ( a, b ) => a + b );
	 * // [ 11, 22 ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array1 The first array to zip
	 * 
	 * @argument.array2 The second array to zip
	 * 
	 * @argument.callback An optional callback function to receive the item and the current index from both zipped arrays and return a new value.
	 * 
	 * @argument.parallel If true, the function will be invoked in parallel using multiple threads. Defaults to false.
	 * 
	 * @argument.maxThreads The maximum number of threads to use when parallel is true. If not provided the common thread pool will be used. If a boolean value is passed, it will be assigned as the virtual argument.
	 * 
	 * @argument.virtual If true, the function will be invoked using virtual thread. Defaults to false. Ignored if parallel is false.
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
