package ortus.boxlang.runtime.bifs.namespaces.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Test extends BIF {

	private final static Key message = Key.of( "message" );

	/**
	 * Constructor
	 */
	public Test() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", message )
		};
	}

	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		System.out.println( arguments.dereference( message, false ) );
		return true;
	}

}
