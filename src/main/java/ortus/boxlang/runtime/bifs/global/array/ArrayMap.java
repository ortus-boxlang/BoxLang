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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Create a new array by transforming each element using a callback function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayMap extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMap() {
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
	 * Iterates over every entry of the array and calls the closure function to work on the element of the array. The returned value will be set at the
	 * same index in a new array and the new array will be returned
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the current item, and the
	 *                    current index, and the original array. You can alternatively pass a Java Function which will only receive the 1st arg. The function should return the value that will be set at the same index in the new array.
	 * 
	 * @argument.parallel If true, the function will be invoked in parallel using multiple threads. Defaults to false.
	 * 
	 * @argument.maxThreads The maximum number of threads to use when parallel is true. If not provided the common thread pool will be used. If a boolean value is passed, it will be assigned as the virtual argument.
	 * 
	 * @argument.virtual If true, the function will be invoked using virtual thread. Defaults to false. Ingored if parallel is false.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object maxThreads = arguments.get( Key.maxThreads );
		if ( maxThreads instanceof Boolean castBoolean ) {
			// If maxThreads is a boolean, we assign it to virtual
			arguments.put( Key.virtual, castBoolean );
			maxThreads = null;
		}

		CastAttempt<Integer> maxThreadsAttempt = IntegerCaster.attempt( maxThreads );

		return ListUtil.map(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    maxThreadsAttempt.getOrDefault( 0 ),
		    BooleanCaster.cast( arguments.getOrDefault( Key.virtual, false ) )
		);
	}
}
