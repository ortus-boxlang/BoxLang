package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayPrepend extends BIF {

	/**
	 * Constructor
	 */
	public ArrayPrepend() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Append a value to an array
	 *
	 * @param context
	 * @param arguments Argument scope defining the array and value to append.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		actualArray.add( 0, arguments.dereference( Key.value, false ) );
		return actualArray;
	}

}
