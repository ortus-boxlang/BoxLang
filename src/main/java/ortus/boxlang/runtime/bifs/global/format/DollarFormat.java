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

package ortus.boxlang.runtime.bifs.global.format;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class DollarFormat extends BIF {

	/**
	 * Constructor
	 */
	public DollarFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number )
		};
	}

	/**
	 * Formats a number as a U.S. Dollar string with two decimal places, thousands separator, and a dollar sign.
	 * If the number is negative, the return value is enclosed in parentheses.
	 * If the number is an empty string, the function returns "0.00".
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to format as a U.S. Dollar string.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	originalValue	= arguments.get( Key.number );
		double	value			= 0.0;

		if ( originalValue instanceof Double ) {
			value = ( Double ) originalValue;
		} else if ( originalValue instanceof String ) {
			String stringValue = ( String ) originalValue;
			if ( stringValue.isEmpty() ) {
				return "$0.00";
			}
			try {
				value = Double.parseDouble( stringValue );
			} catch ( NumberFormatException e ) {
				throw new BoxRuntimeException( "Cannot format number as U.S. Dollar string; invalid number: " + stringValue );
			}
		}

		String formattedValue = String.format( "%,.2f", StrictMath.abs( value ) );

		if ( value < 0 ) {
			return "($" + formattedValue + ")";
		} else {
			return "$" + formattedValue;
		}
	}

}
