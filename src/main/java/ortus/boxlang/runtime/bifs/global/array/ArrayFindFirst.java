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
import ortus.boxlang.runtime.bifs.global.array.ArrayParallelUtil.ParallelSettings;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

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
	 * Return first item in array that matches the predicate function.
	 *
	 * <pre>
	 * users = [ { name: "Ada" }, { name: "Grace" } ];
	 * users.findFirst( ( user ) => user.name == "Grace" ); // { name: "Grace" }
	 * users.findFirst( ( user ) => user.name == "Linus", "Unknown" ); // "Unknown"
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to get the first item from.
	 * 
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array. You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 * 
	 * @argument.defaultValue The default value to use if the array is empty or no value is returned from the predicate function.
	 * 
	 * @argument.parallel Whether to run the filter in parallel. Defaults to false. If true, the filter will be run in parallel using a ForkJoinPool.
	 * 
	 * @argument.maxThreads The maximum number of threads to use when running the filter in parallel. If not passed it will use the default number of threads for the ForkJoinPool.
	 *                      If parallel is false, this argument is ignored. If a boolean is provided it will be assigned to the virtual argument instead.
	 * 
	 * @argument.virtual (BoxLang only) If true, the function will be invoked using virtual threads. Defaults to false. Ignored if parallel is false.
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

		ParallelSettings	settings	= ArrayParallelUtil.resolveParallelSettings( arguments );
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
