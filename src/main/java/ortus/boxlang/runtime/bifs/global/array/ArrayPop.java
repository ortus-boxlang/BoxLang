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

@BoxBIF( description = "Remove and return the last element from an array" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayPop extends BIF {

	/**
	 * Constructor
	 */
	public ArrayPop() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( false, "any", Key.defaultValue )
		};
	}

	/**
	 * Removes the last element from an array and returns it. The array is modified in place.
	 * If the array is empty, the default value is returned, or null if no default value was given.
	 * Also available as the <code>pop()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to remove the last element from.
	 *
	 * @argument.defaultValue The value to return when the array is empty. If omitted, null is returned for an empty array.
	 *
	 * @return The removed element, or the default value if the array is empty.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		if ( actualArray.size() == 0 ) {
			return arguments.get( Key.defaultValue );
		}

		return actualArray.remove( actualArray.size() - 1 );
	}

}
