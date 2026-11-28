/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.dynamic.casters;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Shared helpers for the numeric casters, so parsing logic isn't duplicated across classes.
 */
public class CasterNumberUtil {

	/**
	 * Determine whether the provided string is a creatable number, ignoring leading zeros.
	 *
	 * {@code NumberUtils.isCreatable()} treats an all-digit string with a leading zero as an octal literal and only accepts
	 * digits 0-7, so values like "099" (or "08", "09") are wrongly rejected, while "0123" is accepted. To avoid that
	 * inconsistency, this strips leading zeros (preserving any optional sign, and not stripping before a decimal point) before the
	 * check. This mirrors how the majority of the number casters behave.
	 *
	 * @param value A probably-hopefully number string value, with an optional leading plus/minus sign.
	 *
	 * @return True if the (zero-stripped) value is creatable.
	 */
	public static boolean isCreatableIgnoringLeadingZeros( String value ) {
		// Strip leading zeros so NumberUtils.isCreatable() doesn't treat them as octal
		String	checkValue	= value;
		int		start		= 0;
		int		len			= checkValue.length();
		// Skip past optional sign
		if ( len > 0 && ( checkValue.charAt( 0 ) == '+' || checkValue.charAt( 0 ) == '-' ) ) {
			start = 1;
		}
		// Find first non-zero digit after sign; stop before a decimal point so "0.5" isn't mangled to ".5"
		while ( start < len - 1 && checkValue.charAt( start ) == '0' && checkValue.charAt( start + 1 ) != '.' ) {
			start++;
		}
		if ( start > 0 ) {
			// Preserve original sign if present
			char first = value.charAt( 0 );
			if ( first == '+' || first == '-' ) {
				checkValue = first + checkValue.substring( start );
			} else {
				checkValue = checkValue.substring( start );
			}
		}
		return NumberUtils.isCreatable( checkValue );
	}

	/**
	 * Determine whether an integer string fits into a long. A number of up to 18 digits always does, a 19-digit number only up to
	 * Long.MAX_VALUE (9223372036854775807), or Long.MIN_VALUE when negative, and a longer number never does. The compiler (integer
	 * literals) and the number casters use this same rule, so a literal and the same digits cast at runtime get the same type.
	 *
	 * @param value An integer string: digits with an optional leading plus/minus sign and leading zeros.
	 *
	 * @return True if the value can be parsed as a long.
	 */
	public static boolean integerStringFitsInLong( String value ) {
		int		len			= value.length();
		int		start		= 0;
		boolean	negative	= false;
		if ( len > 0 && ( value.charAt( 0 ) == '+' || value.charAt( 0 ) == '-' ) ) {
			negative	= value.charAt( 0 ) == '-';
			start		= 1;
		}
		while ( start < len - 1 && value.charAt( start ) == '0' ) {
			start++;
		}
		int digits = len - start;
		if ( digits != 19 ) {
			return digits < 19;
		}
		// 19 digits: compare digit by digit against the bound
		String bound = negative ? "9223372036854775808" : "9223372036854775807";
		for ( int i = 0; i < 19; i++ ) {
			char	digit	= value.charAt( start + i );
			char	limit	= bound.charAt( i );
			if ( digit != limit ) {
				return digit < limit;
			}
		}
		return true;
	}
}