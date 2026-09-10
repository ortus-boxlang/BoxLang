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

import ortus.boxlang.runtime.types.NormalizedValue;

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
	public static Boolean invoke( Object left, Object right ) {
		// Delegate to NormalizedValue for type-aware equality checking
		// The "types" here only correspond with high-level BoxLang types.
		// So an int and long can be equal and both "numeric", but a string will never === a number, even if they represent the same value.
		return NormalizedValue.of( left ).equals( NormalizedValue.of( right ) );
	}

}
