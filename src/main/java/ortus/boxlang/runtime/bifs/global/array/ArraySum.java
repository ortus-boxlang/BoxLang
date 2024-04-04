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
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySum extends BIF {

	/**
	 * Constructor
	 */
	public ArraySum() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Returns the sum of all values in an array
	 *
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		return ArraySum._invoke( actualArray );
	}

	/**
	 * Return length of array
	 *
	 * @param array the array
	 */
	public static double _invoke( Array array ) {
		double accumulator = 0;
		for ( int i = 0; i < array.size(); i++ ) {
			accumulator += DoubleCaster.cast( array.get( i ) );
		}
		return accumulator;
	}

}
