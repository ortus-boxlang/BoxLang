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

import ortus.boxlang.runtime.context.IBoxContext;

/**
 * Performs {@code EQ or ==} comparison and inverts the result
 * Compares numbers as numbers, compares strings case insensitive
 */
public class NotEqualsEquals implements IOperator {

	/**
	 * @param context The context of the caller, used to resolve settings such as the timezone for dates
	 * @param left    The left operand
	 * @param right   The right operand
	 *
	 * @return True if operands are not equal
	 */
	public static Boolean invoke( IBoxContext context, Object left, Object right ) {
		Integer comparison = Compare.attempt( context, left, right, false );

		if ( comparison == null ) {
			return !left.equals( right );
		}

		return comparison != 0;
	}

	/**
	 * @param context       The context of the caller, used to resolve settings such as the timezone for dates
	 * @param left          The left operand
	 * @param right         The right operand
	 * @param caseSensitive Flag to control whether to consider case
	 *
	 * @return True if operands are not equal
	 */
	public static Boolean invoke( IBoxContext context, Object left, Object right, boolean caseSensitive ) {
		Integer comparison = Compare.attempt( context, left, right, caseSensitive );

		if ( comparison == null ) {
			return !left.equals( right );
		}

		return comparison != 0;
	}

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return True if operands are the not equal
	 *
	 * @deprecated Use the overload that takes an {@link IBoxContext} so the caller's context is used
	 */
	@Deprecated
	public static Boolean invoke( Object left, Object right ) {
		return invoke( Compare.defaultContext(), left, right );
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
	 *
	 * @deprecated Use the overload that takes an {@link IBoxContext} so the caller's context is used
	 */
	@Deprecated
	public static Boolean invoke( Object left, Object right, boolean caseSensitive ) {
		return invoke( Compare.defaultContext(), left, right, caseSensitive );
	}

}
