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
public class ArraySwap extends BIF {

	/**
	 * Constructor
	 */
	public ArraySwap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.position1 ),
		    new Argument( true, "any", Key.position2 )
		};
	}

	/**
	 * Swaps array values of an array at specified positions. This function is more efficient than multiple assignment statements
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 * 
	 * @arguments.position1 The first position to swap
	 * 
	 * @arguments.position2 The second position to swap
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	temp		= actualArray.get( arguments.getAsInteger( Key.position1 ) - 1 );

		actualArray.set( arguments.getAsInteger( Key.position1 ) - 1, actualArray.get( arguments.getAsInteger( Key.position2 ) - 1 ) );
		actualArray.set( arguments.getAsInteger( Key.position2 ) - 1, temp );

		if ( arguments.getAsBoolean( BIF.__isMemberExecution ) ) {
			return actualArray;
		}
		return true;
	}

}
