
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

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "FindOneOf", objectArgument = "string" )

public class FindOneOf extends BIF {

	/**
	 * Constructor
	 */
	public FindOneOf() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.set ),
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "integer", Key.start, 1 )
		};
	}

	/**
	 * Finds the first occurrence of any character in a set of characters, from a specified start position.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set of characters to search for the first occurrence of.
	 *
	 * @argument.string The string to search in.
	 *
	 * @argument.start The position from which to start searching in the string. Default is 1.
	 *
	 * @return Returns the position of the first occurrence of the set. If the set is not found, returns zero.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	set		= arguments.getAsString( Key.set );
		String	input	= arguments.getAsString( Key.string );
		int		start	= arguments.getAsInteger( Key.start );

		if ( start < 1 ) {
			// Adjust start to 1 if it's less than 1
			start = 1;
		}

		String	ref		= input.substring( start - 1 );

		int		index	= StringUtils.indexOfAny( ref, set ) + start - 1;

		return index >= 0 ? index + 1 : 0;
	}

}
