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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

public class DateTime implements IType {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Represents the wrapped ZonedDateTime object we enhance
	 */
	protected ZonedDateTime		wrapped;

	/**
	 * The format we use to represent the date time
	 * which defaults to the ODBC format: {ts '''yyyy-MM-dd HH:mm:ss'''}
	 */
	private DateTimeFormatter	formatter						= DateTimeFormatter.ofPattern( ODBC_FORMAT_MASK );

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Formatters
	 */
	public static final String	ODBC_FORMAT_MASK				= "'{ts '''yyyy-MM-dd HH:mm:ss'''}'";
	public static final String	DEFAULT_DATE_FORMAT_MASK		= "dd-MMM-yy";
	public static final String	DEFAULT_TIME_FORMAT_MASK		= "HH:mm a";
	public static final String	DEFAULT_DATETIME_FORMAT_MASK	= "dd-MMM-yyyy HH:mm:ss";

	/**
	 * Metadata object
	 */
	public BoxMeta				$bx;

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
		this( ZonedDateTime.of( LocalDateTime.now(), zoneId ) );
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
	 * Constructor to create DateTime from a Instant
	 *
	 * @param dateTime A zoned date time object
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
		ZonedDateTime parsed = null;
		// try parsing if it fails then our time does not contain timezone info so we fall back to a local zoned date
		try {
			parsed = ZonedDateTime.parse( dateTime, getFormatter( mask ) );
		} catch ( java.time.format.DateTimeParseException e ) {
			parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime, getFormatter( mask ) ), ZoneId.systemDefault() );
		} catch ( Exception e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The the date time value of [%s] could not be parsed using the mask [%s]",
			        dateTime,
			        mask
			    ) );
		}
		this.wrapped = parsed;
	}

	/**
	 * Constructor to create DateTime from a string
	 *
	 * @param dateTime - a string representing the date and time
	 */
	public DateTime( String dateTime ) {
		ZonedDateTime parsed = null;
		try {
			parsed = ZonedDateTime.parse( dateTime, formatter );
		} catch ( java.time.format.DateTimeParseException e ) {
			// First fallback - it has a time without a zone
			try {
				parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime ), ZoneId.systemDefault() );
				// Second fallback - it is only a date and we need to supply a time
			} catch ( java.time.format.DateTimeParseException x ) {
				parsed = ZonedDateTime.of( LocalDateTime.of( LocalDate.parse( dateTime ), LocalTime.MIN ), ZoneId.systemDefault() );
			} catch ( Exception x ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The the date time value of [%s] could not be parsed as a valid date or datetime",
				        dateTime
				    ) );
			}
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
	 * @param timezone     The timezone
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day,
	    Integer hour,
	    Integer minute,
	    Integer second,
	    Integer milliseconds,
	    String timezone ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        hour,
		        minute,
		        second,
		        milliseconds * 1000000,
		        ( timezone != null ) ? ZoneId.of( timezone ) : ZoneId.systemDefault()
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
	    String timezone ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        0,
		        0,
		        0,
		        0,
		        ( timezone != null ) ? ZoneId.of( timezone ) : ZoneId.systemDefault()
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
	 * Sets the current formatter with a mask
	 * TODO: SHouldn't this be public?
	 *
	 * @param mask the formatting mask to use
	 */
	private void setFormat( String mask ) {
		this.formatter = DateTimeFormatter.ofPattern( mask );
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

	/**
	 * Returns the date time representation as a string in the specified format mask
	 *
	 * @param mask the formatting mask to use
	 *
	 * @return the date time representation as a string in the specified format mask
	 */
	public String format( String mask ) {
		setFormat( mask );
		return this.formatter.format( wrapped );
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
	 * Returns this date time as an instant
	 *
	 * @return An instant representing this date time
	 */
	public Instant toInstant() {
		return this.wrapped.toInstant();
	}

	/**
	 * Chainable member function to set the format and return the object
	 *
	 * @param mask the formatting mask to use
	 *
	 * @return
	 */
	public DateTime withFormat( String mask ) {
		setFormat( mask );
		return this;
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

}
