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
package ortus.boxlang.runtime.bifs.global.binary;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( alias = "bitShln" )
@BoxBIF( alias = "bitShrn" )
public class BitSh extends BIF {

	/**
	 * Constructor
	 */
	public BitSh() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Performs a bitwise shift-left or shift-right, no-rotation operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number Numeric value to shift.
	 *
	 * @argument.count Number of bits to shift (Integer in the range 0-31, inclusive).
	 *
	 * @return Returns the result of the bitwise shift operation.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key	bifMethodKey	= arguments.getAsKey( BIF.__functionName );

		int	number			= arguments.getAsInteger( Key.number );
		int	count			= arguments.getAsInteger( Key.count );

		// Ensure count is within the valid range (0-31)
		count = Math.max( 0, Math.min( count, 31 ) );

		if ( bifMethodKey.equals( Key.of( "bitshln" ) ) ) {
			return number << count;
		} else if ( bifMethodKey.equals( Key.of( "bitshrn" ) ) ) {
			return number >>> count;
		}

		return null;
	}
}
