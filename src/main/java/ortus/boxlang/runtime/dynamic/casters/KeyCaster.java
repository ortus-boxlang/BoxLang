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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle casting anything to a Key
 */
public class KeyCaster {

	/**
	 * Tests to see if the value can be cast to a Key.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Key
	 *
	 * @return The Key value
	 */
	public static CastAttempt<Key> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Key, throwing exception if we fail
	 *
	 * @param object The value to cast to a Key
	 *
	 * @return The Key value
	 */
	public static Key cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Key
	 *
	 * @param object The value to cast to a Key
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Key value
	 */
	public static Key cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxRuntimeException( "Can't cast null to a Key." );
			} else {
				return null;
			}
		}

		if ( object instanceof Key key ) {
			return key;
		} else if ( object instanceof String str ) {
			return Key.of( str );
		} else {
			CastAttempt<String> castAttempt = StringCaster.attempt( object );
			if ( castAttempt.wasSuccessful() ) {
				return Key.of( castAttempt.get() );
			}
		}

		if ( fail ) {
			throw new BoxRuntimeException( "Can't cast " + object.getClass().getName() + " to a Key." );
		} else {
			return null;
		}
	}

}
