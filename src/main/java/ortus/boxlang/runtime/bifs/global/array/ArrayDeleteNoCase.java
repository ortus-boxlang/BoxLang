package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayDeleteNoCase extends BIF {

	/**
	 * Constructor
	 */
	public ArrayDeleteNoCase() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Delete first occurance of item in array case insensitive
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	value		= arguments.get( Key.value );
		int		index		= ArrayContainsNoCase._invoke( actualArray, value );
		if ( index > 0 ) {
			actualArray.remove( index - 1 );
			return true;
		} else {
			return false;
		}
	}

}
