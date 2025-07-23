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
 * Performs {@code EQ or ===} comparison
 * Compares numbers as numbers, compares strings case insensitive but with type checking
 */
public class EqualsEqualsEquals implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return True if operands are the equal
	 */
	@SuppressWarnings( "unchecked" )
	public static Boolean invoke( Object left, Object right ) {
		if ( left.getClass().isAssignableFrom( right.getClass() ) || right.getClass().isAssignableFrom( left.getClass() ) ) {
			// We need string comparisons to still be case insensitive
			if ( left instanceof String lefString && right instanceof String rightString ) {
				return StringCompare.invoke( lefString, rightString ) == 0;
			} else if ( left instanceof Comparable leftComparable && right instanceof Comparable rightComparable ) {
				return leftComparable.compareTo( rightComparable ) == 0;
			} else {
				// For everything else, NO CASTING! Just check equality between the objects.
				// note, this means an integer and a long will not be equal even if they represent the same value
				return left.equals( right );
			}
		}
		return false;
	}

}
