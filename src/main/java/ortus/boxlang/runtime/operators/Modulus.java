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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math Modulus
 * {@code a = b % c, or a = b mod c}
 */
public class Modulus implements IOperator {

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

			if ( right.doubleValue() == 0 ) {
				throw new BoxRuntimeException( "You cannot divide by zero." );
			}

			BigDecimal	bdLeft	= leftIsBD ? ( BigDecimal ) left : BigDecimalCaster.cast( left );
			BigDecimal	bdRight	= rightIsBD ? ( BigDecimal ) right : BigDecimalCaster.cast( right );
			return bdLeft.remainder( bdRight, MathUtil.getMathContext() );
		}

		if ( right.doubleValue() == 0 ) {
			throw new BoxRuntimeException( "You cannot divide by zero." );
		}

		return left.doubleValue() % right.doubleValue();
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
