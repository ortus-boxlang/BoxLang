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

import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * I handle casting anything to a Binary
 */
public class BinaryCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Binary.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Binary
	 *
	 * @return The Binary value
	 */
	public static CastAttempt<byte[]> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Binary, throwing exception if we fail
	 *
	 * @param object The value to cast to a Binary
	 *
	 * @return The Binary value
	 */
	public static byte[] cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Binary
	 *
	 * @param object The value to cast to a Binary
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Binary value
	 */
	public static byte[] cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Binary." );
			} else {
				return null;
			}
		}

		if ( object instanceof byte[] bo ) {
			return bo;
		}

		// Do we throw?
		if ( fail ) {
			throw new BoxCastException( "Can't cast " + TypeUtil.getObjectName( object ) + " to a Binary." );
		}

		return null;
	}

}
