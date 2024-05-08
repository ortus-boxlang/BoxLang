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

import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Utility class for validating user-level data types such as credit cards, postal codes, phone numbers, and URLs.
 *
 * Is not concerned with whether the data is parseable or can be converted to a given type, only whether it is in a valid format.
 */
public class ValidationUtil {

	/**
	 * Regular expression Pattern to match a URL with a `http`, `https`, `ftp`, or `file` scheme.
	 *
	 * @see https://regex101.com/r/kWhB1u/1
	 */
	public static final Pattern	URL				= Pattern.compile( "^(https?|ftp|file)://([A-Za-z0-90.]*)/?([-a-zA-Z0-9.+&@#/]+)?(\\??[^\\s]*)$" );

	/**
	 * Regular expression Pattern to match a North American Numbering Plan (NANP) telephone number. This does not support international numbers.
	 */
	public static final Pattern	TELEPHONE		= Pattern.compile(
	    "^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$" );

	/**
	 * Regular expression Pattern to match a United States Postal Service (USPS) ZIP Code.
	 */
	public static final Pattern	ZIPCODE			= Pattern.compile( "\\d{5}([ -]?\\d{4})?" );

	/**
	 * Regular expression Pattern to match a Social Security Number (SSN).
	 */
	public static final Pattern	SSN				= Pattern.compile( "^(?!219099999|078051120)(?!666|000|9\\d{2})\\d{3}(?!00)\\d{2}(?!0{4})\\d{4}$" );

	/**
	 * Regular expression to match a Version 4 Universally Unique Identifier (UUID), in a
	 * case-insensitive fashion.
	 *
	 * @see https://gitlab.com/jamietanna/uuid/-/blob/v0.2.0/uuid-core/src/main/java/me/jvt/uuid/Patterns.java
	 */
	public static final Pattern	UUID_V4			= Pattern
	    .compile( "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}" );

	/**
	 * Regular expression to match a Version 4 Universally Unique Identifier (UUID), in a
	 * case-insensitive fashion.
	 */
	public static final Pattern	UUID_PATTERN	= Pattern
	    .compile( "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}[a-fA-F0-9]{12}" );

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
		if ( cardNumber == null || !cardNumber.matches( "[0-9 ,_-]+" ) ) {
			// cardNumber contains characters other than digit, space, or underscore
			return false;
		}
		String sanitized = cardNumber.replaceAll( "[^0-9]", "" );
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
		return UUID_V4.matcher( uuid ).matches();
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
		return UUID_PATTERN.matcher( uuid ).matches();
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
		return SSN.matcher(
		    ssn.replace( "-", "" ).replace( " ", "" )
		).matches();
	}

	/**
	 * Validates a North American Numbering Plan (NANP) telephone number. This does not support international numbers.
	 *
	 * @param phone Phone number in string format. Dash-delimited, space-delimited, or no-dash variants are all supported.
	 *
	 * @return Boolean indicating whether the given string is a valid US or North American telephone number.
	 */
	public static boolean isValidTelephone( String phone ) {
		return TELEPHONE.matcher( phone ).matches();
	}

	/**
	 * Validates a URL string.
	 *
	 * @param url URL in string format. Must include a scheme, such as `http`, `https`, `ftp`, or `file`.
	 *
	 * @return Boolean indicating whether the given string is a valid URL.
	 */
	public static boolean isValidURL( String url ) {
		return URL.matcher( url ).matches();
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
		return ZIPCODE.matcher( zipCode ).matches();
	}
}
