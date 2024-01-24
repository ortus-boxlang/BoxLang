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
package ortus.boxlang.runtime.bifs.global.binary;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class BitMaskSet extends BIF {

	/**
	 * Constructor
	 */
	public BitMaskSet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number ),
		    new Argument( true, "integer", Key.mask ),
		    new Argument( true, "integer", Key.start ),
		    new Argument( true, "integer", Key.length )
		};
	}

	/**
	 * Performs a bitwise mask set operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number Numeric value for the bitwise mask set.
	 * 
	 * @argument.mask Numeric value for the mask.
	 * 
	 * @argument.start Start bit for the set mask (Integer in the range 0-31, inclusive).
	 * 
	 * @argument.length Length of bits in the set mask (Integer in the range 0-31, inclusive).
	 *
	 * @throws BoxRuntimeException If length or start is not in the range 0-31, inclusive.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		int	number	= arguments.getAsInteger( Key.number );
		int	mask	= arguments.getAsInteger( Key.mask );
		int	start	= arguments.getAsInteger( Key.start );
		int	length	= arguments.getAsInteger( Key.length );

		if ( start < 0 || start > 31 ) {
			throw new BoxRuntimeException( "Start must be in the range 0-31, inclusive." );
		}

		if ( length < 0 || length > 31 ) {
			throw new BoxRuntimeException( "Length must be in the range 0-31, inclusive." );
		}

		// Create a bitmask with 'length' consecutive 1 bits starting from position 0
		int bitmask = ( ( 1 << length ) - 1 ) << start;

		// Ensure 'mask' only contains 'length' bits by performing a bitwise AND with a bitmask
		mask &= ( 1 << length ) - 1;

		// Perform a bitwise mask set operation on 'number' using the created bitmask and adjusted 'mask'
		return ( number & ~bitmask ) | ( mask << start );
	}
}
