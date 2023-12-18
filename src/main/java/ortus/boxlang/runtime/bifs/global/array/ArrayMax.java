package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayMax extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMax() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Return length of array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		double	max			= 0;
		for ( int i = 0; i < actualArray.size(); i++ ) {
			max = Math.max( max, DoubleCaster.cast( actualArray.get( i ) ) );
		}
		return max;
	}

}
