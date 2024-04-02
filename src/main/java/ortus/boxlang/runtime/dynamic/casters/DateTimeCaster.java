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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.time.DateUtils;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I cast to DateTime objects
 */
public class DateTimeCaster {

	private static final String[] COMMON_PATTERNS = {

	    // Localized Date/Time formats
	    "EEE, dd MMM yyyy HH:mm:ss zzz", // Full DateTime (e.g., Tue, 02 Apr 2024 21:01:00 CEST) - Similar to FULL_FULL
	    "dd MMM yyyy HH:mm:ss",         // Long DateTime (e.g., 02 Apr 2024 21:01:00) - Similar to LONG_LONG
	    "dd-MMM-yyyy HH:mm:ss",         // Medium DateTime (e.g., 02-Apr-2024 21:01:00) - Might need adjustment based on locale
	    "dd/MM/yyyy HH:mm:ss",         // Short DateTime (e.g., 02/04/2024 21:01:00) - Might need adjustment based on locale
	    "dd.MM.yyyy HH:mm:ss",         // Short DateTime (e.g., 02.04.2024 21:01:00) - Might need adjustment based on locale

	    // ISO formats
	    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",  // Date-time with milliseconds and offset
	    "yyyy-MM-dd'T'HH:mm:ss.SSS",     // Date-time with milliseconds
	    "yyyy-MM-dd'T'HH:mm:ssZ",        // Date-time with offset (Z)
	    "yyyy-MM-dd'T'HH:mm:ssX",        // Date-time with offset (X)
	    "yyyy-MM-dd'T'HH:mm:ss",         // Date-time

	    // ODBC formats
	    "yyyyMMddHHmmss",                // OBCDateTime - Potential ODBC format

	    // Localized Date formats
	    "EEE, dd MMM yyyy",            // Full Date (e.g., Tue, 02 Apr 2024) - Similar to FULL
	    "dd MMM yyyy",                   // Long Date (e.g., 02 Apr 2024) - Similar to LONG
	    "dd-MMM-yyyy",                   // Medium Date (e.g., 02-Apr-2024) - Might need adjustment based on locale
	    "dd/MMM/yyyy",                   // Medium Date (e.g., 02-Apr-2024) - Might need adjustment based on locale
	    "dd.MMM.yyyy",                   // Medium Date (e.g., 02.Apr.2024) - Might need adjustment based on locale

	    "dd MM yyyy",                   // Short Date (e.g., 02.04.2024) - Might need adjustment based on locale
	    "dd-MM-yyyy",                   // Short Date (e.g., 02-04-2024) - Might need adjustment based on locale
	    "dd/MM/yyyy",                   // Short Date (e.g., 02/04/2024) - Might need adjustment based on locale
	    "dd.MM.yyyy",                   // Short Date (e.g., 02.04.2024) - Might need adjustment based on locale

	    // Localized Date formats - Month First
	    "MMM dd yyyy",                   // Long Date (e.g., Apr 02 2024)
	    "MMM-dd-yyyy",                   // Medium Date (e.g., Apr-02-2024) - Might need adjustment based on locale
	    "MMM/dd/yyyy",                   // Medium Date (e.g., Apr/02/2024) - Might need adjustment based on locale
	    "MMM.dd.yyyy",                   // Medium Date (e.g., Apr.02.2024) - Might need adjustment based on locale

	    // Localized Date formats - Month First (Short)
	    "MM dd yyyy",                   // Short Date (e.g., 04 02 2024) - Might need adjustment based on locale
	    "MM-dd-yyyy",                   // Short Date (e.g., 04-02-2024) - Might need adjustment based on locale
	    "MM/dd/yyyy",                   // Short Date (e.g., 04/02/2024) - Might need adjustment based on locale
	    "MM.dd.yyyy",                   // Short Date (e.g., 04.02.2024) - Might need adjustment based on locale

	    // ISO format
	    "yyyy-MM-dd",                   // ISODate (e.g., 2024-04-02)
	    "yyyy/MM/dd",                   // ISODate (e.g., 2024/04/02)
	    "yyyy.MM.dd",                   // ISODate (e.g., 2024.04.02)

	    // ODBC format
	    "yyyyMMdd"                     // ODBCDate - Potential ODBC format
	};

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
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static DateTime cast( Object object ) {
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
	public static DateTime cast( Object object, Boolean fail ) {
		return cast( object, fail, ZoneId.systemDefault() );
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
	public static DateTime cast( Object object, Boolean fail, ZoneId timezone ) {

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
			return targetDateTime;
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

		// We have a java.util.Date object
		if ( object instanceof java.util.Date targetDate ) {
			return new DateTime( targetDate.toInstant().atZone( timezone ) );
		}

		// We have a java.sql.Date object
		if ( object instanceof java.sql.Date targetDate ) {
			return new DateTime( targetDate.toLocalDate().atStartOfDay( timezone ) );
		}

		// We have a java.sql.Timestamp object
		if ( object instanceof java.sql.Timestamp targetTimestamp ) {
			return new DateTime( targetTimestamp.toInstant().atZone( timezone ) );
		}

		// Try to cast it to a String and see if we can parse it
		var targetString = StringCaster.cast( object, fail );

		// Timestamp string "^\{ts ([^\}])*\}" - {ts 2023-01-01 12:00:00}
		if ( targetString.matches( "^\\{ts ([^\\}]*)\\}" ) ) {
			return new DateTime(
			    ZonedDateTime.parse( targetString, ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ODBCDateTime" ) )
			);
		}

		// Now let's go to Apache commons lang for its date parsing
		try {
			return new DateTime( DateUtils.parseDate( targetString, COMMON_PATTERNS ) );
		} catch ( java.text.ParseException e ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + object + "] to a DateTime.", e );
			}
			return null;
		}

	}

}
