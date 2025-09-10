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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
	 * @param object The value to cast
	 *
	 * @return The value
	 */
	public static CastAttempt<DateTime> attempt( Object object, IBoxContext context ) {
		return CastAttempt.ofNullable( cast( object, false, context ) );
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
	 *
	 * @return The value, or null when cannot be cast
	 */
	public static DateTime cast( Object object, Boolean fail, ZoneId timezone, Boolean clone, IBoxContext context ) {
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

		// This check needs to run BEFORE the next one since a java.sql.Date IS a java.util.Date, but the toInstance() method will throw an unchecked exception
		if ( object instanceof java.sql.Date sDate ) {
			return new DateTime( sDate, timezone );
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

		// Try to cast it to a String and see if we can parse it
		var targetString = StringCaster.attempt( object ).getOrDefault( null );

		// If null, we could not cast it to a string
		if ( targetString == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + object.toString() + "] to a String." );
			}
			return null;
		}

		try {
			// Timestamp string "^\{ts ([^\}])*\}" - {ts 2023-01-01 12:00:00}
			if ( RegexBuilder.of( targetString, RegexBuilder.TIMESTAMP ).matches() ) {
				return new DateTime(
				    LocalDateTime.parse(
				        targetString.trim(),
				        ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ODBCDateTime" )
				    )
				);
			}
		} catch ( Throwable e2 ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast [" + targetString + "] to a DateTime." );
			}
			return null;
		}

		// Now let's go to Apache commons lang for its date parsing
		try {
			return LocalizationUtil.parseFromCommonPatterns( targetString );
		} catch ( java.time.format.DateTimeParseException e ) {
			try {
				return new DateTime( targetString, timezone );
			} catch ( Throwable e2 ) {
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
		return switch ( object ) {
			case DateTime d -> {
				yield true;
			}
			case java.time.ZonedDateTime d -> {
				yield true;
			}
			case java.util.Calendar d -> {
				yield true;
			}
			case java.time.LocalDateTime d -> {
				yield true;
			}
			case java.time.LocalDate d -> {
				yield true;
			}
			case java.sql.Date d -> {
				yield true;
			}
			case java.util.Date d -> {
				yield true;
			}
			default -> {
				yield false;
			}
		};
	}

}
