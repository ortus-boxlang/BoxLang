package ortus.boxlang.runtime.bifs.global.math;

import java.util.Random;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class Rand extends BIF {

	static Random rand = new Random();

	/**
	 * Constructor
	 */
	public Rand() {
		super();
		arguments = new Argument[] {
		    new Argument( false, "string", Key.algorithm )
		};
	}

	/**
	 * 
	 * Return a random double between 0 and 1
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.algorithm The algorithm to use to generate the random number.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.get( Key.algorithm ) != null ) {
			return _invoke( arguments.getAsString( Key.algorithm ) );
		}
		return _invoke();
	}

	public static double _invoke() {
		return rand.nextDouble();
	}

	public double _invoke( String algorithm ) {
		throw new BoxRuntimeException( "The algorithm argument has not yet been implemented" );
	}

}
