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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.util.MathUtil;

@BoxBIF
public class RandRange extends BIF {

	/**
	 * Constructor
	 */
	public RandRange() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.NUMERIC, Key.number1 ),
		    new Argument( true, Argument.NUMERIC, Key.number2 ),
		    new Argument( false, Argument.STRING, Key.algorithm )
		};
	}

	/**
	 * Return a random int between number1 and number 2
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 */
	public Number _invoke( IBoxContext context, ArgumentsScope arguments ) {

		Number	number1		= arguments.getAsNumber( Key.number1 );
		Number	number2		= arguments.getAsNumber( Key.number2 );
		Long	seed		= context.getAttachment( Key.bxRandomSeed );
		String	algorithm	= arguments.getAsString( Key.algorithm );

		boolean	isNumber1	= number1 instanceof BigDecimal;
		boolean	isNumber2	= number2 instanceof BigDecimal;
		// If at least one side was a BigDecimal, we will compare as BigDecimal
		if ( isNumber1 || isNumber2 ) {
			BigDecimal	bdl	= isNumber1 ? ( BigDecimal ) number1 : BigDecimalCaster.cast( number1 );
			BigDecimal	bdr	= isNumber2 ? ( BigDecimal ) number2 : BigDecimalCaster.cast( number2 );
			return bdl.add( new BigDecimal( Rand._invoke( algorithm, seed ), MathUtil.getMathContext() ).multiply( bdr.subtract( bdl ) ) ).setScale( 0,
			    RoundingMode.DOWN );
		}

		// For integer values, ensure the range is inclusive of the top number
		long	lower	= number1.longValue();
		long	upper	= number2.longValue();
		return lower + ( long ) ( Rand._invoke( algorithm, seed ) ) * ( upper - lower + 1 );
	}

}
