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
// TODO: Remove this after tranpiler is in place
@BoxBIF( alias = "chr" )
public class Char extends BIF {

	/**
	 * Constructor
	 */
	public Char() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number )
		};
	}

	/**
	 * Convert a numeric UCS-2 code to a character
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The UCS-2 code value to convert.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		int code = arguments.getAsInteger( Key.number );

		// Check if the code is within the valid range
		if ( code < 0 ) {
			throw new BoxRuntimeException( "Code must be a non-negative number" );
		} else if ( code >= 0 && code <= 65535 ) {
			// Convert the numeric UCS-2 code to a character
			return String.valueOf( ( char ) code );
		} else {
			return ""; // Return an empty string for invalid codes
		}
	}
}
