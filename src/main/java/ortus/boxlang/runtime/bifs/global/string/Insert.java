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

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Insert", objectArgument = "string" )
public class Insert extends BIF {

	/**
	 * Constructor
	 */
	public Insert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.substring ),
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.position )
		};
	}

	/**
	 * Inserts a substring into another string at a specified position.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.substring The string to insert.
	 * 
	 * @argument.originalString The original string.
	 * 
	 * @argument.position The position at which to insert the string.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	substring		= arguments.getAsString( Key.substring );
		String	originalString	= arguments.getAsString( Key.string );
		int		position		= arguments.getAsInteger( Key.position );

		if ( substring == null || originalString == null ) {
			throw new BoxRuntimeException( "Both substring and originalString must be non-null" );
		}

		if ( position < 0 || position > originalString.length() ) {
			throw new BoxRuntimeException( "Position must be within the range [0, " + originalString.length() + "]" );
		}

		StringBuilder result = new StringBuilder( originalString );
		result.insert( position, substring );

		return result.toString();
	}
}
