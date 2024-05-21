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

@BoxBIF
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
	 * Extracts a sub array from an existing array.
	 *
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Array _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		start		= arguments.getAsInteger( Key.start ) - 1;
		int		length		= arguments.getAsInteger( Key.length );
		Array	outputArray	= new Array();

		if ( start < 0 ) {
			start = actualArray.size() + start;
		}

		if ( length < 0 ) {
			length = actualArray.size() + length - 1;
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
