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

import java.util.HashMap;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a boolean
 */
public class BooleanCaster implements IBoxCaster {

	/**
	 * Well-Known-Text representations of boolean values
	 */
	private static final Struct wkt = new Struct(
	    new HashMap<Key, Boolean>() {

		    {
			    put( Key.of( "Y" ), true );
			    put( Key.of( "N" ), false );
			    put( Key.of( "Yes" ), true );
			    put( Key.of( "No" ), false );
			    put( Key.of( "true" ), true );
			    put( Key.of( "false" ), false );
		    }
	    }
	);

	/**
	 * Tests to see if the value can be cast to a boolean.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a boolean
	 *
	 * @return The boolean value
	 */
	public static CastAttempt<Boolean> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
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
	 * Used to cast anything to a boolean
	 *
	 * @param object The value to cast to a boolean
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The boolean value, or null when cannot be cast
	 */
	public static Boolean cast( Object object, Boolean fail ) {
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
		if ( object instanceof String str ) {
			Key aliasKey = Key.of( str.trim() );
			if ( wkt.containsKey( aliasKey ) ) {
				return wkt.getAsBoolean( aliasKey );
			}
			// Is string a number
			CastAttempt<Double> numberAttempt = DoubleCaster.attempt( str );
			if ( numberAttempt.wasSuccessful() ) {
				// Positive and negative numbers are true, zero is false
				return numberAttempt.get() != 0;
			}
			if ( fail ) {
				throw new BoxCastException(
				    String.format( "String [%s] cannot be cast to a boolean", str )
				);
			} else {
				return null;
			}
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
