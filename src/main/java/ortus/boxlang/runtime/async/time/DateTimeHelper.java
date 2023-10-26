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
package ortus.boxlang.runtime.async.time;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

/**
 * We represent a static date/time helper class that assists with time units on date/time conversions
 * It doesn't hold any date/time information.
 *
 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/temporal/ChronoUnit.html
 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html
 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneOffset.html
 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Instant.html
 */
public class DateTimeHelper {

	public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	/**
	 * Get the current date/time as a Java LocalDateTime object in the system timezone
	 *
	 * @return Right now as a Java LocalDateTime object
	 */
	public static LocalDateTime now() {
		return now( getSystemTimezone() );
	}

	/**
	 * Get the current date/time as a Java LocalDateTime object in the passed timezone
	 *
	 * @param timezone The timezone to use when converting the date/time as a string
	 *
	 * @return Right now as a Java LocalDateTime object
	 */
	public static LocalDateTime now( ZoneId timezone ) {
		return LocalDateTime.now( timezone );
	}

	/**
	 * Get the current date/time as a Java LocalDateTime object in the passed timezone
	 *
	 * @param timezone The timezone to use when converting the date/time as a string
	 *
	 * @return Right now as a Java LocalDateTime object
	 */
	public static LocalDateTime now( String timezone ) {
		return now( ZoneId.of( timezone ) );
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to a Java instant temporal object
	 *
	 * @param target The date/time or string object representing the date/time
	 *
	 * @throws IllegalArgumentException If the target is not a string or java.util.Date
	 *
	 * @return A Java temporal object as java.time.Instant
	 */
	public static Instant toInstant( Object target ) throws IllegalArgumentException {
		if ( target instanceof String castedTarget ) {
			return Instant.parse( castedTarget );
		} else if ( target instanceof java.util.Date castedTarget ) {
			return castedTarget.toInstant();
		} else {
			throw new IllegalArgumentException( "Unsupported date/time" );
		}
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDateTime class so we can use them as Temporal objects
	 *
	 * @param target   The java.util.Date or string object representing the date/time
	 *
	 * @param timezone If passed, we will use this timezone to build the temporal object. Else we default to UTC
	 *
	 * @return A Java temporal object as java.time.LocalDateTime
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static LocalDateTime toLocalDateTime( Object target, ZoneId timezone ) {
		return toInstant( target )
		    .atZone( timezone == null ? ZoneOffset.UTC : timezone )
		    .toLocalDateTime();
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDateTime class so we can use them as Temporal objects
	 *
	 * @param target   The java.util.Date or string object representing the date/time
	 * @param timezone The timezone to use when converting the date/time as a string
	 *
	 * @return A Java temporal object as java.time.LocalDateTime
	 */
	public static LocalDateTime toLocalDateTime( Object target, String timezone ) {
		return toLocalDateTime( target, timezone == null || timezone.length() == 0 ? null : ZoneId.of( timezone ) );
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDateTime class so we can use them as Temporal objects
	 * using the UTC timezone as default
	 *
	 * @param target The java.util.Date or string object representing the date/time
	 *
	 * @return A Java temporal object as java.time.LocalDateTime
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static LocalDateTime toLocalDateTime( Object target ) {
		return toLocalDateTime( target, ZoneOffset.UTC );
	}

	/**
	 * Convert an incoming ISO-8601 formatted string to a Java LocalDateTime object
	 *
	 * @param target The ISO-8601 formatted string
	 *
	 * @return a java LocalDateTime object
	 */
	public static LocalDateTime parse( String target ) {
		return LocalDateTime.parse( target, ISO_DATE_FORMATTER );
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDate class so we can use them as Temporal objects
	 *
	 * @param target   The java.util.Date date/time or string object representing the date/time
	 * @param timezone If passed, we will use this timezone to build the temporal object. Else we default to UTC
	 *
	 * @return A Java temporal object as java.time.LocalDate
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static LocalDate toLocalDate( Object target, ZoneId timezone ) {
		return toInstant( target )
		    .atZone( timezone == null ? ZoneOffset.UTC : timezone )
		    .toLocalDate();
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDate class so we can use them as Temporal objects
	 *
	 * @param target   The java.util.Date date/time or string object representing the date/time
	 * @param timezone If passed, we will use this timezone to build the temporal object. Else we default to UTC
	 *
	 * @return A Java temporal object as java.time.LocalDate
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static LocalDate toLocalDate( Object target, String timezone ) {
		return toLocalDate( target, timezone == null || timezone.length() == 0 ? null : ZoneId.of( timezone ) );
	}

	/**
	 * Convert any legacy java.util.Date or string date/time object to the new Java.time.LocalDate class so we can use them as Temporal objects.
	 * Using the UTC timezone as default
	 *
	 * @param target The java.util.Date date/time or string object representing the date/time
	 *
	 * @return A Java temporal object as java.time.LocalDate
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static LocalDate toLocalDate( Object target ) {
		return toLocalDate( target, ZoneOffset.UTC );
	}

	/**
	 * Get the Java Zone ID of the passed in timezone identifier string
	 *
	 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html
	 *
	 * @param timezone The String timezone identifier
	 *
	 * @return Java Timezone java.time.ZoneId
	 *
	 * @throws DateTimeException  - if the zone ID has an invalid format
	 * @throws ZoneRulesException - if the zone ID is a region ID that cannot be found
	 */
	public static ZoneId getTimezone( String timezone ) {
		return ZoneId.of( timezone );
	}

	/**
	 * This queries TimeZone.getDefault() to find the default time-zone and converts it to a ZoneId. If the system default time-zone is changed, then the
	 * result of this method will also change.
	 *
	 * @see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html
	 *
	 * @return Java Timezone java.time.ZoneId
	 */
	public static ZoneId getSystemTimezone() {
		return ZoneId.systemDefault();
	}

	/**
	 * Generate an iso8601 formatted string from an incoming date/time object
	 *
	 * @param dateTime The input datetime or if not passed, the current date/time
	 * @param toUTC    By default, we convert all times to UTC for standardization
	 */
	public static String getIsoTime( LocalDateTime dateTime, boolean toUTC ) {
		return dateTime.atZone( toUTC ? ZoneOffset.UTC : getSystemTimezone() ).format( ISO_DATE_FORMATTER );
	}

	/**
	 * Generate an iso8601 formatted string from an incoming date/time object
	 *
	 * @param dateTime The input datetime or if not passed, the current date/time
	 *
	 * @return The iso8601 formatted string
	 */
	public static String getIsoTime( LocalDateTime dateTime ) {
		return getIsoTime( dateTime, true );
	}

}
