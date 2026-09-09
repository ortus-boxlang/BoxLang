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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.util.LocalizationUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * I cast to DateTime objects
 */
public class DateTimeCaster implements IBoxCaster {

	public static boolean convertParsedDatesToLocalZone = false;

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<DateTime> attempt( Object object ) {
		return attempt( object, BoxRuntime.getInstance().getRuntimeContext() );
	}

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object  The value to cast
	 * @param context The context in which the casting is being performed.
	 *
	 * @return The value
	 */
	public static CastAttempt<DateTime> attempt( Object object, IBoxContext context ) {
		return CastAttempt.ofNullable( cast( object, false, context ) );
	}

	/**
	 * Tests to see if the value can be cast.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object   The value to cast
	 * @param context  The context in which the casting is being performed.
	 * @param timezone The ZoneId to ensure a timezone is applied
	 *
	 * @return The value
	 */
	public static CastAttempt<DateTime> attempt( Object object, IBoxContext context, ZoneId timezone, Locale locale ) {
		return CastAttempt.ofNullable( cast( object, false, timezone, false, context, locale ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static DateTime cast( Object object ) {
		IBoxContext context = RequestBoxContext.getCurrent();
		if ( context == null ) {
			context = BoxRuntime.getInstance().getRuntimeContext();
		}
		ZoneId timezone = LocalizationUtil.parseZoneId( null, context );
		return cast( object, true, timezone, context );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static DateTime cast( Object object, IBoxContext context ) {
		ZoneId timezone = LocalizationUtil.parseZoneId( null, context );
		return cast( object, true, timezone, context );
	}

	/**
	 * Used to cast anything
	 *
	 * @param object The value to cast
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static DateTime cast( Object object, Boolean fail, IBoxContext context ) {
		ZoneId timezone = LocalizationUtil.parseZoneId( null, context );
		return cast( object, fail, timezone, context );
	}

	/**
	 * Used to cast anything to a DateTime object. We start off by testing the object
	 * against commonly known Java date objects, and then try to parse the object as a
	 * string. If we fail, we return null.
	 *
	 * @param object   The value to cast
	 * @param fail     True to throw exception when failing.
	 * @param timezone The ZoneId to ensure a timezone is applied
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static DateTime cast( Object object, Boolean fail, ZoneId timezone, IBoxContext context ) {
		return cast( object, fail, timezone, false, context );
	}

	/**
	 * Used to cast anything to a DateTime object. We start off by testing the object
	 * against commonly known Java date objects, and then try to parse the object as a
	 * string. If we fail, we return null.
	 *
	 * @param object   The value to cast
	 * @param fail     True to throw exception when failing.
	 * @param timezone The ZoneId to ensure a timezone is applied
	 * @param clone    If true, will return a clone of the object if it was originally a DateTime.
	 * @param context  The context in which the casting is being performed.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static DateTime cast( Object object, Boolean fail, ZoneId timezone, Boolean clone, IBoxContext context ) {
		return cast( object, fail, timezone, clone, context, null );
	}

	/**
	 * Used to cast anything to a DateTime object. We start off by testing the object
	 * against commonly known Java date objects, and then try to parse the object as a
	 * string. If we fail, we return null.
	 *
	 * @param object   The value to cast
	 * @param fail     True to throw exception when failing.
	 * @param timezone The ZoneId to ensure a timezone is applied
	 * @param clone    If true, will return a clone of the object if it was originally a DateTime.
	 * @param context  The context in which the casting is being performed.
	 * @param locale   The locale to use when parsing date strings.
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static DateTime cast( Object object, Boolean fail, ZoneId timezone, Boolean clone, IBoxContext context, Locale locale ) {
		if ( timezone == null ) {
			if ( context == null ) {
				context = RequestBoxContext.getCurrent();
				if ( context == null ) {
					context = BoxRuntime.getInstance().getRuntimeContext();
				}
			}
			timezone = LocalizationUtil.parseZoneId( null, context );
		}

		// Null is null
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a DateTime." );
			} else {
				return null;
			}
		}

		// Unwrap the object
		object = DynamicObject.unWrap( object );

		// We have a DateTime object
		if ( object instanceof DateTime targetDateTime ) {
			return clone ? targetDateTime.clone() : targetDateTime;
		}

		// We have a ZonedDateTime object
		if ( object instanceof java.time.ZonedDateTime targetZonedDateTime ) {
			return new DateTime( targetZonedDateTime );
		}

		// we have a Calendar object
		if ( object instanceof java.util.Calendar targetCalendar ) {
			return new DateTime( targetCalendar.toInstant().atZone( timezone ) );
		}

		// We have a LocalDateTime object
		if ( object instanceof java.time.LocalDateTime targetLocalDateTime ) {
			return new DateTime( targetLocalDateTime.atZone( timezone ) );
		}

		// We have a LocalDate object
		if ( object instanceof java.time.LocalDate targetLocalDate ) {
			return new DateTime( targetLocalDate.atStartOfDay( timezone ) );
		}

		// This check needs to run BEFORE the next one since a java.sql.Date IS a java.util.Date, but the toInstant() method will throw an UnsupportedOperationException
		// https://docs.oracle.com/javase/8/docs/api/java/sql/Date.html#toInstant--
		if ( object instanceof java.sql.Date sDate ) {
			return new DateTime( sDate, timezone );
		}

		// This check needs to run BEFORE the next one since a java.sql.Time IS a java.util.Date, but the toInstant() method will throw an UnsupportedOperationException
		// https://docs.oracle.com/javase/8/docs/api/java/sql/Time.html#toInstant--
		if ( object instanceof java.sql.Time sTime ) {
			return new DateTime( sTime, timezone );
		}

		// We have a java.util.Date object
		if ( object instanceof java.util.Date targetDate ) {
			return new DateTime( targetDate.toInstant().atZone( timezone ) );
		}

		// We have a java.sql.Timestamp object
		if ( object instanceof java.sql.Timestamp targetTimestamp ) {
			return new DateTime( targetTimestamp.toInstant().atZone( timezone ) );
		}

		// We have a java.time.LocalTime; object
		if ( object instanceof LocalTime targetTimestamp ) {
			return new DateTime( targetTimestamp );
		}

		// Test if it is a numeric and is zero - which is the epoch
		if ( object instanceof Number nObject && nObject.doubleValue() == 0 ) {
			return new DateTime( Instant.EPOCH.atZone( ZoneId.of( "UTC" ) ) );
		}

		// Try to cast it to a String and see if we can parse it
		var targetString = StringCaster.attempt( object ).getOrDefault( null );

		// If null, we could not cast it to a string
		if ( targetString == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + object.toString() + "] to a String." );
			}
			return null;
		}

		// replace not standard spaces (like nbsp and nnsb) with standard spaces to ensure consistency for our masks
		targetString = DateTime.sanitizeStringSpaces( targetString );

		// Fast pre-rejection: if the string can't possibly be a date, bail out early
		if ( isQuickReject( targetString ) ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + targetString + "] to a DateTime." );
			}
			return null;
		}

		try {
			// Timestamp string "^\{ts ([^\}])*\}" - {ts 2023-01-01 12:00:00} or {ts '2023-01-01 12:00:00'}
			if ( targetString.trim().startsWith( "{ts" ) ) {
				Matcher tsMatcher = RegexBuilder.TIMESTAMP.matcher( targetString );
				if ( tsMatcher.matches() ) {
					return new DateTime(
					    LocalDateTime.parse(
					        targetString.trim(),
					        ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ODBCDateTime" )
					    ),
					    timezone
					);
				}
			}
			// ODBC Date string "^\{d ([^\}])*\}" - {d 2023-01-01} or {d '2023-01-01'}
			else if ( targetString.trim().startsWith( "{d" ) ) {
				Matcher dateMatcher = RegexBuilder.ODBC_DATE.matcher( targetString );
				if ( dateMatcher.matches() ) {
					return new DateTime(
					    LocalDate.parse(
					        targetString.trim(),
					        ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ODBCDate" )
					    ).atStartOfDay(), timezone
					);
				}
			}
			// ODBC Time string "^\{t ([^\}])*\}" - {t 12:00:00} or {t '12:00:00'}
			else if ( targetString.startsWith( "{t" ) ) {
				Matcher timeMatcher = RegexBuilder.ODBC_TIME.matcher( targetString );
				if ( timeMatcher.matches() ) {
					return new DateTime(
					    LocalTime.parse(
					        targetString.trim(),
					        ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ODBCTime" )
					    )
					);
				}
			}
		} catch ( Throwable e2 ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + targetString + "] to a DateTime." );
			}
			return null;
		}

		if ( targetString.trim().length() == 0 ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast an empty string to a DateTime." );
			}
			return null;
		}

		// If we have a locale, try locale-specific parsing first
		if ( locale != null ) {
			try {
				return new DateTime( targetString, locale, timezone );
			} catch ( Throwable e ) {
				// If locale-specific parsing failed, fall back to common patterns
			}
		}

		// Try common patterns as fallback
		DateTime parsed = LocalizationUtil.parseFromCommonPatterns( targetString, timezone );

		if ( parsed != null ) {
			return parsed;
		} else {
			// No locale specified or both locale and common patterns failed, try default locale parsing
			try {
				return new DateTime( targetString, timezone );
			} catch ( Throwable e ) {
				if ( fail ) {
					throw new BoxCastException( "Can't cast [" + targetString + "] to a DateTime." );
				}
				return null;
			}
		}

	}

	/**
	 * This is not meant as a cast or instance of check really-- just a conveneince method of known date classes
	 * to differentiate a variable that could possibly be cast to a date (like) a string from a variable which is
	 * ALREADY an instance of a specific date class.
	 *
	 * If this method returns true for an object, that means it SHOULD successfully cast to a DateTime
	 *
	 * @param object The object to check
	 *
	 * @return True if the object is a known date class
	 */
	public static boolean isKnownDateClass( Object object ) {
		return object != null && isKnownDateClass( object.getClass() );
	}

	/**
	 * Checks if the given class is a known date class that can be successfully cast to a DateTime.
	 *
	 * @param clazz The class to check
	 *
	 * @return True if the class is a known date class
	 */
	public static boolean isKnownDateClass( Class<?> clazz ) {
		return DateTime.class.isAssignableFrom( clazz )
		    || java.time.ZonedDateTime.class.isAssignableFrom( clazz )
		    || java.util.Calendar.class.isAssignableFrom( clazz )
		    || java.time.LocalDateTime.class.isAssignableFrom( clazz )
		    || java.time.LocalDate.class.isAssignableFrom( clazz )
		    || java.sql.Date.class.isAssignableFrom( clazz )
		    || java.util.Date.class.isAssignableFrom( clazz )
		    || java.time.Instant.class.isAssignableFrom( clazz );
	}

	/**
	 * Fast pre-rejection check for strings that cannot possibly be dates.
	 * <p>
	 * This is a cheap, single-pass, zero-allocation scan that runs BEFORE any
	 * expensive date parsing machinery (regex matching, DateTimeFormatter.parse,
	 * locale parsing). Its goal is to reject strings that are structurally
	 * impossible to be dates so we never pay the cost of the full parse pipeline
	 * for garbage input.
	 * <p>
	 * IMPORTANT: This must be conservative. It uses a BLACKLIST, not a whitelist.
	 * Only characters that appear in NO supported date format are rejected.
	 * Everything else — including CJK (年/月/日), accented Latin (é, ä, ñ),
	 * and other Unicode — passes through so the locale-specific parsing paths
	 * can handle them.
	 *
	 * @param s the input string to check
	 *
	 * @return {@code true} if this string cannot possibly be a date (reject it),
	 *         {@code false} if it might be a date and should be parsed normally
	 */
	public static boolean isQuickReject( String s ) {
		// Length sanity: the shortest date we support is a time-only "1:00" (4 chars);
		// the longest is a full ZonedDateTime/JS toString with zone name (~52 chars).
		// Anything shorter or longer cannot be a date.
		int len = s.length();
		if ( len < 4 || len > 60 ) {
			return true;
		}

		// Every date must contain at least one digit (year, day, month, hour, etc.)
		boolean hasDigit = false;
		for ( int i = 0; i < len; i++ ) {
			char c = s.charAt( i );

			if ( c >= '0' && c <= '9' ) {
				// Digit found — mark it and continue
				hasDigit = true;
				continue;
			}

			// BLACKLIST: these characters appear in NO date format anywhere.
			// Any of them means the string is guaranteed not to be a date.
			if ( c == '_' || c == '#' || c == '@' || c == '!' || c == '~' || c == '`' ||
			    c == '^' || c == '&' || c == '*' || c == '=' || c == '|' || c == '\\' ||
			    c == '"' || c == ';' || c == '<' || c == '>' || c == '?' ) {
				// Instant reject — saves us from the entire parse pipeline.
				return true;
			}

			if ( c < 0x20 && c != '\t' ) {
				// Control characters (other than tab) never appear in dates
				return true;
			}

			// NOTE: every other character (letters, digits, separators, CJK,
			// accented Latin, etc.) falls through WITHOUT rejection — the
			// locale-specific parsing paths handle those.
		}

		// If we never saw a digit, this cannot be a date (no year/month/day/hour)
		return !hasDigit;
	}

}
