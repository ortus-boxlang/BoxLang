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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "SpanIncluding" )
public class SpanIncluding extends BIF {

	/**
	 * Constructor
	 */
	public SpanIncluding() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.string, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( true, Argument.STRING, Key.set, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Gets characters from a string, from the beginning to a character that is NOT in a specified set of characters.
	 * The search is case-sensitive.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to extract from
	 *
	 * @argument.set The set of chracters to exclude from the span.
	 *
	 * @return A string; characters from string, from the beginning to a character that is not in set.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get any characters from the set that can be found in the string.
		String	input		= arguments.getAsString( Key.string );
		String	set			= arguments.getAsString( Key.set );
		int		inputLength	= input.length();
		int		i			= 0;

		// Loop through the string, looking for a character that is in the set.
		for ( i = 0; i < inputLength; i++ ) {
			if ( set.indexOf( input.charAt( i ) ) == -1 ) {
				break;
			}
		}

		// Return the substring from the beginning of the string to the character that is not in the set.
		return input.substring( 0, i );
	}
}
