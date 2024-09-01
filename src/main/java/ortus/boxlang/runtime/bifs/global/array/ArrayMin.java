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
import ortus.boxlang.runtime.bifs.global.math.Min;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayMin extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMin() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.array )
		};
	}

	/**
	 * Return length of array
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.array The array to get min value from
	 */
	public Number _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Number	min			= 0;
		if ( actualArray.size() > 0 ) {
			min = NumberCaster.cast( actualArray.get( 0 ) );
		}
		for ( int i = 1; i < actualArray.size(); i++ ) {
			min = Min._invoke( min, NumberCaster.cast( actualArray.get( i ) ) );
		}
		return min;
	}

}
