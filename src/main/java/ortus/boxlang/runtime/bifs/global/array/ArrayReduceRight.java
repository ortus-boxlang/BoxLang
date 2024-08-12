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
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayReduceRight extends BIF {

	/**
	 * Constructor
	 */
	public ArrayReduceRight() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "function:BiFunction", Key.callback ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * This function iterates over every element of the array and calls the closure to work on that element. It will reduce the array to a single value,
	 * from the right to the left, and return it.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the accumulator, the current item, and the
	 *                    current index. You can alternatively pass a Java BiFunction which will only receive the first 2 args. The function should return the new accumulator value.
	 *
	 * @argument.initialValue The initial value of the accumulator
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		actualArray	= ArrayCaster.cast( arguments.get( Key.array ) );
		Object		accumulator	= arguments.get( Key.initialValue );
		Function	func		= arguments.getAsFunction( Key.callback );

		for ( int i = actualArray.size() - 1; i >= 0; i-- ) {
			if ( func.requiresStrictArguments() ) {
				accumulator = context.invokeFunction( func, new Object[] { accumulator, actualArray.get( i ) } );
			} else {
				accumulator = context.invokeFunction( func, new Object[] { accumulator, actualArray.get( i ), i + 1, actualArray } );
			}
		}

		return accumulator;
	}
}
