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

@BoxBIF( description = "Extract a portion of an array" )
@BoxBIF( alias = "ArrayMid" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "mid" )
public class ArraySlice extends BIF {

	/**
	 * Constructor
	 */
	public ArraySlice() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "integer", Key.start, 1 ),
		    new Argument( false, "integer", Key.length, 0 )
		};
	}

	/**
	 * Extracts a portion of an array and returns it as a new array. The original array is not modified.
	 * Indexes are one-based. A negative start counts from the end of the array (-1 is the last element). A length of 0 (default) returns
	 * everything from
	 * start to the end of the array, and a negative length stops that many elements before the end of the array. Elements beyond the end of the
	 * array are ignored.
	 * Also available as <code>ArrayMid</code>, and as the <code>slice()</code> and <code>mid()</code> member functions on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to slice.
	 *
	 * @argument.start The one-based position to start the slice from. Negative values count from the end of the array. Defaults to 1.
	 *
	 * @argument.length The number of elements to return. 0 (default) returns all elements from start to the end of the array. A negative value
	 *                  leaves out that many elements from the end.
	 *
	 * @return A new array containing the extracted elements.
	 */
	public Array _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		start		= arguments.getAsInteger( Key.start ) - 1;
		int		length		= arguments.getAsInteger( Key.length );
		Array	outputArray	= new Array();

		if ( start < 0 ) {
			start = actualArray.size() + arguments.getAsInteger( Key.start );
		}

		if ( length < 0 ) {
			length = ( actualArray.size() + length ) - start;
		} else if ( length == 0 ) {
			length = actualArray.size() - start;
		}

		for ( int i = start; i < start + length; i++ ) {
			if ( i < actualArray.size() ) {
				outputArray.add( actualArray.get( i ) );
			}
		}

		return outputArray;
	}

}
