package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayLen extends BIF {

	private final static Key	obj			= Key.of( "obj" );

	public static Argument[]	arguments	= new Argument[] {
	    new Argument( true, "any", obj )
	};

	public static int invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = ArrayCaster.cast( arguments.dereference( obj, false ) );
		return actualArray.size();
	}

}
