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

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySplice extends BIF {

	/**
	 * Constructor
	 */
	public ArraySplice() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "numeric", Key.index ),
		    new Argument( false, "numeric", Key.elementCountForRemoval, 0 ),
		    new Argument( false, "array", Key.replacements )
		};
	}

	/**
	 * Modifies an array by removing elements and adding new elements. It starts from the index, removes as many elements as specified by
	 * elementCountForRemoval, and puts the replacements starting from index position.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to splice
	 * 
	 * @argument.index The initial position to remove or insert from
	 * 
	 * @argument.elementCountForRemoval The number of elemetns to remove
	 * 
	 * @argument.replacements An array of elements to insert
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	arr			= arguments.getAsArray( Key.array );
		Integer	startIndex	= IntegerCaster.cast( arguments.get( Key.index ) );
		Integer	toRemove	= IntegerCaster.cast( arguments.get( Key.elementCountForRemoval ) );

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
