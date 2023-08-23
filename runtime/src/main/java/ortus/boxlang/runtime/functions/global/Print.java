package ortus.boxlang.runtime.functions.global;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.functions.BIF;

public class Print extends BIF {

	public static Object invoke( IBoxContext context, String message ) throws RuntimeException {
		System.out.print( message );
		return true;
	}
}
