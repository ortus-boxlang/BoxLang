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
 * Performs == comparison
 * Compares numbers as numbers, compares strings case insensitive
 */
public class EqualsEquals implements IOperator {

	/**
	 * @return True if operands are the equal
	 */
	static Boolean invoke( Object left, Object right ) {
		// TODO: actually if inputs are numeric, don't just cast and catch.
		try{
			return Double.valueOf( left.toString() ).compare( Double.valueOf( right.toString() ) ) == 0;
		} catch( NumberFormatException e ) {
			// If the operands are not numbers, ignore
		}

		// TODO: This is too simplistic
		if( left instanceof String || right instanceof String ) {
			return left.toString().equalsIgnoreCase( right.toString() );
		}

		// This prolly isn't the correct default
		return left.equals( right );
	}

}
