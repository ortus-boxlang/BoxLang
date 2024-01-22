
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.Duration;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class CreateTimeSpan extends BIF {

	/**
	 * Constructor
	 */
	public CreateTimeSpan() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "long", Key.days ),
		    new Argument( true, "long", Key.hours ),
		    new Argument( true, "long", Key.minutes ),
		    new Argument( true, "long", Key.seconds ),
		    new Argument( false, "long", Key.milliseconds, 0l )
		};
	}

	/**
	 * Creates a timespan {@link java.time.Duration}
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.days The number of days in the timespan
	 *
	 * @argument.hours The number of hours in the timespan
	 *
	 * @argument.minutes The number of minutes in the timespan
	 *
	 * @argument.seconds The number of seconds in the timespan
	 *
	 * @argument.milliseconds The number of milliseconds in the timespan
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Duration.ofDays( arguments.getAsLong( Key.days ) )
		    .plusHours( arguments.getAsLong( Key.hours ) )
		    .plusMinutes( arguments.getAsLong( Key.minutes ) )
		    .plusSeconds( arguments.getAsLong( Key.seconds ) )
		    .plusMillis( arguments.getAsLong( Key.milliseconds ) );
	}

}
