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
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * I handle casting anything to a Double
 */
public class DoubleCaster {

	/**
	 * Tests to see if the value can be cast to a Double.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Double
	 *
	 * @return The Double value
	 */
	public static CastAttempt<Double> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Double, throwing exception if we fail
	 *
	 * @param object The value to cast to a Double
	 *
	 * @return The Double value
	 */
	public static Double cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a double
	 *
	 * @param object The value to cast to a double
	 * @param fail   If true, throw exception if we fail
	 *
	 * @return The double value
	 */
	public static Double cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return Double.valueOf( 0 );
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Double d ) {
			return d;
		}
		if ( object instanceof Number num ) {
			return num.doubleValue();
		}

		if ( object instanceof Boolean bool ) {
			return bool ? 1D : 0D;
		}

		if ( object instanceof String str ) {
			// String true and yes are truthy
			if ( str.equalsIgnoreCase( "true" ) || str.equalsIgnoreCase( "yes" ) ) {
				return 1D;
				// String false and no are truthy
			} else if ( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "no" ) ) {
				return 0D;
			}
		}

		// Try to parse the string as a double
		String	stringValue	= StringCaster.cast( object, false );
		Double	result		= parseDouble( stringValue );
		if ( result != null ) {
			return result;
		}

		// Verify if we can throw an exception
		if ( fail ) {
			throw new ApplicationException( String.format( "Can't cast %s to a double.", object.toString() ) );
		} else {
			return null;
		}

	}

	/**
	 * Determine whether the provided string is castable to a Double.
	 *
	 * @param value A probably-hopefully double string value, with an optional plus/minus sign.
	 *
	 * @return Optional - parsed Double if all string characters are digits, with an optional sign and decimal point. Empty optional for empty string,
	 *         null, floats, alpha characters, etc.
	 */
	private static Double parseDouble( String value ) {
		if ( value == null )
			return null;

		int signMultiplier = value.startsWith( "-" ) ? -1 : 1;
		value = value.trim();
		if ( value.startsWith( "-" ) || value.startsWith( "+" ) ) {
			value = value.substring( 1 );
		}
		if ( value.isBlank() )
			return null;

		char[]	charArray		= value.toCharArray();
		double	intValue		= 0.0;
		double	fractionValue	= 0.0;

		/**
		 * @TODO: Support 'NAN' and 'INFINITY' strings?
		 *        https://github.com/openjdk/jdk17/blob/master/src/java.base/share/classes/jdk/internal/math/FloatingDecimal.java#L1854-L1865
		 */

		// Get decimal point and validate characters are numeric
		int		decimalIndex	= -1;
		boolean	hasDecimal		= false;
		for ( int i = 0; i < charArray.length; i++ ) {
			if ( charArray[ i ] == '.' ) {
				if ( hasDecimal ) {
					// Multiple decimal points; throw or return null!
					return null;
				}
				decimalIndex	= i;
				hasDecimal		= true;
				continue;
			}
			if ( !Character.isDigit( charArray[ i ] ) ) {
				return null;
			}
		}

		// Process the integer part
		int integerEnd = charArray.length - 1;
		if ( hasDecimal ) {
			integerEnd = decimalIndex - 1;
		}
		for ( int i = 0; i <= integerEnd; i++ ) {
			int digit = charArray[ i ] - '0';
			intValue += ( digit * Math.pow( 10, ( integerEnd - i ) ) );
		}

		// Process the fractional part
		if ( hasDecimal ) {
			var	decimalStart	= decimalIndex + 1;
			var	decimalEnd		= charArray.length;
			for ( int i = decimalStart, j = 1; i < decimalEnd; i++, j++ ) {
				int digit = charArray[ i ] - '0';
				fractionValue += digit * Math.pow( 10, -j );
			}
		}
		return signMultiplier * ( intValue + fractionValue );
	}

}
