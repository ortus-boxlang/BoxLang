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
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;
import ortus.boxlang.runtime.types.util.DateTimeHelper;

@BoxBIF( description = "Calculate the difference between two dates" )
@BoxMember( type = BoxLangType.DATETIME, name = "diff", objectArgument = "date1" )
public class DateDiff extends BIF {

	private static final class parts {

		private static final Key	years			= Key.of( "yyyy" );
		private static final Key	quarters		= Key.of( "q" );
		private static final Key	months			= Key.of( "m" );
		private static final Key	days			= Key.of( "d" );
		private static final Key	daysAlt			= Key.of( "y" );
		private static final Key	weeks			= Key.of( "w" );
		private static final Key	weeksAlt		= Key.of( "ww" );
		private static final Key	weekDays		= Key.of( "wd" );
		private static final Key	hours			= Key.of( "h" );
		private static final Key	minutes			= Key.of( "n" );
		private static final Key	seconds			= Key.of( "s" );
		private static final Key	milliseconds	= Key.of( "l" );
	}

	// Allows us to use a primitive for our return value, which is much faster
	private static final long IMPROBABLE_RESULT = -999999999999l;

	/**
	 * Constructor
	 */
	public DateDiff() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.datepart ),
		    new Argument( true, "any", Key.date1 ),
		    new Argument( true, "any", Key.date2 )
		};
	}

	/**
	 * Returns the number of whole units of the requested date part between two dates. The result is positive when date2 is after date1 and
	 * negative when it is before. Also available as the member function {@code date.diff( datepart, date2 )}.
	 * Note that {@code w} and {@code ww} both count whole weeks, while {@code wd} counts weekdays (Monday to Friday).
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.datepart The datepart code in which to express the difference: yyyy (years), q (quarters), m (months), d or y (days),
	 *                    w or ww (weeks), wd (weekdays), h (hours), n (minutes), s (seconds), l (milliseconds). Any other value throws an exception.
	 *
	 * @argument.date1 The reference date object or date string
	 *
	 * @argument.date2 The date object or date string to compare against date1
	 *
	 * @return The difference as a whole number.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key			datePart	= Key.of( arguments.getAsString( Key.datepart ) );
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );

		DateTime	date1		= DateTimeHelper.castIncludeFractionalDays( arguments.get( Key.date1 ), timezone, context );
		DateTime	date2		= DateTimeHelper.castIncludeFractionalDays( arguments.get( Key.date2 ), timezone, context );

		long		result		= IMPROBABLE_RESULT;
		// @formatter:off
		// prettier-ignore
		result =
			datePart.equals( parts.years ) ? ChronoUnit.YEARS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.quarters ) ? ChronoUnit.MONTHS.between( date1.getWrapped(), date2.getWrapped() ) / 3l
			: datePart.equals( parts.months ) ? ChronoUnit.MONTHS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.days ) || datePart.equals( parts.daysAlt ) ? ChronoUnit.DAYS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.weeks ) || datePart.equals( parts.weeksAlt ) ? ChronoUnit.WEEKS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.weekDays ) ? getWeekDaysBetween( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.hours ) ? ChronoUnit.HOURS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.minutes ) ? ChronoUnit.MINUTES.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.seconds ) ? ChronoUnit.SECONDS.between( date1.getWrapped(), date2.getWrapped() )
			: datePart.equals( parts.milliseconds ) ? ChronoUnit.MILLIS.between( date1.getWrapped(), date2.getWrapped() )
			: result;

			//  A standard null equality check will not work here so we have to let the Long throw the null pointer, if necessary
			if( result == IMPROBABLE_RESULT ){
				throw new BoxRuntimeException(
					String.format(
						"The datepart [%s] is not supported for the method DateDiff or the member function Date.diff",
						datePart.getName()
					)
				);
			}

		return result;
	}

	public static Long getWeekDaysBetween( final ZonedDateTime date1, final ZonedDateTime date2 ) {
		final DayOfWeek	date1DOW			= date1.getDayOfWeek();
		final DayOfWeek	date2DOW			= date2.getDayOfWeek();

		final long		days				= ChronoUnit.DAYS.between( date1, date2 );
		final long		daysWithoutWeekends	= days - 2 * ( ( days + date1DOW.getValue() ) / 7 );

		// adjust for starting and ending on a Sunday:
		return daysWithoutWeekends + ( date1DOW.equals( DayOfWeek.SUNDAY ) ? 1 : 0 ) + ( date2DOW.equals( DayOfWeek.SUNDAY ) ? 1 : 0 );
	}

}
