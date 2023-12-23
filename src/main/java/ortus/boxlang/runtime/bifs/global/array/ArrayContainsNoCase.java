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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayContainsNoCase extends BIF {

	/**
	 * Constructor
	 */
	public ArrayContainsNoCase() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Return int position of value in array, case insensitive
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.array The array to be searched.
	 * 
	 * @argument.value The value to search for, or a Function to invoke that returns true/false.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	value		= arguments.get( Key.value );

		if ( value instanceof Function callback ) {
			for ( int i = 0; i < actualArray.size(); i++ ) {
				if ( BooleanCaster.cast( context.invokeFunction( callback, new Object[] { actualArray.get( i ), i + 1, actualArray } ) ) ) {
					return i + 1;
				}
			}
			return 0;
		}
		return ArrayContainsNoCase._invoke( actualArray, value );
	}

	/**
	 * Return int position of value in array, case insensitive
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public static int _invoke( Array array, Object value ) {
		for ( int i = 0; i < array.size(); i++ ) {
			if ( Compare.invoke( array.get( i ), value, false ) == 0 ) {
				return i + 1;
			}
		}
		return 0;
	}

}
