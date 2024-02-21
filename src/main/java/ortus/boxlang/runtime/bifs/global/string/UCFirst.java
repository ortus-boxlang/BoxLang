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
@BoxMember( type = BoxLangType.STRING, name = "UCFirst" )
public class UCFirst extends BIF {

	/**
	 * Constructor
	 */
	public UCFirst() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "boolean", Key.doAll, false ),
		    new Argument( false, "boolean", Key.doLowerIfAllUppercase, false )
		};
	}

	/**
	 * Transform the first letter of a string to uppercase or the first letter of each word, and optionally lowercase uppercase characters.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to transform.
	 * 
	 * @argument.doAll Boolean flag indicating whether to transform the first letter of each word.
	 * 
	 * @argument.doLowerIfAllUppercase Boolean flag indicating whether to lowercase uppercase characters.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input					= arguments.getAsString( Key.string );
		boolean	doAll					= arguments.getAsBoolean( Key.doAll );
		boolean	doLowerIfAllUppercase	= arguments.getAsBoolean( Key.doLowerIfAllUppercase );

		if ( input.isEmpty() ) {
			return "";
		}

		StringBuilder	result	= new StringBuilder();
		String[]		words	= input.split( "\\s+" );

		for ( int i = 0; i < words.length; i++ ) {
			String word = words[ i ];

			if ( doLowerIfAllUppercase && word.toUpperCase().equals( word ) ) {
				word = word.toLowerCase();
			}

			if ( doAll || i == 0 ) {
				char	firstChar		= word.charAt( 0 );
				char	upperFirstChar	= Character.toUpperCase( firstChar );
				word = upperFirstChar + word.substring( 1 );
			}

			result.append( word );

			if ( i < words.length - 1 ) {
				result.append( " " );
			}
		}

		return result.toString();
	}
}
