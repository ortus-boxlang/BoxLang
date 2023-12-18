package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ArrayClear extends BIF {

	private final static Key array = Key.of( "array" );

	/**
	 * Constructor
	 */
	public ArrayClear() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", array )
		};
	}

	/**
	 * Clear all items from array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object incoming = arguments.dereference( array, false );

		if ( incoming.getClass().isArray() ) {
			throw new BoxRuntimeException( "ArrayClear does not support Java arrays" );
		}
		Array actualArray = ArrayCaster.cast( incoming );
		actualArray.clear();
		// CF Compat, but dumb
		return true;
	}

}
