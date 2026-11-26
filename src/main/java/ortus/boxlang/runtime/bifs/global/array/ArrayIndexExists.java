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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Check if an index exists in an array" )
@BoxBIF( alias = "ArrayIsDefined" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "isDefined" )
public class ArrayIndexExists extends BIF {

	/**
	 * Constructor
	 */
	public ArrayIndexExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.index )
		};
	}

	/**
	 * Checks whether an array has a non-null element at the given one-based index.
	 * Returns false for indexes less than 1, greater than the array size, or where the element is null.
	 * Also available as <code>ArrayIsDefined</code>, and as the <code>indexExists()</code> and <code>isDefined()</code> member functions on
	 * arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to check.
	 *
	 * @argument.index The one-based index to check.
	 *
	 * @return True if a non-null element exists at the index, false otherwise.
	 */
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		index		= IntegerCaster.cast( arguments.get( Key.index ) ) - 1;

		return index >= 0 && index < actualArray.size() && actualArray.get( index ) != null;
	}

}
