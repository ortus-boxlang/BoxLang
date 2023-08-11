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
 * Performs EQ, GT, and LT comparisons
 * Compares numbers as numbers, compares strings case insensitive
 */
public class Compare implements IOperator {

	/**
	 * @return 1 if greater than, -1 if less than, = if equal
	 */
	@SuppressWarnings("unchecked")
	static int invoke( Object left, Object right ) {

		if( left == null ) {
			left = "";
		}

		if( right == null ) {
			right = "";
		}

		// TODO: actually if inputs are numeric, don't just cast and catch.
		try{
			return Double.valueOf( left.toString() ).compareTo( Double.valueOf( right.toString() ) );
		} catch( NumberFormatException e ) {
			// If the operands are not numbers, ignore
		}

		// TODO: This is too simplistic
		if( left instanceof String || right instanceof String ) {
			return left.toString().compareToIgnoreCase( right.toString() );
		}

		if( left instanceof Comparable && right instanceof Comparable ) {
			return ((Comparable)left).compareTo( (Comparable)right );
		}

		// TODO: Dates

		throw new RuntimeException(
			String.format( "Can't compare [%s] against [%s]", left.getClass().getName(), right.getClass().getName() )
		);
	}

}
