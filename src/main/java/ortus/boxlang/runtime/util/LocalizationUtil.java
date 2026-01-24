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

import java.lang.ref.SoftReference;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.LocaleUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A Collection of Common Static Properties and Methods to support Localization
 **/
public final class LocalizationUtil {

	/**
	 * The runtime logger
	 */
	private static final LoggingService													loggingService						= BoxRuntime.getInstance()
	    .getLoggingService();

	/**
	 * Cache for DateTimeFormatter instances keyed by cache key string.
	 * Uses SoftReference to allow garbage collection under memory pressure.
	 */
	private static final ConcurrentHashMap<String, SoftReference<DateTimeFormatter>>	formatterCache						= new ConcurrentHashMap<>();

	/**
	 * Cache keys for different formatter types
	 */
	private static final String															LOCALE_ZONED_DATETIME_PREFIX		= "locale_zoned_datetime_";
	private static final String															ALT_LOCALE_ZONED_DATETIME_PREFIX	= "alt_locale_zoned_datetime_";
	private static final String															LOCALE_DATETIME_PREFIX				= "locale_datetime_";
	private static final String															LOCALE_DATE_PREFIX					= "locale_date_";
	private static final String															LOCALE_TIME_PREFIX					= "locale_time_";
	private static final String															PATTERN_FORMATTER_PREFIX			= "pattern_formatter_";
	private static final String															LOCALE_PATTERN_FORMATTER_PREFIX		= "locale_pattern_formatter_";

	/**
	 * A wrapper class that combines a regex pattern with its corresponding DateTimeFormatter
	 * for efficient pattern matching before attempting to parse.
	 */
	public static class CommonFormatter {

		private final Pattern				pattern;
		private final DateTimeFormatter		formatter;
		private final String				description;
		private final String				regexPattern;
		private final String				datePattern;
		private final TemporalQuery<?>[]	optimizedQueries;

		private static final Pattern		dateMatchPattern		= Pattern.compile( ".*[yMLdDjgEFwWu].*" );
		private static final Pattern		timeMatchPattern		= Pattern.compile( ".*[HhKkmsSnA].*" );
		private static final Pattern		timezoneMatchPattern	= Pattern.compile( ".*[zZVvXxOo].*" );
		private static final Pattern		offsetMatchPattern		= Pattern.compile( ".*[XxZO].*" );

		public CommonFormatter( String regexPattern, String datePattern, String description ) {
			this.regexPattern		= regexPattern;
			this.datePattern		= datePattern;
			this.pattern			= Pattern.compile( regexPattern );
			this.formatter			= LocalizationUtil.getPatternFormatter( datePattern, Locale.US );
			this.description		= description;
			this.optimizedQueries	= determineOptimalTemporalQueries( datePattern );
		}

		public boolean matches( String input ) {
			return this.pattern.matcher( input ).matches();
		}

		public DateTimeFormatter getFormatter() {
			return this.formatter;
		}

		public String getDescription() {
			return this.description;
		}

		public String getRegexPattern() {
			return this.regexPattern;
		}

		public String getDatePattern() {
			return this.datePattern;
		}

		public TemporalQuery<?>[] getOptimizedQueries() {
			return this.optimizedQueries;
		}

		/**
		 * Analyzes a DateTimeFormatter pattern to determine which TemporalQuery functions
		 * should be used with parseBest() based on the temporal components present in the pattern.
		 * This optimization reduces unnecessary parsing attempts and improves performance.
		 * 
		 * @param pattern The DateTimeFormatter pattern string to analyze
		 * 
		 * @return Array of TemporalQuery functions in optimal order for parseBest()
		 */
		public static TemporalQuery<?>[] determineOptimalTemporalQueries( String pattern ) {
			List<TemporalQuery<?>>	queries		= new ArrayList<>();

			// Pattern analysis flags - more comprehensive pattern detection
			boolean					hasDate		= dateMatchPattern.matcher( pattern ).matches(); // year, month, day, week patterns
			boolean					hasTime		= timeMatchPattern.matcher( pattern ).matches(); // hour, minute, second, am/pm patterns
			boolean					hasTimezone	= timezoneMatchPattern.matcher( pattern ).matches(); // timezone patterns
			boolean					hasOffset	= offsetMatchPattern.matcher( pattern ).matches(); // offset patterns (subset of timezone)

			// Conservative approach: always include the most likely types first, but include all for safety
			// This still provides optimization by ordering, while ensuring compatibility

			if ( hasDate && hasTime && ( hasTimezone || hasOffset ) ) {
				// Full date-time with timezone - prioritize timezone-aware types
				if ( hasOffset ) {
					queries.add( OffsetDateTime::from ); // ISO offsets like +01:00, Z
				}
				queries.add( ZonedDateTime::from ); // Named timezones
				queries.add( LocalDateTime::from ); // Fallback for when timezone parsing fails
				queries.add( Instant::from ); // Alternative for timestamp patterns
				queries.add( LocalDate::from ); // Safety fallback
				queries.add( LocalTime::from ); // Safety fallback
			} else if ( hasDate && hasTime ) {
				// Date and time without explicit timezone - most common case
				queries.add( LocalDateTime::from ); // Primary target
				queries.add( LocalDate::from ); // In case time parsing fails
				queries.add( LocalTime::from ); // In case date parsing fails
				// Add timezone types as fallback in case pattern analysis missed timezone info
				queries.add( ZonedDateTime::from );
				queries.add( OffsetDateTime::from );
				queries.add( Instant::from );
			} else if ( hasDate ) {
				// Date only patterns
				queries.add( LocalDate::from );
				queries.add( LocalDateTime::from ); // Safety fallback
				queries.add( ZonedDateTime::from );
				queries.add( OffsetDateTime::from );
				queries.add( LocalTime::from );
				queries.add( Instant::from );
			} else if ( hasTime ) {
				// Time only patterns
				queries.add( LocalTime::from );
				queries.add( LocalDateTime::from ); // Safety fallback
				queries.add( LocalDate::from );
				queries.add( ZonedDateTime::from );
				queries.add( OffsetDateTime::from );
				queries.add( Instant::from );
			} else {
				// Fallback for patterns we couldn't categorize - use all types starting with the most verbose
				queries.add( ZonedDateTime::from );
				queries.add( OffsetDateTime::from );
				queries.add( LocalDateTime::from );
				queries.add( LocalDate::from );
				queries.add( LocalTime::from );
				queries.add( Instant::from );
			}

			return queries.toArray( new TemporalQuery<?>[ 0 ] );
		}
	}

	/**
	 * Static final ArrayList containing all common date format configurations.
	 * Each entry is a Map containing regexPattern, datePattern, and description.
	 */

	/**
	 * Cache for CommonFormatter instances that use regex for fast pattern matching
	 */
	private static final List<CommonFormatter>		commonFormatters	= getCommonFormatters();

	/**
	 * A struct of common locale constants
	 */
	public static final LinkedHashMap<Key, Locale>	COMMON_LOCALES		= new LinkedHashMap<Key, Locale>();
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
		COMMON_LOCALES.put( Key.of( "US" ), buildLocale( "en", "US" ) );
		COMMON_LOCALES.put( Key.of( "United States" ), buildLocale( "en", "US" ) );
	}

	/**
	 * More Lookups
	 */
	public static final Array					ISO_COUNTRIES	= new Array( Locale.getISOCountries() );
	public static final Array					ISO_LANGUAGES	= new Array( Locale.getISOLanguages() );

	/**
	 * A collection of common locale aliases which are used by both ACF and Lucee
	 */
	public static final HashMap<Key, Locale>	LOCALE_ALIASES	= new LinkedHashMap<Key, Locale>();
	static {
		LOCALE_ALIASES.put( Key.of( "Albanian (Albania)" ), buildLocale( "sq", "AL" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Algeria)" ), buildLocale( "ar", "DZ" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Bahrain)" ), buildLocale( "ar", "BH" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Egypt)" ), buildLocale( "ar", "EG" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Iraq)" ), buildLocale( "ar", "IQ" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Jordan)" ), buildLocale( "ar", "JO" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Kuwait)" ), buildLocale( "ar", "KW" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Lebanon)" ), buildLocale( "ar", "LB" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Libya)" ), buildLocale( "ar", "LY" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Morocco)" ), buildLocale( "ar", "MA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Oman)" ), buildLocale( "ar", "OM" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Qatar)" ), buildLocale( "ar", "QA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Saudi Arabia)" ), buildLocale( "ar", "SA" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Sudan)" ), buildLocale( "ar", "SD" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Syria)" ), buildLocale( "ar", "SY" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Tunisia)" ), buildLocale( "ar", "TN" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (United Arab Emirates)" ), buildLocale( "ar", "AE" ) );
		LOCALE_ALIASES.put( Key.of( "Arabic (Yemen)" ), buildLocale( "ar", "YE" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (China)" ), Locale.CHINA );
		LOCALE_ALIASES.put( Key.of( "Chinese (Hong Kong)" ), buildLocale( "zh", "HK" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (Singapore)" ), buildLocale( "zh", "SG" ) );
		LOCALE_ALIASES.put( Key.of( "Chinese (Taiwan)" ), buildLocale( "zh", "TW" ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Belgian)" ), buildLocale( "nl", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Belgium)" ), LOCALE_ALIASES.get( Key.of( "dutch (belgian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "Dutch (Standard)" ), buildLocale( "nl", "NL" ) );
		LOCALE_ALIASES.put( Key.of( "English (Australian)" ), buildLocale( "en", "AU" ) );
		LOCALE_ALIASES.put( Key.of( "English (Australia)" ), LOCALE_ALIASES.get( Key.of( "english (australian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (Canadian)" ), Locale.CANADA );
		LOCALE_ALIASES.put( Key.of( "English (Canada)" ), Locale.CANADA );
		LOCALE_ALIASES.put( Key.of( "English (New zealand)" ), buildLocale( "en", "NZ" ) );
		LOCALE_ALIASES.put( Key.of( "English (UK)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (United Kingdom)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (GB)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (Great Britan)" ), Locale.UK );
		LOCALE_ALIASES.put( Key.of( "English (US)" ), buildLocale( "en", "US" ) );
		LOCALE_ALIASES.put( Key.of( "English (USA)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (United States)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "English (United States of America)" ), LOCALE_ALIASES.get( Key.of( "English (US)" ) ) );
		LOCALE_ALIASES.put( Key.of( "French (Belgium)" ), buildLocale( "fr", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "French (Belgian)" ), buildLocale( "fr", "BE" ) );
		LOCALE_ALIASES.put( Key.of( "French (Canadian)" ), Locale.CANADA_FRENCH );
		LOCALE_ALIASES.put( Key.of( "French (Canadia)" ), Locale.CANADA_FRENCH );
		LOCALE_ALIASES.put( Key.of( "French (Standard)" ), Locale.FRANCE );
		LOCALE_ALIASES.put( Key.of( "French (Swiss)" ), buildLocale( "fr", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "German (Austrian)" ), buildLocale( "de", "AT" ) );
		LOCALE_ALIASES.put( Key.of( "German (Austria)" ), buildLocale( "de", "AT" ) );
		LOCALE_ALIASES.put( Key.of( "German (Standard)" ), Locale.GERMANY );
		LOCALE_ALIASES.put( Key.of( "German (Swiss)" ), buildLocale( "de", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Italian (Standard)" ), Locale.ITALIAN );
		LOCALE_ALIASES.put( Key.of( "Italian (Swiss)" ), buildLocale( "it", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Japanese" ), Locale.JAPANESE );
		LOCALE_ALIASES.put( Key.of( "Korean" ), Locale.KOREAN );
		LOCALE_ALIASES.put( Key.of( "Norwegian (Bokmal)" ), buildLocale( "no", "NO" ) );
		LOCALE_ALIASES.put( Key.of( "Norwegian (Nynorsk)" ), buildLocale( "no", "NO" ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Brazilian)" ), buildLocale( "pt", "BR" ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Brazil)" ), LOCALE_ALIASES.get( Key.of( "portuguese (brazilian)" ) ) );
		LOCALE_ALIASES.put( Key.of( "Portuguese (Standard)" ), buildLocale( "pt", "PT" ) );
		LOCALE_ALIASES.put( Key.of( "Rhaeto-Romance (Swiss)" ), buildLocale( "rm", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Rhaeto-Romance (Swiss)" ), buildLocale( "rm", "CH" ) );
		LOCALE_ALIASES.put( Key.of( "Spanish (Modern)" ), buildLocale( "es", "ES" ) );
		LOCALE_ALIASES.put( Key.of( "Spanish (Standard)" ), buildLocale( "es", "ES" ) );
		LOCALE_ALIASES.put( Key.of( "Swedish" ), buildLocale( "sv", "SE" ) );
		LOCALE_ALIASES.put( Key.of( "Welsh" ), buildLocale( "cy", "GB" ) );
	}

	/**
	 * Common Number formatter instances
	 */
	public static final Struct COMMON_NUMBER_FORMATTERS = new Struct( new HashMap<Key, java.text.NumberFormat>() );
	static {

		COMMON_NUMBER_FORMATTERS.put(
		    Key.of( "USD" ),
		    DecimalFormat.getCurrencyInstance( LocalizationUtil.COMMON_LOCALES.get( Key.of( "US" ) ) )
		);

		COMMON_NUMBER_FORMATTERS.put(
		    Key.of( "EURO" ),
		    DecimalFormat.getCurrencyInstance( LocalizationUtil.COMMON_LOCALES.get( Key.of( "German" ) ) )
		);
	}

	/**
	 * Common DateTime formatter instances
	 */
	public static final Key						DEFAULT_NUMBER_FORMAT_KEY	= Key.of( "," );

	/**
	 * Common number format patterns and shorthand variations
	 */
	public static final HashMap<Key, String>	NUMBER_FORMAT_PATTERNS		= new HashMap<Key, String>();
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
		NUMBER_FORMAT_PATTERNS.put( DEFAULT_NUMBER_FORMAT_KEY, "#,##0.#" );
	}

	public static final String					CURRENCY_TYPE_LOCAL				= "local";
	public static final String					CURRENCY_TYPE_INTERNATIONAL		= "international";
	public static final String					CURRENCY_TYPE_NONE				= "none";

	/**
	 * Pattern matchers for testing date time strings
	 */

	// Matches long form date strings like "Jan 1, 2023" or "January 1, 2023"
	public static final Pattern					REGEX_LONGFORM_PATTERN			= Pattern.compile(
	    "(Jan(uary)?|Feb(ruary)?|Mar(ch)?|Apr(il)?|May|Jun(e)?|Jul(y)?|Aug(ust)?|Sep(tember)?|Oct(ober)?|Nov(ember)?|Dec(ember)?)\\s+\\d{1,2},?\\s+\\d{4}"
	);
	// Matches timezone offset like +01:00, -08:00, or Z
	public static final Pattern					REGEX_TZ_OFFSET_PATTERN			= Pattern.compile( ".*[+-][0-9]{2}:?[0-9]{2}|Z$" );

	// Matches timezone id like PST or UTC
	public static final Pattern					REGEX_TZ_ABBREVIATION_PATTERN	= Pattern.compile( "[A-Z]{3,}" );

	/**
	 * A struct of ZoneID aliases ( e.g. PST )
	 */
	public static final HashMap<Key, String>	ZONE_ALIASES					= new HashMap<Key, String>();
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
		oLocale = COMMON_LOCALES.get( Key.of( requestedLocale ) );
		if ( oLocale != null )
			return oLocale;

		// See if it's an alias
		oLocale = LOCALE_ALIASES.get( Key.of( requestedLocale ) );
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
		    locale.getDisplayLanguage( LocalizationUtil.COMMON_LOCALES.get( Key.of( "US" ) ) ),
		    BooleanCaster.cast( locale.getVariant().length() ) ? locale.getVariant()
		        : locale.getDisplayCountry( LocalizationUtil.COMMON_LOCALES.get( Key.of( "US" ) ) )
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
		RequestBoxContext requestContext = context.getRequestContext();
		return parseLocaleOrDefault(
		    arguments.getAsString( Key.locale ),
		    requestContext != null && requestContext.getLocale() != null ? requestContext.getLocale() : ( Locale ) context.getConfig().get( Key.locale )
		);
	}

	/**
	 * Parses a locale from a string
	 *
	 * @param locale The string representation of the locale
	 *
	 * @return The Locale object or null if the locale could not be parsed
	 */
	public static Locale getParsedLocale( String locale ) {
		Locale localeObj = null;
		if ( locale != null ) {
			var		localeParts	= locale.split( "[-_ ]" );
			String	ISOLang		= localeParts[ 0 ];
			String	ISOCountry	= null;
			if ( localeParts.length > 1 ) {
				ISOCountry = localeParts[ 1 ];
			}
			localeObj = ISOCountry == null ? buildLocale( ISOLang ) : buildLocale( ISOLang, ISOCountry );
		} else {
			localeObj = Locale.getDefault();
		}
		return localeObj;
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
				return ZoneId.of( ZONE_ALIASES.get( zoneKey ) );
			} else {
				ZoneId parsed = parseZoneId( timezone );
				return parsed != null
				    ? parsed
				    : ( ZoneId ) context.getConfig().get( Key.timezone );
			}
		} else if ( context != null ) {
			RequestBoxContext requestContext = context.getRequestContext();
			if ( requestContext != null && requestContext.getTimezone() != null ) {
				return requestContext.getTimezone();
			} else {
				return ( ZoneId ) context.getConfig().get( Key.timezone );
			}
		} else {
			IBoxContext runtimeContext = BoxRuntime.getInstance().getRuntimeContext();
			return ( ZoneId ) runtimeContext.getConfig().get( Key.timezone );
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
				return ZoneId.of( ZONE_ALIASES.get( zoneKey ) );
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

		// If it's empty just skip out
		if ( stringValue.isBlank() ) {
			return null;
		}

		// If we have an international format with the currency code we need to replace it with the symbol
		if ( stringValue.length() >= currencyCode.length() && stringValue.substring( 0, currencyCode.length() ).equals( currencyCode ) ) {
			stringValue = stringValue
			    .replace( currencyCode, parser.getCurrency().getSymbol() )
			    .replace( parser.getCurrency().getSymbol() + " ", parser.getCurrency().getSymbol() );
		}
		Number parsed = null;
		try {
			parsed = parser.parse( stringValue );
		} catch ( ParseException e ) {
			loggingService.getRuntimeLogger().debug( "Error parsing currency value: " + stringValue + ". The message received was: " + e.getMessage() );
		}
		return parsed == null ? null : parsed.doubleValue();
	}

	/**
	 * Parses a localized number string
	 *
	 * @param value  The value to be parsed
	 * @param locale The locale object to apply to the parse operation
	 *
	 * @return The parsed number or null if the value could not be parsed
	 */
	public static Number parseLocalizedNumber( Object value, Locale locale ) {
		return parseLocalizedNumber( StringCaster.cast( value ), locale );
	}

	/**
	 * Parses a localized number string
	 *
	 * @param value  The value to be parsed
	 * @param locale The locale object to apply to the parse operation
	 *
	 * @return The parsed number or null if the value could not be parsed
	 */
	public static Number parseLocalizedNumber( String value, Locale locale ) {
		DecimalFormat	parser			= ( DecimalFormat ) DecimalFormat.getInstance( locale );
		String			parseable		= StringCaster.cast( value );
		ParsePosition	parsePosition	= new ParsePosition( 0 );
		if ( parseable.length() >= 20 || parseable.contains( "E" ) || parseable.contains( "e" ) || parseable.contains( "." ) ) {
			parser.setParseBigDecimal( true );
		}
		// parser.setParseBigDecimal( true );
		Number parseResult = parser.parse( parseable, parsePosition );
		return parsePosition.getIndex() == parseable.length() && parseResult != null ? parseResult : null;
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
			format = NUMBER_FORMAT_PATTERNS.get( formatKey );
		}
		return new DecimalFormat( format, localizedDecimalSymbols( locale ) );
	}

	/**
	 * Initializes the CommonFormatter instances with regex patterns for fast date string matching.
	 * This method creates optimized formatters that test regex patterns before attempting to parse.
	 * All common date-time patterns are represented with appropriate regex patterns for efficient matching.
	 * 
	 * @return List of CommonFormatter instances, ordered so that the most frequently used date-time patterns appear first to optimize matching performance.
	 */
	private static List<CommonFormatter> getCommonFormatters() {

		// We maintain this here to keep it out of the head of the class as it is only used once.
		final List<Map<String, String>>	COMMON_FORMAT_DEFINITIONS	= new ArrayList<Map<String, String>>() {

		// @formatter:off
		{
			// ========== Highest Priority Patterns Come First ==========

			// Ultra-specific: Exact ISO Z format for speed (2024-04-02T21:01:00Z)
			add( Map.of(
				"regexPattern", "^\\d{4}-\\d{2}-\\d{1,2}T\\d{2}:\\d{2}:\\d{2}Z$",
				"datePattern", "yyyy-MM-d'T'HH:mm:ssX",
				"description", "ISO DateTime with Z timezone - ultra-specific"
			) );

			// Pattern for space separated with single decimal "2024-01-14 00:00:01.1"
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{1}$",
				"datePattern", "yyyy-MM-dd HH:mm:ss.S",
				"description", "ISO space format with single decimal"
			) );

			// Medium format with timezone abbreviation (for "Nov 22, 2022 11:01:51 CET")
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[A-Z]{3,4}$",
				"datePattern", "MMM d, yyyy HH:mm:ss zzz",
				"description",
				"Medium format with timezone (Nov 22, 2022 11:01:51 CET)"
			) );

			// ========== ISO Basic Formats ==========

			// Date-time with 12-hour format and meridian (double digit hour)
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{1,2}\\s+\\d{2}:\\d{1,2}(?::\\d{2})?\\s+[APap][Mm]$",
				"datePattern", "yyyy-MM-d h:mm[:ss] a",
				"description",
				"ISO date with 12-hour time and AM/PM (double digit hour)"
			) );

			// Consolidated: ISO with optional T/space, seconds, microseconds, offset
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{1,2}[T\\s]\\d{2}:\\d{2}(?:\\:\\d{2})?(?:\\.\\d{1,6})?(?:[+-]\\d{2}:?\\d{2}|Z)?$",
				"datePattern", "yyyy-MM-d['T'][ ]HH:mm[:ss][.SSSSSS][XXX]",
				"description",
				"ISO with optional T/space, seconds, microseconds, offset"
			) );

			// Consolidated: ISO with optional T/space, seconds, milliseconds, offset
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{1,2}[T\\s]\\d{2}:\\d{2}(?:\\:\\d{2})?(?:\\.\\d{1,3})?(?:[+-]\\d{2}:?\\d{2}|Z)?$",
				"datePattern", "yyyy-MM-d['T'][ ]HH:mm[:ss][.SSS][XXX]",
				"description",
				"ISO with optional T/space, seconds, milliseconds, offset"
			) );

			// Consolidated: ISO with optional T/space, seconds, basic offset
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{1,2}[T\\s]\\d{2}:\\d{2}(?:\\:\\d{2})?(?:Z|[+-]\\d{2})?$",
				"datePattern", "yyyy-MM-d['T'][ ]HH:mm[:ss][Z][X]",
				"description", "ISO with optional T/space, seconds, basic offset"
			) );

			// ========== Specific Edge Case Patterns ==========

			// Specific format for Jul 17, 2017 9:29:40 PM - single digit day and hour with seconds
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1}:\\d{2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d, yyyy h:mm:ss a",
				"description", "Month single-digit-day, year hour:min:sec AM/PM"
			) );

			// Med DateTime specific format with double digit day, single digit hour (e.g., Nov-05-2025 8:43am)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}-\\d{1,2}-\\d{4}\\s+\\d{1}:\\d{2}\\s*[APap][Mm]$",
				"datePattern", "MMM-d-yyyy h:mm[ ]a",
				"description", "Month-DD-YYYY H:MM AM/PM format"
			) );

			// Med DateTime specific format with double digit day, and 24 hour time with optional seconds
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}-\\d{1,2}-\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "MMM-d-yyyy HH:mm[:ss]",
				"description", "Month-DD-YYYY HH:MM[:SS] format"
			) );

			// ========== Localized Date/Time Formats ==========

			// Full DateTime with full day name (e.g., Tuesday, 02 Apr 2024 21:01:00 CEST)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,},?\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "EEEE[,] d MMM yyyy HH:mm:ss[ zzz]",
				"description",
				"Full day name, day month year time with optional timezone"
			) );

			// Full DateTime with abbreviated day name (e.g., Tue, 02 Apr 2024 21:01:00 CEST)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3},?\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "EEE[,] d MMM yyyy HH:mm:ss[ zzz]",
				"description",
				"Abbreviated day name, day month year time with optional timezone"
			) );

			// Long DateTime with single digit day (e.g., 2 Apr 2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "d MMM yyyy HH:mm[:ss]",
				"description", "Single digit day month year time"
			) );

			// Medium DateTime with single digit day (e.g., 2-Apr-2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}-[A-Za-z]{3}-\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "d-MMM-yyyy HH:mm[:ss]",
				"description", "Single digit day-month-year time"
			) );

			// Medium DateTime with double digit day (e.g., 02-Apr-2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}-[A-Za-z]{3}-\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "d-MMM-yyyy HH:mm[:ss]",
				"description", "Double digit day-month-year time"
			) );

			// Full month name with double digit day and optional zone offset (e.g., April 2, 2024 21:01:00, January, 05 2026 17:39:13 -0600)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3,},?\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+(?:[A-Z]{3,4}|[+-]\\d{4}))?$",
				"datePattern", "MMMM[,] dd[,] yyyy HH:mm:ss[ x]",
				"description",
				"Full month double digit day year time with optional offset"
			) );

			// Full month name with double digit day (e.g., April 02, 2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,},?\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMMM[,] d[,] yyyy HH:mm:ss[ zzz]",
				"description",
				"Full month double digit day year time with optional timezone"
			) );

			// Full month name with day and AM/PM (e.g., April 2, 2024 9:01 AM)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,},?\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{1}:\\d{2}\\s+[APap][Mm](?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMMM[,] d[,] yyyy h:mm a[ zzz]",
				"description",
				"Full month single digit day year time AM/PM with optional timezone"
			) );

			// Full month name with day no seconds (e.g. April 2 2024 21:01)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,},?\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMMM[,] d[,] yyyy HH:mm[ zzz]",
				"description",
				"Full month single digit day year time no seconds with optional timezone"
			) );

			// ========== Consolidated Patterns for Medium Date/Time ==========

			// Medium DateTime No Seconds and AM/PM with single digit day/hour
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}[,]+\\s+\\d{1}:\\d{2}\\s*[APap][Mm](?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy[,] h:mm[ ]a[ zzz]",
				"description",
				"Month single day year single hour:min AM/PM with optional timezone"
			) );

			// Medium DateTime No Seconds and AM/PM with double digit day/hour
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{2}:\\d{2}\\s*[APap][Mm](?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy hh:mm[ ]a[ zzz]",
				"description",
				"Month double day year double hour:min AM/PM with optional timezone"
			) );

			// Medium DateTime with Seconds and AM/PM with single digit day/hour
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{1}:\\d{2}:\\d{2}\\s*[APap][Mm](?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy h:mm:ss[ ]a[ zzz]",
				"description",
				"Month single day year single hour:min:sec AM/PM with optional timezone"
			) );

			// Medium DateTime with Seconds and AM/PM with double digit day/hour
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s*[APap][Mm](?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy h:mm:ss[ ]a[ zzz]",
				"description",
				"Month double day year hour:min:sec AM/PM with optional timezone"
			) );

			// Medium DateTime with single digit day (e.g., Apr 2, 2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy HH:mm:ss[ zzz]",
				"description",
				"Month single day year time:sec with optional timezone"
			) );

			// Medium DateTime with double digit day (e.g., Apr 02, 2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy HH:mm:ss[ zzz]",
				"description",
				"Month double day year time:sec with optional timezone"
			) );


			// Medium DateTime No Seconds with double digit day
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}[,\\-\\s]+\\d{1,2}[,\\-\\s]+\\d{4}\\s+\\d{2}:\\d{2}(?:\\s+[A-Z]{3,4})?$",
				"datePattern", "MMM[,][- ]d[,][- ]yyyy HH:mm[ zzz]",
				"description",
				"Month double day year time no seconds with optional timezone"
			) );

			// ========== US Format Date/Time ==========

			// US Short DateTime with AM/PM and seconds (e.g., 02/04/2024 04:01:00 PM)
			add( Map.of(
				"regexPattern",
				"^\\d{2}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?\\s+[APap][Mm]$",
				"datePattern", "MM/d/yyyy hh:mm[:ss] a",
				"description", "US date MM/dd/yyyy with time and AM/PM"
			) );

			// US Short DateTime 24-hour with optional seconds (e.g., 02/04/2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^\\d{2}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "MM/d/yyyy HH:mm[:ss]",
				"description", "US date MM/dd/yyyy with 24-hour time"
			) );


			// US Short DateTime with AM/PM no seconds (e.g., 02/04/2024 04:01 PM)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "M/d/yyyy h:mm a",
				"description", "US date MM/dd/yyyy with time AM/PM no seconds"
			) );


			// Short DateTime with medium month and 24 hour time (e.g., Feb/04/2024 22:01[:00])
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "MMM/d/yyyy HH:mm[:ss]",
				"description", "Month/DD/YYYY with 24-hour time"
			) );

			// US Short DateTime with no seconds and no meridian (e.g., 11/21/2025 1:05)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}$",
				"datePattern", "M/d/yyyy H:mm",
				"description", "US date MM/dd/yyyy with time no seconds"
			) );

			// European dot format datetime (e.g., 02.04.2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\s+\\d{2}:\\d{2}(?::\\d{2})?$",
				"datePattern", "d.M.yyyy HH:mm[:ss]",
				"description", "European DD.MM.YYYY with time"
			) );

			// Long month DateTime (e.g., April 02, 2024 21:01:00)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,}\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}$",
				"datePattern", "LLLL d[,] yyyy HH:mm:ss",
				"description", "Full month name DD, YYYY with time"
			) );

			// Long month DateTime with AM/PM (e.g., April 02, 2024 05:01 AM)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,}\\s+\\d{1,2},?\\s+\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "LLLL d[,] yyyy h:mm a",
				"description", "Full month name DD, YYYY with time AM/PM"
			) );

			// ========== java.util.Date toString Format ==========

			// Default DateTime (e.g., Tue Apr 02 21:01:00 CET 2024)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3},?\\s+[A-Za-z]{3},?\\s+\\d{1,2},?\\s+\\d{2}:\\d{2}:\\d{2}\\s+[A-Z]{3,4}\\s+\\d{4}$",
				"datePattern", "EEE[,] MMM[,] d[,] HH:mm:ss zzz yyyy",
				"description", "Java Date toString format"
			) );

			// ========== ODBC Formats ==========

			// ODBC DateTime compact format (yyyyMMddHHmmss)
			add( Map.of(
				"regexPattern", "^\\d{14}$",
				"datePattern", "yyyyMMddHHmmss",
				"description", "ODBC DateTime compact format"
			) );

			// ODBC Date compact format (yyyyMMdd)
			add( Map.of(
				"regexPattern", "^\\d{8}$",
				"datePattern", "yyyyMMdd",
				"description", "ODBC Date compact format"
			) );

			// ========== Alternative/International Date Patterns ==========

			// Alternative international format (yyyy/MM/dd)
			add( Map.of(
				"regexPattern", "^\\d{4}/\\d{1,2}/\\d{1,2}$",
				"datePattern", "yyyy/M/d",
				"description", "International year/month/day format"
			) );

			// Short form two-digit year (M-d-yy)
			add( Map.of(
				"regexPattern", "^\\d{1,2}-\\d{1,2}-\\d{2}$",
				"datePattern", "M-d-yy",
				"description", "Short month-day-year format"
			) );

			// ========== US Localized Date Formats ==========

			// Long Date with space separators (e.g., Apr 02 2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{4}$",
				"datePattern", "MMM d yyyy",
				"description", "Month day year with spaces"
			) );

			// Long Date with dash separators (e.g., Apr-02-2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{3}-\\d{1,2}-\\d{4}$",
				"datePattern", "MMM-d-yyyy",
				"description", "Month-day-year with dashes"
			) );

			// Long Date with slash separators (e.g., Apr/02/2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{3}/\\d{1,2}/\\d{4}$",
				"datePattern", "MMM/d/yyyy",
				"description", "Month/day/year with slashes"
			) );

			// Long Date with dot separators (e.g., Apr.02.2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{3}\\.\\d{2}\\.\\d{4}$",
				"datePattern", "MMM.dd.yyyy",
				"description", "Month.day.year with dots"
			) );

			// Short Date with space separators (e.g., 04 02 2024)
			add( Map.of(
				"regexPattern", "^\\d{2}\\s\\d{1,2}\\s\\d{4}$",
				"datePattern", "MM d yyyy",
				"description", "MM dd yyyy with spaces"
			) );

			// Short Date with dash separators (e.g., 04-02-2024)
			add( Map.of(
				"regexPattern", "^\\d{2}-\\d{1,2}-\\d{4}$",
				"datePattern", "MM-d-yyyy",
				"description", "MM-dd-yyyy with dashes"
			) );

			// Short Date with slash separators (e.g., 04/02/2024)
			add( Map.of(
				"regexPattern", "^\\d{2}/\\d{1,2}/\\d{4}$",
				"datePattern", "MM/d/yyyy",
				"description", "MM/dd/yyyy with slashes"
			) );

			// Short Date with dot separators (e.g., 04.02.2024)
			add( Map.of(
				"regexPattern", "^\\d{2}\\.\\d{2}\\.\\d{4}$",
				"datePattern", "MM.dd.yyyy",
				"description", "MM.dd.yyyy with dots"
			) );

			// ========== European and International Date Formats ==========

			// Full Date with optional full day name and comma (e.g., Tuesday, 02 Apr 2024)
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3,},?\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}$",
				"datePattern", "EEE[E][,] d MMM yyyy",
				"description", "Day name, day month year"
			) );

			// Long Date (e.g., 02 Apr 2024)
			add( Map.of(
				"regexPattern", "^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}$",
				"datePattern", "d MMM yyyy",
				"description", "Day month year"
			) );

			// Medium Date with a two-digit year (e.g., 02-Apr-24)
			add( Map.of(
				"regexPattern", "^\\d{1,2}-[A-Za-z]{3}-\\d{2}$",
				"datePattern", "d-MMM-yy",
				"description", "Day-month-year with two-digit year"
			) );

			// Medium Date with flexible separators (e.g., 02-Apr-2024, 02/Apr/2024, 02.Apr.2024)
			add( Map.of(
				"regexPattern", "^\\d{1,2}[./-][A-Za-z]{3}[./-]\\d{4}$",
				"datePattern", "d[-/.]MMM[-/.]yyyy",
				"description", "Day-month-year with flexible separators"
			) );

			// Med Date (e.g., Apr 02, 2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{3},?\\s+\\d{1,2},?\\s+\\d{4}$",
				"datePattern", "MMM[,] d[,] yyyy",
				"description", "Month day, year with optional commas"
			) );

			// Long month Date (e.g., April 02, 2024)
			add( Map.of(
				"regexPattern", "^[A-Za-z]{4,},?\\s+\\d{1,2},?\\s+\\d{4}$",
				"datePattern", "MMMM[,] d[,] yyyy",
				"description", "Full month day, year with optional commas"
			) );

			// European day-first with flexible separators (e.g., 02-04-2024, 02/04/2024, 02.04.2024)
			add( Map.of(
				"regexPattern", "^\\d{1,2}[ ./-]\\d{1,2}[ ./-]\\d{4}$",
				"datePattern", "d[ /-.]M[ /-.]yyyy",
				"description", "European day-first with flexible separators"
			) );

			// ISO date with flexible separators (e.g., 2024-04-02, 2024/04/02, 2024.04.02)
			add( Map.of(
				"regexPattern", "^\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}$",
				"datePattern", "yyyy[-/.]M[-/.]d",
				"description", "ISO year-month-day with flexible separators"
			) );

			// Basic ISO date (handles 2024-04-02)
			add( Map.of(
				"regexPattern", "^\\d{4}-\\d{1,2}-\\d{1,2}$",
				"datePattern", "yyyy-M-d",
				"description", "ISO Date format"
			) );

			// ========== Time-Only Formats ==========

			// Time-only formats with optional AM/PM
			add( Map.of(
				"regexPattern", "^\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[APap][Mm])?$",
				"datePattern", "HH:mm[:ss][ a]",
				"description", "Time only format with optional AM/PM"
			) );

			// ========== Missing Test Patterns ==========

			// Pattern for "Jan 20, 2024 00:00:00" - medium format with seconds
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}$",
				"datePattern", "MMM d, yyyy HH:mm:ss",
				"description",
				"Medium format without timezone (Jan 20, 2024 00:00:00)"
			) );

			// Pattern for "2024.01.01" - ISO date with dots
			add( Map.of(
				"regexPattern", "^\\d{4}\\.\\d{2}\\.\\d{2}$",
				"datePattern", "yyyy.MM.dd",
				"description", "ISO date with dots (2024.01.01)"
			) );

			// Pattern for "14.01.2024" - European date with dots
			add( Map.of(
				"regexPattern", "^\\d{2}\\.\\d{2}\\.\\d{4}$",
				"datePattern", "dd.MM.yyyy",
				"description", "European date with dots (14.01.2024)"
			) );

			// Pattern for "Nov-05-2025 8:43am" - medium format with lowercase am/pm
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}-\\d{1,2}-\\d{4}\\s+\\d{1,2}:\\d{2}\\s*[APap][Mm]$",
				"datePattern", "MMM-d-yyyy h:mm a",
				"description",
				"Medium format with lowercase am/pm (Nov-05-2025 8:43am)"
			) );

			// Pattern for "Jul 17, 2017 9:29:40 PM" - single digit day with seconds and meridian
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d, yyyy h:mm:ss a",
				"description",
				"Medium format single digit day/hour with seconds and AM/PM"
			) );

			// Pattern for "Mar 22, 2025 05:21 PM" - medium format without seconds and AM/PM
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d, yyyy h:mm a",
				"description",
				"Medium format single digit day/hour no seconds with AM/PM"
			) );

			// Pattern for "Mar 22 2025 05:21 PM" - medium format without comma and seconds
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d yyyy h:mm a",
				"description",
				"Medium format without comma no seconds with AM/PM"
			) );

			// Pattern for "March 22 2025 5:21 PM" - full month name without comma
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{4,}\\s+\\d{1,2}\\s+\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMMM d yyyy h:mm a",
				"description",
				"Full month name without comma no seconds with AM/PM"
			) );

			// Pattern for single digit month/day formats like "1/1/2024"
			add( Map.of(
				"regexPattern", "^\\d{1,2}/\\d{1,2}/\\d{4}$",
				"datePattern", "M/d/yyyy",
				"description", "US date with single digit month/day (1/1/2024)"
			) );

			// Pattern for single digit year/month/day like "2018/9/6"
			add( Map.of(
				"regexPattern", "^\\d{4}/\\d{1,2}/\\d{1,2}$",
				"datePattern", "yyyy/M/d",
				"description",
				"International date with single digit month/day (2018/9/6)"
			) );

			// Pattern for "02-Apr-2024" - European format with dashes
			add( Map.of(
				"regexPattern", "^\\d{2}-[A-Za-z]{3}-\\d{4}$",
				"datePattern", "dd-MMM-yyyy",
				"description", "European date-month-year (02-Apr-2024)"
			) );

			// Pattern for medium format with narrow no-break space "Nov 20, 2025 10:40:09 AM"
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}[\\s\\u00A0\\u202F]+\\d{1,2}:\\d{2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d, yyyy h:mm:ss a",
				"description", "Medium format with various spaces and AM/PM"
			) );

			// ========== Additional DateTimeCasterTest Patterns ==========

			// Pattern for "03/28/2025 04:32 PM" - US slash format with time and AM/PM (single digit hour)
			add( Map.of(
				"regexPattern",
				"^\\d{2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MM/d/yyyy h:mm a",
				"description", "US date with time and AM/PM (single digit hour)"
			) );

			// Pattern for "Tue, 02 Apr 2024" - day name prefix with comma
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3},\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}$",
				"datePattern", "EEE, d MMM yyyy",
				"description", "Day name, day month year"
			) );

			// Pattern for "02 Apr 2024 21:01:00" - space format with time
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}$",
				"datePattern", "d MMM yyyy HH:mm:ss",
				"description", "Day month year with time"
			) );

			// Pattern for "Nov/21/2025 00:01:00" - month name slash format with time
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}$",
				"datePattern", "MMM/d/yyyy HH:mm:ss",
				"description", "Month/day/year with time"
			) );

			// Pattern for "Tue, 02 Apr 2024 21:01:00 CEST" - full day name with timezone
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3},\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}\\s+[A-Z]{3,4}$",
				"datePattern", "EEE, d MMM yyyy HH:mm:ss zzz",
				"description", "Day name, day month year with time and timezone"
			) );

			// Pattern for "02 Apr 2024" - day first with spaces
			add( Map.of(
				"regexPattern", "^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}$",
				"datePattern", "d MMM yyyy",
				"description", "Day month year with spaces"
			) );

			// Pattern for "1899-12-31 06:10 PM" - ISO date with AM/PM time (single digit hour)
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "yyyy-MM-dd h:mm a",
				"description", "ISO date with AM/PM time (single digit hour)"
			) );

			// Pattern for "Tue Nov 22 11:01:51 CET 2022" - java.util.Date toString format
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+[A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+[A-Z]{3,4}\\s+\\d{4}$",
				"datePattern", "EEE MMM d HH:mm:ss zzz yyyy",
				"description", "Java Date toString format without commas"
			) );

			// ========== Additional Missing Patterns ==========

			// Pattern for "20240402210100" - compact ODBC timestamp
			add( Map.of(
				"regexPattern", "^\\d{14}$",
				"datePattern", "yyyyMMddHHmmss",
				"description", "Compact ODBC timestamp (20240402210100)"
			) );

			// Pattern for "Nov 22, 2022 11:01:51 CET" - already handled but might need priority
			// This is already handled by earlier pattern

			// Pattern for "Jul 17, 2017 9:29:40 PM" - single digit hour/day with AM/PM
			add( Map.of(
				"regexPattern",
				"^[A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "MMM d, yyyy h:mm:ss a",
				"description",
				"Medium format with single digit day/hour AM/PM seconds"
			) );

			// Pattern for lowercase am/pm "2021-01-01 12:00:00 pm"
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "yyyy-MM-dd h:mm:ss a",
				"description", "ISO format with lowercase am/pm"
			) );

			// Pattern for slash date with lowercase pm "7/20/2025 1:00 pm"
			add( Map.of(
				"regexPattern",
				"^\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}\\s+[APap][Mm]$",
				"datePattern", "M/d/yyyy h:mm a",
				"description", "US date format with lowercase am/pm"
			) );

			// Pattern for microsecond precision "2024-01-14T00:00:01.0001Z"
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{4}Z$",
				"datePattern", "yyyy-MM-dd'T'HH:mm:ss.SSSSXXX",
				"description", "ISO with microsecond precision"
			) );

			// Pattern for microsecond without Z "2024-01-14T00:00:01.0001"
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{4}$",
				"datePattern", "yyyy-MM-dd'T'HH:mm:ss.SSSS",
				"description", "ISO with microsecond precision no timezone"
			) );

			// Spanish date formats
			add( Map.of(
				"regexPattern", "^\\d{1,2}\\s+de\\s+\\w+\\s+de\\s+\\d{4}$",
				"datePattern", "d 'de' MMMM 'de' yyyy",
				"description", "Spanish long-form date"
			) );

			// ISO with timezone offset without colon
			add( Map.of(
				"regexPattern",
				"^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}$",
				"datePattern", "yyyy-MM-dd'T'HH:mm:ssZ",
				"description", "ISO with timezone offset without colon"
			) );
		}
		// @formatter:on
																	};

		List<CommonFormatter>			formatters					= new ArrayList<>();

		// Build CommonFormatter instances from static definitions
		for ( Map<String, String> definition : COMMON_FORMAT_DEFINITIONS ) {
			formatters.add( new CommonFormatter(
			    definition.get( "regexPattern" ),
			    definition.get( "datePattern" ),
			    definition.get( "description" )
			) );
		}

		return formatters;
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
	 * Parses a date-time string using common patterns and returns a {@link DateTime} object.
	 * The method attempts to parse the input string into various {@link TemporalAccessor} types,
	 * such as {@link OffsetDateTime}, {@link ZonedDateTime}, {@link LocalDateTime}, {@link LocalDate},
	 * {@link LocalTime}, or {@link Instant}. If successful, it wraps the parsed result in a {@link DateTime}.
	 *
	 * @param dateTime the date-time string to parse
	 * @param timezone the timezone to apply to parsed dates without timezone information (optional, ignored if null)
	 * 
	 * @return a {@link DateTime} object representing the parsed date-time
	 * 
	 * @throws BoxRuntimeException if the input string cannot be parsed into a supported {@link TemporalAccessor}
	 */
	public static DateTime parseFromCommonPatterns( String dateTime, ZoneId timezone ) {

		for ( CommonFormatter formatter : commonFormatters ) {
			if ( formatter.matches( dateTime ) ) {
				try {
					// Use pattern-optimized TemporalQuery functions to prioritize parsing into the most specific type
					// The queries are pre-computed based on the pattern's temporal components
					TemporalAccessor date = formatter.getFormatter().parseBest(
					    dateTime,
					    formatter.getOptimizedQueries()
					);

					// Parse timezone if not provided and date does not already contain timezone info
					if ( timezone == null && ( ! ( date instanceof ZonedDateTime ) && ! ( date instanceof OffsetDateTime ) ) ) {
						timezone = parseZoneId( null, RequestBoxContext.getCurrent() );
					}

					if ( date instanceof ZonedDateTime castZonedDateTime ) {
						return new DateTime( castZonedDateTime );
					} else if ( date instanceof OffsetDateTime castOffsetDateTime ) {
						return new DateTime( castOffsetDateTime );
					} else if ( date instanceof LocalDateTime castLocalDateTime ) {
						// Apply timezone if provided, otherwise use the existing DateTime constructor behavior
						return timezone != null
						    ? new DateTime( castLocalDateTime.atZone( timezone ) )
						    : new DateTime( castLocalDateTime );
					} else if ( date instanceof LocalDate castLocalDate ) {
						// Apply timezone if provided, otherwise use the existing DateTime constructor behavior
						return timezone != null
						    ? new DateTime( castLocalDate.atStartOfDay( timezone ) )
						    : new DateTime( castLocalDate );
					} else if ( date instanceof LocalTime castLocalTime ) {
						// Apply timezone if provided, otherwise use the existing DateTime constructor behavior
						return timezone != null
						    ? new DateTime( castLocalTime.atDate( LocalDate.now() ).atZone( timezone ) )
						    : new DateTime( castLocalTime );
					} else if ( date instanceof Instant castInstant ) {
						return new DateTime( castInstant );
					} else {
						throw new BoxRuntimeException(
						    String.format(
						        "The TemporalAccessor instanceof [%s] does not have a valid DateTime constructor",
						        date.getClass().getName()
						    )
						);
					}
				} catch ( Exception e ) {
					loggingService.getRuntimeLogger().trace(
					    "Error parsing date time with common formatter.  The pattern [" + formatter.getDescription() + "] failed with error: "
					        + e.getMessage() );
				}
			}
		}

		return null;
	}

	/**
	 * Parses a date-time string using common patterns and returns a {@link DateTime} object.
	 * Uses the existing DateTime constructor behavior for dates without timezone information.
	 *
	 * @param dateTime the date-time string to parse
	 * 
	 * @return a {@link DateTime} object representing the parsed date-time
	 * 
	 * @throws BoxRuntimeException if the input string cannot be parsed into a supported {@link TemporalAccessor}
	 */
	public static DateTime parseFromCommonPatterns( String dateTime ) {
		return parseFromCommonPatterns( dateTime, null );
	}

	/**
	 * Parses a date string into a ZonedDateTime instance
	 *
	 * @param dateTime the date time string to parse
	 * @param locale   the locale
	 * @param timezone the timezone
	 *
	 * @return
	 */
	public static ZonedDateTime parseFromString( String dateTime, Locale locale, ZoneId timezone ) {

		Boolean	likelyHasDate			= dateTime.contains( "/" ) || dateTime.contains( "-" );
		Boolean	likelyIsLongFormDate	= !likelyHasDate && REGEX_LONGFORM_PATTERN.matcher( dateTime ).find();
		Boolean	likelyHasTime			= dateTime.contains( ":" );
		Boolean	likelyHasDateTime		= ( likelyIsLongFormDate || likelyHasDate ) && likelyHasTime;
		Boolean	likelyIsTimeOnly		= !likelyHasDate && !likelyIsLongFormDate && likelyHasTime;
		Boolean	likelyContainsTimezone	= dateTime.charAt( dateTime.length() - 1 ) == 'Z'
		    || REGEX_TZ_OFFSET_PATTERN.matcher( dateTime ).matches() || REGEX_TZ_ABBREVIATION_PATTERN.matcher( dateTime ).matches();
		Boolean	containsNonLatin		= Stream.of( dateTime.split( "" ) )
		    .anyMatch( c -> Character.UnicodeBlock.of( c.charAt( 0 ) ) != Character.UnicodeBlock.BASIC_LATIN );

		if ( !containsNonLatin ) {
			try {
				return likelyContainsTimezone
				    ? ZonedDateTime.parse( dateTime, getLocaleZonedDateTimeParsers( locale ) )
				    : ( likelyHasDateTime
				        ? ZonedDateTime.of( LocalDateTime.parse( dateTime, getLocaleDateTimeParsers( locale ) ), timezone )
				        : ( !likelyIsTimeOnly
				            ? ZonedDateTime.of(
				                LocalDateTime.of( LocalDate.parse( dateTime, getLocaleDateParsers( locale ) ), LocalTime.MIN ),
				                timezone )
				            : ZonedDateTime.of( LocalDate.MIN, LocalTime.parse( dateTime, getLocaleTimeParsers( locale ) ), timezone ) ) );
			} catch ( java.time.format.DateTimeParseException e ) {
				// this catches a conflicting offset issue with the ISO_OFFSET_DATE_TIME parser
				// TODO: Try to find a pattern matcher with optionals to handle all of the ISO offset formats
				if ( likelyContainsTimezone ) {
					try {
						return ZonedDateTime.parse( dateTime, getAltLocaleZonedDateTimeParsers( locale ) );
					} catch ( java.time.format.DateTimeParseException x ) {
						throw new BoxRuntimeException(
						    String.format(
						        "The date time value of [%s] could not be parsed as a valid date or datetime using the locale of [%s]",
						        dateTime,
						        locale.getDisplayName()
						    ), x );
					}
				}
				throw new BoxRuntimeException(
				    String.format(
				        "The date time value of [%s] could not be parsed as a valid date or datetime using the locale of [%s]",
				        dateTime,
				        locale.getDisplayName()
				    ), e );
			}
		} else {
			return parseFromUnicodeString( dateTime, locale, timezone );
		}

	}

	/**
	 * Parses a date time string that contains non-latin characters
	 * This method is the slowest method of parsing a date time string. It is only used for localized date/time string values
	 *
	 * @param dateTime
	 * @param locale
	 * @param timezone
	 *
	 * @return
	 */
	public static ZonedDateTime parseFromUnicodeString( String dateTime, Locale locale, ZoneId timezone ) {

		ZonedDateTime parsed = null;

		// // try parsing if it fails then our time does not contain timezone info so we fall back to a local zoned date
		try {
			parsed = ZonedDateTime.parse( dateTime, getLocaleZonedDateTimeParsers( locale ) );
		} catch ( java.time.format.DateTimeParseException e ) {
			// First fallback - it has a time without a zone
			try {
				parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime, getLocaleDateTimeParsers( locale ) ),
				    timezone );
				// Second fallback - it is only a date and we need to supply a time
			} catch ( java.time.format.DateTimeParseException x ) {
				try {
					parsed = ZonedDateTime.of(
					    LocalDateTime.of( LocalDate.parse( dateTime, getLocaleDateParsers( locale ) ), LocalTime.MIN ),
					    timezone );
					// last fallback - this is a time only value
				} catch ( java.time.format.DateTimeParseException z ) {
					parsed = ZonedDateTime.of( LocalDate.MIN, LocalTime.parse( dateTime, getLocaleTimeParsers( locale ) ),
					    ZoneId.systemDefault() );
				}
			} catch ( Exception x ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The date time value of [%s] could not be parsed as a valid date or datetime using the locale of [%s]",
				        dateTime,
				        locale.getDisplayName()
				    ), x );
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The date time value of [%s] could not be parsed with a locale of [%s]",
			        dateTime,
			        locale.getDisplayName()
			    ), e );
		}

		return parsed;
	}

	/**
	 * Returns a new DateTimeFormatterBuilder instance set to lenient parsing
	 *
	 * @return
	 */
	public static DateTimeFormatterBuilder newLenientDateTimeFormatterBuilder() {
		return new DateTimeFormatterBuilder().parseLenient();
	}

	/**
	 * Gets a formatter from cache or creates and caches it if not present.
	 * Uses SoftReference for memory-sensitive caching.
	 * 
	 * <p>
	 * DateTimeFormatter creation is resource-intensive because it involves:
	 * - Building complex DateTimeFormatterBuilder instances
	 * - Parsing multiple pattern strings
	 * - Configuring locale-specific formatting rules
	 * - Creating optional parsing chains for multiple formats
	 * 
	 * Caching provides significant performance improvements for date parsing operations
	 * while SoftReference ensures cache entries can be garbage collected under memory pressure.
	 * </p>
	 *
	 * @param cacheKey the cache key for the formatter
	 * @param supplier the function that creates the formatter if not cached
	 * 
	 * @return the cached or newly created DateTimeFormatter
	 */
	private static DateTimeFormatter getOrCreateFormatter( String cacheKey, java.util.function.Supplier<DateTimeFormatter> supplier ) {
		SoftReference<DateTimeFormatter>	ref			= formatterCache.get( cacheKey );
		DateTimeFormatter					formatter	= ref != null ? ref.get() : null;

		if ( formatter == null ) {
			// Use compute to avoid race condition
			formatterCache.compute( cacheKey, ( key, existingRef ) -> {
				DateTimeFormatter existing = existingRef != null ? existingRef.get() : null;
				if ( existing == null ) {
					return new SoftReference<>( supplier.get() );
				}
				return existingRef;
			} );
			ref			= formatterCache.get( cacheKey );
			formatter	= ref.get();
		}

		return formatter;
	}

	/**
	 * Creates a cache key for locale-specific formatters
	 *
	 * @param prefix the cache key prefix
	 * @param locale the locale to include in the key
	 * 
	 * @return the cache key string
	 */
	private static String createLocaleCacheKey( String prefix, Locale locale ) {
		return prefix + locale.toString();
	}

	/**
	 * Returns a localized set of ZonedDateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */
	public static DateTimeFormatter getLocaleZonedDateTimeParsers( Locale locale ) {
		String cacheKey = createLocaleCacheKey( LOCALE_ZONED_DATETIME_PREFIX, locale );
		return getOrCreateFormatter( cacheKey, () -> appendLocaleZonedDateTimeParsers( newLenientDateTimeFormatterBuilder(), locale ).toFormatter( locale )
		);
	}

	/**
	 * Returns a localized set of ZonedDateTime parsers with the alt format
	 * TODO: Try to find a pattern matcher with optionals to handle all of the ISO offset formats
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */
	public static DateTimeFormatter getAltLocaleZonedDateTimeParsers( Locale locale ) {
		String cacheKey = createLocaleCacheKey( ALT_LOCALE_ZONED_DATETIME_PREFIX, locale );
		return getOrCreateFormatter( cacheKey, () -> appendAltLocaleZonedDateTimeParsers( newLenientDateTimeFormatterBuilder(), locale ).toFormatter( locale )
		);
	}

	/**
	 * Appends the locale zoned date time parsers to a format buiilder instance
	 *
	 * @param builder the DateTimeFormatterBuilder instance
	 * @param locale  the Locale object which informs the formatters/parsers
	 *
	 * @return the builder instance
	 */
	public static DateTimeFormatterBuilder appendLocaleZonedDateTimeParsers( DateTimeFormatterBuilder builder, Locale locale ) {
		// The first pattern needs to go first enclosed in optionals - otherwise some ISO dates are incorrectly parsed
		return builder.appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ISO_ZONED_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
	}

	/**
	 * Appends the an alt offset format locale zoned date time parsers to a format buiilder instance this particular pattern conflicts with `DateTimeFormatter.ISO_OFFSET_DATE_TIME` so they can't be used in the same builder
	 *
	 * @param builder the DateTimeFormatterBuilder instance
	 * @param locale  the Locale object which informs the formatters/parsers
	 *
	 * @return the builder instance
	 */
	public static DateTimeFormatterBuilder appendAltLocaleZonedDateTimeParsers( DateTimeFormatterBuilder builder, Locale locale ) {
		// The first pattern needs to go first enclosed in optionals - otherwise some ISO dates are incorrectly parsed
		return builder.appendOptional( DateTimeFormatter.ofPattern( "[uuuu-MM-dd'T'HH:mm:ssX]" ) );
	}

	/**
	 * Returns a localized set of DateTime parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */
	public static DateTimeFormatter getLocaleDateTimeParsers( Locale locale ) {
		String cacheKey = createLocaleCacheKey( LOCALE_DATETIME_PREFIX, locale );
		return getOrCreateFormatter( cacheKey, () -> appendLocaleDateTimeParsers( newLenientDateTimeFormatterBuilder(), locale ).toFormatter( locale )
		);
	}

	/**
	 * Appends the locale date time parsers to a format buiilder instance
	 *
	 * @param builder the DateTimeFormatterBuilder instance
	 * @param locale  the Locale object which informs the formatters/parsers
	 *
	 * @return the builder instance
	 */
	public static DateTimeFormatterBuilder appendLocaleDateTimeParsers( DateTimeFormatterBuilder builder, Locale locale ) {
		return builder.appendOptional( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT, FormatStyle.SHORT ).withLocale( locale ) )
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
		    .appendOptional(
		        new DateTimeFormatterBuilder()
		            .parseCaseInsensitive()
		            .appendPattern( "yyyy-MM-dd hh:mm:ss a" )
		            .toFormatter( locale )
		    )
		    .appendOptional(
		        DateTimeFormatter.ofPattern( "MMMM d yyyy HH:mm" )
		    )
		    .appendOptional( DateTime.ISO_DATE_TIME_MILIS_FORMATTER )
		    .appendOptional( DateTime.ISO_DATE_TIME_MILIS_NO_T_FORMATTER )
		    .appendOptional( DateTime.ISO_DATE_TIME_VARIATION_FORMATTER )
		    .appendOptional( DateTime.DEFAULT_DATETIME_FORMATTER )
		    .appendOptional( DateTime.TS_FORMATTER )
		    .appendOptional( DateTime.JS_COMMON_TO_STRING_FORMATTER )
		    .appendOptional( DateTime.JS_COMMON_ALT_STRING_FORMATTER )
		    .appendOptional( DateTime.ZONED_DATE_TIME_TO_STRING_FORMATTER )
		    .appendOptional( DateTimeFormatter.ISO_INSTANT )
		    .appendOptional( DateTimeFormatter.ISO_DATE_TIME )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE_TIME );
	}

	/**
	 * Returns a localized set of Date parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleDateParsers( Locale locale ) {
		String cacheKey = createLocaleCacheKey( LOCALE_DATE_PREFIX, locale );
		return getOrCreateFormatter( cacheKey, () -> appendLocaleDateParsers( newLenientDateTimeFormatterBuilder(), locale ).toFormatter( locale )
		);
	}

	/**
	 * Appends the locale date parsers to a format buiilder instance
	 *
	 * @param builder the DateTimeFormatterBuilder instance
	 * @param locale  the Locale object which informs the formatters/parsers
	 *
	 * @return the builder instance
	 */
	public static DateTimeFormatterBuilder appendLocaleDateParsers( DateTimeFormatterBuilder builder, Locale locale ) {
		return builder.appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ).withLocale( locale ) )
		    // The ISO date methods don't account for leading zeros :(
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "yyyy.MM.dd" ) )
		    .appendOptional( DateTimeFormatter.ofPattern( "MM/dd/yyyy" ) )
		    .appendOptional( DateTime.DEFAULT_DATE_FORMATTER )
		    .appendOptional( DateTimeFormatter.ISO_DATE )
		    .appendOptional( DateTimeFormatter.ISO_LOCAL_DATE )
		    .appendOptional( DateTimeFormatter.BASIC_ISO_DATE );
	}

	/**
	 * Returns a localized set of Time parsers
	 *
	 * @param locale the Locale object which informs the formatters/parsers
	 *
	 * @return the localized DateTimeFormatter object
	 */

	public static DateTimeFormatter getLocaleTimeParsers( Locale locale ) {
		String cacheKey = createLocaleCacheKey( LOCALE_TIME_PREFIX, locale );
		return getOrCreateFormatter( cacheKey, () -> appendLocaleTimeParsers( newLenientDateTimeFormatterBuilder(), locale ).toFormatter( locale )
		);
	}

	/**
	 * Appends the locale time parsers to a format buiilder instance
	 *
	 * @param builder the DateTimeFormatterBuilder instance
	 * @param locale  the Locale object which informs the formatters/parsers
	 *
	 * @return the builder instance
	 */
	public static DateTimeFormatterBuilder appendLocaleTimeParsers( DateTimeFormatterBuilder builder, Locale locale ) {
		return builder.appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ).withLocale( locale ) )
		    .appendOptional( DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ).withLocale( locale ) )
		    .appendOptional( DateTime.DEFAULT_TIME_FORMATTER )
		    .appendOptional( DateTimeFormatter.ISO_TIME );
	}

	/**
	 * Gets a cached DateTimeFormatter for the specified pattern using the default locale.
	 * Uses memory-sensitive caching with SoftReference to allow garbage collection under memory pressure.
	 *
	 * @param pattern the date/time pattern string
	 * 
	 * @return the cached or newly created DateTimeFormatter
	 */
	public static DateTimeFormatter getPatternFormatter( String pattern ) {
		String cacheKey = PATTERN_FORMATTER_PREFIX + pattern;
		return getOrCreateFormatter( cacheKey, () -> newLenientDateTimeFormatterBuilder().parseCaseInsensitive().appendPattern( pattern ).toFormatter()
		);
	}

	/**
	 * Gets a cached DateTimeFormatter for the specified pattern and locale.
	 * Uses memory-sensitive caching with SoftReference to allow garbage collection under memory pressure.
	 *
	 * @param pattern the date/time pattern string
	 * @param locale  the locale to use for the formatter
	 * 
	 * @return the cached or newly created DateTimeFormatter
	 */
	public static DateTimeFormatter getPatternFormatter( String pattern, Locale locale ) {
		String cacheKey = LOCALE_PATTERN_FORMATTER_PREFIX + pattern + "_" + locale.toString();
		return getOrCreateFormatter( cacheKey, () -> newLenientDateTimeFormatterBuilder().parseCaseInsensitive().appendPattern( pattern ).toFormatter( locale )
		);
	}

	/**
	 * Clears all cached formatters
	 */
	public static void clearAllFormatterCaches() {
		formatterCache.clear();
	}

	/**
	 * Convenience method to build a locale from only a language
	 *
	 * @param language
	 *
	 * @return Locale the locale object
	 */
	public static Locale buildLocale( String language ) {
		return new Locale.Builder().setLanguage( language ).build();
	}

	/**
	 * Convenience method to build a locale from a language and region
	 *
	 * @param language
	 * @param region
	 *
	 * @return Locale the locale object
	 */
	public static Locale buildLocale( String language, String region ) {
		return new Locale.Builder().setLanguage( language ).setRegion( region ).build();
	}

	/**
	 * Convenience method to build a locale from a language, region an variant
	 *
	 * @param language
	 * @param region
	 * @param variant
	 *
	 * @return Locale the locale object
	 */
	public static Locale buildLocale( String language, String region, String variant ) {
		return new Locale.Builder().setLanguage( language ).setRegion( region ).setVariant( variant ).build();
	}
}
