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
import java.util.Map;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.GreaterThan;
import ortus.boxlang.runtime.operators.LessThan;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * I handle casting anything to a boolean
 */
public class BooleanCaster implements IBoxCaster {

	/**
	 * Well-Known-Text representations of boolean values
	 */
	private static final IStruct wkt = Struct.of(
	    Key.of( "Yes" ), true,
	    Key.of( "No" ), false,
	    Key.of( "true" ), true,
	    Key.of( "false" ), false
	);

	/**
	 * Tests to see if the value can be cast to a boolean.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was successful,
	 * or can be interrogated to proceed otherwise.
	 *
	 * @param object The value to cast to a boolean
	 *
	 * @return The boolean value
	 */
	public static CastAttempt<Boolean> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Tests to see if the value can be cast to a boolean.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was successful,
	 * or can be interrogated to proceed otherwise.
	 *
	 * @param object The value to cast to a boolean
	 * @param loose  True to allow for truthy and falsey values when casting
	 *
	 * @return The boolean value
	 */
	public static CastAttempt<Boolean> attempt( Object object, Boolean loose ) {
		return CastAttempt.ofNullable( cast( object, false, loose, true ) );
	}

	/**
	 * Tests to see if the value can be cast to a boolean.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was successful,
	 * or can be interrogated to proceed otherwise.
	 *
	 * @param object             The value to cast to a boolean
	 * @param loose              True to allow for truthy and falsey values when casting
	 * @param numbersAreBooleans True to treat numbers as booleans (non-zero is true, zero is false)
	 *
	 * @return The boolean value
	 */
	public static CastAttempt<Boolean> attempt( Object object, Boolean loose, boolean numbersAreBooleans ) {
		return CastAttempt.ofNullable( cast( object, false, loose, numbersAreBooleans ) );
	}

	/**
	 * Used to cast anything to a boolean, throwing exception if we fail
	 *
	 * @param object The value to cast to a boolean
	 *
	 * @return The boolean value
	 */
	public static Boolean cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a boolean, throwing exception if we fail
	 *
	 * @param object The value to cast to a boolean
	 * @param fail   True to throw exception when failing
	 *
	 * @return The boolean value
	 */
	public static Boolean cast( Object object, Boolean fail ) {
		return cast( object, fail, true );
	}

	/**
	 * Used to cast anything to a boolean
	 *
	 * @param object The value to cast to a boolean
	 * @param fail   True to throw exception when failing.
	 * @param loose  True to allow for truthy and falsey values when casting
	 *
	 * @return The boolean value, or null when cannot be cast
	 */
	public static Boolean cast( Object object, Boolean fail, Boolean loose ) {
		return cast( object, fail, loose, true );
	}

	/**
	 * Used to cast anything to a boolean
	 *
	 * @param object             The value to cast to a boolean
	 * @param fail               True to throw exception when failing.
	 * @param loose              True to allow for truthy and falsey values when casting
	 * @param numbersAreBooleans True to treat numbers as booleans (non-zero is true, zero is false)
	 *
	 * @return The boolean value, or null when cannot be cast
	 */
	public static Boolean cast( Object object, Boolean fail, Boolean loose, boolean numbersAreBooleans ) {
		if ( object == null ) {
			return false;
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Boolean bool ) {
			return bool;
		}

		// Quick number check first
		if ( numbersAreBooleans && object instanceof Number num ) {
			// Positive and negative numbers are true, zero is false
			return num.doubleValue() != 0;
		}

		// Check for char
		if ( object instanceof Character ch ) {
			// If 1, are true
			// If 0, are false
			return switch ( ch ) {
				case '1' -> true;
				case '0' -> false;
				default -> {
					if ( fail ) {
						throw new BoxCastException(
						    String.format( "Character [%s] cannot be cast to a boolean", ch )
						);
					} else {
						yield null;
					}
				}
			};
		}

		// Check for string
		if ( object instanceof String str ) {
			Boolean boolResult = parseBooleanString( str );
			if ( boolResult != null ) {
				return boolResult;
			}
			if ( numbersAreBooleans ) {
				// Is string a number
				CastAttempt<Number> numberAttempt = NumberCaster.attempt( str );
				if ( numberAttempt.wasSuccessful() ) {
					// Positive and negative numbers are true, zero is false
					return GreaterThan.invoke( numberAttempt.get(), 0 ) || LessThan.invoke( numberAttempt.get(), 0 );
				}
			}
			if ( fail ) {
				throw new BoxCastException(
				    String.format( "String [%s] cannot be cast to a boolean", str )
				);
			} else {
				return null;
			}
		}

		// Truthy / Falsey Values for collections and lists
		// True - 1 or more items
		// False - 0 items
		if ( loose ) {
			// performance improvement https://openjdk.org/jeps/441
			return switch ( object ) {
				case Array castedArray -> !castedArray.isEmpty();
				case List<?> castedList -> !castedList.isEmpty();
				case Struct castedStruct -> !castedStruct.isEmpty();
				case Map<?, ?> castedMap -> !castedMap.isEmpty();
				case Query castedQuery -> !castedQuery.isEmpty();
				default -> {
					if ( fail ) {
						throw new BoxCastException(
						    String.format( "Value [%s] cannot be cast to a boolean", TypeUtil.getObjectName( object ) ) );
					} else {
						yield null;
					}
				}
			};
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Value [%s] cannot be cast to a boolean", TypeUtil.getObjectName( object ) )
			);
		} else {
			return null;
		}
	}

	/**
	 * Get the well-known-text boolean strings
	 * 
	 * @return The well-known-text boolean strings
	 */
	public static IStruct getBooleanStrings() {
		return wkt;
	}

	/**
	 * High-performance boolean string parser that avoids Key/Struct overhead.
	 * Uses string length to funnel into specific checks, and the bitwise OR trick
	 * ({@code ch | 0x20}) for case-insensitive ASCII letter comparison without allocation.
	 *
	 * @param str The string to parse (will be trimmed)
	 *
	 * @return {@code Boolean.TRUE}, {@code Boolean.FALSE}, or {@code null} if not a boolean string
	 */
	public static Boolean parseBooleanString( String str ) {
		int	start	= 0;
		int	end		= str.length();

		// Inline trim: skip leading whitespace
		while ( start < end && str.charAt( start ) <= ' ' ) {
			start++;
		}
		// Skip trailing whitespace
		while ( end > start && str.charAt( end - 1 ) <= ' ' ) {
			end--;
		}

		int len = end - start;

		switch ( len ) {
			case 2 : // "no"
				if ( ( str.charAt( start ) | 0x20 ) == 'n'
				    && ( str.charAt( start + 1 ) | 0x20 ) == 'o' ) {
					return Boolean.FALSE;
				}
				return null;
			case 3 : // "yes"
				if ( ( str.charAt( start ) | 0x20 ) == 'y'
				    && ( str.charAt( start + 1 ) | 0x20 ) == 'e'
				    && ( str.charAt( start + 2 ) | 0x20 ) == 's' ) {
					return Boolean.TRUE;
				}
				return null;
			case 4 : // "true"
				if ( ( str.charAt( start ) | 0x20 ) == 't'
				    && ( str.charAt( start + 1 ) | 0x20 ) == 'r'
				    && ( str.charAt( start + 2 ) | 0x20 ) == 'u'
				    && ( str.charAt( start + 3 ) | 0x20 ) == 'e' ) {
					return Boolean.TRUE;
				}
				return null;
			case 5 : // "false"
				if ( ( str.charAt( start ) | 0x20 ) == 'f'
				    && ( str.charAt( start + 1 ) | 0x20 ) == 'a'
				    && ( str.charAt( start + 2 ) | 0x20 ) == 'l'
				    && ( str.charAt( start + 3 ) | 0x20 ) == 's'
				    && ( str.charAt( start + 4 ) | 0x20 ) == 'e' ) {
					return Boolean.FALSE;
				}
				return null;
			default :
				return null;
		}
	}

}
