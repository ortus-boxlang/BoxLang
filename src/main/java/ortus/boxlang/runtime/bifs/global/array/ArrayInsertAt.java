package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayInsertAt extends BIF {

	/**
	 * Constructor
	 */
	public ArrayInsertAt() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array ),
		    new Argument( true, "integer", Key.position ),
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
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		position	= arguments.getAsInteger( Key.position );
		actualArray.add( position - 1, arguments.get( Key.value ) );
		return actualArray;
	}

}
