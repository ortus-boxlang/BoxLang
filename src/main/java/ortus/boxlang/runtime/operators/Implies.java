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
 * Performs an implication check on the two values. Will return true in every case except true IMP false.
 * {@code expr IMP expr}
 */
public class Implies implements IOperator {

	public static Object invoke( Object left, Object right ) {
		Boolean	leftBool	= BooleanCaster.cast( left );
		Boolean	rightBool	= BooleanCaster.cast( right );

		if ( leftBool && !rightBool ) {
			return false;
		}

		return true;
	}

}
