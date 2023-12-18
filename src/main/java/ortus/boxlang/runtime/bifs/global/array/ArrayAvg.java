package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayAvg extends BIF {

	/**
	 * Constructor
	 */
	public ArrayAvg() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "Array", Key.array )
		};
	}

	/**
	 * Return length of array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		return ArraySum._invoke( actualArray ) / actualArray.size();
	}

}
