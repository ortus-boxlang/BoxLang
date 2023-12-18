package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;

public class ArrayContainsNoCase extends BIF {

	/**
	 * Constructor
	 */
	public ArrayContainsNoCase() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Return int position of value in array, case insensitive
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	value		= arguments.get( Key.value );

		if ( value instanceof Function callback ) {
			for ( int i = 0; i < actualArray.size(); i++ ) {
				if ( BooleanCaster.cast( context.invokeFunction( actualArray.get( i ), new Object[] { actualArray.get( i ), i + 1, actualArray } ) ) ) {
					return i + 1;
				}
			}
			return 0;
		}
		return ArrayContainsNoCase._invoke( actualArray, value );
	}

	/**
	 * Return int position of value in array, case insensitive
	 * 
	 * @param context
	 * @param arguments Argument scope defining the array.
	 */
	public static int _invoke( Array array, Object value ) {
		for ( int i = 0; i < array.size(); i++ ) {
			if ( Compare.invoke( array.get( i ), value, false ) == 0 ) {
				return i + 1;
			}
		}
		return 0;
	}

}
