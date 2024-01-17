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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class RepeatString extends BIF {

	/**
	 * Constructor
	 */
	public RepeatString() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Create a string that contains a specified number of repetitions of the specified string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to repeat.
	 * 
	 * @argument.count The number of times to repeat the string.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		int		count	= arguments.getAsInteger( Key.count );

		if ( count < 0 ) {
			throw new BoxRuntimeException( "Count must be a non-negative number" );
		}

		if ( count == 0 ) {
			return "";
		}

		StringBuilder repeatedString = new StringBuilder();
		for ( int i = 0; i < count; i++ ) {
			repeatedString.append( input );
		}

		return repeatedString.toString();
	}
}
