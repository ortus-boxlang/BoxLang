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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Return the ASCII value of the first character in a string" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ascii" )
public class Ascii extends BIF {

	/**
	 * Constructor
	 */
	public Ascii() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ) // Change argument type to string
		};
	}

	/**
	 * Return the numeric character code of the first character of a string. Only the first character is used; any remaining characters are
	 * ignored.
	 * The value is the UTF-16 code unit of that character.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string whose first character is converted. Must contain at least one character.
	 *
	 * @throws BoxRuntimeException if the string is empty.
	 *
	 * @return The integer code of the first character.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String inputString = arguments.getAsString( Key.string );

		// Check if the input string has a single character
		if ( inputString.length() >= 1 ) {
			char character = inputString.charAt( 0 );

			// Return the ASCII value of the character
			return ( int ) character;
		} else {
			throw new BoxRuntimeException( "Input string must contain a single character" );
		}
	}
}
