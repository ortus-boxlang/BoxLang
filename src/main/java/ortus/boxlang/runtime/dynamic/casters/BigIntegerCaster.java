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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting BigInteger
 */
public class BigIntegerCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<BigInteger> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static BigInteger cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything
	 *
	 * @param object The value to cast
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static BigInteger cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return BigInteger.ZERO;
		}

		if ( object instanceof BigInteger bd ) {
			return bd;
		}

		if ( object instanceof BigDecimal bd ) {
			return bd.toBigInteger();
		}

		if ( object instanceof Integer i ) {
			return BigInteger.valueOf( i );
		}

		if ( object instanceof Long l ) {
			return BigInteger.valueOf( l );
		}

		if ( object instanceof Number num ) {
			return BigInteger.valueOf( num.longValue() );
		}

		if ( object instanceof Boolean bool ) {
			return bool ? BigInteger.ONE : BigInteger.ZERO;
		}

		if ( object instanceof String str ) {
			// String true and yes are truthy
			if ( str.equalsIgnoreCase( "true" ) || str.equalsIgnoreCase( "yes" ) ) {
				return BigInteger.ONE;
				// String false and no are truthy
			} else if ( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "no" ) ) {
				return BigInteger.ZERO;
			}
		}

		// Try to parse the string as a double
		String		stringValue	= StringCaster.cast( object, false );
		BigInteger	result		= parseBigInteger( stringValue );
		if ( result != null ) {
			return result;
		}

		// Verify if we can throw an exception
		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a BigInteger.", object.toString() ) );
		} else {
			return null;
		}

	}

	/**
	 * Determine whether the provided string is castable to a BigInteger.
	 *
	 * @param value A probably-hopefully BigInteger string value, with an optional plus/minus sign.
	 *
	 * @return Optional - parsed BigInteger if all string characters are digits, with an optional sign
	 */
	private static BigInteger parseBigInteger( String value ) {
		if ( NumberUtils.isCreatable( value ) ) {
			try {
				return new BigInteger( value );
			} catch ( Exception e ) {
				return null;
			}

		}

		return null;
	}

}
