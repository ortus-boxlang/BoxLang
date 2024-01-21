
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
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
	 * Returns the numeric difference in the requested date part between two dates
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date1 The reference date object
	 *
	 * @argument.date2 The date which to compare against date1
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key			datePart	= Key.of( arguments.getAsString( Key.datepart ) );
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	date1		= DateTimeCaster.cast( arguments.get( Key.date1 ), true, timezone );
		DateTime	date2		= DateTimeCaster.cast( arguments.get( Key.date2 ), true, timezone );

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
