package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayClear extends BIF {

	/**
	 * Constructor
	 */
	public ArrayClear() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array )
		};
	}

	/**
	 * Clear all items from array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		actualArray.clear();
		// CF Compat, but dumb
		return true;
	}

}
