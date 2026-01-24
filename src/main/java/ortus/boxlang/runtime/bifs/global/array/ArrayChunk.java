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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Chunks the array into an array of arrays of the specified size" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayChunk extends BIF {

	/**
	 * Constructor
	 */
	public ArrayChunk() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, Argument.INTEGER, Key.length )
		};
	}

	/**
	 * Chunks the array into an array of arrays of the specified size.
	 * The final chunk may be shorter if the array does not divide evenly.
	 *
	 * <pre>
	 * numbers = [ 1, 2, 3, 4, 5 ];
	 * numbers.chunk( 2 ); // [ [ 1, 2 ], [ 3, 4 ], [ 5 ] ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to chunk.
	 *
	 * @argument.length The size of each chunk.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		int		length			= arguments.getAsInteger( Key.length );
		Array	a				= arguments.getAsArray( Key.array );
		int		currentCount	= 1;
		Array	results			= Array.of();
		Array	chunked			= Array.of();
		for ( int i = 0; i < a.size(); i++ ) {
			if ( currentCount > length ) {
				results.add( chunked );
				chunked			= Array.of();
				currentCount	= 1;
			}
			chunked.add( a.get( i ) );
			currentCount++;
		}
		if ( chunked.size() != 0 ) {
			results.add( chunked );
		}
		return results;
	}
}
