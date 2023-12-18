package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class RandRange extends BIF {

	/**
	 * Constructor
	 */
	public RandRange() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "numeric", Key.number1 ),
		    new Argument( true, "numeric", Key.number2 ),
		    new Argument( Key.algorithm )
		};
	}

	/**
	 * 
	 * Return a random int between number1 and number 2
	 * 
	 * @param context
	 * @param arguments Argument scope defining the minimum and maximum (not inclusive) values for the range.
	 * 
	 * @return
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.get( Key.algorithm ) != null ) {
			throw new BoxRuntimeException( "The algorithm argument has not yet been implemented" );
		}

		double	number1	= arguments.getAsDouble( Key.number1 );
		double	number2	= arguments.getAsDouble( Key.number2 );

		return ( int ) ( number1 + Rand._invoke() * ( number2 - number1 ) );
	}

}
