package ortus.boxlang.runtime.bifs.namespaces.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Abs extends BIF {

	/**
	 * Constructor
	 */
	public Abs() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "numeric", Key.value )
		};
	}

	/**
	 * Returns the absolute value of a number
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.value The number to return the absolute value of
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Math.abs( arguments.getAsDouble( Key.value ) );
	}

}
