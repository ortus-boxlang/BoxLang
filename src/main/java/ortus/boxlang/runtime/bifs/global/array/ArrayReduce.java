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

@BoxBIF( description = "Reduce an array to a single value using a callback function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayReduce extends BIF {

	/**
	 * Constructor
	 */
	public ArrayReduce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "function:BiFunction", Key.callback ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * Reduces an array to a single value by invoking the callback for each element from left to right.
	 * The callback receives four arguments: the accumulator, the current value, the one-based index and the array, and must return the new
	 * accumulator value.
	 * A Java BiFunction may also be passed, in which case it only receives the accumulator and the value.
	 * Also available as the <code>reduce()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce.
	 *
	 * @argument.callback The function to invoke for each item. It receives the accumulator, the current value, the one-based index and the
	 *                    array, and returns the new accumulator.
	 *
	 * @argument.initialValue The initial value of the accumulator. If omitted, the accumulator starts as null.
	 *
	 * @return The final accumulator value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {

		return ListUtil.reduce(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.get( Key.initialValue )
		);

	}
}
