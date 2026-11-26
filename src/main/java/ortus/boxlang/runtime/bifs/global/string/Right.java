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

@BoxBIF( description = "Return the rightmost characters of a string" )
@BoxMember( type = BoxLangType.STRING_STRICT )
public class Right extends BIF {

	/**
	 * Constructor
	 */
	public Right() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Extract the rightmost characters of a string.
	 * A negative count returns all but the first |count| characters of the string; if |count| is larger than the string, the whole string is
	 * returned.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to extract from.
	 *
	 * @argument.count The number of characters to retrieve. If it is larger than the string, the whole string is returned.
	 *
	 * @throws BoxRuntimeException if the count is zero.
	 *
	 * @return The rightmost characters of the string, or an empty string if the string is null.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return right( arguments.getAsString( Key.string ), arguments.getAsInteger( Key.count ) );
	}

	public static String right( String input, int count ) {
		if ( input == null ) {
			return "";
		}

		if ( count == 0 ) {
			throw new BoxRuntimeException( "Count cannot be zero." );
		}

		if ( count > 0 ) {
			if ( count > input.length() ) {
				count = input.length();
			}
			return input.substring( input.length() - count );
		}

		int start = -count;
		if ( start > input.length() ) {
			return input;
		}
		return input.substring( start );
	}
}
