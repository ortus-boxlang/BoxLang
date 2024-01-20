
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
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		zone	= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		TimeZone	tz		= TimeZone.getTimeZone( zone );
		Locale		locale	= LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );
		return new Struct(
		    new HashMap<String, Object>() {

			    {
				    put( "DSTOffset", tz.getDSTSavings() );
				    put( "id", tz.getID() );
				    put( "isDSTon", tz.inDaylightTime( new Date() ) );
				    put( "name", tz.getDisplayName( false, TimeZone.LONG, locale ) );
				    put( "nameDST", tz.getDisplayName( true, TimeZone.LONG, locale ) );
				    put( "offset", tz.getRawOffset() / 6000 );
				    put( "shortName", tz.getDisplayName( false, TimeZone.SHORT, locale ) );
				    put( "shortNameDST", tz.getDisplayName( false, TimeZone.SHORT, locale ) );
				    put( "timezone", tz.getID() );
				    put( "utcHourOffset", tz.getRawOffset() / 3600000 );
				    put( "utcMinuteOffset", tz.getRawOffset() / 60000 );
				    put( "utcSecondOffset", Math.abs( tz.getRawOffset() / 6000 ) );
			    }
		    }
		);
	}

}
