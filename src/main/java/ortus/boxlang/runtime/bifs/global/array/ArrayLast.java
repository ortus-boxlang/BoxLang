package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ArrayLast extends BIF {

	/**
	 * Constructor
	 */
	public ArrayLast() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Return first item in array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = arguments.getAsArray( Key.array );
		if ( actualArray.size() > 0 ) {
			return actualArray.get( actualArray.size() - 1 );
		} else {
			throw new BoxRuntimeException( "Cannot return last element of array; array is empty " );
		}
	}

}
