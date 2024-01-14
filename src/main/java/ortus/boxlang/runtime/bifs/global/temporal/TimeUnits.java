
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

	/**
	 * Map of method names to BIF names
	 */
	public final static Struct methodMap = new Struct(
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

	/**
	 * Constructor
	 */
	public TimeUnits() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "timezone", Key.timezone )
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
		if ( methodMap.containsKey( bifMethodKey ) ) {
			methodName = ( String ) methodMap.get( ( Object ) bifMethodKey );
			return dateRef.dereferenceAndInvoke( context, Key.of( methodName ), arguments, false );
		} else {
			switch ( bifMethodKey.getName().toLowerCase() ) {
				case "month" : {
					return dateRef.getWrapped().getMonth().getValue();
				}
				case "monthasstring" : {
					return dateRef.clone().format( "MMMM" );
				}
				case "monthshortasstring" : {
					return dateRef.clone().format( "MMM" );
				}
				case "daysinmonth" : {
					return dateRef.getWrapped().getMonth().length( dateRef.isLeapYear() );
				}
				case "daysinyear" : {
					return Year.of( dateRef.getWrapped().getYear() ).length();
				}
				case "dayofweek" : {
					return dateRef.clone().getWrapped().getDayOfWeek().getValue();
				}
				case "dayofweekasstring" : {
					return dateRef.clone().format( "eeee" );
				}
				case "dayofweekshortasstring" : {
					return dateRef.clone().format( "eee" );
				}
				case "millisecond" : {
					return dateRef.getWrapped().getNano() / 1000000;
				}
				case "offset" : {
					return dateRef.clone().format( "xxxx" );
				}
				case "gettimezone" :
				case "timezone" : {
					return dateRef.clone().format( "v" );
				}
				default : {
					throw new BoxRuntimeException(
					    String.format(
					        "The method [%s] is not present in the [%s] object",
					        arguments.getAsString( Key.of( __functionName ) ),
					        dateRef.getClass().getSimpleName()
					    )
					);
				}

			}

		}
	}

}
