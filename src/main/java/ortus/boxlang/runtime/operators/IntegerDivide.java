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
	public static BigDecimal invoke( Object left, Object right ) {
		BigDecimal	leftBigDecimal	= BigDecimalCaster.cast( left );
		BigDecimal	rightBigDecimal	= BigDecimalCaster.cast( right );

		BigDecimal	flooredLeft		= leftBigDecimal.setScale( 0, RoundingMode.FLOOR );
		BigDecimal	flooredRight	= rightBigDecimal.setScale( 0, RoundingMode.FLOOR );

		if ( flooredRight.doubleValue() == 0 ) {
			throw new BoxRuntimeException( "You cannot divide by zero." );
		}

		return flooredLeft.divideToIntegralValue( flooredRight, MathUtil.getMathContext() );
	}

}
