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

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "LJustify" )
public class LJustify extends BIF {

	/**
	 * Constructor
	 */
	public LJustify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.length )
		};
	}

	/**
	 * Left justifies characters in a string of a specified length.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to left-justify.
	 *
	 * @argument.length The specified length of the resulting string.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		int		length	= arguments.getAsInteger( Key.length );

		// Check if the specified length is valid
		if ( length <= 0 ) {
			throw new BoxRuntimeException( "Length must be greater than 0" );
		}

		// Calculate the number of characters to pad
		int paddingCount = length - input.length();

		if ( paddingCount <= 0 ) {
			// If the input string is longer or equal to the specified length, return the original string
			return input;
		} else {
			// Create a StringBuilder to build the justified string
			StringBuilder justifiedString = new StringBuilder( input );

			// Append spaces to left-justify the string
			for ( int i = 0; i < paddingCount; i++ ) {
				justifiedString.append( " " );
			}

			return justifiedString.toString();
		}
	}
}
