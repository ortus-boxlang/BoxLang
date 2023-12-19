package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Array;

public class ArrayNew extends BIF {

	/**
	 * Constructor
	 */
	public ArrayNew() {
		super();
	}

	/**
	 * Return new array
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new Array();
	}

}
