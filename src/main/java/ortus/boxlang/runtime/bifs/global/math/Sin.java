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
import java.math.MathContext;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.MathUtil;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC, name = "Sin" )
public class Sin extends BIF {

	/**
	 * Constructor
	 */
	public Sin() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the sine of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the sine of, entered in radians.
	 */
	public Number _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return _invoke( arguments.getAsNumber( Key.number ) );
	}

	/**
	 * Returns the sine of a big decimal
	 * I tried and tried, but couldn't get any of the algorithms to work for bigdecimal]
	 * so I gave up and just used the double value for now
	 * 
	 * @param x  The big decimal to calculate the sine of
	 * @param mc The math context to use
	 * 
	 * @return The sine of the big decimal
	 */
	public static BigDecimal sin( BigDecimal x, MathContext mc ) {
		return new BigDecimal( StrictMath.sin( x.doubleValue() ) );
	}

	public static Number _invoke( Number value ) {
		if ( value instanceof BigDecimal bd ) {
			return sin( bd, MathUtil.getMathContext() );
		}
		return StrictMath.sin( value.doubleValue() );
	}

}
