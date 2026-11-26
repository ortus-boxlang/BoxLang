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

@BoxBIF( description = "Remove or replace elements in an array and return the removed elements" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySplice extends BIF {

	/**
	 * Constructor
	 */
	public ArraySplice() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "Integer", Key.index ),
		    new Argument( false, "Integer", Key.elementCountForRemoval, 0 ),
		    new Argument( false, "array", Key.replacements )
		};
	}

	/**
	 * Removes and/or inserts elements in an array at a given position. The array is modified in place.
	 * Starting at the one-based index, elementCountForRemoval elements are removed, and then the replacements (if any) are inserted at that
	 * position.
	 * A negative index counts from the end of the array. Note that the array itself is returned, not the removed elements.
	 * Also available as the <code>splice()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to splice.
	 *
	 * @argument.index The one-based position to start removing or inserting at. Negative values count from the end of the array.
	 *
	 * @argument.elementCountForRemoval The number of elements to remove, starting at index. Defaults to 0.
	 *
	 * @argument.replacements An array of elements to insert at the index position. Optional.
	 *
	 * @return The same array, after the removals and insertions.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	arr			= arguments.getAsArray( Key.array );
		Integer	startIndex	= arguments.getAsInteger( Key.index );
		Integer	toRemove	= arguments.getAsInteger( Key.elementCountForRemoval );

		if ( startIndex < 0 ) {
			startIndex = ( arr.size() - ( ( startIndex * -1 ) % arr.size() ) ) + 1;
		}

		startIndex -= 1;

		if ( toRemove > 0 ) {
			toRemove -= 1;
			for ( int i = startIndex + toRemove; i >= startIndex; i-- ) {
				arr.removeAt( i );
			}
		}

		Array replacements = arguments.getAsArray( Key.replacements );

		if ( replacements != null ) {
			arr.addAll( startIndex, replacements );
		}

		return arr;
	}

}
