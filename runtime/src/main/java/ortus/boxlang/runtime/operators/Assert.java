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
 * Performs an assertion check on an incoming expression value.
 * It returns {@code true} if it passes the assertion, else it throws an AssertionError
 *
 * {@code assert "hello" contains "he", asssert expression }
 */
public class Assert implements IOperator {

	/**
	 * @param result The result of the boolean expression
	 *
	 * @return true if the boolean result is true, else an exception
	 *
	 * @throws AssertionError if the result is false or null
	 */
	public static Boolean invoke( Object result ) throws AssertionError {

		/**
		 * TODO: if the result is a closure/lambda, check it and call it
		 * if( result instanceOf Closure || result instanceOf Lambda ){
		 * result = result.invoke();
		 * }
		 */

		if ( result != null && BooleanCaster.cast( result ) ) {
			return true;
		}

		throw new AssertionError( "The assertion failed!" );
	}

}
