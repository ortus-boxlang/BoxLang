
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;
import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( alias = "Year" )
@BoxBIF( alias = "Month" )
@BoxBIF( alias = "Day" )
@BoxBIF( alias = "DayOfWeek" )
@BoxBIF( alias = "DayOfWeekAsString" )
@BoxBIF( alias = "DayOfWeekShortAsString" )
@BoxBIF( alias = "DayOfYear" )
@BoxBIF( alias = "Hour" )
@BoxBIF( alias = "Minute" )
@BoxBIF( alias = "Second" )
@BoxBIF( alias = "Millisecond" )
@BoxBIF( alias = "Nanosecond" )
@BoxBIF( alias = "Offset" )
@BoxBIF( alias = "GetTimezone" )
@BoxMember( type = BoxLangType.DATETIME )

public class TimeUnits extends BIF {

	/**
	 * Map of method names to BIF names
	 */
	public final static Struct methodMap = new Struct(
	    new HashMap<String, String>() {

		    {
			    put( "Year", "getYear" );
			    put( "Month", "getMonth" );
			    put( "Day", "getDayOfMonth" );
			    put( "DayOfWeek", "getDayOfWeek" );
			    put( "DayOfYear", "getDayOfYear" );
			    put( "Hour", "getHour" );
			    put( "Minute", "getMinute" );
			    put( "Second", "getSecond" );
			    put( "Nanosecond", "getNano" );
			    put( "Offset", "getOffset" );
			    put( "GetTimeZone", "getZone" );
		    }
	    }
	);

	/**
	 * Constructor
	 */
	public TimeUnits() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "date", Key.date ),
		    new Argument( false, "timezone", Key.timezone )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime dateRef = DateTimeCaster.cast( arguments.get( "Key.date" ) );

		if ( arguments.get( Key.timezone ) != null ) {
			dateRef = dateRef.clone( ZoneId.of( arguments.getAsString( Key.timezone ) ) );
		}

		Key		bifMethodKey	= arguments.getAsKey( __functionName );
		String	methodName		= null;
		if ( methodMap.containsKey( bifMethodKey ) ) {
			methodName = ( String ) methodMap.get( ( Object ) bifMethodKey );
			return DynamicJavaInteropService.dereferenceAndInvoke( dateRef, context, Key.of( methodName ), arguments, false );
		} else {
			switch ( bifMethodKey.getName().toLowerCase() ) {
				case "dayofweekasstring" : {
					return dateRef.clone().format( "dddd" );
				}
				case "dayofweekasshortstring" : {
					return dateRef.clone().format( "ddd" );
				}
				case "millisecond" : {
					return dateRef.getWrapped().getNano() / 1000000;
				}
				default : {
					throw new BoxRuntimeException(
					    String.format(
					        "The method [%s] is not present in the [%s] object",
					        arguments.getAsString( __functionName ),
					        dateRef.getClass().getSimpleName()
					    )
					);
				}

			}

		}
	}

}
