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
@BoxMember( type = BoxLangType.STRING, name = "StripCR" )
public class StripCR extends BIF {

	/**
	 * Constructor
	 */
	public StripCR() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string )
		};
	}

	/**
	 * Deletes return characters from a string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string or variable that contains the text.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			inputString		= arguments.getAsString( Key.string );
		StringBuilder	strippedString	= new StringBuilder( inputString.length() );

		int				currentPos		= 0;
		int				carriageReturnPos;

		while ( ( carriageReturnPos = inputString.indexOf( '\r', currentPos ) ) != -1 ) {
			strippedString.append( inputString, currentPos, carriageReturnPos );
			currentPos = carriageReturnPos + 1;
		}

		if ( currentPos < inputString.length() ) {
			strippedString.append( inputString, currentPos, inputString.length() );
		}

		return strippedString.toString();
	}
}
