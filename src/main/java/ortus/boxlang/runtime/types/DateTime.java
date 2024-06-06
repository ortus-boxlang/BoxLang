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
package ortus.boxlang.runtime.types;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.util.LocalizationUtil;

/**
 * A DateTime object that wraps a ZonedDateTime object and provides additional functionality
 * for date time manipulation and formatting the BoxLang way.
 */
public class DateTime implements IType, IReferenceable, Serializable, ValueWriter, ChronoZonedDateTime<LocalDate> {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Serial version UID
	 */
	private static final long				serialVersionUID							= 1L;

	/**
	 * Represents the wrapped ZonedDateTime object we enhance
	 */
	protected ZonedDateTime					wrapped;

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Formatters
	 */
	// This mask matches the Lucee default - @TODO ISO would be a better default - can we change this
	public static final String				TS_FORMAT_MASK								= "'{ts '''yyyy-MM-dd HH:mm:ss'''}'";
	public static final String				DEFAULT_DATE_FORMAT_MASK					= "dd-MMM-yy";
	public static final String				DEFAULT_TIME_FORMAT_MASK					= "HH:mm a";
	public static final String				DEFAULT_DATETIME_FORMAT_MASK				= "dd-MMM-yyyy HH:mm:ss";
	public static final String				ISO_DATE_TIME_VARIATION_FORMAT_MASK			= "yyyy-MM-dd HH:mm:ss";
	public static final String				ISO_DATE_TIME_MILIS_FORMAT_MASK				= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final String				ISO_OFFSET_DATE_TIME_NOMILLIS_FORMAT_MASK	= "yyyy-MM-dd'T'HH:mm:ssXXX";
	// <a href="https://learn.microsoft.com/en-us/sql/odbc/reference/develop-app/date-time-and-timestamp-literals">The ODBC default format masks</a>
	public static final String				ODBC_DATE_TIME_FORMAT_MASK					= TS_FORMAT_MASK;
	public static final String				ODBC_DATE_FORMAT_MASK						= "'{d '''yyyy-MM-dd'''}'";
	public static final String				ODBC_TIME_FORMAT_MASK						= "'{t '''HH:mm:ss'''}'";

	/**
	 * Common Modes
	 */
	public static final String				MODE_DATE									= "Date";
	public static final String				MODE_TIME									= "Time";
	public static final String				MODE_DATETIME								= "DateTime";

	/**
	 * Common Formatters Map so we can easily access them by name
	 */
	public static final IStruct				COMMON_FORMATTERS							= Struct.of(
	    "fullDateTime", DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL, FormatStyle.FULL ),
	    "longDateTime", DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG, FormatStyle.LONG ),
	    "mediumDateTime", DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM, FormatStyle.MEDIUM ),
	    "shortDateTime", DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT, FormatStyle.SHORT ),
	    "ISODateTime", DateTimeFormatter.ofPattern( ISO_OFFSET_DATE_TIME_NOMILLIS_FORMAT_MASK ),
	    "ISO8601DateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME,
	    "ODBCDateTime", DateTimeFormatter.ofPattern( ODBC_DATE_TIME_FORMAT_MASK ),
	    "fullDate", DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ),
	    "longDate", DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ),
	    "mediumDate", DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ),
	    "shortDate", DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ),
	    "ISODate", DateTimeFormatter.ISO_DATE,
	    "ISO8601Date", DateTimeFormatter.ISO_DATE,
	    "ODBCDate", DateTimeFormatter.ofPattern( ODBC_DATE_FORMAT_MASK ),
	    "fullTime", DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ),
	    "longTime", DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ),
	    "mediumTime", DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ),
	    "shortTime", DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ),
	    "ISOTime", DateTimeFormatter.ISO_TIME,
	    "ISO8601Time", DateTimeFormatter.ISO_TIME,
	    "ODBCTime", DateTimeFormatter.ofPattern( ODBC_TIME_FORMAT_MASK )
	);

	/**
	 * The format we use to represent the date time
	 * which defaults to the ODBC format: {ts '''yyyy-MM-dd HH:mm:ss'''}
	 */
	private transient DateTimeFormatter		formatter									= DateTimeFormatter.ofPattern( TS_FORMAT_MASK );

	/**
	 * Function service
	 */
	private static final FunctionService	functionService								= BoxRuntime.getInstance().getFunctionService();

	/**
	 * Metadata object
	 */
	public transient BoxMeta				$bx;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create default DateTime representing the current instance and the default timezone
	 */
	public DateTime() {
		this( ZoneId.systemDefault() );
	}

	/**
	 * Constructor to create DateTime with a timezone
	 *
	 * @param zoneId The timezone to use
	 */
	public DateTime( ZoneId zoneId ) {
		this( ZonedDateTime.now( zoneId ) );
	}

	/**
	 * Constructor to create DateTime from a ZonedDateTime object
	 *
	 * @param dateTime A zoned date time object
	 */
	public DateTime( ZonedDateTime dateTime ) {
		this.wrapped = dateTime;
	}

	/**
	 * Constructor to create DateTime from a java.util.Date object
	 * This will use the system default timezone
	 *
	 * @param date The date object
	 */
	public DateTime( java.util.Date date ) {
		this(
		    ( date instanceof java.sql.Date sqlDate )
		        ? ZonedDateTime.of( sqlDate.toLocalDate(), LocalTime.of( 0, 0 ), ZoneId.systemDefault() )
		        : ( date instanceof java.sql.Time sqlTime )
		            ? ZonedDateTime.of( LocalDate.EPOCH, sqlTime.toLocalTime(), ZoneId.systemDefault() )
		            : date.toInstant().atZone( ZoneId.systemDefault() )
		);
	}

	/**
	 * Constructor to create DateTime from a java.sql.Date object which has no time component
	 * This will use the system default timezone
	 *
	 * @param date The date object
	 */
	public DateTime( java.sql.Date date ) {
		this( ZonedDateTime.of( date.toLocalDate(), LocalTime.of( 0, 0 ), ZoneId.systemDefault() ) );
	}

	/**
	 * Constructor to create DateTime from a java.sql.Time object which has no date component
	 * This will use the system default timezone
	 *
	 * @param time The time object
	 */
	public DateTime( java.sql.Time time ) {
		this( ZonedDateTime.of( LocalDate.EPOCH, time.toLocalTime(), ZoneId.systemDefault() ) );
	}

	/**
	 * Constructor to create DateTime from a LocalDateTime object
	 * This will use the system default timezone
	 *
	 * @param dateTime A local date time object
	 */
	public DateTime( LocalDateTime dateTime ) {
		this( ZonedDateTime.of( dateTime, ZoneId.systemDefault() ) );
	}

	/**
	 * Constructor to create DateTime from a LocalDate object
	 * This will use the system default timezone
	 *
	 * @param date A local date object
	 */
	public DateTime( LocalDate date ) {
		this( ZonedDateTime.of( date.atStartOfDay(), ZoneId.systemDefault() ) );
	}

	/**
	 * Constructor to create DateTime from a Instant
	 *
	 * @param instant An instant object
	 */
	public DateTime( Instant instant ) {
		this.wrapped = ZonedDateTime.ofInstant( instant, ZoneId.systemDefault() );
	}

	/**
	 * Constructor to create DateTime from a time string and a mask
	 *
	 * @param dateTime - a string representing the date and time
	 * @param mask     - a string representing the mask
	 */
	public DateTime( String dateTime, String mask ) {
		this( dateTime, mask, ZoneId.systemDefault() );
	}

	/**
	 * Constructor to create DateTime from a time string and a mask
	 *
	 * @param dateTime - a string representing the date and time
	 * @param mask     - a string representing the mask
	 */
	public DateTime( String dateTime, String mask, ZoneId timezone ) {
		ZonedDateTime parsed = null;
		// try parsing if it fails then our time does not contain timezone info so we fall back to a local zoned date
		try {
			parsed = ZonedDateTime.parse( dateTime, getFormatter( mask ) );
		} catch ( java.time.format.DateTimeParseException e ) {
			// First fallback - it has a time without a zone
			try {
				parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime, getFormatter( mask ) ), timezone );
				// Second fallback - it is only a date and we need to supply a time
			} catch ( java.time.format.DateTimeParseException x ) {
				try {
					parsed = ZonedDateTime.of( LocalDateTime.of( LocalDate.parse( dateTime, getFormatter( mask ) ), LocalTime.MIN ), timezone );
					// last fallback - this is a time only value
				} catch ( java.time.format.DateTimeParseException z ) {
					parsed = ZonedDateTime.of( LocalDate.MIN, LocalTime.parse( dateTime, getFormatter( mask ) ), timezone );
				}
			} catch ( Exception x ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The the date time value of [%s] could not be parsed as a valid date or datetime",
				        dateTime
				    ), x );
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The the date time value of [%s] could not be parsed using the mask [%s]",
			        dateTime,
			        mask
			    ), e );
		}
		this.wrapped = parsed;
	}

	/**
	 * Constructor to create DateTime from a string, using the system locale and timezone
	 *
	 * @param dateTime - a string representing the date and time
	 */
	public DateTime( String dateTime ) {
		this( dateTime, ZoneId.systemDefault() );
	}

	/**
	 * Constructor to create DateTime from a string with a specified timezone, using the system locale
	 *
	 * @param dateTime - a string representing the date and time
	 * @param timezone - the timezone string
	 */
	public DateTime( String dateTime, ZoneId timezone ) {
		this( dateTime, Locale.getDefault(), timezone );
	}

	/**
	 * Constructor to create DateTime from a datetime string from a specific locale and timezone
	 *
	 * @param dateTime - a string representing the date and time
	 * @param locale   - a locale object used to assist in parsing the string
	 * @param timezone The timezone to assign to the string, if an offset or zone is not provided in the value
	 */
	public DateTime( String dateTime, Locale locale, ZoneId timezone ) {
		ZonedDateTime parsed = null;
		this.formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale( locale );
		// try parsing if it fails then our time does not contain timezone info so we fall back to a local zoned date
		try {
			parsed = ZonedDateTime.parse( dateTime, LocalizationUtil.getLocaleZonedDateTimeParsers( locale ) );
		} catch ( java.time.format.DateTimeParseException e ) {
			// First fallback - it has a time without a zone
			try {
				parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime, LocalizationUtil.getLocaleDateTimeParsers( locale ) ),
				    timezone );
				// Second fallback - it is only a date and we need to supply a time
			} catch ( java.time.format.DateTimeParseException x ) {
				try {
					parsed = ZonedDateTime.of(
					    LocalDateTime.of( LocalDate.parse( dateTime, LocalizationUtil.getLocaleDateParsers( locale ) ), LocalTime.MIN ),
					    timezone );
					// last fallback - this is a time only value
				} catch ( java.time.format.DateTimeParseException z ) {
					parsed = ZonedDateTime.of( LocalDate.MIN, LocalTime.parse( dateTime, LocalizationUtil.getLocaleTimeParsers( locale ) ),
					    ZoneId.systemDefault() );
				}
			} catch ( Exception x ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The the date time value of [%s] could not be parsed as a valid date or datetimea locale of [%s]",
				        dateTime,
				        locale.getDisplayName()
				    ), x );
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The the date time value of [%s] could not be parsed with a locale of [%s]",
			        dateTime,
			        locale.getDisplayName()
			    ), e );
		}
		this.wrapped = parsed;
	}

	/**
	 * Constructor to create DateTime from a numerics through millisecond
	 *
	 * @param year         The year
	 * @param month        The month
	 * @param day          The day
	 * @param hour         The hour
	 * @param minute       The minute
	 * @param second       The second
	 * @param milliseconds The milliseconds
	 * @param timezone     The ZoneId for the timezone
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day,
	    Integer hour,
	    Integer minute,
	    Integer second,
	    Integer milliseconds,
	    ZoneId timezone ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        hour,
		        minute,
		        second,
		        milliseconds * 1000000,
		        timezone == null ? ZoneId.systemDefault() : timezone
		    )
		);
	}

	/**
	 * Constructor to create DateTime from a numerics through day in the default timezone
	 *
	 * @param year  The year
	 * @param month The month
	 * @param day   The day
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day ) {
		this( year, month, day, null );
	}

	/**
	 * Constructor to create DateTime from a numerics through day with a timezone
	 *
	 * @param year     The year
	 * @param month    The month
	 * @param day      The day
	 * @param timezone The timezone
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day,
	    ZoneId timezone ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        0,
		        0,
		        0,
		        0,
		        ( timezone != null ) ? timezone : ZoneId.systemDefault()
		    )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Convenience methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns a DateTime formatter from a pattern passed in
	 *
	 * @param pattern the pattern to use
	 *
	 * @return the DateTimeFormatter object with the pattern
	 */
	private static DateTimeFormatter getFormatter( String pattern ) {
		return DateTimeFormatter.ofPattern( pattern );
	}

	/**
	 * Convenience method to get a common date time formatter if it exists in the {@link DateTime#COMMON_FORMATTERS} map
	 * else it will return a new DateTimeFormatter instance according to the passed mask.
	 *
	 * @param mask The mask to use with a postfix of {@code DateTime} or a common formatter key:
	 *             fullDateTime, longDateTime, mediumDateTime, shortDateTime,
	 *             ISODateTime, ISO8601DateTime, ODBCDateTime
	 *
	 * @return The DateTimeFormatter object
	 */
	public static DateTimeFormatter getDateTimeFormatter( String mask ) {
		return ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.getOrDefault(
		    Key.of( mask + MODE_DATETIME ),
		    DateTimeFormatter.ofPattern( mask )
		);
	}

	/**
	 * Chainable member function to set the format and return the object
	 *
	 * @param mask the formatting mask to use
	 *
	 * @return
	 */
	public DateTime setFormat( String mask ) {
		this.formatter = DateTimeFormatter.ofPattern( mask );
		return this;
	}

	/**
	 * Alternate format setter which accepts a DateTimeFormatter object
	 *
	 * @param formatter A DateTimeFormatter instance
	 *
	 * @return
	 */
	public DateTime setFormat( DateTimeFormatter formatter ) {
		this.formatter = formatter;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * IType Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Interface method to return the string representation
	 **/
	public String asString() {
		return toString();
	}

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Type Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the hashcode of the wrapped object
	 */
	@Override
	public int hashCode() {
		return Objects.hash( wrapped, formatter );
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 *
	 * @param obj The reference object with which to compare.
	 *
	 * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;
		DateTime other = ( DateTime ) obj;
		return Objects.equals( wrapped, other.wrapped ) &&
		    Objects.equals( formatter, other.formatter );
	}

	/**
	 * Returns the datetime representation as a string
	 **/
	@Override
	public String toString() {
		return this.formatter.format( this.wrapped );
	}

	/*
	 * Clones this object to produce a new object
	 */
	public DateTime clone() {
		return clone( this.wrapped.getZone() );
	}

	/**
	 * Clones this object to produce a new object
	 *
	 * @param timezone the string timezone to cast the clone to
	 */
	public DateTime clone( ZoneId timezone ) {
		return new DateTime( ZonedDateTime.ofInstant( this.wrapped.toInstant(), timezone != null ? timezone : this.wrapped.getZone() ) );
	}

	/**
	 * Determines whether the year of this object is a leap year
	 *
	 * @return boolean
	 */
	public Boolean isLeapYear() {
		return Year.isLeap( this.wrapped.getYear() );
	}

	/**
	 * Returns the date time representation as a string in the specified format mask
	 *
	 * @param mask the formatting mask to use
	 *
	 * @return the date time representation as a string in the specified format mask
	 */
	public String format( String mask ) {
		return this.format( Locale.getDefault(), mask );
	}

	/**
	 * Returns the date time representation as a string with the provided formatter
	 *
	 * @param formatter The DateTimeFormatter instance
	 *
	 * @return the date time representation as a string in the specified format mask
	 */
	@Override
	public String format( DateTimeFormatter formatter ) {
		return this.wrapped.format( formatter );
	}

	/**
	 * Returns the date time representation as a string in the specified locale
	 *
	 * @param locale the locale to use
	 * @param mask   the formatting mask to use
	 *
	 * @return the date time representation as a string in the specified format mask
	 */
	public String format( Locale locale, String mask ) {
		if ( mask == null ) {
			return this.wrapped.format( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG, FormatStyle.LONG ).withLocale( locale ) );
		} else {
			return this.format( getDateTimeFormatter( mask ).withLocale( locale ) );
		}
	}

	/**
	 * Returns the date time representation as a string in the specified format mask
	 *
	 * @return
	 */
	public String toISOString() {
		this.formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		return toString();
	}

	/**
	 * Returns this date time in epoch time ( seconds )
	 *
	 * @return The epoch time in seconds
	 */
	public Long toEpoch() {
		return this.wrapped.toEpochSecond();
	}

	/**
	 * Returns this date time in epoch milliseconds
	 *
	 * @return The epoch time in milliseconds
	 */
	public Long toEpochMillis() {
		return this.wrapped.toInstant().toEpochMilli();
	}

	/**
	 * Allows the date object to be modified by a convention of unit ( datepart ) and quantity. Supports the DateAdd BIF
	 *
	 * @param unit     - an abbreviation for a time unit
	 *                 d/y - day
	 *                 m - month
	 *                 yyyy - year
	 *                 w - weekdays
	 *                 ww - weeks
	 *                 h - hours
	 *                 n - minutes
	 *                 s - seconds
	 *                 l - milliseconds
	 * @param quantity a positive or negative quantity of the unit to modify the DateTime
	 *
	 * @return the DateTime instance
	 */
	public DateTime modify( String unit, Long quantity ) {
		switch ( unit ) {
			case "d" :
			case "y" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusDays( quantity ) : wrapped.minusDays( Math.abs( quantity ) );
				break;
			case "yyyy" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusYears( quantity ) : wrapped.minusYears( Math.abs( quantity ) );
				break;
			case "q" :
				Long multiplier = 3l;
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusMonths( quantity * multiplier ) : wrapped.minusMonths( Math.abs( quantity ) * multiplier );
				break;
			case "m" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusMonths( quantity ) : wrapped.minusMonths( Math.abs( quantity ) );
				break;
			case "w" :
				Integer dayOfWeek = wrapped.getDayOfWeek().getValue();
				switch ( dayOfWeek ) {
					case 5 :
						quantity = quantity + 2l;
						break;
					case 6 :
						quantity = quantity + 1l;
				}
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusDays( quantity ) : wrapped.minusDays( Math.abs( quantity ) );
				break;
			case "ww" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusWeeks( quantity ) : wrapped.minusWeeks( Math.abs( quantity ) );
				break;
			case "h" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusHours( quantity ) : wrapped.minusHours( Math.abs( quantity ) );
				break;
			case "n" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusMinutes( quantity ) : wrapped.minusMinutes( Math.abs( quantity ) );
				break;
			case "s" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plusSeconds( quantity ) : wrapped.minusSeconds( Math.abs( quantity ) );
				break;
			case "l" :
				wrapped = Long.signum( quantity ) == 1 ? wrapped.plus( quantity, ChronoUnit.MILLIS ) : wrapped.minus( Math.abs( quantity ), ChronoUnit.MILLIS );
				break;
		}
		return this;
	}

	/**
	 * Converts this date to a specified timezone - this may result in a change to the date and time
	 *
	 * @param timezone the ZoneId of the timezone to convert to
	 *
	 * @return a new converted DateTime instance
	 */
	public DateTime convertToZone( ZoneId timezone ) {
		return new DateTime( getWrapped().withZoneSameInstant( timezone ) );
	}

	/**
	 * Sets the timezone of the current wrapped date time
	 *
	 * @param timeZone The string representation of the timezone; e.g. "America/New_York", "UTC", "Asia/Tokyo" etc.
	 *
	 * @return The new DateTime object with the timezone set
	 */
	public DateTime setTimezone( String timeZone ) {
		return setTimezone( ZoneId.of( timeZone ) );
	}

	/**
	 * Sets the timezone of the current wrapped date time using a ZoneId object
	 *
	 * @param zoneId The ZoneId object to use
	 *
	 * @return The new DateTime object with the timezone set
	 */
	public DateTime setTimezone( ZoneId zoneId ) {
		this.wrapped = wrapped.withZoneSameLocal( zoneId );
		return this;
	}

	/**
	 * Get's the original wrapped ZonedDateTime object
	 *
	 * @return The original wrapped ZonedDateTime object
	 */
	public ZonedDateTime getWrapped() {
		return this.wrapped;
	}

	/**
	 * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object.
	 */
	public Long getTime() {
		return this.wrapped.toInstant().toEpochMilli();
	}

	/**
	 * --------------------------------------------------------------------------
	 * IReferenceable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( IBoxContext context, Key key, Object value ) {
		DynamicInteropService.setField( this, key.getName().toLowerCase(), value );
		return this;
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {
		try {
			return DynamicInteropService.getField( this, key.getName().toLowerCase() ).get();
		} catch ( NoSuchElementException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The property [%s] does not exist or is not public in the class [%s].",
			        key.getName(),
			        this.getClass().getSimpleName()
			    )
			);
		}
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.DATETIME );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		if ( DynamicInteropService.hasMethodNoCase( this.getClass(), name.getName() ) ) {
			return DynamicInteropService.invoke( this, name.getName(), safe, positionalArguments );
		} else if ( DynamicInteropService.hasMethodNoCase( this.wrapped.getClass(), name.getName() ) ) {
			return DynamicInteropService.invoke( this.wrapped, name.getName(), safe, positionalArguments );
		} else if ( DynamicInteropService.hasMethodNoCase( this.getClass(), "get" + name.getName() ) ) {
			return DynamicInteropService.invoke( this.wrapped, "get" + name.getName(), safe, positionalArguments );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "The method [%s] is not present in the [%s] object",
			        name.getName(),
			        this.getClass().getSimpleName()
			    )
			);
		}
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.DATETIME );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}
		if ( DynamicInteropService.hasMethodNoCase( this.getClass(), name.getName() ) ) {
			return DynamicInteropService.invoke( this, name.getName(), safe, namedArguments );
			// no args - just pass through to the wrapped methods
		} else if ( DynamicInteropService.hasMethodNoCase( this.wrapped.getClass(), name.getName() ) ) {
			return DynamicInteropService.invoke( this.wrapped, name.getName(), safe );
		} else if ( DynamicInteropService.hasMethodNoCase( this.getClass(), "get" + name.getName() ) ) {
			return DynamicInteropService.invoke( this.wrapped, "get" + name.getName(), safe );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "The method [%s] is not present in the [%s] object",
			        name.getName(),
			        this.getClass().getSimpleName()
			    )
			);
		}
	}

	/**
	 * Comparable interface method
	 *
	 * @param other The other DateTime object to compare to
	 *
	 * @return The comparison result: -1 if less, 0 if equal, 1 if greater
	 */
	@Override
	public int compareTo( ChronoZonedDateTime<?> other ) {
		if ( other instanceof DateTime castedDateTime ) {
			return getWrapped().compareTo( castedDateTime.getWrapped() );
		}
		if ( other instanceof ZonedDateTime castedDateTime ) {
			return getWrapped().compareTo( castedDateTime );
		}
		return getWrapped().compareTo( DateTimeCaster.cast( other ).getWrapped() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * JSON Serialization
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		DateTime dateTime = ( DateTime ) value;
		g.writeString( dateTime.toISOString() );
	}

	@Override
	public Class<?> valueType() {
		return DateTime.class;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Temporal Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public Chronology getChronology() {
		return this.wrapped.getChronology();
	}

	@Override
	public int get( TemporalField field ) {
		return this.wrapped.get( field );
	}

	@Override
	public long getLong( TemporalField field ) {
		return this.wrapped.getLong( field );
	}

	@Override
	public ZoneOffset getOffset() {
		return this.wrapped.getOffset();
	}

	@Override
	public boolean isAfter( ChronoZonedDateTime<?> other ) {
		return this.wrapped.isAfter( other );
	}

	@Override
	public boolean isBefore( ChronoZonedDateTime<?> other ) {
		return this.wrapped.isBefore( other );
	}

	@Override
	public boolean isEqual( ChronoZonedDateTime<?> other ) {
		return this.wrapped.isEqual( other );
	}

	@Override
	public boolean isSupported( TemporalField field ) {
		return this.wrapped.isSupported( field );
	}

	@Override
	public boolean isSupported( TemporalUnit unit ) {
		return this.wrapped.isSupported( unit );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> minus( long amountToSubtract, TemporalUnit unit ) {
		return this.wrapped.minus( amountToSubtract, unit );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> minus( TemporalAmount amount ) {
		return this.wrapped.minus( amount );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> plus( long amountToSubtract, TemporalUnit unit ) {
		return this.wrapped.plus( amountToSubtract, unit );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> plus( TemporalAmount amount ) {
		return this.wrapped.plus( amount );
	}

	@Override
	public <R> R query( TemporalQuery<R> query ) {
		return this.wrapped.query( query );
	}

	@Override
	public ValueRange range( TemporalField field ) {
		return this.wrapped.range( field );
	}

	@Override
	public long toEpochSecond() {
		return this.wrapped.toEpochSecond();
	}

	@Override
	public Instant toInstant() {
		return this.wrapped.toInstant();
	}

	@Override
	public LocalDate toLocalDate() {
		return this.wrapped.toLocalDate();
	}

	@Override
	public ChronoLocalDateTime<LocalDate> toLocalDateTime() {
		return this.wrapped.toLocalDateTime();
	}

	@Override
	public LocalTime toLocalTime() {
		return this.wrapped.toLocalTime();
	}

	@Override
	public long until( Temporal endExclusive, TemporalUnit unit ) {
		return this.wrapped.until( endExclusive, unit );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> with( TemporalAdjuster adjuster ) {
		return this.wrapped.with( adjuster );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> with( TemporalField field, long newValue ) {
		return this.wrapped.with( field, newValue );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> withEarlierOffsetAtOverlap() {
		return this.wrapped.withEarlierOffsetAtOverlap();
	}

	@Override
	public ChronoZonedDateTime<LocalDate> withLaterOffsetAtOverlap() {
		return this.wrapped.withLaterOffsetAtOverlap();
	}

	@Override
	public ChronoZonedDateTime<LocalDate> withZoneSameInstant( ZoneId zone ) {
		return this.wrapped.withZoneSameInstant( zone );
	}

	@Override
	public ChronoZonedDateTime<LocalDate> withZoneSameLocal( ZoneId zone ) {
		return this.wrapped.withZoneSameLocal( zone );
	}

	@Override
	public ZoneId getZone() {
		return this.wrapped.getZone();
	}

}
