package ortus.boxlang.runtime.bifs.namespaces.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Abs extends BIF {

	private final static Key value = Key.of( "value" );

	/**
	 * Constructor
	 */
	public Abs() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", value )
		};
	}

	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Math.abs( DoubleCaster.cast( arguments.dereference( value, false ) ) );
	}

}
