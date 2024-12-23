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

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * I handle casting anything to a Number, but I will use the "smallest" number type possible
 * So Integer, Long, Double, BigDecimal
 */
public class NumberCaster implements IBoxCaster {

	public static boolean booleansAreNumbers = false;

	/**
	 * Tests to see if the value can be cast to a Number.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Number
	 *
	 * @return The Number value
	 */
	public static CastAttempt<Number> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Number, throwing exception if we fail
	 *
	 * @param object The value to cast to a Number
	 *
	 * @return The Number value
	 */
	public static Number cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Number
	 *
	 * @param object The value to cast to a Number
	 * @param fail   If true, throw exception if we fail
	 *
	 * @return The Number value
	 */
	public static Number cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return 0;
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Integer i ) {
			return i;
		}
		if ( object instanceof Long l ) {
			return l;
		}
		if ( object instanceof Double d ) {
			return d;
		}
		if ( object instanceof BigDecimal bd ) {
			return bd;
		}
		if ( object instanceof BigInteger bi ) {
			return new BigDecimal( bi, MathUtil.getMathContext() );
		}

		if ( object instanceof Number num ) {
			return new BigDecimal( num.doubleValue(), MathUtil.getMathContext() );
		}

		// Only here for compat. This "hidden setting" can be toggled by the compat module
		if ( booleansAreNumbers ) {
			if ( object instanceof Boolean bool ) {
				return bool ? 1 : 0;
			}

			if ( object instanceof String str ) {
				// String true and yes are truthy
				if ( str.equalsIgnoreCase( "true" ) || str.equalsIgnoreCase( "yes" ) ) {
					return 1;
					// String false and no are truthy
				} else if ( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "no" ) ) {
					return 0;
				}
			}
		}

		// Try to parse the string as a Number
		String	stringValue	= StringCaster.cast( object, false );
		Number	result		= parseNumber( stringValue );
		if ( result != null ) {
			return result;
		}

		// Verify if we can throw an exception
		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a Number.", object.toString() ) );
		} else {
			return null;
		}

	}

	/**
	 * Determine whether the provided string is castable to a Number.
	 *
	 * @param value A probably-hopefully Number string value, with an optional plus/minus sign.
	 *
	 * @return Optional - parsed Number if all string characters are digits, with an optional sign and decimal point. Empty optional for empty string,
	 *         null, floats, alpha characters, etc.
	 */
	private static Number parseNumber( String value ) {
		if ( value == null )
			return null;
		// strip trailing period
		if ( value.endsWith( "." ) ) {
			value = value.substring( 0, value.length() - 1 );
		}
		if ( NumberUtils.isCreatable( value ) ) {
			try {
				int len = value.length();
				// If there is a decimal point or scientific notation, return a BigDecimal
				if ( value.contains( "." ) || value.contains( "e" ) || value.contains( "E" ) ) {
					// Use BigDecimal for high precision math or if the number is too large
					if ( MathUtil.isHighPrecisionMath() || len > 19 ) {
						return new BigDecimal( value, MathUtil.getMathContext() );
					} else {
						return Double.parseDouble( value );
					}
				}

				// 10 or fewer chars can use an int literal
				if ( len < 10 ) {
					return Integer.parseInt( value );
				} else if ( len <= 19 ) {
					// 11-19 chars needs a long literal
					return Long.parseLong( value );
				} else {
					// 20 or more chars needs a BigDecimal
					return new BigDecimal( value, MathUtil.getMathContext() );
				}
			} catch ( Exception e ) {
				return null;
			}
		}

		return null;
	}

}
