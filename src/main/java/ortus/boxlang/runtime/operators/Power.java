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
package ortus.boxlang.runtime.operators;

import java.math.BigDecimal;

import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math Power for BoxLang
 * {@code a = 2 ^ 3}
 */
public class Power implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The the result
	 */
	public static Number invoke( Object left, Object right ) {
		return invoke( NumberCaster.cast( left ), NumberCaster.cast( right ) );
	}

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The the result
	 */
	public static Number invoke( Number left, Number right ) {
		boolean	leftIsBD	= false;
		boolean	rightIsBD	= false;

		if ( MathUtil.isHighPrecisionMath() || ( leftIsBD = ( left instanceof BigDecimal ) ) || ( rightIsBD = ( right instanceof BigDecimal ) ) ) {
			BigDecimal	bdLeft	= leftIsBD ? ( BigDecimal ) left : BigDecimalCaster.cast( left );
			BigDecimal	bdRight	= rightIsBD ? ( BigDecimal ) right : BigDecimalCaster.cast( right );
			if ( bdRight.stripTrailingZeros().scale() <= 0 ) {
				return bdLeft.pow( bdRight.intValueExact(), MathUtil.getMathContext() );
			} else {
				BigDecimal	logBase	= new BigDecimal( Math.log( bdLeft.doubleValue() ), MathUtil.getMathContext() );
				BigDecimal	result	= new BigDecimal( Math.exp( logBase.multiply( bdRight ).doubleValue() ), MathUtil.getMathContext() );
				return result;
			}
		}

		return Math.pow( left.doubleValue(), right.doubleValue() );
	}

}
