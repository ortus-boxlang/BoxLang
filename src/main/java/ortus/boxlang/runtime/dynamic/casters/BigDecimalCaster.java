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

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * I handle casting anything
 */
public class BigDecimalCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<BigDecimal> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static BigDecimal cast( Object object ) {
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
	public static BigDecimal cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return BigDecimal.ZERO;
		}

		if ( object instanceof BigDecimal bd ) {
			return bd;
		}

		// Any existing known number class like int, long, or double
		if ( object instanceof Number num ) {
			return new BigDecimal( num.doubleValue(), MathUtil.getMathContext() );
		}

		if ( object instanceof Boolean bool ) {
			return bool ? BigDecimal.ONE : BigDecimal.ZERO;
		}

		if ( object instanceof String str ) {
			// String true and yes are truthy
			if ( str.equalsIgnoreCase( "true" ) || str.equalsIgnoreCase( "yes" ) ) {
				return BigDecimal.ONE;
				// String false and no are truthy
			} else if ( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "no" ) ) {
				return BigDecimal.ZERO;
			}
		}

		// Try to parse the string as a double
		String		stringValue	= StringCaster.cast( object, false );
		BigDecimal	result		= parseBigDecimal( stringValue );
		if ( result != null ) {
			return result;
		}

		// Verify if we can throw an exception
		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a BigDecimal.", object.toString() ) );
		} else {
			return null;
		}

	}

	/**
	 * Determine whether the provided string is castable to a BigDecimal.
	 *
	 * @param value A probably-hopefully BigDecimal string value, with an optional plus/minus sign.
	 *
	 * @return Optional - parsed BigDecimal if all string characters are digits, with an optional sign and decimal point.
	 */
	private static BigDecimal parseBigDecimal( String value ) {
		if ( NumberUtils.isCreatable( value ) ) {
			try {
				return new BigDecimal( value, MathUtil.getMathContext() );
			} catch ( Exception e ) {
				return null;
			}

		}

		return null;
	}

}
