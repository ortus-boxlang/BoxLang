
package ortus.boxlang.runtime.bifs.global.temporal;

import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class DatePart extends TimeUnits {

	private static final Struct parts = new Struct(
	    new HashMap<String, Object>() {

		    {
			    put( "d", bifMethods.day );
			    put( "yyyy", Key.of( "Year" ) );
			    put( "q", bifMethods.quarter );
			    put( "m", bifMethods.month );
			    put( "y", Key.of( "DayOfYear" ) );
			    put( "w", bifMethods.dayOfWeek );
			    put( "ww", bifMethods.weekOfYear );
			    put( "h", Key.of( "Hour" ) );
			    put( "n", Key.of( "Minute" ) );
			    put( "s", Key.of( "Second" ) );
			    put( "l", bifMethods.millis );
		    }
	    }
	);

	/**
	 * Constructor
	 */
	public DatePart() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.datepart ),
		    new Argument( true, "any", Key.date ),
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
		Key datePart = Key.of( arguments.getAsString( Key.datepart ) );
		if ( !parts.containsKey( datePart ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The key [%s] is not supported for the DatePart method",
			        datePart
			    )
			);
		}
		arguments.put( BIF.__functionName, parts.get( datePart ) );
		return super.invoke( context, arguments );
	}

}
