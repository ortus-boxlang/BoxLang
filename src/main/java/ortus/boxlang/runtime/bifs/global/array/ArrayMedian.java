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

import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayMedian extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMedian() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Return the median value of an array. Will only work on arrays that contain only numeric values.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.array The array to get median value from
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array			actualArray	= arguments.getAsArray( Key.array );
		List<Double>	vals		= actualArray.stream().map( ( x ) -> DoubleCaster.cast( x ) ).collect( Collectors.toList() );
		int				size		= actualArray.size();

		vals.sort( Compare::invoke );

		// length is odd
		if ( size % 2 == 1 ) {
			return vals.get( size / 2 );
		}

		int median = size / 2;

		return ( vals.get( median - 1 ) + vals.get( median ) ) / 2;
	}

}
