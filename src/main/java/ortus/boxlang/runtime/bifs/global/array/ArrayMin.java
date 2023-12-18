package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayMin extends BIF {

	private final static Key array = Key.of( "array" );

	/**
	 * Constructor
	 */
	public ArrayMin() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", array )
		};
	}

	/**
	 * Return length of array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= ArrayCaster.cast( arguments.dereference( array, false ) );
		double	min			= 0;
		for ( int i = 0; i < actualArray.size(); i++ ) {
			min = Math.min( min, DoubleCaster.cast( actualArray.get( i ) ) );
		}
		return min;
	}

}
