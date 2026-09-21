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
import ortus.boxlang.runtime.dynamic.casters.SetCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math Multiply, with overloads for set intersection when both operands are {@link BoxSet}.
 * {@code a = b * c}
 */
public class Multiply implements IOperator {

	/**
	 * Generic dispatch: returns a {@link BoxSet} (intersection) when either operand is a {@link BoxSet}
	 * and the other coerces to one; otherwise delegates to numeric multiplication.
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The product (Number) for numeric operands, or a new {@link BoxSet} for set operands.
	 */
	public static Object invoke( Object left, Object right ) {
		if ( left instanceof BoxSet bsl ) {
			var rs = SetCaster.attemptLoose( right );
			if ( rs.wasSuccessful() ) {
				return bsl.intersection( rs.get() );
			}
		} else if ( right instanceof BoxSet bsr ) {
			var ls = SetCaster.attemptLoose( left );
			if ( ls.wasSuccessful() ) {
				return ls.get().intersection( bsr );
			}
		}
		return invoke( NumberCaster.cast( true, true, left ), NumberCaster.cast( true, true, right ) );
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
			return bdLeft.multiply( bdRight, MathUtil.getMathContext() );
		}

		return left.doubleValue() * right.doubleValue();
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key.
	 * Returns Object since set-on-set multiplication produces a {@link BoxSet} (intersection).
	 *
	 * @return The result
	 */
	public static Object invoke( IBoxContext context, Object target, Key name, Object right ) {
		Object result = invoke( context.unwrapQueryColumn( Referencer.get( context, target, name, false ) ), right );
		Referencer.set( context, target, name, result );
		return result;
	}

}
