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
@BoxMember( type = BoxLangType.NUMERIC, name = "Round" )
public class Round extends BIF {

	/**
	 * Constructor
	 */
	public Round() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number ),
		    new Argument( false, "integer", Key.precision, 0 )
		};
	}

	/**
	 * Rounds a number to the closest integer.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to be rounded.
	 * 
	 * @argument.precision The number of decimal places to round to (default is 0).
	 */
	public Number _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number	value		= arguments.getAsNumber( Key.number );
		int		precision	= arguments.getAsInteger( Key.precision );

		if ( value instanceof BigDecimal bd ) {
			return bd.setScale( precision, RoundingMode.HALF_UP );
		}
		double scale = Math.pow( 10, precision );
		return Math.round( value.doubleValue() * scale ) / scale;
	}
}
