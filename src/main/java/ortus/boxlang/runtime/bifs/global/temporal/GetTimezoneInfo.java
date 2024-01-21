
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class GetTimezoneInfo extends BIF {

	/**
	 * Constructor
	 */
	public GetTimezoneInfo() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.timezone ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Retrieves a struct of information about the timezone
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.timezone optional, a specific timezone to retrieve information on
	 *
	 * @argument.locale optional, a specific locale for language output of the timezone name fields
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		zone	= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		TimeZone	tz		= TimeZone.getTimeZone( zone );
		Locale		locale	= LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );
		if ( locale == null ) {
			locale = Locale.getDefault();
		}
		final Locale assignedLocale = locale;
		return new Struct(
		    new HashMap<String, Object>() {

			    {
				    put( "DSTOffset", tz.getDSTSavings() );
				    put( "id", tz.getID() );
				    put( "isDSTon", tz.inDaylightTime( new Date() ) );
				    put( "name", tz.getDisplayName( false, TimeZone.LONG, assignedLocale ) );
				    put( "nameDST", tz.getDisplayName( true, TimeZone.LONG, assignedLocale ) );
				    put( "offset", tz.getRawOffset() / 6000 );
				    put( "shortName", tz.getDisplayName( false, TimeZone.SHORT, assignedLocale ) );
				    put( "shortNameDST", tz.getDisplayName( false, TimeZone.SHORT, assignedLocale ) );
				    put( "timezone", tz.getID() );
				    put( "utcHourOffset", tz.getRawOffset() / 3600000 );
				    put( "utcMinuteOffset", tz.getRawOffset() / 60000 );
				    put( "utcSecondOffset", Math.abs( tz.getRawOffset() / 6000 ) );
			    }
		    }
		);
	}

}
