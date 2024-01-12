
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF
public class Now extends BIF {

	/**
	 * Constructor
	 */
	public Now() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.timezone, "" )
		};
	}

	/**
	 * Returns the current DateTimeObject representing the current zoned instance
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.timezone A timezone to use for the DateTime object, defaults to the system default
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String timezone = arguments.getAsString( Key.timezone ).trim();
		return ( timezone.isEmpty() ) ? new DateTime() : new DateTime( ZoneId.of( timezone ) );
	}

}
