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
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
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
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 *
	 * @return 1 if greater than, -1 if less than, 0 if equal
	 */
	public static int invoke( Object left, Object right, Boolean caseSensitive ) {
		return attempt( left, right, caseSensitive, true );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 * @param fail          True to throw an exception if the left and right arguments cannot be compared
	 *
	 * @return 1 if greater than, -1 if less than, 0 if equal
	 */
	public static Integer attempt( Object left, Object right, Boolean caseSensitive, boolean fail ) {
		return attempt( left, right, caseSensitive, fail, Locale.US );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 * @param fail          True to throw an exception if the left and right arguments cannot be compared
	 * @param locale        The locale to use for comparison
	 *
	 * @return 1 if greater than, -1 if less than, 0 if equal
	 */
	@SuppressWarnings( "unchecked" )
	public static Integer attempt( Object left, Object right, Boolean caseSensitive, boolean fail, Locale locale ) {
		// Two nulls are equal
		if ( left == null && right == null ) {
			return 0;
		}
		// null is less than than non null
		if ( left == null && right != null ) {
			return -1;
		}
		// Non null is greater than null
		if ( left != null && right == null ) {
			return 1;
		}

		left	= DynamicObject.unWrap( left );
		right	= DynamicObject.unWrap( right );

		// Date comparison
		if ( DateTimeCaster.isKnownDateClass( left ) || DateTimeCaster.isKnownDateClass( right ) ) {
			CastAttempt<DateTime> ref = DateTimeCaster.attempt( left );
			if ( ref.wasSuccessful() ) {
				CastAttempt<DateTime> target = DateTimeCaster.attempt( right );
				if ( target.wasSuccessful() ) {
					return ref.get().compareTo( target.get() );
				}
			}
		}

		// Numeric comparison
		CastAttempt<Number> leftAttempt = NumberCaster.attempt( left );
		if ( leftAttempt.wasSuccessful() ) {
			CastAttempt<Number> rightAttempt = NumberCaster.attempt( right );

			if ( rightAttempt.wasSuccessful() ) {
				boolean	isLeft	= leftAttempt.get() instanceof BigDecimal;
				boolean	isRight	= rightAttempt.get() instanceof BigDecimal;
				// If at least one side was a BigDecimal, we will compare as BigDecimal
				if ( isLeft || isRight ) {
					BigDecimal	bdl	= isLeft ? ( BigDecimal ) leftAttempt.get() : BigDecimalCaster.cast( leftAttempt.get() );
					BigDecimal	bdr	= isRight ? ( BigDecimal ) rightAttempt.get() : BigDecimalCaster.cast( rightAttempt.get() );
					return bdl.compareTo( bdr );
				}
				// Otherwise, we will compare as Double
				return Double.valueOf( leftAttempt.get().doubleValue() ).compareTo( rightAttempt.get().doubleValue() );
			}
		}

		// Check boolean
		CastAttempt<Boolean> leftBooleanAttempt = BooleanCaster.attempt( left, false );
		if ( leftBooleanAttempt.wasSuccessful() ) {
			CastAttempt<Boolean> rightBooleanAttempt = BooleanCaster.attempt( right, false );

			if ( rightBooleanAttempt.wasSuccessful() ) {
				return Boolean.compare( leftBooleanAttempt.get(), rightBooleanAttempt.get() );
			}
		}

		// String comparison
		if ( left instanceof String || right instanceof String ) {
			if ( !caseSensitive ) {
				left	= StringUtils.lowerCase( left.toString(), locale );
				right	= StringUtils.lowerCase( right.toString(), locale );
			}

			return StringCompare.invoke( StringCaster.cast( left ), StringCaster.cast( right ), caseSensitive );

		}

		// Fallback, see if both objects are comparable
		if ( left instanceof Comparable && right instanceof Comparable
		    && ( left.getClass().isAssignableFrom( right.getClass() ) || right.getClass().isAssignableFrom( left.getClass() ) ) ) {
			return caseSensitive
			    && left instanceof Key keyLeft
			    && right instanceof Key keyRight
			        ? keyLeft.compareToWithCase( keyRight )
			        : ( ( Comparable<Object> ) left ).compareTo( ( Comparable<Object> ) right );
		}

		if ( fail ) {
			throw new BoxRuntimeException(
			    String.format( "Can't compare [%s] against [%s]", left.getClass().getName(), right.getClass().getName() )
			);
		}

		return null;
	}

}
