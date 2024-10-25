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

import java.util.List;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;

/**
 * I handle casting anything to a Array that needs to be modifiable
 * This means I reject
 * - Native Java arrays
 * - ImmutableLists
 * - UmmodifiableLists
 */
public class ModifiableArrayCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Array.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Array
	 *
	 * @return The Array value
	 */
	public static CastAttempt<Array> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Array, throwing exception if we fail
	 *
	 * @param object The value to cast to a Array
	 *
	 * @return The Array value
	 */
	public static Array cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Array that needs to be modifiable
	 *
	 * @param object The value to cast to a Array
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Array value
	 */
	public static Array cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Array." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof UnmodifiableArray ) {
			throw new BoxCastException( "Can't cast UnmodifiableArray to a modifiable Array." );
		}

		if ( object.getClass().isArray() ) {
			throw new BoxCastException( "Can't cast a Java Array to a modifiable Array." );
		}

		// Immutable list is not a specific class, so we have to get clever
		if ( object instanceof List && object.getClass().getName().contains( "Immutable" ) ) {
			throw new BoxCastException( "Can't cast Unmodifiable List to a modifiable Array." );
		}

		// delegate to ArrayCaster
		return ArrayCaster.cast( object, fail );
	}

}
