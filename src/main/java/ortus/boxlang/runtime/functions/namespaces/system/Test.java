package ortus.boxlang.runtime.functions.namespaces.system;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.functions.BIF;

public class Test extends BIF {

	public static Object invoke( IBoxContext context, String message ) {
		System.out.println( message );
		return true;
	}

}
