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

import java.time.ZonedDateTime;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( alias = "dateSetDay", description = "Sets the day of the month on a date object" )
@BoxBIF( alias = "dateSetHour", description = "Sets the hour on a date object" )
@BoxBIF( alias = "dateSetMinute", description = "Sets the minute on a date object" )
@BoxBIF( alias = "dateSetMonth", description = "Sets the month on a date object" )
@BoxBIF( alias = "dateSetSecond", description = "Sets the second on a date object" )
@BoxBIF( alias = "dateSetYear", description = "Sets the year on a date object" )
@BoxMember( type = BoxLangType.DATETIME, name = "setDay", objectArgument = "date" )
@BoxMember( type = BoxLangType.DATETIME, name = "setHour", objectArgument = "date" )
@BoxMember( type = BoxLangType.DATETIME, name = "setMinute", objectArgument = "date" )
@BoxMember( type = BoxLangType.DATETIME, name = "setMonth", objectArgument = "date" )
@BoxMember( type = BoxLangType.DATETIME, name = "setSecond", objectArgument = "date" )
@BoxMember( type = BoxLangType.DATETIME, name = "setYear", objectArgument = "date" )
public class DateSet extends BIF {

	private static final String	BIF_PREFIX		= "dateset";
	private static final String	MEMBER_PREFIX	= "set";

	private static final String	UNIT_YEAR		= "year";
	private static final String	UNIT_MONTH		= "month";
	private static final String	UNIT_DAY		= "day";
	private static final String	UNIT_HOUR		= "hour";
	private static final String	UNIT_MINUTE		= "minute";
	private static final String	UNIT_SECOND		= "second";

	/**
	 * Constructor
	 */
	public DateSet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( true, "integer", Key.value )
		};
	}

	/**
	 * Sets a single date/time unit on a date object, mutating it in place and returning it.
	 * Out-of-range values roll over (e.g. {@code setDay( 50 )} advances into following months) while
	 * zero/negative values clamp to the first valid unit (e.g. {@code setDay( 0 )} becomes day 1).
	 *
	 * @function.dateSetDay Sets the day of the month on a date object
	 *
	 * @function.dateSetHour Sets the hour on a date object
	 *
	 * @function.dateSetMinute Sets the minute on a date object
	 *
	 * @function.dateSetMonth Sets the month on a date object
	 *
	 * @function.dateSetSecond Sets the second on a date object
	 *
	 * @function.dateSetYear Sets the year on a date object
	 *
	 * @argument.date The date object to modify
	 *
	 * @argument.value The value to set for the given date/time unit
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime	dateRef			= DateTimeCaster.cast( arguments.get( Key.date ), true, context );
		int			value			= arguments.getAsInteger( Key.value );

		// Resolve the target unit from the invoked BIF alias (e.g. dateSetDay) or member name (e.g. setDay)
		String		functionName	= arguments.getAsKey( BIF.__functionName ).getName().toLowerCase();
		String		unit;
		if ( functionName.startsWith( BIF_PREFIX ) ) {
			unit = functionName.substring( BIF_PREFIX.length() );
		} else if ( functionName.startsWith( MEMBER_PREFIX ) ) {
			unit = functionName.substring( MEMBER_PREFIX.length() );
		} else {
			unit = functionName;
		}

		ZonedDateTime adjusted = dateRef.getWrapped();

		switch ( unit ) {
			case UNIT_YEAR :
				adjusted = adjusted.plusYears( Math.max( 1, value ) - adjusted.getYear() );
				break;
			case UNIT_MONTH :
				adjusted = adjusted.plusMonths( Math.max( 1, value ) - adjusted.getMonthValue() );
				break;
			case UNIT_DAY :
				adjusted = adjusted.plusDays( Math.max( 1, value ) - adjusted.getDayOfMonth() );
				break;
			case UNIT_HOUR :
				adjusted = adjusted.plusHours( Math.max( 0, value ) - adjusted.getHour() );
				break;
			case UNIT_MINUTE :
				adjusted = adjusted.plusMinutes( Math.max( 0, value ) - adjusted.getMinute() );
				break;
			case UNIT_SECOND :
				adjusted = adjusted.plusSeconds( Math.max( 0, value ) - adjusted.getSecond() );
				break;
			default :
				throw new BoxRuntimeException( String.format( "The date setter [%s] is not recognized.", functionName ) );
		}

		return dateRef.setWrapped( adjusted );
	}

}
