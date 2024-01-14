
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF

public class ParseDateTime extends BIF {

	/**
	 * Constructor
	 */
	public ParseDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.format ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Parses a datetime string or object
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date the date, datetime string or an object
	 * 
	 * @argument.format the format mask to use in parsing
	 * 
	 * @argument.timezone the timezone to apply to the parsed datetime
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	dateRef		= arguments.get( Key.date );
		String	format		= arguments.getAsString( Key.format );
		String	timezone	= arguments.getAsString( Key.timezone );
		if ( dateRef instanceof DateTime ) {
			DateTime dateObj = DateTimeCaster.cast( dateRef );
			if ( format != null ) {
				dateObj.setFormat( format );
			}
			if ( timezone != null ) {
				dateObj.setTimezone( timezone );
			}
			return dateObj;
		}
		if ( format != null ) {
			DateTime dateObj = new DateTime( StringCaster.cast( dateRef ), format );
			return timezone != null ? dateObj.setTimezone( timezone ) : dateObj;
		} else {
			return timezone != null
			    ? new DateTime( StringCaster.cast( dateRef ), ZoneId.of( timezone ) )
			    : new DateTime( StringCaster.cast( dateRef ) );
		}
	}

}
