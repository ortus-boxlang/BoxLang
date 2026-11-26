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

@BoxBIF( description = "Left or right justify text within a specified length" )
@BoxBIF( alias = "LJustify" )
@BoxBIF( alias = "RJustify" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "LJustify" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "RJustify" )

public class Justify extends BIF {

	/**
	 * Constructor
	 */
	public Justify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.length )
		};
	}

	/**
	 * Left or right justify a string within a field of a specified length by padding it with spaces. <code>LJustify</code> keeps the text on
	 * the left and pads on the right, while <code>RJustify</code> keeps the text on the right and pads on the left.
	 * Both are also available as string member functions.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to justify.
	 *
	 * @argument.length The length of the resulting string. If the string is already this long or longer, it is returned unchanged.
	 *
	 * @throws BoxRuntimeException if the length is zero or negative.
	 *
	 * @return The space-padded string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );

		String	input			= arguments.getAsString( Key.string );
		int		length			= arguments.getAsInteger( Key.length );

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
			StringBuilder justifiedString;
			// Create a StringBuilder to build the justified string
			if ( bifMethodKey.equals( Key.lJustify ) ) {
				justifiedString = new StringBuilder( input );
			} else if ( bifMethodKey.equals( Key.rJustify ) ) {
				justifiedString = new StringBuilder();
			} else {
				throw new BoxRuntimeException( "Invalid BIF method key" );
			}

			// Append spaces to left-justify the string
			for ( int i = 0; i < paddingCount; i++ ) {
				justifiedString.append( " " );
			}

			if ( bifMethodKey.equals( Key.rJustify ) ) {
				// Append the input string to right-justify the string
				justifiedString.append( input );
			}

			return justifiedString.toString();
		}
	}
}
