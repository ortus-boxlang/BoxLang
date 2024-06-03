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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayShift extends BIF {

	/**
	 * Constructor
	 */
	public ArrayShift() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( false, "any", Key.defaultValue ),
		};
	}

	/**
	 * Removes the first element from an array and returns the removed element. This method changes the length of the array. If used on an empty array, an
	 * exception will be thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to shift
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualObj		= arguments.getAsArray( Key.array );
		Object	defaultValue	= arguments.get( Key.defaultValue );
		if ( actualObj.size() == 0 && defaultValue != null ) {
			return defaultValue;
		} else if ( actualObj.size() == 0 ) {
			throw new BoxRuntimeException( "Cannot shift an element from an empty array without a default value" );
		}
		return actualObj.removeAt( 0 );
	}

}
