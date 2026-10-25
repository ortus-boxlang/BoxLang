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
 * Performs {@code GTE or >=} comparison
 * Compares numbers as numbers, compares strings case insensitive
 */
public class GreaterThanEqual implements IOperator {

	/**
	 * @param context The context of the caller, used to resolve settings such as the timezone for dates
	 * @param left    The left operand
	 * @param right   The right operand
	 *
	 * @return True if left is greater than or equal to right
	 */
	public static Boolean invoke( IBoxContext context, Object left, Object right ) {
		return Compare.invoke( context, left, right ) >= 0;
	}

	/**
	 * @deprecated Use {@link #invoke(IBoxContext, Object, Object)} so the caller's context is used
	 *
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return True if left is greater than or equal to right
	 */
	@Deprecated
	public static Boolean invoke( Object left, Object right ) {
		return invoke( Compare.defaultContext(), left, right );
	}

}
