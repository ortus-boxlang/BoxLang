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

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

public class DateTime implements IType {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	protected static ZonedDateTime		wrapped;

	/**
	 * Metadata object
	 */
	public BoxMeta						$bx;

	private static DateTimeFormatter	formatter;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create default DateTime representing the current instance
	 */
	public DateTime() {
		this( ZonedDateTime.of( LocalDateTime.now(), ZoneId.systemDefault() ) );
	}

	/**
	 * Constructor to create DateTime from a ZonedDateTime object
	 *
	 * @param dateTime A zoned date time object
	 */
	public DateTime( ZonedDateTime dateTime ) {
		// Set our default formatter to match Lucee's ODBC pattern
		setFormat( "'{ts '''yyyy-MM-dd HH:mm:ss'''}'" );
		wrapped = dateTime;
	}

	/**
	 * Constructor to create DateTime from a time string and a mask
	 *
	 * @param dateTime - a string representing the date and time
	 * @param mask     - a string representing the mask
	 */
	public DateTime( String dateTime, String mask ) {
		// Set our default formatter to match Lucee's ODBC pattern
		setFormat( "'{ts '''yyyy-MM-dd HH:mm:ss'''}'" );
		ZonedDateTime parsed = null;
		// try parsing if it fails then our time does not contain timezone info so we fall back to a local zoned date
		try {
			parsed = ZonedDateTime.parse( dateTime, getFormatter( mask ) );
		} catch ( java.time.format.DateTimeParseException e ) {
			parsed = ZonedDateTime.of( LocalDateTime.parse( dateTime, getFormatter( mask ) ), ZoneId.systemDefault() );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( String.format(
			    "The the date time value of [" + dateTime + "] could not be parsed using the mask [" + mask + "]"
			) );
		}
		wrapped = parsed;
	}

	/**
	 * Constructor to create DateTime from a string
	 *
	 * @param dateTime - a string representing the date and time
	 */
	public DateTime( String dateTime ) {
		// Set our default formatter to match Lucee's ODBC pattern
		setFormat( "'{ts '''yyyy-MM-dd HH:mm:ss'''}'" );
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
				throw new BoxRuntimeException( String.format(
				    "The the date time value of [" + dateTime + "] could not be parsed as a valid date or datetime"
				) );
			}
		}

		wrapped = parsed;
	}

	/**
	 * Constructor to create DateTime from a numerics through millisecond
	 *
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @param milliseconds
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day,
	    Integer hour,
	    Integer minute,
	    Integer second,
	    Integer milliseconds ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        hour,
		        minute,
		        second,
		        milliseconds * 1000000,
		        ZoneId.systemDefault()
		    )
		);
	}

	/**
	 * Constructor to create DateTime from a numerics through day
	 *
	 * @param year
	 * @param month
	 * @param day
	 */
	public DateTime(
	    Integer year,
	    Integer month,
	    Integer day ) {
		this(
		    ZonedDateTime.of(
		        year,
		        month,
		        day,
		        0,
		        0,
		        0,
		        0,
		        ZoneId.systemDefault()
		    )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static convenience methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns a DateTime formatter from a pattern
	 */
	private static DateTimeFormatter getFormatter( String pattern ) {
		return DateTimeFormatter.ofPattern( pattern );
	}

	/**
	 * Sets the current formatter with a mask
	 *
	 * @param mask the formatting mask to use
	 */
	private static void setFormat( String mask ) {
		formatter = DateTimeFormatter.ofPattern( mask );
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
	 * Member Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the datetime represntation as a string
	 **/
	@Override
	public String toString() {
		return formatter.format( wrapped );
	}

	/**
	 * Returns the date time representation as a string in the specified format mask
	 *
	 * @param mask the formatting mask to use
	 *
	 * @return
	 */
	public String format( String mask ) {
		setFormat( mask );
		return formatter.format( wrapped );
	}

	/**
	 * Returns the date time representation as a string in the specified format mask
	 *
	 * @return
	 */
	public String toISOString() {
		formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		return toString();
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
	 *
	 * @param timeZone
	 *
	 * @return
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
	 * Sets the timezone
	 *
	 * @param timeZone the string representation of the timezone
	 */
	public DateTime setTimezone( String timeZone ) {
		wrapped = wrapped.withZoneSameLocal( ZoneId.of( timeZone ) );
		return this;
	}

}
