package ortus.boxlang.runtime.bifs.namespaces.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;

public class Test extends BIF {

	public static Object invoke( IBoxContext context, String message ) {
		System.out.println( message );
		return true;
	}

}
