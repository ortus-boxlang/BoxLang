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
public class BitMaskClear extends BIF {

	/**
	 * Constructor
	 */
	public BitMaskClear() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number ),
		    new Argument( true, "integer", Key.start ),
		    new Argument( true, "integer", Key.length )
		};
	}

	/**
	 * Performs a bitwise mask clear operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number 32-bit signed integer on which the mask clear operation is performed.
	 * 
	 * @argument.start Start bit for the clear mask (Integer in the range 0-31, inclusive).
	 * 
	 * @argument.length Length of bits in the clear mask (Integer in the range 0-31, inclusive).
	 *
	 * @throws BoxRuntimeException If length or start is not in the range 0-31, inclusive.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		int	number	= arguments.getAsInteger( Key.number );
		int	start	= arguments.getAsInteger( Key.start );
		int	length	= arguments.getAsInteger( Key.length );

		if ( start < 0 || start > 31 ) {
			throw new BoxRuntimeException( "Start must be in the range 0-31, inclusive." );
		}

		if ( length < 0 || length > 31 ) {
			throw new BoxRuntimeException( "Length must be in the range 0-31, inclusive." );
		}

		// Create a bitmask with 'length' consecutive 1 bits starting from position 'start'
		int bitmask = ( 1 << length ) - 1 << start;

		// Perform a bitwise mask clear operation on 'number' using the created bitmask
		return number & ~bitmask;
	}
}
