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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * Performs EQ, GT, and LT comparisons
 * Compares numbers as numbers, compares strings case insensitive
 */
public class Compare implements IOperator {

	private static BoxRuntime	runtime					= BoxRuntime.getInstance();

	/**
	 * Flag to allow compat to set the comparison mode for dates to loose
	 */
	public static boolean		lenientDateComparison	= false;

	/**
	 * null is equal to empty string
	 */
	public static boolean		nullEqualsEmptyString	= false;

	/**
	 * Invokes the comparison
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static int invoke( Object left, Object right ) {
		return invoke( left, right, false, true, Locale.US );
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
	public static int invoke( Object left, Object right, boolean caseSensitive ) {
		return invoke( left, right, caseSensitive, true, Locale.US );
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
	public static Integer attempt( Object left, Object right, boolean caseSensitive ) {
		return attempt( left, right, caseSensitive, Locale.US );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 * @param locale        The locale to use for comparison
	 *
	 * @return 1 if greater than, -1 if less than, 0 if equal
	 */
	public static Integer attempt( Object left, Object right, boolean caseSensitive, Locale locale ) {
		return invoke( left, right, caseSensitive, false, locale );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive *
	 * @param fail          True to throw an exception if the left and right arguments cannot be compared
	 * @param locale        The locale to use for comparison
	 *
	 * @return 1 if greater than, -1 if less than, 0 if equal. Null if fail=false and the left and right arguments cannot be compared
	 */
	@SuppressWarnings( "unchecked" )
	public static Integer invoke( Object left, Object right, boolean caseSensitive, boolean fail, Locale locale ) {
		// Two nulls are equal
		if ( left == null && right == null ) {
			return 0;
		}

		// This is here for CF compat, off unless toggled by compat module
		if ( nullEqualsEmptyString ) {
			if ( left == null && "".equals( right ) ) {
				return 0;
			}
			if ( right == null && "".equals( left ) ) {
				return 0;
			}
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
					if ( !lenientDateComparison ) {
						// the chrono date time will return a numeric comparator rather than 0,1,-1
						int comparison = ref.get().compareTo( target.get() );
						return comparison == 0 ? 0 : ( comparison < 0 ? -1 : 1 );
					} else {
						Key dateCompareKey = Key.of( "DateCompare" );
						return IntegerCaster.cast(
						    runtime.getFunctionService().getGlobalFunction( dateCompareKey ).invoke(
						        runtime.getRuntimeContext(),
						        Struct.of(
						            Key.date1, ref.get(),
						            Key.date2, target.get(),
						            Key.datepart, "s"
						        ),
						        false,
						        dateCompareKey
						    )
						);

					}
				}
			}
		}

		// Numeric comparison
		CastAttempt<Number> leftAttempt = NumberCaster.attempt( left, true );
		if ( leftAttempt.wasSuccessful() ) {
			CastAttempt<Number> rightAttempt = NumberCaster.attempt( right, true );

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
		// This only works if at least one operand is already a java.lang.String
		// What if both are a Character or CharSequence?
		// TODO: we may want to try the string casters every time regardless of the type
		if ( left instanceof String || right instanceof String ) {
			CastAttempt<String>	leftStringAttempt	= StringCaster.attempt( left );
			CastAttempt<String>	rightStringAttempt	= StringCaster.attempt( right );

			if ( leftStringAttempt.wasSuccessful() && rightStringAttempt.wasSuccessful() ) {
				return StringCompare.invoke( leftStringAttempt.get(), rightStringAttempt.get(), caseSensitive );
			}
			// If both sides weren't castable to a string, then just ignore this check.

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
			    String.format( "Can't compare [%s] against [%s]", TypeUtil.getObjectName( left ), TypeUtil.getObjectName( right ) )
			);
		}

		return null;
	}

	/**
	 * Converts a compare result to an integer. This allows sort callbacks to return any numeric value.
	 * 
	 * @param result The compare result
	 * 
	 * @return The integer comparison value
	 */
	public static Integer convertCompareResultToInteger( Object result ) {
		Double numResult = NumberCaster.cast( result ).doubleValue();
		if ( numResult == 0 ) {
			return 0;
		} else if ( numResult > 0 ) {
			return 1;
		} else {
			return -1;
		}
	}

}
