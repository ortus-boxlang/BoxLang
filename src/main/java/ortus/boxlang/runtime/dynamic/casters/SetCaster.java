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
 * I handle casting to a {@link BoxSet}.
 *
 * <p>
 * This caster has two modes:
 * <ul>
 * <li><b>Strict (default)</b> — {@link #cast(Object)} and {@link #attempt(Object)} only succeed
 * when the source is already a Set (a {@link BoxSet} or any {@link java.util.Set}). This is
 * the mode used by member-function dispatch and argument validation so that calling
 * {@code myArrayList.size()} or {@code myArrayList.add(x)} is not hijacked by Set member
 * functions just because Arrays/Lists are convertible to Sets.</li>
 * <li><b>Loose</b> — {@link #castLoose(Object)} additionally accepts arrays, BoxLang
 * {@code Array}s, any {@link Collection}, {@link QueryColumn}, {@link XML}, and bounded
 * iterable {@link Range}s, deduplicating into a new {@link BoxSet}. Used by explicit
 * conversion BIFs ({@code setNew}, {@code toSet}, {@code castAs Set}).</li>
 * </ul>
 *
 * <p>
 * Strings are NOT auto-cast to sets — use {@code listToArray(...).toSet()} for a
 * delimited-string conversion.
 */
public class SetCaster implements IBoxCaster {

	/**
	 * Strict attempt: succeeds only when {@code object} is already a {@link BoxSet} or {@link java.util.Set}.
	 */
	public static CastAttempt<BoxSet> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Strict cast — throws if {@code object} is not already a Set.
	 */
	public static BoxSet cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Strict cast.
	 *
	 * @param object The value to cast
	 * @param fail   True to throw on failure, false to return null
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
		if ( object instanceof Set<?> existing ) {
			// Wrap (no copy) so that mutations on the BoxSet propagate to the original
			// Java Set — same contract as Array wrapping an ArrayList.
			return BoxSet.wrap( ( Set<Object> ) existing );
		}

		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a Set.", TypeUtil.getObjectName( object ) ) );
		}
		return null;
	}

	/**
	 * Loose attempt: also accepts arrays, lists, ranges, query columns, XML — anything
	 * containing elements that can be deduplicated into a new {@link BoxSet}.
	 */
	public static CastAttempt<BoxSet> attemptLoose( Object object ) {
		return CastAttempt.ofNullable( castLoose( object, false ) );
	}

	/**
	 * Loose cast — throws on failure.
	 */
	public static BoxSet castLoose( Object object ) {
		return castLoose( object, true );
	}

	/**
	 * Loose cast.
	 *
	 * @param object The value to cast
	 * @param fail   True to throw on failure, false to return null
	 */
	@SuppressWarnings( "unchecked" )
	public static BoxSet castLoose( Object object, Boolean fail ) {
		// Try the strict path first — preserves existing Set identity.
		BoxSet strict = cast( object, false );
		if ( strict != null ) {
			return strict;
		}
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Set." );
			}
			return null;
		}
		object = DynamicObject.unWrap( object );

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
