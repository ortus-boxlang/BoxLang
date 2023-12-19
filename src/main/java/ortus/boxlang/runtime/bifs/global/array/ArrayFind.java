package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFind extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFind() {
		super();
		arguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Return int position of value in array, case sensitive
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.array The array to be searched.
	 * 
	 * @argument.value The value to found.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	value		= arguments.get( Key.value );

		return ArrayContains._invoke( actualArray, value );
	}

}
