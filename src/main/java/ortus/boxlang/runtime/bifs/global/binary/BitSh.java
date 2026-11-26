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

@BoxBIF( description = "Perform bitwise shift operations (left or right)" )
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
	 * Performs a bitwise shift (no rotation) on a 32-bit integer. It is registered through two aliases:
	 * <ul>
	 * <li><code>bitShln( number, count )</code> shifts the bits left, filling with zeros (the result wraps as a 32-bit signed integer).</li>
	 * <li><code>bitShrn( number, count )</code> shifts the bits right with zero fill (an unsigned shift, so negative numbers
	 * become large positive numbers).</li>
	 * </ul>
	 * Calling the <code>bitSh</code> name directly does not perform a shift and returns null. The count is limited to the range 0-31.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number 32-bit signed integer value to shift.
	 *
	 * @argument.count Number of bits to shift. Values outside 0-31 are limited to that range.
	 *
	 * @return The result of the bitwise shift operation as a 32-bit integer.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
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
