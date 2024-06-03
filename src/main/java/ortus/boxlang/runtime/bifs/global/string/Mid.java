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
@BoxMember( type = BoxLangType.STRING, name = "Mid" )
public class Mid extends BIF {

	/**
	 * Constructor
	 */
	public Mid() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.start ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Extract a substring from a string
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to extract from
	 *
	 * @argument.start The position of the first character to retrieve.
	 *
	 * @argument.count The number of characters to retrieve.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		int		start	= arguments.getAsInteger( Key.start );
		int		count	= arguments.getAsInteger( Key.count );

		// Check if start and count are within valid bounds
		if ( start < 1 ) {
			start = 1; // Adjust start to 1 if it's less than 1
		}

		if ( count < 1 ) {
			count = 0; // Set count to 0 if it's less than 1
		}

		// Calculate end position
		int end = start + count - 1;

		// Check if start is within the valid range
		if ( start <= input.length() ) {
			// Ensure end doesn't exceed the length of the input string
			if ( end > input.length() ) {
				end = input.length() - 1;
			}

			// Extract the substring
			return input.substring( start - 1, end );
		} else {
			return ""; // Return an empty string if start is out of bounds
		}
	}
}
