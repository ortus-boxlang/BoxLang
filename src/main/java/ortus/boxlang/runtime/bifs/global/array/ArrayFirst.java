package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ArrayFirst extends BIF {

	private final static Key array = Key.of( "array" );

	/**
	 * Constructor
	 */
	public ArrayFirst() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", array )
		};
	}

	/**
	 * Return first item in array
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = ArrayCaster.cast( arguments.dereference( array, false ) );
		if ( actualArray.size() > 0 ) {
			return actualArray.get( 0 );
		} else {
			throw new BoxRuntimeException( "Cannot return first element of array; array is empty " );
		}
	}

}
