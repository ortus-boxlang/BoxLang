
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.DATETIME, name = "compare" )
@BoxMember( type = BoxLangType.DATETIME, name = "compareTo" )
public class DateCompare extends BIF {

	/**
	 * Constructor
	 */
	public DateCompare() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date1 ),
		    new Argument( true, "any", Key.date2 ),
		    new Argument( false, "string", Key.datepart )
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
		String		datePart	= arguments.getAsString( Key.datepart );
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	date1		= DateTimeCaster.cast( arguments.get( Key.date1 ), true, timezone );
		DateTime	date2		= DateTimeCaster.cast( arguments.get( Key.date2 ), true, timezone );

		if ( datePart == null ) {
			return date1.toEpochMillis().compareTo( date2.toEpochMillis() );
		} else {
			if ( datePart.equals( "m" ) ) {
				datePart = "M";
			}
			return IntegerCaster.cast( date1.format( datePart ) ).compareTo( IntegerCaster.cast( date2.format( datePart ) ) );

		}
	}

}
