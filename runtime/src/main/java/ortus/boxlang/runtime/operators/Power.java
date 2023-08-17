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

import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;

/**
 * Performs Math Power for BoxLang
 * {@code a = 2 ^ 3}
 */
public class Power implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The the result
	 */
	public static Double invoke( Object left, Object right ) {
		return Math.pow( DoubleCaster.cast( left ), DoubleCaster.cast( right ) );
	}

}
