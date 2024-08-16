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

/**
 * Performs {@code EQ or ==} comparison and inverts the result
 * Compares numbers as numbers, compares strings case insensitive
 */
public class NotEqualsEquals implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return True if operands are the not equal
	 */
	public static Boolean invoke( Object left, Object right ) {
		Integer comparison = Compare.attempt( left, right, false, false );

		if ( comparison == null ) {
			return !left.equals( right );
		}

		return comparison != 0;
	}

	public static Boolean invokeInverse( Object left, Object right ) {
		return invoke( left, right );
	}

	/**
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Flag to control whether to consider case
	 *
	 * @return True if operands are not equal
	 */
	public static Boolean invoke( Object left, Object right, boolean caseSensitive ) {
		Integer comparison = Compare.attempt( left, right, caseSensitive, false );

		if ( comparison == null ) {
			return !left.equals( right );
		}

		return comparison != 0;
	}

}
