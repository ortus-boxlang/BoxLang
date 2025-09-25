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
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;
import ortus.boxlang.runtime.util.ValidationUtil;

/**
 * I handle casting anything to a UUID
 */
public class UUIDCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a UUID.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a UUID
	 *
	 * @return The UUID value
	 */
	public static CastAttempt<String> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a UUID, throwing exception if we fail
	 *
	 * @param object The value to cast to a UUID
	 *
	 * @return The UUID value
	 */
	public static String cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a UUID
	 *
	 * @param object The value to cast to a UUID
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The UUID value
	 */
	public static String cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a UUID." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );
		String sObject;
		if ( object instanceof String str ) {
			sObject = str;
		} else {
			CastAttempt<String> castAttempt = StringCaster.attempt( object );
			if ( castAttempt.wasSuccessful() ) {
				sObject = castAttempt.get();
			} else {
				if ( fail ) {
					throw new BoxCastException( "Can't cast " + TypeUtil.getObjectName( object ) + " to a UUID." );
				} else {
					return null;
				}
			}
		}

		if ( ValidationUtil.isValidUUID( sObject ) ) {
			return sObject;
		}

		// Do we throw?
		if ( fail ) {
			throw new BoxCastException( "Can't cast " + TypeUtil.getObjectName( object ) + " to a UUID." );
		}

		return null;
	}

}
