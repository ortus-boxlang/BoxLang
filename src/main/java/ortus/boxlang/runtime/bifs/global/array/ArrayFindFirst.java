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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.ListUtil.ParallelSettings;

@BoxBIF( description = "Return first item in array that matches the predicate function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFindFirst extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFindFirst() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, Argument.FUNCTION, Key.callback ),
		    new Argument( false, Argument.ANY, Key.defaultValue ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Returns the first element of an array for which the predicate function returns true.
	 * The callback receives three arguments: the value, the one-based index and the array. A Java Predicate may also be passed, in which case it
	 * only receives the value.
	 * If no element matches (or the array is empty) the default value is returned; if no default value was given an exception is thrown.
	 * <p>
	 * <h2>Parallel Execution</h2>
	 * If <code>parallel</code> is true and no <code>maxThreads</code> is passed, the work runs in parallel using the common ForkJoinPool.
	 * If <code>maxThreads</code> is passed, a new ForkJoinPool with that many threads is created for the call and destroyed afterwards, so it
	 * may be less efficient for repeated calls.
	 * 
	 * <pre>
	 * users = [ { name: "Ada" }, { name: "Grace" } ]
	 * users.findFirst( ( user ) => user.name == "Grace" ) // { name: "Grace" }
	 * users.findFirst( ( user ) => user.name == "Linus", "Unknown" ) // "Unknown"
	 * </pre>
	 * 
	 * Also available as the <code>findFirst()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to search.
	 *
	 * @argument.callback The function to invoke for each item. It receives the value, the one-based index and the array. A Java Predicate is
	 *                    also accepted and only receives the value.
	 *
	 * @argument.defaultValue The value to return if the array is empty or no element matches. If omitted, an exception is thrown in those cases.
	 *
	 * @argument.parallel Whether to run the search in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If not passed, the default ForkJoinPool thread count
	 *                      is used. Ignored if parallel is false. If a boolean is passed, it is assigned to the virtual argument instead.
	 *
	 * @argument.virtual If true, the function is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return The first matching element, or the default value.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray		= arguments.getAsArray( Key.array );
		Object	defaultValue	= arguments.get( Key.defaultValue );
		if ( actualArray.size() <= 0 ) {
			if ( defaultValue != null ) {
				return defaultValue;
			} else {
				throw new BoxRuntimeException( "Cannot retrieve the first record of an empty array." );
			}
		}

		Function	callback	= arguments.getAsFunction( Key.callback );
		boolean		parallel	= arguments.getAsBoolean( Key.parallel );
		if ( !parallel ) {
			int indexFound = actualArray.findIndex( callback, context );
			if ( indexFound > 0 ) {
				return actualArray.get( indexFound - 1 );
			}
			if ( defaultValue != null ) {
				return defaultValue;
			}
			throw new BoxRuntimeException( "Could not find any results that matched the predicate function." );
		}

		ParallelSettings	settings	= ListUtil.resolveParallelSettings( arguments );
		Array				filtered	= ListUtil.filter(
		    actualArray,
		    callback,
		    context,
		    parallel,
		    settings.maxThreads(),
		    settings.virtual()
		);

		if ( filtered.size() > 0 ) {
			return filtered.get( 0 );
		}
		if ( defaultValue != null ) {
			return defaultValue;
		}
		throw new BoxRuntimeException( "Could not find any results that matched the predicate function." );

	}

}
