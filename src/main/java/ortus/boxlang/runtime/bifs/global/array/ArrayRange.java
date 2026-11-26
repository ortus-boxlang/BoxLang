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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.BLCollector;

@BoxBIF( description = "Create an array with a range of values" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayRange extends BIF {

	/**
	 * Constructor
	 */
	public ArrayRange() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.ANY, Key.from, 1 ),
		    new Argument( false, Argument.NUMERIC, Key.to )
		};
	}

	/**
	 * Builds an array of consecutive integers from a start value to an end value (inclusive).
	 * The range can be given with the range syntax <code>"{start}..{end}"</code> in the from argument, or with the from and to arguments.
	 * Negative ranges are supported.
	 * If to is omitted it defaults to from, and if to is less than from it is set to from, so the result always has at least one element.
	 * <pre>
	 * arrayRange( "1..5" ) // [ 1, 2, 3, 4, 5 ]
	 * arrayRange( "-2..2" ) // [ -2, -1, 0, 1, 2 ]
	 * arrayRange( 1, 3 ) // [ 1, 2, 3 ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.from The first value of the range (defaults to 1), or a range string in the form "start..end".
	 *
	 * @argument.to The last value of the range. Defaults to the from value.
	 *
	 * @return An array of the integers in the range.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	incomingFrom	= arguments.get( Key.from );
		Object	incomingTo		= arguments.get( Key.to );
		Integer	from			= 1;
		Integer	to				= null;

		// shortcut notation: 1..5
		if ( incomingFrom instanceof String castedFrom && castedFrom.contains( ".." ) ) {
			String[] range = castedFrom.split( "\\.{2}" );
			from	= IntegerCaster.cast( range[ 0 ] );
			to		= range.length > 1 ? IntegerCaster.cast( range[ 1 ] ) : from;
		} else {
			from	= IntegerCaster.cast( incomingFrom );
			to		= incomingTo != null ? IntegerCaster.cast( incomingTo ) : from;
		}

		// Cap to if larger than from
		if ( to < from ) {
			to = from;
		}

		return IntStream
		    .rangeClosed( from, to )
		    .boxed()
		    .collect( BLCollector.toArray() );
	}

}
