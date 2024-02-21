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
@BoxMember( type = BoxLangType.STRING, name = "LTrim" )
public class LTrim extends BIF {

	/**
	 * Constructor
	 */
	public LTrim() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		};
	}

	/**
	 * Trim leading whitespace from a string
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to trim
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input		= arguments.getAsString( Key.string );
		int		startIndex	= 0;

		// Find the index of the first non-whitespace character
		while ( startIndex < input.length() && Character.isWhitespace( input.charAt( startIndex ) ) ) {
			startIndex++;
		}

		// Return the substring starting from the first non-whitespace character
		return startIndex < input.length() ? input.substring( startIndex ) : "";
	}
}
