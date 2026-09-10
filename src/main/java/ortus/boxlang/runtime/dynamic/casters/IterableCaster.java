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

import java.util.Collection;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * I handle casting anything to an Iterable for use in for-in loops.
 * Unlike {@link CollectionCaster}, this returns {@link Iterable} rather than
 * {@link java.util.Collection}, which means types like {@link ortus.boxlang.runtime.types.Range}
 * can be iterated lazily without being fully realized into an array.
 */
public class IterableCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to an iterable.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * successful, or can be interrogated to proceed otherwise.
	 *
	 * @param object The value to cast to an iterable
	 *
	 * @return The iterable value
	 */
	public static CastAttempt<Iterable<Object>> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to an iterable, throwing exception if we fail
	 *
	 * @param object The value to cast to an iterable
	 *
	 * @return The iterable value
	 */
	public static Iterable<Object> cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to an iterable
	 *
	 * @param object The value to cast to an iterable
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The iterable value
	 */
	@SuppressWarnings( "unchecked" )
	public static Iterable<Object> cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to an Iterable." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		// If it's already Iterable (Collection, Range, List, Set, etc.), just use it directly.
		if ( object instanceof Iterable iterable ) {
			return iterable;
		}

		// Fall back to CollectionCaster for everything else (arrays, maps, structs, XML, strings, etc.)
		Collection<Object> collection = CollectionCaster.cast( object, false );
		if ( collection != null ) {
			return collection;
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Can't cast [%s] to an Iterable.", TypeUtil.getObjectName( object ) )
			);
		} else {
			return null;
		}
	}

}
