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

@BoxBIF
public class BitShln extends BIF {

	/**
	 * Constructor
	 */
	public BitShln() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Performs a bitwise shift-left, no-rotation operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number Numeric value to shift left.
	 *
	 * @argument.count Number of bits to shift to the left (Integer in the range 0-31, inclusive).
	 *
	 * @return Returns the result of the bitwise shift-left operation.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		int	number	= arguments.getAsInteger( Key.number );
		int	count	= arguments.getAsInteger( Key.count );

		// Ensure count is within the valid range (0-31)
		count = Math.max( 0, Math.min( count, 31 ) );

		return number << count;
	}
}
