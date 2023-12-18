package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Min extends BIF {

	private final static Key	number1	= Key.of( "number1" );
	private final static Key	number2	= Key.of( "number2" );

	/**
	 * Constructor
	 */
	public Min() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", number1 ),
		    new Argument( true, "any", number2 )
		};
	}

	/**
	 * 
	 * Return larger of two numbers
	 * 
	 * @param context
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		double	number1	= DoubleCaster.cast( arguments.dereference( this.number1, false ) );
		double	number2	= DoubleCaster.cast( arguments.dereference( this.number2, false ) );
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
