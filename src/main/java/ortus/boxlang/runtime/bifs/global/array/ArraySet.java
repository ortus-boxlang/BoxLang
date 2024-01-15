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

package ortus.boxlang.runtime.bifs.global.array;

import java.util.stream.IntStream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )

public class ArraySet extends BIF {

	/**
	 * Constructor
	 */
	public ArraySet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "any", Key.start ),
		    new Argument( true, "any", Key.end ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * In a one-dimensional array, sets the elements in a specified
	 * index range to a value. Useful for initializing an array after
	 * a call to arrayNew.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualObj	= arguments.getAsArray( Key.array );
		Integer	start		= arguments.getAsInteger( Key.start );
		Integer	end			= arguments.getAsInteger( Key.end );
		Object	value		= arguments.get( Key.value );

		// expand the array using null values
		if ( end > actualObj.size() ) {
			IntStream.range( actualObj.size() - 1, end - 1 ).forEach( i -> actualObj.add( null ) );
		}

		IntStream.range( start, end + 1 ).forEach( i -> {
			if ( i - 1 >= actualObj.size() ) {
				actualObj.add( value );
			} else {
				actualObj.set( i - 1, value );
			}
		} );

		return true;
	}

}
