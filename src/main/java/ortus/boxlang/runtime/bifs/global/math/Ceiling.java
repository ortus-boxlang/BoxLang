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

package ortus.boxlang.runtime.bifs.global.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
public class Ceiling extends BIF {

	/**
	 * Constructor
	 */
	public Ceiling() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Determines the closest integer that is greater than a specified floating point number.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number for which to find the ceiling value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return _invoke( arguments.getAsNumber( Key.number ) );
	}

	public static Number _invoke( Number num ) {
		if ( num instanceof BigDecimal bd ) {
			return bd.setScale( 0, RoundingMode.CEILING );
		}
		return StrictMath.ceil( num.doubleValue() );
	}
}
