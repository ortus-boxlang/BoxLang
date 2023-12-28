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

import java.util.Optional;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Performs EQ, GT, and LT comparisons
 * Compares numbers as numbers, compares strings case insensitive
 */
public class Compare implements IOperator {

	/**
	 * Invokes the comparison
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static int invoke( Object left, Object right ) {
		return invoke( left, right, false );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static Optional<Integer> attempt( Object left, Object right ) {
		return attempt( left, right, false );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	@SuppressWarnings( "unchecked" )
	public static int invoke( Object left, Object right, Boolean caseSensitive ) {
		Optional<Integer> comparison = attempt( left, right, caseSensitive );

		if ( comparison.isEmpty() ) {
			throw new BoxRuntimeException(
			    String.format( "Can't compare [%s] against [%s]", left.getClass().getName(), right.getClass().getName() )
			);
		}

		return comparison.get();
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static Optional<Integer> attempt( Object left, Object right, Boolean caseSensitive ) {
		// Two nulls are equal
		if ( left == null && right == null ) {
			return Optional.of( 0 );
		}
		// null is less than than non null
		if ( left == null && right != null ) {
			return Optional.of( -1 );
		}
		// Non null is greater than null
		if ( left != null && right == null ) {
			return Optional.of( 1 );
		}

		left	= DynamicObject.unWrap( left );
		right	= DynamicObject.unWrap( right );

		CastAttempt<Double> leftAttempt = DoubleCaster.attempt( left );
		if ( leftAttempt.wasSuccessful() ) {
			CastAttempt<Double> rightAttempt = DoubleCaster.attempt( right );

			if ( rightAttempt.wasSuccessful() ) {
				return Optional.of( leftAttempt.get().compareTo( rightAttempt.get() ) );
			}
		}

		// TODO: This is too simplistic
		if ( left instanceof String || right instanceof String ) {
			if ( caseSensitive ) {
				return Optional.of( left.toString().compareTo( right.toString() ) );
			} else {
				return Optional.of( left.toString().compareToIgnoreCase( right.toString() ) );
			}
		}

		if ( left instanceof Comparable && right instanceof Comparable ) {
			return Optional.of( ( ( Comparable<Object> ) left ).compareTo( ( Comparable<Object> ) right ) );
		}

		// TODO: Dates

		return Optional.empty();
	}

}
