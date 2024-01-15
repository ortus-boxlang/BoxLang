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

@BoxBIF( alias = "Year" )
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

	static final class bifReference {

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
		public final static Struct	memberMap				= new Struct(
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
		DateTime dateRef = DateTimeCaster.cast( arguments.get( Key.date ) );

		if ( arguments.get( Key.timezone ) != null ) {
			dateRef = dateRef.clone( ZoneId.of( arguments.getAsString( Key.timezone ) ) );
		}

		Key		bifMethodKey	= arguments.getAsKey( __functionName );
		String	methodName		= null;
		if ( bifReference.memberMap.containsKey( bifMethodKey ) ) {
			methodName = ( String ) bifReference.memberMap.get( ( Object ) bifMethodKey );
			return dateRef.dereferenceAndInvoke( context, Key.of( methodName ), arguments, false );
		} else {
			// @formatter:off
			// prettier-ignore
			Object result =
				bifMethodKey.equals( bifReference.month ) ? dateRef.getWrapped().getMonth().getValue()
				: bifMethodKey.equals( bifReference.monthAsString ) ? dateRef.clone().format( MONTH_LONG_FORMAT )
				: bifMethodKey.equals( bifReference.monthShortAsString ) ? dateRef.clone().format( MONTH_SHORT_FORMAT )
				: bifMethodKey.equals( bifReference.day ) ? dateRef.getWrapped().getDayOfMonth()
				: bifMethodKey.equals( bifReference.dayOfWeek ) ? dateRef.clone().getWrapped().getDayOfWeek().getValue()
				: bifMethodKey.equals( bifReference.dayOfWeekAsString ) ? dateRef.clone().format( DOW_LONG_FORMAT )
				: bifMethodKey.equals( bifReference.dayOfWeekShortAsString ) ? dateRef.clone().format( DOW_SHORT_FORMAT )
				: bifMethodKey.equals( bifReference.daysInMonth ) ? dateRef.getWrapped().getMonth().length( dateRef.isLeapYear() )
				: bifMethodKey.equals( bifReference.daysInYear ) ? Year.of( dateRef.getWrapped().getYear() ).length()
				: bifMethodKey.equals( bifReference.millis ) ? dateRef.getWrapped().getNano() / 1000000
				: bifMethodKey.equals( bifReference.offset ) ? dateRef.clone().format( OFFSET_FORMAT )
				: bifMethodKey.equals( bifReference.timeZone ) || bifReference.getTimeZone.equals( bifMethodKey )
				? dateRef.clone().format( TZ_SHORT_FORMAT )
				: null;
			// @formatter:on
			if ( result == null ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The method [%s] is not present in the [%s] object",
				        arguments.getAsString( Key.of( __functionName ) ),
				        dateRef.getClass().getSimpleName()
				    )
				);
			}

			return result;

		}
	}

}
