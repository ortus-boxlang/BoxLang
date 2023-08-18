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
 * Performs elvis operator (null coaslescing)
 * {@code expr ?: expr}
 *
 * Note: Any deferencing performed in the evaluation of of the left operand must be done safely
 * So,
 * {@code foo.bar ?: expr}
 * must be the equivalent of
 * {@code foo?.bar ?: expr}
 */
public class Elvis implements IOperator {

	public static Object invoke( Object left, Object right ) {
		if ( left != null ) {
			return left;
		} else {
			return right;
		}
	}

}
