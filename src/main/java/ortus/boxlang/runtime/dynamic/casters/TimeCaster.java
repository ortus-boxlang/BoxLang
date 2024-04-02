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

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I cast to Time objects
 */
public class TimeCaster {

	private static final String[] COMMON_FORMATS = {
	    "h:mm:ss a", // 12-hour format with seconds and AM/PM (e.g., 2:30:59 PM)
	    "h:mm a", // 12-hour format with AM/PM (e.g., 2:30 PM)
	    "h a", // 12-hour format with AM/PM (e.g., 2 PM)
	    "HH:mm:ss",  // 24-hour format with seconds (e.g., 14:30:59)
	    "HH:mm",  // 24-hour format (e.g., 14:30)
	    "HH", // 24-hour format (e.g., 14)
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
	public static CastAttempt<LocalTime> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static LocalTime cast( Object object ) {
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
	public static LocalTime cast( Object object, Boolean fail ) {
		return cast( object, fail, ZoneId.systemDefault() );
	}

	/**
	 * Used to cast anything to a LocalTime object.
	 *
	 * @param object   The value to cast
	 * @param fail     True to throw exception when failing.
	 * @param timezone The ZoneId to ensure a timezone is applied
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static LocalTime cast( Object object, Boolean fail, ZoneId timezone ) {

		// Null is null
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a LocalTime." );
			} else {
				return null;
			}
		}

		// Unwrap the object
		object = DynamicObject.unWrap( object );

		// We have a DateTime object
		if ( object instanceof DateTime targetDateTime ) {
			return targetDateTime.toLocalTime();
		}

		// We have a ZonedDateTime object
		if ( object instanceof java.time.ZonedDateTime targetZonedDateTime ) {
			return targetZonedDateTime.toLocalTime();
		}

		// we have a Calendar object
		if ( object instanceof java.util.Calendar targetCalendar ) {
			return LocalTime.of(
			    targetCalendar.get( java.util.Calendar.HOUR_OF_DAY ),
			    targetCalendar.get( java.util.Calendar.MINUTE ),
			    targetCalendar.get( java.util.Calendar.SECOND )
			);
		}

		// We have a LocalDateTime object
		if ( object instanceof java.time.LocalDateTime targetLocalDateTime ) {
			return targetLocalDateTime.toLocalTime();
		}

		// We have a LocalDate object
		if ( object instanceof java.time.LocalDate targetLocalDate ) {
			return LocalTime.of( 0, 0, 0 );
		}

		// We have a java.util.Date object
		if ( object instanceof java.util.Date targetDate ) {
			return LocalTime.ofInstant( targetDate.toInstant(), timezone );
		}

		// We have a java.sql.Date object
		if ( object instanceof java.sql.Date targetDate ) {
			return LocalTime.of( 0, 0, 0 );
		}

		// We have a java.sql.Timestamp object
		if ( object instanceof java.sql.Timestamp targetTimestamp ) {
			return targetTimestamp.toLocalDateTime().toLocalTime();
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

		// Try to parse the string
		for ( String format : COMMON_FORMATS ) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern( format );
				return LocalTime.parse( targetString, formatter );
			} catch ( DateTimeParseException e ) {
				// Ignore parse exception and try the next format
			}
		}

		if ( fail ) {
			throw new BoxCastException( "Can't cast [" + object + "] to a LocalTime." );
		}
		return null;
	}

}
