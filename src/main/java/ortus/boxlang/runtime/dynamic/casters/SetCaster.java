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
import java.util.Set;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Range;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * I handle casting anything to a {@link BoxSet}.
 *
 * <p>
 * Accepted sources:
 * <ul>
 * <li>{@code BoxSet} — pass-through</li>
 * <li>{@code java.util.Set} — wrapped in a default {@code BoxSet}</li>
 * <li>{@code Array} / {@code List} / {@code Collection} — deduplicated into a default {@code BoxSet}</li>
 * <li>{@code Object[]} or any native array — deduplicated into a default {@code BoxSet}</li>
 * <li>{@link Range} — materialized via {@link Range#toArray()} when bounded and iterable</li>
 * <li>{@link QueryColumn} — column data deduplicated into a set</li>
 * <li>{@link XML} — siblings of the same name deduplicated into a set</li>
 * </ul>
 *
 * <p>
 * Strings are NOT auto-cast to sets — use {@code listToArray(...).toSet()} if a
 * delimited-string conversion is needed.
 */
public class SetCaster implements IBoxCaster {

	/**
	 * Tests whether the value can be cast to a {@link BoxSet}.
	 *
	 * @param object The value to attempt to cast
	 *
	 * @return A {@link CastAttempt} which is successful if the cast worked
	 */
	public static CastAttempt<BoxSet> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Cast anything to a {@link BoxSet}, throwing on failure.
	 *
	 * @param object The value to cast
	 *
	 * @return The {@link BoxSet} value
	 */
	public static BoxSet cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Cast anything to a {@link BoxSet}.
	 *
	 * @param object The value to cast
	 * @param fail   True to throw an exception on failure, false to return null
	 *
	 * @return The {@link BoxSet} value, or null if the cast fails and {@code fail} is false
	 */
	@SuppressWarnings( "unchecked" )
	public static BoxSet cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Set." );
			}
			return null;
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof BoxSet bs ) {
			return bs;
		}

		// Native Java arrays
		if ( object.getClass().isArray() ) {
			if ( object instanceof Object[] array ) {
				BoxSet s = new BoxSet();
				for ( Object e : array ) {
					s.add( e );
				}
				return s;
			}
			int		length	= java.lang.reflect.Array.getLength( object );
			BoxSet	s		= new BoxSet();
			for ( int i = 0; i < length; i++ ) {
				s.add( java.lang.reflect.Array.get( object, i ) );
			}
			return s;
		}

		switch ( object ) {
			case Set<?> existing -> {
				return BoxSet.fromCollection( ( Collection<Object> ) existing );
			}
			case Array arr -> {
				return BoxSet.fromCollection( arr );
			}
			case Collection<?> coll -> {
				return BoxSet.fromCollection( ( Collection<Object> ) coll );
			}
			case QueryColumn col -> {
				return BoxSet.fromCollection( col.getColumnDataAsArray() );
			}
			case XML xml -> {
				return BoxSet.fromCollection( xml.getSiblingsOfSameName() );
			}
			case Range<?> range -> {
				if ( range.isIterable() && range.isBounded() ) {
					return BoxSet.fromCollection( range.toArray() );
				}
				if ( fail ) {
					throw new BoxCastException( String.format( "Can't cast Range [%s] to a Set.", range.asString() ) );
				}
				return null;
			}
			default -> {
			}
		}

		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a Set.", TypeUtil.getObjectName( object ) ) );
		}
		return null;
	}

}
