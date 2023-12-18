package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayDeleteAt extends BIF {

	private final static Key	array	= Key.of( "array" );
	private final static Key	index	= Key.of( "index " );

	/**
	 * Constructor
	 */
	public ArrayDeleteAt() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", array ),
		    new Argument( true, "any", index )
		};
	}

	/**
	 * Delete item at specified index in array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= ArrayCaster.cast( arguments.dereference( array, false ) );
		int		index		= IntegerCaster.cast( arguments.dereference( this.index, false ) );
		actualArray.remove( index - 1 );
		return true;

	}

}
