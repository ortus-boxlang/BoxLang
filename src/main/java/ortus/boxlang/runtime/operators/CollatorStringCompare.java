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

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.LocalizationUtil;

/**
 * Operator to compare two strings and bypass any additional cast attempts
 */
public class CollatorStringCompare implements IOperator {

	/**
	 * Invokes the comparison
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static int invoke( String left, String right ) {
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
	public static int invoke( String left, String right, Boolean caseSensitive ) {
		return attempt( left, right, caseSensitive, true );
	}

	/**
	 * Invokes the comparison
	 *
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Whether to compare strings case sensitive
	 * @param locale        The locale to use for comparison
	 *
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	public static int invoke( String left, String right, Boolean caseSensitive, Locale locale ) {
		return attempt( left, right, caseSensitive, true, locale );
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
	public static Integer attempt( String left, String right, Boolean caseSensitive, boolean fail ) {
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
	public static Integer attempt( String left, String right, Boolean caseSensitive, boolean fail, Locale locale ) {
		// Use Collator for proper locale-based comparison
		Collator collator = Collator.getInstance( locale );
		collator.setStrength( caseSensitive ? Collator.IDENTICAL : Collator.TERTIARY );
		collator.setDecomposition( Collator.CANONICAL_DECOMPOSITION );
		return collator.getCollationKey( left.toString() )
		    .compareTo( collator.getCollationKey( right.toString() ) );
	}

}
