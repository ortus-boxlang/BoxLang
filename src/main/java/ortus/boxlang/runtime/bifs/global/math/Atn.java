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
public class Atn extends BIF {

	/**
	 * Constructor
	 */
	public Atn() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the arc tangent (inverse tangent) of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.number The number to calculate the arc tangent of
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return _invoke( arguments.getAsNumber( Key.number ) );
	}

	/**
	 * Calculate the arc tangent of a BigDecimal using the Taylor series expansion.
	 *
	 * @param x  The BigDecimal to calculate the arc tangent of.
	 * @param mc The MathContext to use for the calculation.
	 * 
	 * @return The arc tangent of x.
	 */
	private static BigDecimal atan( BigDecimal x, MathContext mc ) {
		BigDecimal	result		= BigDecimal.ZERO;
		BigDecimal	term		= x;
		BigDecimal	xSquared	= x.multiply( x, mc );
		int			n			= 1;
		BigDecimal	threshold	= BigDecimal.ONE.scaleByPowerOfTen( -mc.getPrecision() );

		while ( term.abs().compareTo( threshold ) > 0 ) {
			if ( n % 2 != 0 ) {
				result = result.add( term, mc );
			} else {
				result = result.subtract( term, mc );
			}
			term = term.multiply( xSquared, mc ).divide( BigDecimal.valueOf( 2 * n + 1 ), mc );
			n++;
		}

		return result;
	}

	public static Number _invoke( Number num ) {
		if ( num instanceof BigDecimal bd ) {
			return atan( bd, MathUtil.getMathContext() );
		}
		return StrictMath.atan( num.doubleValue() );
	}
}
