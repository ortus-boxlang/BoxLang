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

@BoxBIF( description = "Find the position of a substring in a string" )
@BoxBIF( alias = "FindNoCase" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "Find", objectArgument = "string" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "FindNoCase", objectArgument = "string" )
public class Find extends BIF {

	/**
	 * Constructor
	 */
	public Find() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.substring ),
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "integer", Key.start, 1 )
		};
	}

	/**
	 * Finds the first occurrence of a substring in a string, from a specified start position.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.substring The string you are looking for.
	 * 
	 * @argument.string The string to search in.
	 * 
	 * @argument.start The position from which to start searching in the string. Default is 1.
	 *
	 * @return Returns the position of the first occurrence of the substring. If the substring is not found, returns zero.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key bifMethodKey = arguments.getAsKey( BIF.__functionName );

		return find( arguments.getAsString( Key.string ), arguments.getAsString( Key.substring ), arguments.getAsInteger( Key.start ),
		    bifMethodKey.equals( Key.findNoCase ) );
	}

	public static int find( String input, String substring, int start, boolean noCase ) {
		if ( input == null ) {
			return 0;
		}
		if ( start < 1 ) {
			start = 1;
		}
		int position = noCase ? input.toLowerCase().indexOf( substring.toLowerCase(), start - 1 ) + 1 : input.indexOf( substring, start - 1 ) + 1;
		return position > 0 ? position : 0;
	}
}
