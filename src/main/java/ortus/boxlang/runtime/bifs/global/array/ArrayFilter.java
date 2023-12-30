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

import java.util.function.IntPredicate;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.CompletableFuture;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFilter extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel ),
		    new Argument( false, "numeric", Key.maxThreads, 20 ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * Used to filter an array to items for which the closure function returns true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array.
	 *
	 * @argument.parallel Specifies whether the items can be executed in parallel
	 *
	 * @argument.maxThreads The maximum number of threads to use when parallel = true
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {

		Array			actualArray	= ArrayCaster.cast( arguments.get( "array" ) );
		Function		func		= ( Function ) arguments.get( Key.callback );
		IntPredicate	test		= idx -> ( boolean ) context.invokeFunction( func, new Object[] { actualArray.get( idx ), idx + 1, actualArray } );

		ForkJoinPool	pool		= null;
		if ( BooleanCaster.cast( arguments.getOrDefault( "parallel", false ) ) ) {
			pool = new ForkJoinPool( IntegerCaster.cast( arguments.getOrDefault( "maxThreads", 20 ) ) );
		}

		return ArrayCaster.cast(
		    pool == null
		        ? actualArray.intStream()
		            .filter( test )
		            .mapToObj( idx -> actualArray.get( idx ) )
		            .toArray()

		        : CompletableFuture.supplyAsync(
		            () -> actualArray.intStream().parallel().filter( test ).mapToObj( idx -> actualArray.get( idx ) ),
		            pool
		        ).join().toArray()
		);
	}
}
