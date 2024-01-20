/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( alias = "Year" )
@BoxBIF( alias = "Quarter" )
@BoxBIF( alias = "Month" )
@BoxBIF( alias = "MonthAsString" )
@BoxBIF( alias = "MonthShortAsString" )
@BoxBIF( alias = "Day" )
@BoxBIF( alias = "DayOfWeek" )
@BoxBIF( alias = "DayOfWeekAsString" )
@BoxBIF( alias = "DayOfWeekShortAsString" )
@BoxBIF( alias = "DaysInMonth" )
@BoxBIF( alias = "DaysInYear" )
@BoxBIF( alias = "DayOfYear" )
@BoxBIF( alias = "Hour" )
@BoxBIF( alias = "Minute" )
@BoxBIF( alias = "Second" )
@BoxBIF( alias = "Millisecond" )
@BoxBIF( alias = "Nanosecond" )
@BoxBIF( alias = "Offset" )
@BoxBIF( alias = "GetTimezone" )
@BoxMember( type = BoxLangType.DATETIME, name = "year" )
@BoxMember( type = BoxLangType.DATETIME, name = "quarter" )
@BoxMember( type = BoxLangType.DATETIME, name = "month" )
@BoxMember( type = BoxLangType.DATETIME, name = "monthAsString" )
@BoxMember( type = BoxLangType.DATETIME, name = "monthShortAsString" )
@BoxMember( type = BoxLangType.DATETIME, name = "day" )
@BoxMember( type = BoxLangType.DATETIME, name = "dayOfWeek" )
@BoxMember( type = BoxLangType.DATETIME, name = "dayOfWeekAsString" )
@BoxMember( type = BoxLangType.DATETIME, name = "dayOfWeekShortAsString" )
@BoxMember( type = BoxLangType.DATETIME, name = "daysInMonth" )
@BoxMember( type = BoxLangType.DATETIME, name = "daysInYear" )
@BoxMember( type = BoxLangType.DATETIME, name = "dayOfYear" )
@BoxMember( type = BoxLangType.DATETIME, name = "hour" )
@BoxMember( type = BoxLangType.DATETIME, name = "minute" )
@BoxMember( type = BoxLangType.DATETIME, name = "second" )
@BoxMember( type = BoxLangType.DATETIME, name = "millisecond" )
@BoxMember( type = BoxLangType.DATETIME, name = "nanosecond" )
@BoxMember( type = BoxLangType.DATETIME, name = "offset" )
@BoxMember( type = BoxLangType.DATETIME, name = "timezone" )

public class TimeUnits extends BIF {

	public static final String	OFFSET_FORMAT		= "xxxx";
	public static final String	TZ_SHORT_FORMAT		= "v";
	public static final String	TZ_LONG_FORMAT		= "z";
	public static final String	MONTH_SHORT_FORMAT	= "MMM";
	public static final String	MONTH_LONG_FORMAT	= "MMMM";
	public static final String	DOW_SHORT_FORMAT	= "eee";
	public static final String	DOW_LONG_FORMAT		= "eeee";

	static final class bifMethods {

		public static final Key		quarter					= Key.quarter;
		public static final Key		month					= Key.month;
		public static final Key		monthAsString			= Key.of( "monthAsString" );
		public static final Key		monthShortAsString		= Key.of( "monthShortAsString" );
		public static final Key		day						= Key.day;
		public static final Key		dayOfWeek				= Key.of( "dayOfWeek" );
		public static final Key		dayOfWeekAsString		= Key.of( "dayOfWeekAsString" );
		public static final Key		dayOfWeekShortAsString	= Key.of( "dayOfWeekShortAsString" );
		public static final Key		daysInMonth				= Key.of( "daysInMonth" );
		public static final Key		daysInYear				= Key.of( "daysInYear" );
		public static final Key		millis					= Key.millisecond;
		public static final Key		offset					= Key.of( "offset" );
		public static final Key		timeZone				= Key.timezone;
		public static final Key		getTimeZone				= Key.of( "getTimeZone" );

		/**
		 * Map of method names to BIF names
		 */
		public final static Struct	memberMethods			= new Struct(
		    new HashMap<String, String>() {

			    {
				    put( "Year", "getYear" );
				    put( "Day", "getDayOfMonth" );
				    put( "DayOfYear", "getDayOfYear" );
				    put( "Hour", "getHour" );
				    put( "Minute", "getMinute" );
				    put( "Second", "getSecond" );
				    put( "Nanosecond", "getNano" );
			    }
		    }
		);

	}

	/**
	 * Constructor
	 */
	public TimeUnits() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Provides the BIF and member functions for all time unit request with no arguments
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The DateTime object or datetime string representation
	 *
	 * @argument.timezone The timezone with which to cast the result
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime dateRef = DateTimeCaster.cast( arguments.get( Key.date ), true,
		    LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context ) );

		if ( arguments.get( Key.timezone ) != null ) {
			dateRef = dateRef.clone( ZoneId.of( arguments.getAsString( Key.timezone ) ) );
		}

		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		String	methodName		= null;
		if ( bifMethods.memberMethods.containsKey( bifMethodKey ) ) {
			methodName = ( String ) bifMethods.memberMethods.get( ( Object ) bifMethodKey );
			return dateRef.dereferenceAndInvoke( context, Key.of( methodName ), arguments, false );
		} else {
			// @formatter:off
			// prettier-ignore
			Object result =
				bifMethodKey.equals( bifMethods.quarter ) ? dateRef.getWrapped().get( IsoFields.QUARTER_OF_YEAR )
				: bifMethodKey.equals( bifMethods.month ) ? dateRef.getWrapped().getMonth().getValue()
				: bifMethodKey.equals( bifMethods.monthAsString ) ? dateRef.clone().format( MONTH_LONG_FORMAT )
				: bifMethodKey.equals( bifMethods.monthShortAsString ) ? dateRef.clone().format( MONTH_SHORT_FORMAT )
				: bifMethodKey.equals( bifMethods.day ) ? dateRef.getWrapped().getDayOfMonth()
				: bifMethodKey.equals( bifMethods.dayOfWeek ) ? dateRef.clone().getWrapped().getDayOfWeek().getValue()
				: bifMethodKey.equals( bifMethods.dayOfWeekAsString ) ? dateRef.clone().format( DOW_LONG_FORMAT )
				: bifMethodKey.equals( bifMethods.dayOfWeekShortAsString ) ? dateRef.clone().format( DOW_SHORT_FORMAT )
				: bifMethodKey.equals( bifMethods.daysInMonth ) ? dateRef.getWrapped().getMonth().length( dateRef.isLeapYear() )
				: bifMethodKey.equals( bifMethods.daysInYear ) ? Year.of( dateRef.getWrapped().getYear() ).length()
				: bifMethodKey.equals( bifMethods.millis ) ? dateRef.getWrapped().getNano() / 1000000
				: bifMethodKey.equals( bifMethods.offset ) ? dateRef.clone().format( OFFSET_FORMAT )
				: (
					bifMethodKey.equals( bifMethods.timeZone )
					||
					bifMethodKey.equals( bifMethods.getTimeZone )
				  ) ? dateRef.clone().format( TZ_SHORT_FORMAT )
				: null;
			// @formatter:on
			if ( result == null ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The method [%s] is not present in the [%s] object",
				        arguments.getAsString( BIF.__functionName ),
				        dateRef.getClass().getSimpleName()
				    )
				);
			}

			return result;

		}
	}

}
