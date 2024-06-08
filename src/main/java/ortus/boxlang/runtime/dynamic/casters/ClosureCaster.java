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
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a Closure
 */
public class ClosureCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Function.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Closure
	 *
	 * @return The Closure value
	 */
	public static CastAttempt<Closure> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Closure, throwing exception if we fail
	 *
	 * @param object The value to cast to a Closure
	 *
	 * @return The Closure value
	 */
	public static Closure cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Closure
	 *
	 * @param object The value to cast to a Closure
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Closure value, or null when cannot be cast
	 */
	public static Closure cast( Object object, boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Null cannot be cast to a Closure" );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Closure fun ) {
			return fun;
		} else {
			if ( fail ) {
				throw new BoxCastException(
				    String.format( "Value [%s] cannot be cast to a Closure", object.getClass().getName() )
				);
			} else {
				return null;
			}
		}

	}

}
