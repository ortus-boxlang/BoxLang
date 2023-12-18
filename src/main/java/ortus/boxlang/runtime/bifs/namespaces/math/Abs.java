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

	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Math.abs( arguments.getAsDouble( Key.value ) );
	}

}
