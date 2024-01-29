package ortus.boxlang.runtime.util;

import java.util.regex.Pattern;

public class ValidationUtil {

	/**
	 * Regular expression, in {@link String} form, to match a United States Postal Service (USPS)
	 * ZIP Code.
	 */
	public static final String	ZIPCODE_STRING			= "\\d{5}([ -]?\\d{4})?";
	/**
	 * Regular expression Pattern to match a United States Postal Service (USPS) ZIP Code.
	 */
	public static final Pattern	ZIPCODE					= Pattern.compile( ZIPCODE_STRING );

	/**
	 * Regular expression, in {@link String} form, to match a Social Security Number (SSN).
	 */
	public static final String	SSN_STRING				= "^(?!219099999|078051120)(?!666|000|9\\d{2})\\d{3}(?!00)\\d{2}(?!0{4})\\d{4}$";

	/**
	 * Regular expression Pattern to match a Social Security Number (SSN).
	 */
	public static final Pattern	SSN						= Pattern.compile( SSN_STRING );

	/**
	 * Regular expression, in {@link String} form, to match a Version 4 Universally Unique Identifier
	 * (UUID), in a case-insensitive fashion.
	 *
	 * @see https://gitlab.com/jamietanna/uuid/-/blob/v0.2.0/uuid-core/src/main/java/me/jvt/uuid/Patterns.java
	 */
	public static final String	UUID_V4_STRING			= "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}";
	/**
	 * Regular expression to match a Version 4 Universally Unique Identifier (UUID), in a
	 * case-insensitive fashion.
	 */
	public static final Pattern	UUID_V4					= Pattern.compile( UUID_V4_STRING );

	/**
	 * Regular expression, in {@link String} form, to match a Version 4 Universally Unique Identifier
	 * (UUID), in a case-insensitive fashion.
	 *
	 * @see https://gitlab.com/jamietanna/uuid/-/blob/v0.2.0/uuid-core/src/main/java/me/jvt/uuid/Patterns.java
	 */
	public static final String	CFML_COMPAT_UUID_STRING	= "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}[a-fA-F0-9]{12}";
	/**
	 * Regular expression to match a Version 4 Universally Unique Identifier (UUID), in a
	 * case-insensitive fashion.
	 */
	public static final Pattern	CFCOMPAT_UUID_PATTERN	= Pattern.compile( CFML_COMPAT_UUID_STRING );

	/**
	 * Validates the given string is a valid Version 4 UUID.
	 * <p>
	 * <strong>Beware:</strong> This will <em>not</em> match values from <code>createUUID()</code>. For that, use {@link #isValidCFUUID(String)}.
	 *
	 * @param uuid String to check for a valid CFML-compatible UUID format.
	 *
	 * @return Boolean indicating whether the given string is a valid CFML-compatible UUID.
	 */
	public static boolean isValidUUID( String uuid ) {
		return UUID_V4.matcher( uuid ).matches();
	}

	/**
	 * Validates the given string is a valid CFML-compatible UUID.
	 * <p>
	 * A CFML-compat UUID is a version 4 UUID with the final hypen removed. If you want to validate a standard UUID, use {@link #isValidUUID(String)}.
	 *
	 * @param uuid String to check for a valid CFML-compatible UUID format.
	 *
	 * @return Boolean indicating whether the given string is a valid CFML-compatible UUID.
	 */
	public static boolean isValidCFUUID( String uuid ) {
		return CFCOMPAT_UUID_PATTERN.matcher( uuid ).matches();
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
