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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a Array
 */
public class ArrayCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to an Array.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was successfull
	 * , or can be interrogated to proceed otherwise.
	 *
	 * @param object The value to cast to an Array
	 *
	 * @return The Array value
	 */
	public static CastAttempt<Array> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to an Array, throwing exception if we fail
	 *
	 * @param object The value to cast to an Array
	 *
	 * @return The Array value
	 */
	public static Array cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to an Array
	 *
	 * @param object The value to cast to an Array
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Array value
	 */
	@SuppressWarnings( "unchecked" )
	public static Array cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Array." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof Array col ) {
			return col;
		}

		if ( object.getClass().isArray() ) {
			// Arrays of Object are easier to cast
			if ( object instanceof Object[] array ) {
				return Array.of( array );
			}

			// for primitive arrays, we must copy like so
			int		length	= java.lang.reflect.Array.getLength( object );
			Array	arr		= new Array();

			for ( int i = 0; i < length; i++ ) {
				arr.add( java.lang.reflect.Array.get( object, i ) );
			}
			return arr;
		}

		switch ( object ) {
			case List list -> {
				return Array.fromList( ( List<Object> ) object );
			}
			case ArgumentsScope args -> {
				return args.asArray();
			}
			case QueryColumn col -> {
				return col.getColumnDataAsArray();
			}
			default -> {
			}
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Can't cast [%s] to a Array.", object.getClass().getName() )
			);
		} else {
			return null;
		}
	}

}
