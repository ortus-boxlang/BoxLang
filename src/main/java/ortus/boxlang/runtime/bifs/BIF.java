package ortus.boxlang.runtime.bifs;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public abstract class BIF {

	public static Object invoke( IBoxContext context, Object... arguments ) {
		throw new BoxRuntimeException( "Please implement the function" );
	}

}
