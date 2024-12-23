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

/**
 * I handle casting anything to a boolean
 */
public class BooleanCaster implements IBoxCaster {

	/**
	 * Well-Known-Text representations of boolean values
	 */
	private static final IStruct wkt = Struct.of(
	    Key.of( "Y" ), true,
	    Key.of( "N" ), false,
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
	 *
	 * @return The boolean value
	 */
	public static CastAttempt<Boolean> attempt( Object object, Boolean loose ) {
		return CastAttempt.ofNullable( cast( object, false, loose ) );
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
		if ( object == null ) {
			return false;
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Boolean bool ) {
			return bool;
		}

		// Quick number check first
		if ( object instanceof Number num ) {
			// Positive and negative numbers are true, zero is false
			return num.doubleValue() != 0;
		}

		// Check for char
		if ( object instanceof Character ch ) {
			// If y, Y, t, T, 1, are true
			// If n, N, f, F, 0, are false
			return switch ( ch ) {
				case 'y', 'Y', 't', 'T', '1' -> true;
				case 'n', 'N', 'f', 'F', '0' -> false;
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
			Key aliasKey = Key.of( str.trim() );
			if ( wkt.containsKey( aliasKey ) ) {
				return wkt.getAsBoolean( aliasKey );
			}
			// Is string a number
			CastAttempt<Number> numberAttempt = NumberCaster.attempt( str );
			if ( numberAttempt.wasSuccessful() ) {
				// Positive and negative numbers are true, zero is false
				return GreaterThan.invoke( numberAttempt.get(), 0 ) || LessThan.invoke( numberAttempt.get(), 0 );
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
						    String.format( "Value [%s] cannot be cast to a boolean", object.getClass().getName() ) );
					} else {
						yield null;
					}
				}
			};
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Value [%s] cannot be cast to a boolean", object.getClass().getName() )
			);
		} else {
			return null;
		}
	}

}
