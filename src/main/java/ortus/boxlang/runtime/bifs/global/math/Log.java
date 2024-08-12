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
@BoxMember( type = BoxLangType.NUMERIC )
public class Log extends BIF {

	/**
	 * Constructor
	 */
	public Log() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the natural logarithm of a number.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the natural logarithm of
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number number = arguments.getAsNumber( Key.number );
		if ( number instanceof BigDecimal bd ) {
			return log( bd, MathUtil.getMathContext() );
		}
		return StrictMath.log( number.doubleValue() );
	}

	/**
	 * Computes the natural logarithm of a BigDecimal using Newton's method.
	 *
	 * @param value The BigDecimal value to compute the logarithm for.
	 * @param mc    The MathContext to control the precision.
	 * 
	 * @return The natural logarithm of the value.
	 */
	public static BigDecimal log( BigDecimal value, MathContext mc ) {
		if ( value.compareTo( BigDecimal.ZERO ) <= 0 ) {
			throw new ArithmeticException( "Logarithm of non-positive value" );
		}

		// Initial guess
		BigDecimal	x				= BigDecimal.valueOf( Math.log( value.doubleValue() ) );
		BigDecimal	tolerance		= BigDecimal.ONE.scaleByPowerOfTen( -mc.getPrecision() );
		int			maxIterations	= 1000; // Maximum number of iterations
		int			iteration		= 0;

		while ( iteration < maxIterations ) {
			BigDecimal	exp		= Exp.exp( x, mc );
			BigDecimal	diff	= value.subtract( exp, mc );
			if ( diff.abs().compareTo( tolerance ) < 0 ) {
				break;
			}
			x = x.add( diff.divide( exp, mc ), mc );
			iteration++;
		}

		// Return the best approximation after maxIterations
		return x;
	}
}
