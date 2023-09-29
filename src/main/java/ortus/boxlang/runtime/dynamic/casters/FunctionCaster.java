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
package ortus.boxlang.runtime.dynamic.casters;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * I handle casting anything to a Function
 */
public class FunctionCaster {

	/**
	 * Tests to see if the value can be cast to a Function.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Function
	 *
	 * @return The Function value
	 */
	public static CastAttempt<Function> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Function, throwing exception if we fail
	 *
	 * @param object The value to cast to a Function
	 *
	 * @return The Function value
	 */
	public static Function cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Function
	 *
	 * @param object The value to cast to a Function
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Function value, or null when cannot be cast
	 */
	public static Function cast( Object object, boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new ApplicationException( "Null cannot be cast to a Function" );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Function ) {
			return ( Function ) object;
		} else {
			if ( fail ) {
				throw new ApplicationException(
				    String.format( "Value [%s] cannot be cast to a Function", object.getClass().getName() )
				);
			} else {
				return null;
			}
		}

	}

}
