package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayDeleteAt extends BIF {

	/**
	 * Constructor
	 */
	public ArrayDeleteAt() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array ),
		    new Argument( true, "any", Key.index )
		};
	}

	/**
	 * Delete item at specified index in array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		index		= IntegerCaster.cast( arguments.get( Key.index ) );
		actualArray.remove( index - 1 );
		return true;

	}

}
