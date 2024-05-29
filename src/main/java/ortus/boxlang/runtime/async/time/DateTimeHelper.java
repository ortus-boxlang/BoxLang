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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.zone.ZoneRulesException;
import java.util.concurrent.TimeUnit;

import javax.management.InvalidAttributeValueException;

/**
 * We represent a static date/time helper class that assists with time units on date/time conversions
 * It doesn't hold any date/time information.
 *
 * <ul>
 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ChronoUnit.html">Java.time.ChronoUnit</a>
 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html">Java.time.ZoneId</a>
 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneOffset.html">Java.time.ZoneOffset</a>
 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Instant.html">Java.time.Instant</a>
 * </ul>
 */
public class DateTimeHelper {

	public static final DateTimeFormatter	ISO_DATE_FORMATTER	= DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	public static final DateTimeFormatter	ISO_DATE_ONLY		= DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

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
	 * <ul>
	 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html">Java.time.ZoneId</a></li>
	 * </ul>
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
	 * <ul>
	 * <li><a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/ZoneId.html">Java.time.ZoneId</a></li>
	 * </ul>
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

	/**
	 * Determines the number of days in a month.
	 *
	 * @param now The date/time to use to determine the number of days in the month
	 *
	 * @return The number of days in the month
	 */
	public static int daysInMonth( LocalDateTime now ) {
		return now.getMonth().maxLength();
	}

	/**
	 * Determines the number of days in the current month
	 *
	 * @return The number of days in the month
	 */
	public static int daysInMonth() {
		return daysInMonth( now() );
	}

	/**
	 * This utility method gives us the first business day of the month in Java format
	 *
	 * @param time     The specific time using 24 hour format => HH:mm, defaults to midnight
	 * @param addMonth Boolean to specify adding a month to today's date
	 * @param now      The date to use as the starting point, defaults to now()
	 *
	 * @return The first business day of the month
	 */
	public static LocalDateTime getFirstBusinessDayOfTheMonth( String time, Boolean addMonth, LocalDateTime now ) {
		// Adding a month?
		if ( addMonth ) {
			now = now.plusMonths( 1 );
		}
		// Get the last day of the month
		return now
		    // First business day of the month
		    .with( TemporalAdjusters.firstInMonth( DayOfWeek.MONDAY ) )
		    // Specific Time
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );
	}

	/**
	 * This utility method gives us the first business day of the month in Java format
	 *
	 * @param time     The specific time using 24 hour format => HH:mm, defaults to midnight
	 * @param addMonth Boolean to specify adding a month to today's date
	 * @param timezone The timezone to use
	 *
	 * @return The first business day of the month
	 */
	public static LocalDateTime getFirstBusinessDayOfTheMonth( String time, Boolean addMonth, ZoneId timezone ) {
		return getFirstBusinessDayOfTheMonth( time, addMonth, DateTimeHelper.now( timezone ) );
	}

	/**
	 * This utility method gives us the first business day of the month in Java format
	 *
	 * @param timezone The timezone to use
	 *
	 * @return The first business day of the month
	 */
	public static LocalDateTime getFirstBusinessDayOfTheMonth( ZoneId timezone ) {
		return getFirstBusinessDayOfTheMonth( "00:00", false, DateTimeHelper.now( timezone ) );
	}

	/**
	 * This utility method gives us the first business day of the month in Java format
	 *
	 * @return The first business day of the month
	 */
	public static LocalDateTime getFirstBusinessDayOfTheMonth() {
		return getFirstBusinessDayOfTheMonth( "00:00", false, DateTimeHelper.now( getSystemTimezone() ) );
	}

	/**
	 * This utility method gives us the last business day of the month in Java format
	 *
	 * @param time     The specific time using 24 hour format => HH:mm, defaults to midnight
	 * @param addMonth Boolean to specify adding a month to today's date
	 * @param now      The date to use as the starting point, defaults to now()
	 *
	 * @return The last business day of the month
	 */
	public static LocalDateTime getLastBusinessDayOfTheMonth( String time, Boolean addMonth, LocalDateTime now ) {
		// Adding a month?
		if ( addMonth ) {
			now = now.plusMonths( 1 );
		}
		// Get the last day of the month
		LocalDateTime lastDay = now
		    // last business day of the month
		    .with( TemporalAdjusters.lastDayOfMonth() )
		    // Specific Time
		    .withHour( Integer.parseInt( time.split( ":" )[ 0 ] ) )
		    .withMinute( Integer.parseInt( time.split( ":" )[ 1 ] ) )
		    .withSecond( 0 );

		// Verify if on weekend
		switch ( lastDay.getDayOfWeek().getValue() ) {
			// Sunday - 2 days
			case 7 : {
				lastDay = lastDay.minusDays( 2 );
				break;
			}
			// Saturday - 1 day
			case 6 : {
				lastDay = lastDay.minusDays( 1 );
				break;
			}
			default :
				break;
		}

		return lastDay;
	}

	/**
	 * This utility method gives us the last business day of the month in Java format
	 *
	 * @param time     The specific time using 24 hour format => HH:mm, defaults to midnight
	 * @param addMonth Boolean to specify adding a month to today's date
	 * @param timezone The timezone to use
	 *
	 * @return The last business day of the month
	 */
	public static LocalDateTime getLastBusinessDayOfTheMonth( String time, Boolean addMonth, ZoneId timezone ) {
		return getLastBusinessDayOfTheMonth( time, addMonth, DateTimeHelper.now( timezone ) );
	}

	/**
	 * This utility method gives us the last business day of the month in Java format
	 *
	 * @param timezone The timezone to use
	 *
	 * @return The last business day of the month
	 */
	public static LocalDateTime getLastBusinessDayOfTheMonth( ZoneId timezone ) {
		return getLastBusinessDayOfTheMonth( "00:00", false, DateTimeHelper.now( timezone ) );
	}

	/**
	 * This utility method gives us the last business day of the month in Java format
	 *
	 * @return The last business day of the month
	 */
	public static LocalDateTime getLastBusinessDayOfTheMonth() {
		return getLastBusinessDayOfTheMonth( "00:00", false, DateTimeHelper.now( getSystemTimezone() ) );
	}

	/**
	 * Validates an incoming string to adhere to HH: mm while allowing a user to simply enter an hour value
	 *
	 * @param time The time to validate
	 *
	 * @return The validated time
	 *
	 * @throws InvalidAttributeValueException
	 */
	public static String validateTime( String time ) throws InvalidAttributeValueException {
		if ( !time.matches( "^([0-1][0-9]|[2][0-3]):[0-5][0-9]$" ) ) {

			// Do we have only hours?
			if ( time.contains( ":" ) ) {
				throw new InvalidAttributeValueException( "Invalid time representation (" + time + "). Time is represented in 24 hour minute format => HH:mm" );
			}

			return validateTime( time + ":00" );
		}

		return time;
	}

	/**
	 * Transforms the incoming value in the specified time unit to seconds
	 *
	 * @param value  The value to convert to seconds
	 * @param target The time unit to transform
	 *
	 * @return The time unit in seconds
	 */
	public static long timeUnitToSeconds( long value, TimeUnit target ) {
		// transform all to seconds
		switch ( target ) {
			case SECONDS :
				value = value * 60 * 60 * 24;
				break;
			case HOURS :
				value = value * 60 * 60;
				break;
			case MINUTES :
				value = value * 60;
				break;
			case MILLISECONDS :
				value = value / 1000;
				break;
			case MICROSECONDS :
				value = value / 1000000;
				break;
			case NANOSECONDS :
				value = value / 1000000000;
				break;
			default :
				break;
		}
		return value;
	}

	/**
	 * This method adds a specific amount of a TimeUnit into the incoming target LocalDateTime
	 *
	 * @param target   The target to add the amount to
	 * @param amount   The amount of time to add to the initial next run date time
	 * @param timeUnit The time unit to use for the period
	 *
	 * @return The calculated dateTime
	 */
	public static LocalDateTime dateTimeAdd( LocalDateTime target, long amount, TimeUnit timeUnit ) {
		return switch ( timeUnit ) {
			case DAYS -> target.plusDays( amount );
			case HOURS -> target.plusHours( amount );
			case MINUTES -> target.plusMinutes( amount );
			case MILLISECONDS -> target.plusSeconds( amount / 1000 );
			case MICROSECONDS -> target.plusNanos( amount * 1000 );
			case NANOSECONDS -> target.plusNanos( amount );
			default -> target.plusSeconds( amount );
		};
	}

}
