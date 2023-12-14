package ortus.boxlang.runtime.bifs.global;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

public class Println extends BIF {

	private final static Key	message		= Key.of( "message" );

	public static Argument[]	arguments	= new Argument[] {
	    new Argument( true, "any", message )
	};

	public static Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		System.out.println( arguments.dereference( message, false ) );
		return true;
	}
}
