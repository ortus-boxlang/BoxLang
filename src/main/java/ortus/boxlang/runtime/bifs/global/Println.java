package ortus.boxlang.runtime.bifs.global;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Println extends BIF {

	private final static Key message = Key.of( "message" );

	/**
	 * Constructor
	 */
	public Println() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", message )
		};
	}

	/**
	 * Print a message with line break to the console
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		System.out.println( arguments.dereference( message, false ) );
		return true;
	}
}
