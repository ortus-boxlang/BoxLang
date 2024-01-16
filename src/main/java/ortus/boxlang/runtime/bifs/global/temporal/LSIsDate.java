
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class LSIsDate extends BIF {

	/**
	 * Constructor
	 */
	public LSIsDate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone )
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
		Object	dateRef		= arguments.get( Key.date );
		String	timezone	= arguments.getAsString( Key.timezone );
		String	locale		= arguments.getAsString( Key.locale );
		Locale	localeObj	= DateTime.getParsedLocale( locale );
		ZoneId	zoneId		= null;

		if ( dateRef instanceof DateTime ) {
			return true;
		}
		try {
			zoneId = timezone != null ? ZoneId.of( timezone ) : ZoneId.systemDefault();
		} catch ( ZoneRulesException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The value [%s] is not a valid timezone.",
			        timezone
			    ),
			    e
			);
		}

		try {
			new DateTime( StringCaster.cast( dateRef ), localeObj, zoneId );
			return true;
		} catch ( Exception e ) {
			return false;
		}

	}

}
