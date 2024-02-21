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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Val" )
public class Val extends BIF {

	/**
	 * Constructor
	 */
	public Val() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string )
		};
	}

	/**
	 * Converts numeric characters and the first period found that occur at the beginning of a string to a number. A period not accompianied by at least
	 * one numeric digit will be ignored. If no numeric digits are found at the start of the string, zero will be returned.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to parse
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		var		input		= arguments.getAsString( Key.string );
		var		result		= new StringBuilder();
		boolean	foundDot	= false;
		// Loop over each character in the string
		for ( var c : input.getBytes() ) {
			// If the character is not a digit
			if ( ( c < 48 || c > 57 ) && c != 46 ) {
				// we're done
				break;
			}
			// The FIRST period is allowed
			if ( c == 46 ) {
				// But subsequent periods are not
				if ( foundDot ) {
					break;
				}
				foundDot = true;
			}
			// Build up the result
			result.append( ( char ) c );
		}
		// If no digits were found, return 0
		if ( result.length() == 0 ) {
			return 0;
		}
		// A single period is not a valid number
		if ( result.toString().equals( "." ) ) {
			return 0;
		}
		// Return the result as a double
		return Double.parseDouble( result.toString() );
	}
}
