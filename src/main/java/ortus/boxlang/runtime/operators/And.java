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

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

/**
 * Performs Logical "and"
 * {@code true && true}
 */
public class And implements IOperator {

	/**
	 * @param left  Object to compare to right
	 * @param right Object to compare to left
	 *
	 * @return The result
	 */
	public static Boolean invoke( Object left, Object right ) {
		return BooleanCaster.cast( left ) && BooleanCaster.cast( right );
	}

}
