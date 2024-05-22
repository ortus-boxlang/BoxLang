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

import java.text.Collator;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

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
	 * @return 1 if greater than, -1 if less than, = if equal
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
	 * @return 1 if greater than, -1 if less than, = if equal
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
	 * @return 1 if greater than, -1 if less than, = if equal
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

		CastAttempt<Double> leftAttempt = DoubleCaster.attempt( left );
		if ( leftAttempt.wasSuccessful() ) {
			CastAttempt<Double> rightAttempt = DoubleCaster.attempt( right );

			if ( rightAttempt.wasSuccessful() ) {
				return leftAttempt.get().compareTo( rightAttempt.get() );
			}
		}

		if ( left instanceof String || right instanceof String ) {
			if ( !caseSensitive ) {
				left	= StringUtils.lowerCase( left.toString(), locale );
				right	= StringUtils.lowerCase( right.toString(), locale );
			} else {
				// Assume that if the case sensitive argument is passed as false, dates are not expected
				if ( DateTimeCaster.attempt( left ).wasSuccessful() && DateTimeCaster.attempt( right ).wasSuccessful() ) {
					// TODO: This is potentially slow with multiple failures - we need to add some validation methods in to the DateTime cast attempt
					DateTime	ref		= DateTimeCaster.cast( left );
					DateTime	target	= DateTimeCaster.cast( right );
					return ref.compareTo( target );
				}
			}

			Boolean containsUnicode = Stream.of( left.toString(), right.toString() )
			    .anyMatch( s -> s.codePoints().anyMatch( c -> c > 127 ) );

			// if our locale is different than an EN locale use the Collator
			if ( containsUnicode || ( !locale.equals( ( Locale ) LocalizationUtil.commonLocales.get( "US" ) ) && !locale.equals( Locale.ENGLISH ) ) ) {
				Collator collator = Collator.getInstance( locale );
				return collator.getCollationKey( caseSensitive ? left.toString() : left.toString().toLowerCase( locale ) )
				    .compareTo( collator.getCollationKey( caseSensitive ? right.toString() : right.toString().toLowerCase( locale ) ) );
			} else {
				return caseSensitive
				    ? StringUtils.compare( left.toString(), right.toString() )
				    : StringUtils.compareIgnoreCase( left.toString(), right.toString() );
			}

		}

		if ( left instanceof Comparable && right instanceof Comparable ) {
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
