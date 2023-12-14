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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle casting anything
 */
public class CharacterCaster {

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<Character> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static Character cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything
	 *
	 * @param object The value to cast
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static Character cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxRuntimeException( "Can't cast null to a char." );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Character chr ) {
			return chr;
		}
		if ( object instanceof String str ) {
			if ( str.length() > 0 ) {
				return Character.valueOf( str.charAt( 0 ) );
			}
			if ( fail ) {
				throw new BoxRuntimeException( "Can't cast empty string to a char." );
			} else {
				return null;
			}

		}
		if ( object instanceof Boolean bool ) {
			return Character.valueOf( ( char ) ( bool ? 1 : 0 ) );
		}
		if ( object instanceof Number num ) {
			return Character.valueOf( ( char ) num.doubleValue() );
		}

		if ( fail ) {
			throw new BoxRuntimeException(
			    String.format( "Can't cast [%s] to a char.", object.getClass().getName() )
			);
		} else {
			return null;
		}

	}

}
