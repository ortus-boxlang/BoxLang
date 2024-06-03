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
package ortus.boxlang.runtime.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.LocaleUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct.TYPES;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A Collection of Common Static Properties and Methods to support Localization
 **/
public final class LocalizationUtil {

	/**
	 * A struct of common locale constants
	 */
	public static final Struct COMMON_LOCALES = new Struct( TYPES.LINKED, new LinkedHashMap<Key, Locale>() );
	static {
		COMMON_LOCALES.put( Key.of( "Canada" ), Locale.CANADA );
		COMMON_LOCALES.put( Key.of( "Canadian" ), Locale.CANADA );
		COMMON_LOCALES.put( Key.of( "Canada_French" ), Locale.CANADA_FRENCH );
		COMMON_LOCALES.put( Key.of( "French Canadian" ), Locale.CANADA_FRENCH );
		COMMON_LOCALES.put( Key.of( "China" ), Locale.CHINA );
		COMMON_LOCALES.put( Key.of( "Chinese" ), Locale.CHINESE );
		COMMON_LOCALES.put( Key.of( "English" ), Locale.ENGLISH );
		COMMON_LOCALES.put( Key.of( "France" ), Locale.FRANCE );
		COMMON_LOCALES.put( Key.of( "French" ), Locale.FRENCH );
		COMMON_LOCALES.put( Key.of( "German" ), Locale.GERMAN );
		COMMON_LOCALES.put( Key.of( "Germany" ), Locale.GERMANY );
		COMMON_LOCALES.put( Key.of( "Italian" ), Locale.ITALIAN );
		COMMON_LOCALES.put( Key.of( "Italy" ), Locale.ITALY );
		COMMON_LOCALES.put( Key.of( "Japan" ), Locale.JAPAN );
		COMMON_LOCALES.put( Key.of( "Japanese" ), Locale.JAPANESE );
		COMMON_LOCALES.put( Key.of( "Korea" ), Locale.KOREA );
		COMMON_LOCALES.put( Key.of( "Korean" ), Locale.KOREAN );
		COMMON_LOCALES.put( Key.of( "PRC" ), Locale.PRC );
		COMMON_LOCALES.put( Key.of( "root" ), Locale.ROOT );
		COMMON_LOCALES.put( Key.of( "Simplified_Chinese" ), Locale.SIMPLIFIED_CHINESE );
		COMMON_LOCALES.put( Key.of( "Taiwan" ), Locale.TAIWAN );
		COMMON_LOCALES.put( Key.of( "Traditional_Chinese" ), Locale.TRADITIONAL_CHINESE );
		COMMON_LOCALES.put( Key.of( "UK" ), Locale.UK );
		COMMON_LOCALES.put( Key.of( "United Kingdom" ), Locale.UK );
		COMMON_LOCALES.put( Key.of( "British" ), Locale.UK );
		// We need to use an explicit country setting because new versions of JDK 17 and 21
		// return just "English" with Locale.US
		COMMON_LOCALES.put( Key.of( "US" ), new Locale( "en", "US" ) );
		COMMON_LOCALES.put( Key.of( "United States" ), new Locale( "en", "US" ) );
	}

	public static final Array	ISO_COUNTRIES	= new Array( Locale.getISOCountries() );
	public static final Array	ISO_LANGUAGES	= new Array( Locale.getISOLanguages() );

	/**
	 * A collection of common locale aliases which are used by both ACF and Lucee
	 */
	public static final Struct	LOCALE_ALIASES	= new Struct( TYPES.LINKED, new LinkedHashMap<Key, Locale>() );
	static {
		LOCALE_ALIASES.put( Key.of( "Albanian (Albania)" ), new Locale( "sq", "AL" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Algeria)" ), new Locale( "ar", "DZ" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Bahrain)" ), new Locale( "ar", "BH" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Egypt)" ), new Locale( "ar", "EG" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Iraq)" ), new Locale( "ar", "IQ" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Jordan)" ), new Locale( "ar", "JO" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Kuwait)" ), new Locale( "ar", "KW" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Lebanon)" ), new Locale( "ar", "LB" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Libya)" ), new Locale( "ar", "LY" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Morocco)" ), new Locale( "ar", "MA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Oman)" ), new Locale( "ar", "OM" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Qatar)" ), new Locale( "ar", "QA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Saudi Arabia)" ), new Locale( "ar", "SA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Sudan)" ), new Locale( "ar", "SD" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Syria)" ), new Locale( "ar", "SY" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Tunisia)" ), new Locale( "ar", "TN" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (United Arab Emirates)" ), new Locale( "ar", "AE" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Yemen)" ), new Locale( "ar", "YE" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (China)" ), Locale.CHINA );
		LOCALE_ALIASES.put( Key.of( "Chinese (Hong Kong)" ), new Locale( "zh", "HK" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (Singapore)" ), new Locale( "zh", "SG" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (Taiwan)" ), new Locale( "zh", "TW" ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Belgian)" ), new Locale( "nl", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Belgium)" ), LOCALE_ALIASES.get( Key.of( "dutch (belgian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Standard)" ), new Locale( "nl", "NL" ) );
		LOCALE_ALIASES.put( Key.of( "English (Australian)" ), new Locale( "en", "AU" ) );
		LOCALE_ALIASES.put( Key.of( "English (Australia)" ), LOCALE_ALIASES.get( Key.of( "english (australian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (Canadian)" ), Locale.CANADA );
		LOCALE_ALIASES.put( Key.of( "English (Canada)" ), Locale.CANADA );
		LOCALE_ALIASES.put( Key.of( "English (New zealand)" ), new Locale( "en", "NZ" ) );
		LOCALE_ALIASES.put( Key.of( "English (UK)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (United Kingdom)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (GB)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (Great Britan)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (US)" ), new Locale( "en", "US" ) );
		LOCALE_ALIASES.put( Key.of( "English (USA)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (United States)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (United States of America)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "French (Belgium)" ), new Locale( "fr", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "French (Belgian)" ), new Locale( "fr", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "French (Canadian)" ), Locale.CANADA_FRENCH );
		LOCALE_ALIASES.put( Key.of( "French (Canadia)" ), Locale.CANADA_FRENCH );
		LOCALE_ALIASES.put( Key.of( "French (Standard)" ), Locale.FRANCE );
		LOCALE_ALIASES.put( Key.of( "French (Swiss)" ), new Locale( "fr", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "German (Austrian)" ), new Locale( "de", "AT" ) );
		LOCALE_ALIASES.put( Key.of( "German (Austria)" ), new Locale( "de", "AT" ) );
		LOCALE_ALIASES.put( Key.of( "German (Standard)" ), Locale.GERMANY );
		LOCALE_ALIASES.put( Key.of( "German (Swiss)" ), new Locale( "de", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Italian (Standard)" ), Locale.ITALIAN );
		LOCALE_ALIASES.put( Key.of( "Italian (Swiss)" ), new Locale( "it", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Japanese" ), Locale.JAPANESE );
		LOCALE_ALIASES.put( Key.of( "Korean" ), Locale.KOREAN );
		LOCALE_ALIASES.put( Key.of( "Norwegian (Bokmal)" ), new Locale( "no", "NO" ) );
		LOCALE_ALIASES.put( Key.of( "Norwegian (Nynorsk)" ), new Locale( "no", "NO" ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Brazilian)" ), new Locale( "pt", "BR" ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Brazil)" ), LOCALE_ALIASES.get( Key.of( "portuguese (brazilian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Standard)" ), new Locale( "pt", "PT" ) );
		LOCALE_ALIASES.put( Key.of( "Rhaeto-Romance (Swiss)" ), new Locale( "rm", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Rhaeto-Romance (Swiss)" ), new Locale( "rm", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Spanish (Modern)" ), new Locale( "es", "ES" ) );
		LOCALE_ALIASES.put( Key.of( "Spanish (Standard)" ), new Locale( "es", "ES" ) );
		LOCALE_ALIASES.put( Key.of( "Swedish" ), new Locale( "sv", "SE" ) );
		LOCALE_ALIASES.put( Key.of( "Welsh" ), new Locale( "cy", "GB" ) );
	}

	/**
	 * Common Number formatter instances
	 */
	public static final Struct COMMON_NUMBER_FORMATTERS = new Struct( new HashMap<Key, java.text.NumberFormat>() );
	static {
		COMMON_NUMBER_FORMATTERS.put(
		    Key.of( "USD" ),
		    DecimalFormat.getCurrencyInstance( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) )
		);
		COMMON_NUMBER_FORMATTERS.put(
		    Key.of( "EURO" ),
		    DecimalFormat.getCurrencyInstance( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "German" ) )
		);
	}

	/**
	 * Common DateTime formatter instances
	 */
	public static final Key		DEFAULT_NUMBER_FORMAT_KEY	= Key.of( "," );

	/**
	 * Common number format patterns and shorthand variations
	 */
	public static final Struct	NUMBER_FORMAT_PATTERNS		= new Struct( new HashMap<Key, String>() );
	static {
		NUMBER_FORMAT_PATTERNS.put( Key.of( "()" ), "0;(0)" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,2" ), "#.00" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,3" ), "#.000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,4" ), "#.0000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,5" ), "#.00000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,6" ), "#.000000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,7" ), "#.0000000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,8" ), "#.00000000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "_,9" ), "#.000000000" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "+" ), "+0;-0" );
		NUMBER_FORMAT_PATTERNS.put( Key.of( "-" ), " 0;-0" );
		NUMBER_FORMAT_PATTERNS.put( Key.dollarFormat, "$#,##0.00;($#,##0.00)" );
		NUMBER_FORMAT_PATTERNS.put( DEFAULT_NUMBER_FORMAT_KEY, "#,#00.#" );
	}

	public static final String	CURRENCY_TYPE_LOCAL			= "local";
	public static final String	CURRENCY_TYPE_INTERNATIONAL	= "international";
	public static final String	CURRENCY_TYPE_NONE			= "none";

	/**
	 * A struct of ZoneID aliases ( e.g. PST )
	 */
	public static final Struct	ZONE_ALIASES				= new Struct( new HashMap<Key, String>() );
	static {
		ZONE_ALIASES.putAll(
		    ZoneId.SHORT_IDS.entrySet()
		        .stream()
		        .collect( Collectors.toMap( entry -> Key.of( entry.getKey() ), entry -> entry.getValue() ) )
		);
		ZONE_ALIASES.putAll(
		    ZoneId.getAvailableZoneIds()
		        .stream()
		        .collect( Collectors.toMap( entry -> Key.of( entry ), entry -> entry ) )
		);
		ZONE_ALIASES.put( Key.of( "PDT" ), "America/Los_Angeles" );
		ZONE_ALIASES.put( Key.of( "MDT" ), "America/Denver" );
		ZONE_ALIASES.put( Key.of( "CDT" ), "America/Chicago" );
		ZONE_ALIASES.put( Key.of( "EDT" ), "America/New_York" );
	}

	/**
	 * Parses a locale from a string, handling known common locales and aliases.
	 *
	 * @param requestedLocale The string representation of the requested locale or alias
	 *
	 * @return the Locale object or null if the locale could not be parsed or found in the current JVM
	 */
	public static Locale parseLocale( String requestedLocale ) {
		Locale oLocale = null;

		// If the requested locale is null or empty string, return null
		if ( requestedLocale == null || requestedLocale.isEmpty() ) {
			return null;
		}

		// See if it's a common locale
		oLocale = ( Locale ) COMMON_LOCALES.get( Key.of( requestedLocale ) );
		if ( oLocale != null )
			return oLocale;

		// See if it's an alias
		oLocale = ( Locale ) LOCALE_ALIASES.get( Key.of( requestedLocale ) );
		if ( oLocale != null )
			return oLocale;

		// Let the lib run it's course
		try {
			oLocale = LocaleUtils.toLocale( requestedLocale );
			// Make sure it's a valid locale for this machine or return null
			return LocaleUtils.isAvailableLocale( oLocale ) ? oLocale : null;
		} catch ( IllegalArgumentException e ) {
			// This is not a valid locale
			return null;
		}
	}

	/**
	 * Checks if a locale is available for the current JVM
	 *
	 * @param locale The locale to check
	 *
	 * @return true if the locale is valid, false otherwise
	 */
	public static boolean isAvailableLocale( Locale locale ) {
		return LocaleUtils.isAvailableLocale( locale );
	}

	/**
	 * Get a human-friendly display name for a locale
	 *
	 * @param locale The locale to display
	 *
	 * @return The display name in the format `Language (Country)` or `Language (Variant)` if no country is present
	 */
	public static String getLocaleDisplayName( Locale locale ) {
		return String.format(
		    "%s (%s)",
		    locale.getDisplayLanguage( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) ),
		    BooleanCaster.cast( locale.getVariant().length() ) ? locale.getVariant()
		        : locale.getDisplayCountry( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) )
		);
	}

	/**
	 * Parses a locale and returns a default value if the locale could not be parsed
	 *
	 * @param requestedLocale the string representation of the requested locale or alias
	 * @param defaultLocale   the default locale to use if not found
	 *
	 * @return The Locale object found or the default
	 */
	public static Locale parseLocaleOrDefault( String requestedLocale, Locale defaultLocale ) {
		Locale locale = parseLocale( requestedLocale );
		return locale != null ? locale : defaultLocale;
	}

	/**
	 * Convience method to extract the locale from arguments or context, falling back tothe system default
	 *
	 * @param context   The context from which to extract the default locale
	 * @param arguments The arguments scope which may or may not contain a locale key
	 *
	 * @return The Locale object found or the default
	 */
	public static Locale parseLocaleFromContext( IBoxContext context, ArgumentsScope arguments ) {
		RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );
		return parseLocaleOrDefault(
		    arguments.getAsString( Key.locale ),
		    requestContext.getLocale() != null ? requestContext.getLocale() : ( Locale ) context.getConfig().getAsStruct( Key.runtime ).get( Key.locale )
		);
	}

	/**
	 * Parses a ZoneId from a string, falling back to the context setting, and then the system default
	 *
	 * @param timezone The timezone string representation
	 * @param context  The context to retrieve the config item
	 *
	 * @return the ZoneId instance representing the assigned timezone
	 */
	public static ZoneId parseZoneId( String timezone, IBoxContext context ) {
		if ( timezone != null ) {
			Key zoneKey = Key.of( timezone );
			if ( ZONE_ALIASES.containsKey( zoneKey ) ) {
				return ZoneId.of( ZONE_ALIASES.getAsString( zoneKey ) );
			} else {
				ZoneId parsed = parseZoneId( timezone );
				return parsed != null
				    ? parsed
				    : ( ZoneId ) context.getConfig().getAsStruct( Key.runtime ).get( Key.timezone );
			}
		} else {
			RequestBoxContext requestContext = context.getParentOfType( RequestBoxContext.class );
			if ( requestContext != null && requestContext.getTimezone() != null ) {
				return requestContext.getTimezone();
			} else {
				return ( ZoneId ) context.getConfig().getAsStruct( Key.runtime ).get( Key.timezone );
			}
		}
	}

	/**
	 * Attempts to parse a ZoneId from a string representation - return a null if the zone could not be parsed
	 *
	 * @param timezone The string representation of the timezone
	 *
	 * @return The ZoneId or null
	 */
	public static ZoneId parseZoneId( String timezone ) {
		try {
			Key zoneKey = Key.of( timezone );
			if ( ZONE_ALIASES.containsKey( zoneKey ) ) {
				return ZoneId.of( ZONE_ALIASES.getAsString( zoneKey ) );
			} else {
				return ZoneId.of( timezone );
			}
		} catch ( Exception e ) {
			return null;
		}
	}

	/**
	 * Parses a localized currency string
	 *
	 * @param value  The value to be parsed
	 * @param locale The locale object to apply to the parse operation
	 *
	 * @return
	 */
	public static Double parseLocalizedCurrency( Object value, Locale locale ) {
		DecimalFormat	parser			= ( DecimalFormat ) DecimalFormat.getCurrencyInstance( locale );
		String			stringValue		= StringCaster.cast( value );
		String			currencyCode	= parser.getCurrency().getCurrencyCode();
		// If we have an international format with the currency code we need to replace it with the symbol
		if ( stringValue.substring( 0, currencyCode.length() ).equals( currencyCode ) ) {
			stringValue = stringValue
			    .replace( currencyCode, parser.getCurrency().getSymbol() )
			    .replace( parser.getCurrency().getSymbol() + " ", parser.getCurrency().getSymbol() );
		}
		Number parsed = null;
		try {
			parsed = parser.parse( stringValue );
		} catch ( ParseException e ) {
			System.err.println( "Error parsing currency value: " + stringValue + ". The message received was:" + e.getMessage() );
		}
		return parsed == null ? null : parsed.doubleValue();
	}

	/**
	 * Parses a localized number string
	 *
	 * @param value  The value to be parsed
	 * @param locale The locale object to apply to the parse operation
	 *
	 * @return
	 */
	public static Double parseLocalizedNumber( Object value, Locale locale ) {
		DecimalFormat parser = ( DecimalFormat ) DecimalFormat.getInstance( locale );

		// If we have a non-breaking space as a thousands separator, it will get parsed as a decimal in english locales. ( BL-160 )
		if ( parser.getDecimalFormatSymbols().getGroupingSeparator() == ','
		    && parser.getDecimalFormatSymbols().getDecimalSeparator() == '.'
		    && StringCaster.cast( value ).contains( String.valueOf( ( char ) 160 ) ) ) {
			return null;
		}
		try {
			return parser.parse( StringCaster.cast( value ) ).doubleValue();
		} catch ( ParseException ex ) {
			return null;
		}

	}

	/**
	 * Returns a localized DateTimeFormatter instance
	 *
	 * @param locale the Locale instance to apply to the formatter
	 * @param style  the FormatStyle instance to apply
	 *
	 * @return
	 */
	public static DateTimeFormatter localizedDateFormatter( Locale locale, FormatStyle style ) {
		return DateTimeFormatter.ofLocalizedDate( style ).withLocale( locale );
	}

	/**
	 * Returns a localized currency formatter
	 *
	 * @param locale the Locale instance to apply to the formatter
	 *
	 * @return
	 */
	public static NumberFormat localizedCurrencyFormatter( Locale locale ) {
		return NumberFormat.getCurrencyInstance( locale );
	}

	/**
	 * Returns a localized currency formatter
	 *
	 * @param locale the Locale instance to apply to the formatter
	 * @param type   A recognized currency format type, which will change or remove the currency symbol
	 *
	 * @return
	 */
	public static NumberFormat localizedCurrencyFormatter( Locale locale, String type ) {
		DecimalFormat			formatter	= ( DecimalFormat ) localizedCurrencyFormatter( locale );
		DecimalFormatSymbols	symbols		= localizedDecimalSymbols( locale );
		switch ( type.toLowerCase() ) {
			case CURRENCY_TYPE_INTERNATIONAL : {
				symbols.setCurrencySymbol( symbols.getInternationalCurrencySymbol() + " " );
				break;
			}
			case CURRENCY_TYPE_NONE : {
				symbols.setCurrencySymbol( "" );
				break;
			}
			case CURRENCY_TYPE_LOCAL : {
				// this is the default
				break;
			}
			default : {
				throw new BoxRuntimeException(
				    String.format(
				        "The argument [type] [%s] is not recognized as a valid currency format type.",
				        type
				    )
				);
			}

		}
		formatter.setDecimalFormatSymbols( symbols );
		return formatter;

	}

	/**
	 * Returns a localized decimal formatter
	 *
	 * @param locale the Locale instance to apply to the formatter
	 *
	 * @return
	 */
	public static DecimalFormat localizedDecimalFormatter( Locale locale ) {
		DecimalFormat formatter = ( DecimalFormat ) DecimalFormat.getNumberInstance( locale );
		formatter.setDecimalFormatSymbols( localizedDecimalSymbols( locale ) );
		return formatter;
	}

	/**
	 * Returns a localized decimal formatter
	 *
	 * @param locale the Locale instance to apply to the formatter
	 *
	 * @return
	 */
	public static DecimalFormat localizedDecimalFormatter( Locale locale, String format ) {
		Key formatKey = Key.of( format );
		if ( NUMBER_FORMAT_PATTERNS.containsKey( formatKey ) ) {
			format = NUMBER_FORMAT_PATTERNS.getAsString( formatKey );
		}
		return new DecimalFormat( format, localizedDecimalSymbols( locale ) );
	}

	/**
	 * Returns the localized decimal format symbols for the specified locale
	 *
	 * @param locale the target locale instance
	 *
	 * @return
	 */
	public static DecimalFormatSymbols localizedDecimalSymbols( Locale locale ) {
		DecimalFormatSymbols	symbols		= new DecimalFormatSymbols( locale );
		// fix for some thin space grouping separators not being the expected standard for the locale
		// ( e.g de_AT allows a non-breaking space, but the decimal point is expected )
		Character				thinSpace	= '\u00a0';
		// Useful for debugging some unicode characters as group separators
		// System.out.println( String.format( "\\u%04x", ( int ) symbols.getGroupingSeparator() ) );
		if ( thinSpace.equals( ( Character ) symbols.getGroupingSeparator() ) ) {
			symbols.setGroupingSeparator( '.' );
		}
		return symbols;
	}

	/**
	 * Returns a localized set of ZonedDateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleZonedDateTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    // Localized styles
		    .appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of DateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */
	public static DateTimeFormatter getLocaleDateTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT, FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM, FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG, FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL, FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ).withLocale( locale ) )
		    // Generic styles
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.ISO_DATE_TIME_MILIS_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.ISO_DATE_TIME_VARIATION_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_DATETIME_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_INSTANT )
		    .appendOptional( DateTimeFormatter.ISO_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE_TIME )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of Date parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleDateParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ) )
		    // The ISO date methods don't account for leading zeros :(
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy.MM.dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "MM/dd/yyyy" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_DATE_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_DATE )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE )
		    .appendOptional( DateTimeFormatter.BASIC_ISO_DATE )
		    .toFormatter( locale );
	}

	/**
	 * Returns a localized set of Time parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleTimeParsers( Locale locale ) {
		DateTimeFormatterBuilder formatBuilder = new DateTimeFormatterBuilder();
		return formatBuilder.parseLenient()
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofPattern( DateTime.DEFAULT_TIME_FORMAT_MASK ) )
		    .appendOptional( DateTimeFormatter.ISO_TIME )
		    .toFormatter( locale );
	}
}
