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

/**
 * I handle casting anything
 */
public class IntegerCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<Integer> attempt( Object object ) {
		return attempt( object, false );
	}

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<Integer> attempt( Object object, boolean allowTruncate ) {
		return CastAttempt.ofNullable( cast( allowTruncate, object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static Integer cast( Object object ) {
		return cast( false, object, true );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static Integer cast( boolean allowTruncate, Object object ) {
		return cast( allowTruncate, object, true );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static Integer cast( Object object, Boolean fail ) {
		return cast( false, object, fail );
	}

	/**
	 * Used to cast anything
	 *
	 * @param object The value to cast
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static Integer cast( boolean allowTruncate, Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a int." );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Number num ) {
			return handleNumber( num, allowTruncate, fail );
		}
		if ( object instanceof Boolean bool ) {
			return Integer.valueOf( bool ? 1 : 0 );
		}

		Number num = NumberCaster.cast( object, false );
		if ( num != null ) {
			return handleNumber( num, allowTruncate, fail );
		}
		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a int.", object ) );
		} else {
			return null;
		}

	}

	private static Integer handleNumber( Number num, boolean allowTruncate, boolean fail ) {
		if ( allowTruncate ) {
			return Integer.valueOf( num.intValue() );
		} else {
			// check if there is a decimal portion
			if ( num.doubleValue() % 1 != 0 ) {
				if ( fail ) {
					throw new BoxCastException( String.format( "Can't cast [%s] to a int.", num ) );
				} else {
					return null;
				}
			} else {
				return Integer.valueOf( num.intValue() );
			}
		}
	}

}
