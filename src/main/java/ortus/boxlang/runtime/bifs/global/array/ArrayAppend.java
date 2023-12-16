package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

public class ArrayAppend extends BIF {

	private final static Key	arr		= Key.of( "arr" );
	private final static Key	value	= Key.of( "value" );

	/**
	 * Constructor
	 */
	public ArrayAppend() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "any", arr ),
		    new Argument( true, "any", value )
		};
	}

	/**
	 * Append a value to an array
	 *
	 * @param context
	 * @param arguments Argument scope defining the array and value to append.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = ArrayCaster.cast( arguments.dereference( arr, false ) );
		actualArray.add( arguments.dereference( value, false ) );
		return actualArray;
	}

}
