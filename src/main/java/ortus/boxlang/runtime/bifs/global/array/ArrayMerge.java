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
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayMerge extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMerge() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array1 ),
		    new Argument( true, "array", Key.array2 ),
		    new Argument( true, "boolean", Key.leaveIndex, false )
		};
	}

	/**
	 * This function creates a new array with data from the two passed arrays. To add all the data from one array into another without creating a new
	 * array see the built in function ArrayAppend(arr1, arr2, true).
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.array1 The first array to merge
	 * 
	 * @argument.array2 The second array to merge
	 * 
	 * @argument.leaveIndex Set to true maintain value indexes - if two values have the same index it will keep values from array1
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	arrayOne	= arguments.getAsArray( Key.array1 );
		Array	arrayTwo	= arguments.getAsArray( Key.array2 );
		Boolean	leaveIndex	= arguments.getAsBoolean( Key.leaveIndex );
		Array	result		= new Array();

		if ( !leaveIndex ) {
			result.addAll( arrayOne );
			result.addAll( arrayTwo );

			return result;
		}

		if ( arrayOne.size() >= arrayTwo.size() ) {
			result.addAll( arrayOne );

			return result;
		}

		result.addAll( arrayTwo );

		for ( int i = 0; i < arrayOne.size(); i++ ) {
			result.set( i, arrayOne.get( i ) );
		}

		return result;
	}

}
