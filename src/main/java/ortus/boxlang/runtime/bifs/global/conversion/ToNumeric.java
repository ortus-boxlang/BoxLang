/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.conversion;

import java.math.BigDecimal;
import java.math.BigInteger;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

@BoxBIF
public class ToNumeric extends BIF {

	/**
	 * Constructor
	 */
	public ToNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.value ),
		    new Argument( false, "any", Key.radix )
		};
	}

	/**
	 * Cast a value to a number.
	 *
	 * @argument.value The value to cast.
	 *
	 * @argument.radix The radix to use when casting the value. Valid values are 2-36, "bin", "oct", "dec", and "hex".
	 *
	 * @param context   The context in which the BIF is being executed.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @return The numeric value of the input.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	inputValue	= arguments.getAsString( Key.value );
		Object	radixValue	= arguments.get( Key.radix );

		if ( radixValue == null ) {
			return NumberCaster.cast( inputValue );
		} else {
			int numericRadix = convertStringToRadix( radixValue );
			if ( inputValue.length() >= 20 ) {
				BigInteger bigInteger = new BigInteger( inputValue, numericRadix );
				return new BigDecimal( bigInteger );
			} else {
				return Long.parseLong( inputValue, numericRadix );
			}
		}
	}

	/**
	 * Convert a radix string to an integer.
	 *
	 * @param radixString The radix string to convert.
	 *
	 * @return The integer value of the radix string.
	 */
	private int convertStringToRadix( Object oRadix ) {
		if ( oRadix instanceof Number rn ) {
			int radix = rn.intValue();
			if ( radix >= 2 && radix <= 36 ) {
				return radix;
			} else {
				throw new BoxValidationException( "Radix must be between 2 and 36." );
			}
		}
		String radixString = StringCaster.cast( oRadix ).toLowerCase();
		switch ( radixString ) {
			case "bin" :
				return 2;
			case "oct" :
				return 8;
			case "dec" :
				return 10;
			case "hex" :
				return 16;
			default :
				throw new BoxValidationException( "Invalid radix [" + radixString + "], valid values are [2-36,bin,oct,dec,hex]" );
		}
	}
}
