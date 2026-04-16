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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Remove trailing whitespace from a string, or specific characters if provided" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "RTrim" )
public class RTrim extends BIF {

	/**
	 * Constructor
	 */
	public RTrim() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "string", Key.chars ),
		};
	}

	/**
	 * Trim trailing whitespace from a string.
	 * If chars is provided, each character in the string is treated as a character to trim instead of whitespace.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to trim
	 *
	 * @argument.chars An optional string of characters to trim. Each character is treated individually.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		String	chars	= arguments.getAsString( Key.chars );

		int		end		= input.length();

		if ( chars == null ) {
			// Default path: trim whitespace
			while ( end > 0 && Character.isWhitespace( input.charAt( end - 1 ) ) ) {
				end--;
			}
		} else {
			// Custom chars path: trim specified characters
			while ( end > 0 && chars.indexOf( input.charAt( end - 1 ) ) >= 0 ) {
				end--;
			}
		}

		return end < input.length() ? input.substring( 0, end ) : input;
	}
}
