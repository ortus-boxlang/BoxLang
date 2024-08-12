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
import java.math.RoundingMode;

import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math Integer Division. Remainder is discarded
 * {@code a = b \ c}
 */
public class IntegerDivide implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The the result
	 */
	public static Number invoke( Object left, Object right ) {

		// First turn the operands into numbers
		Number	nLeft		= NumberCaster.cast( left );
		Number	nRight		= NumberCaster.cast( right );

		// Track if either operand is a BigDecimal so we don't have to cast them again
		boolean	leftIsBD	= false;
		boolean	rightIsBD	= false;

		// If we're using high precision math, or either operand is already a BigDecimal, we'll use BigDecimal math
		if ( MathUtil.isHighPrecisionMath() || ( leftIsBD = ( nLeft instanceof BigDecimal ) ) || ( rightIsBD = ( nRight instanceof BigDecimal ) ) ) {

			BigDecimal	bdLeft	= leftIsBD ? ( BigDecimal ) nLeft : BigDecimalCaster.cast( nLeft );
			BigDecimal	bdRight	= rightIsBD ? ( BigDecimal ) nRight : BigDecimalCaster.cast( nRight );

			bdLeft	= bdLeft.setScale( 0, RoundingMode.FLOOR );
			bdRight	= bdRight.setScale( 0, RoundingMode.FLOOR );

			if ( bdRight.doubleValue() == 0 ) {
				throw new BoxRuntimeException( "You cannot divide by zero." );
			}
			return bdLeft.divideToIntegralValue( bdRight, MathUtil.getMathContext() );
		}

		if ( nRight.doubleValue() == 0 ) {
			throw new BoxRuntimeException( "You cannot divide by zero." );
		}

		// Otherwise, we can just divide them
		return nLeft.intValue() / nRight.intValue();
	}

}
