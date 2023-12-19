package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class Min extends BIF {

	/**
	 * Constructor
	 */
	public Min() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "numeric", Key.number1 ),
		    new Argument( true, "numeric", Key.number2 )
		};
	}

	/**
	 * 
	 * Return larger of two numbers
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.number1 The first number
	 * 
	 * @argument.number2 The second number
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		double	number1	= arguments.getAsDouble( Key.number1 );
		double	number2	= arguments.getAsDouble( Key.number2 );
		return Min._invoke( number1, number2 );
	}

	/**
	 * 
	 * Return larger of two numbers
	 * 
	 * @param context
	 */
	public static Object _invoke( double number1, double number2 ) {
		return Math.min( number1, number2 );
	}
}
