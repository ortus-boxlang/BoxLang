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

@BoxBIF( description = "Resize an array to a specified size" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayResize extends BIF {

	/**
	 * Constructor
	 */
	public ArrayResize() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "integer", Key.size )
		};
	}

	/**
	 * Ensures an array has at least the given number of elements by padding it with null elements. The array is modified in place.
	 * An array that already has that many elements (or more) is left unchanged; it is never shrunk.
	 * Also available as the <code>resize()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to resize.
	 *
	 * @argument.size The minimum number of elements the array should have.
	 *
	 * @return The same array, padded to at least the given size.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualObj	= arguments.getAsArray( Key.array );
		Integer	size		= arguments.getAsInteger( Key.size );

		if ( actualObj.size() < size ) {
			for ( int i = actualObj.size(); i < size; i++ ) {
				actualObj.add( null );
			}
		}
		return actualObj;
	}

}
