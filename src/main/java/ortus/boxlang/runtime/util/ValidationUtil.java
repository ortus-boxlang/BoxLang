/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.util;

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.GreaterThanEqual;
import ortus.boxlang.runtime.operators.LessThanEqual;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.UDF;

/**
 * Utility class for validating user-level data types such as credit cards, postal codes, phone numbers, and URLs.
 *
 * Is not concerned with whether the data is parseable or can be converted to a given type, only whether it is in a valid format.
 */
public class ValidationUtil {

	/**
	 * Perform the Lunh algorithm to validate a credit card number.
	 * <p>
	 * Validates that the card is between 12 and 19 digits long, contains no alphabetic characters, and passes the Luhn algorithm.
	 * <p>
	 * Does not validate:
	 * <ul>
	 * <li>the existence of the card number</li>
	 * <li>the card number prefix</li>
	 * <li>whether the card number length matches the prefix. (For example, Visa card numbers can be 13, 16, or 19 characters depending on the sub
	 * brand.)</li>
	 * </ul>
	 * <p>
	 * More extensive validation is required to ensure a card number is valid, and should be performed by either the card issuer or a third-party
	 * validation library such as Apache Commons Validator.
	 *
	 * @param cardNumber String to check for a valid credit card number format.
	 *
	 * @return Boolean indicating whether the given string is a valid credit card number.
	 */
	public static boolean isValidCreditCard( String cardNumber ) {
		int	shortestValidCardLength	= 12;
		int	longestValidCardLength	= 19;
		if ( cardNumber == null || !RegexBuilder.of( cardNumber, RegexBuilder.CREDIT_CARD_NUMBERS ).matches() ) {
			// cardNumber contains characters other than digit, space, or underscore
			return false;
		}
		String sanitized = RegexBuilder.of( cardNumber, RegexBuilder.NO_DIGITS ).replaceAllAndGet( "" );
		if ( sanitized.length() < shortestValidCardLength || sanitized.length() > longestValidCardLength ) {
			return false;
		}
		int		sum			= 0;
		boolean	alternate	= false;
		for ( int i = sanitized.length() - 1; i >= 0; i-- ) {
			int n = Character.getNumericValue( sanitized.charAt( i ) );
			if ( alternate ) {
				n *= 2;
				if ( n > 9 ) {
					n = ( n % 10 ) + 1;
				}
			}
			sum			+= n;
			alternate	= !alternate;
		}
		return ( sum % 10 == 0 );
	}

	/**
	 * Validates the given string is a valid integer value.
	 *
	 * @param value String or object value to check for a valid integer format. Only Integer types or strings containing ONLY digits will return true.
	 *
	 * @return Boolean indicating whether the given string is a valid integer
	 */
	public static boolean isValidInteger( Object value ) {
		return value instanceof Integer || ( value instanceof String stringVal && NumberUtils.isDigits( stringVal ) );
	}

	/**
	 * Validates the given string is a valid numeric value.
	 *
	 * @param value String or object value to check for a valid numeric format, including Number types and strings containing numeric values.
	 *
	 * @return Boolean indicating whether the given string is a valid numeric value.
	 */
	public static boolean isValidNumeric( Object value ) {
		return value instanceof Number || ( value instanceof String stringVal && NumberUtils.isCreatable( stringVal ) );
	}

	/**
	 * Validates the given string is a valid Version 4 UUID - in SQL Server, this is known as a GUID.
	 * <p>
	 * <strong>Beware:</strong> This will <em>not</em> match values from <code>createUUID()</code>. For that, use {@link #isValidUUID(String)}.
	 *
	 * @param uuid String to check for a valid compatible UUID format.
	 *
	 * @return Boolean indicating whether the given string is a valid compatible UUID.
	 */
	public static boolean isValidGUID( String uuid ) {
		return RegexBuilder.of( uuid, RegexBuilder.UUID_V4 ).matches();
	}

	/**
	 * Validates the given string is a valid compatible UUID.
	 * <p>
	 * A compat UUID is a version 4 UUID with the final hypen removed. If you want to validate a standard UUID, use {@link #isValidGUID(String)}.
	 *
	 * @param uuid String to check for a valid compatible UUID format.
	 *
	 * @return Boolean indicating whether the given string is a valid compatible UUID.
	 */
	public static boolean isValidUUID( String uuid ) {
		return RegexBuilder.of( uuid, RegexBuilder.UUID_PATTERN ).matches();
	}

	/**
	 * Validates a Social Security Number (SSN) in the format of 123-45-6789 or 123456789.
	 * <p>
	 * Expressly disallows certain invalid SSNs, such as 000-00-0000, 666-xx-xxxx, and 9xx-xx-xxxx, as well as a few SSNs that have been disallowed since
	 * their accidental publishing to the public.
	 *
	 * @param ssn String to check for a valid SSN format.
	 *
	 * @return Boolean indicating whether the given string is a valid SSN.
	 */
	public static boolean isValidSSN( String ssn ) {
		return RegexBuilder.of( ssn.replace( "-", "" ).replace( " ", "" ), RegexBuilder.SSN ).matches();
	}

	/**
	 * Validates a North American Numbering Plan (NANP) telephone number. This does not support international numbers.
	 *
	 * @param phone Phone number in string format. Dash-delimited, space-delimited, or no-dash variants are all supported.
	 *
	 * @return Boolean indicating whether the given string is a valid US or North American telephone number.
	 */
	public static boolean isValidTelephone( String phone ) {
		return RegexBuilder.of( phone, RegexBuilder.TELEPHONE ).matches();
	}

	/**
	 * Validates a URL string.
	 *
	 * @param url URL in string format. Must include a scheme, such as `http`, `https`, `ftp`, or `file`.
	 *
	 * @return Boolean indicating whether the given string is a valid URL.
	 */
	public static boolean isValidURL( String url ) {
		return RegexBuilder.of( url, RegexBuilder.URL ).matches();
	}

	/**
	 * Validates US-only postal codes. Matches 5-digit and 9-digit (ZIP+4) codes, with or without a space or hyphen separator.
	 * <p>
	 * <strong>Beware:</strong> This method does not check for zip code ranges or existence. For example, zip codes starting with 429 have not been
	 * assigned, and zip codes starting with 987 have been discontinued.
	 * <p>
	 * Does not support international postal codes.
	 *
	 * @param zipCode String to check for a valid zip code format.
	 *
	 * @return Boolean indicating whether the given string is a valid zip code.
	 */
	public static boolean isValidZipCode( String zipCode ) {
		return RegexBuilder.of( zipCode, RegexBuilder.ZIPCODE ).matches();
	}

	/**
	 * Validates a variable name is valid.
	 * <p>
	 * A valid variable name must start with a letter or underscore, and contain only letters, numbers, and underscores.
	 *
	 * @param variableName String to check for a valid variable name format.
	 *
	 * @return Boolean indicating whether the given string is a valid variable name.
	 */
	public static boolean isValidVariableName( String variableName ) {
		return RegexBuilder.of( variableName, RegexBuilder.VALID_VARIABLENAME ).matches();
	}

	/**
	 * Verifies if the incoming object is binary or not
	 *
	 * @param value The object to check
	 */
	public static boolean isBinary( Object value ) {
		return value instanceof byte[];
	}

	/**
	 * Verifies if the incoming object is a float or not
	 *
	 * @param value The object to check
	 */
	public static boolean isFloat( Object value ) {
		if ( value == null ) {
			return false;
		}

		if ( value instanceof Float || value instanceof Double ) {
			return true;
		}

		// Can we cast to string?
		CastAttempt<String> stringAttempt = StringCaster.attempt( value );
		if ( stringAttempt.wasSuccessful() ) {
			try {
				Float.parseFloat( stringAttempt.get() );
				return true;
			} catch ( NumberFormatException e ) {
				return false;
			}
		}

		return false;
	}

	/**
	 * Verifies the incoming object is a Box Class
	 *
	 * @param value The object to check
	 */
	public static boolean isBoxClass( Object value ) {
		return value instanceof IClassRunnable;
	}

	/**
	 * Verifies the incoming object is a Function
	 *
	 * @param value The object to check
	 */
	public static boolean isFunction( Object value ) {
		return value instanceof Function;
	}

	/**
	 * Verifies the incoming object is a Function
	 *
	 * @param value The object to check
	 */
	public static boolean isUDF( Object value ) {
		return value instanceof UDF;
	}

	/**
	 * Verifies the incoming object is a Closure
	 *
	 * @param value The object to check
	 */
	public static boolean isClosure( Object value ) {
		return value instanceof Closure;
	}

	/**
	 * Verifies the incoming object is a Lambda
	 *
	 * @param value The object to check
	 */
	public static boolean isLambda( Object value ) {
		return value instanceof Lambda;
	}

	/**
	 * Verifies if the incoming object is within the
	 * incoming min and max range
	 *
	 * @param value The object to check
	 * @param min   The minimum value
	 * @param max   The maximum value
	 *
	 * @return Boolean indicating if the value is within the range
	 */
	public static boolean isValidRange( Object value, Number min, Number max ) {
		CastAttempt<Number> castedValue = NumberCaster.attempt( value );
		if ( castedValue.wasSuccessful() ) {
			Number nValue = castedValue.get();
			return GreaterThanEqual.invoke( nValue, min ) && LessThanEqual.invoke( nValue, max );
		}
		return false;
	}

	/**
	 * Verifies if the incoming value matches the passed regular expression
	 *
	 * @param value The value to check
	 * @param regex The regular expression to match
	 *
	 * @return Boolean indicating if the value matches the regex
	 */
	public static boolean isValidMatch( String value, String regex ) {
		return RegexBuilder.of( value, regex ).matches();
	}

	/**
	 * Verifies if the incoming value matches the passed regular expression
	 * without case-sensitivity
	 *
	 * @param value The value to check
	 * @param regex The regular expression to match
	 *
	 * @return Boolean indicating if the value matches the regex
	 */
	public static boolean isValidMatchNoCase( String value, String regex ) {
		return isValidMatch( value, "(?i)" + regex );
	}

	/**
	 * Verifies if the incoming object is a valid email address
	 *
	 * @param email The email address to validate
	 */
	public static boolean isValidEmail( String email ) {
		return RegexBuilder.of( email, RegexBuilder.EMAIL ).matches();
	}

	/**
	 * Verifies that the incoming string matches
	 * the incoming regex pattern
	 *
	 * @param value   The string to validate
	 * @param pattern The regex pattern to match
	 */
	public static boolean isValidPattern( String value, String pattern ) {
		return RegexBuilder.of( value, pattern ).matches();
	}

}
