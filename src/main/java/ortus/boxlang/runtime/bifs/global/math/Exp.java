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
public class Exp extends BIF {

	/**
	 * Constructor
	 */
	public Exp() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Calculates the exponent whose base is e that represents a number.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the exponent for.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number value = arguments.getAsNumber( Key.number );
		if ( value instanceof BigDecimal bd ) {
			return exp( bd, MathUtil.getMathContext() );
		}
		return StrictMath.exp( value.doubleValue() );
	}

	/**
	 * Calculates the exponent of a BigDecimal using Taylor series expansion.
	 *
	 * @param x  The BigDecimal value to calculate the exponent for.
	 * @param mc The MathContext to control the precision.
	 * 
	 * @return The exponent of the BigDecimal.
	 */
	public static BigDecimal exp( BigDecimal x, MathContext mc ) {
		BigDecimal	result		= BigDecimal.ONE;
		BigDecimal	term		= BigDecimal.ONE;
		BigDecimal	n			= BigDecimal.ONE;
		BigDecimal	threshold	= new BigDecimal( "1E-10" );

		while ( term.abs().compareTo( threshold ) > 0 ) {
			term	= term.multiply( x, mc ).divide( n, mc );
			result	= result.add( term, mc );
			n		= n.add( BigDecimal.ONE );
		}

		return result;
	}
}
