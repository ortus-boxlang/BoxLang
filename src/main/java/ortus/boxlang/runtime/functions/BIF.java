package ortus.boxlang.runtime.functions;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

public abstract class BIF {

	public static Object invoke( IBoxContext context, Object... arguments ) {
		throw new ApplicationException( "Please implement the function" );
	}

}
