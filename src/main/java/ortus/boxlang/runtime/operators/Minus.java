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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math minus
 * {@code a = b - c}
 */
public class Minus implements IOperator {

	// Define the maximum and minimum safe values for long
	private static final long	MAX_SAFE_LONG	= 4_611_686_018_427_387_903L;
	private static final long	MIN_SAFE_LONG	= -4_611_686_018_427_387_903L;

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The result
	 */
	public static Number invoke( Object left, Object right ) {
		Number	nLeft	= NumberCaster.cast( left );
		Number	nRight	= NumberCaster.cast( right );
		// A couple shortcuts-- if both operands are integers or longs within a certain range, we can just subtract them safely
		if ( nLeft instanceof Integer li ) {
			if ( nRight instanceof Integer ri ) {
				// Check if the result will overflow an int
				long result = ( long ) li - ( long ) ri;
				if ( result > Integer.MAX_VALUE || result < Integer.MIN_VALUE ) {
					return result; // Return as long if it overflows
				} else {
					return ( int ) result; // Return as int if it doesn't overflow
				}
			}
			if ( nRight instanceof Long rl && rl <= MAX_SAFE_LONG && rl >= MIN_SAFE_LONG ) {
				return ( long ) li - rl;
			}
		}
		if ( nRight instanceof Integer ri ) {
			if ( nLeft instanceof Long ll && ll <= MAX_SAFE_LONG && ll >= MIN_SAFE_LONG ) {
				return ll - ( long ) ri;
			}
		}

		// Track if either operand is a BigDecimal so we don't have to cast them again
		boolean	leftIsBD	= false;
		boolean	rightIsBD	= false;

		// If we're using high precision math, or either operand is already a BigDecimal, we'll use BigDecimal math
		if ( MathUtil.isHighPrecisionMath() || ( leftIsBD = ( nLeft instanceof BigDecimal ) ) || ( rightIsBD = ( nRight instanceof BigDecimal ) ) ) {
			BigDecimal	bdLeft	= leftIsBD ? ( BigDecimal ) nLeft : BigDecimalCaster.cast( nLeft );
			BigDecimal	bdRight	= rightIsBD ? ( BigDecimal ) nRight : BigDecimalCaster.cast( nRight );
			return bdLeft.subtract( bdRight, MathUtil.getMathContext() );
		}

		// Otherwise, we can just subtract them as doubles
		return nLeft.doubleValue() - nRight.doubleValue();
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key
	 *
	 * @return The result
	 */
	public static Number invoke( IBoxContext context, Object target, Key name, Object right ) {
		Number result = invoke( Referencer.get( context, target, name, false ), right );
		Referencer.set( context, target, name, result );
		return result;
	}

}
