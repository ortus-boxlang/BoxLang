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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.Function;

/**
 * Performs an assertion check on an incoming expression value.
 * It returns {@code true} if it passes the assertion, else it throws an AssertionError
 *
 * {@code assert "hello" contains "he", assert expression : "optional message" }
 */
public class Assert implements IOperator {

	/**
	 * @param context The current box context
	 * @param result  The result of the boolean expression
	 *
	 * @return true if the boolean result is true, else an exception
	 *
	 * @throws AssertionError if the result is false or null
	 */
	public static Boolean invoke( IBoxContext context, Object result ) throws AssertionError {
		return invoke( context, result, null );
	}

	/**
	 * @param context The current box context
	 * @param result  The result of the boolean expression
	 * @param message An optional message to include in the AssertionError when the assertion fails
	 *
	 * @return true if the boolean result is true, else an exception
	 *
	 * @throws AssertionError if the result is false or null
	 */
	public static Boolean invoke( IBoxContext context, Object result, Object message ) throws AssertionError {

		// if the result is a Function, check it and call it
		if ( result instanceof Function fun ) {
			result = context.invokeFunction( fun );
		}

		if ( result != null && BooleanCaster.cast( result ) ) {
			return true;
		}

		if ( message != null ) {
			throw new AssertionError( StringCaster.cast( message ) );
		}

		throw new AssertionError( "The assertion failed!" );
	}

}
