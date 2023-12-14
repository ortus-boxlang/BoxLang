package ortus.boxlang.runtime.bifs.global;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;

public class Println extends BIF {

	public static Object invoke( IBoxContext context, Object message ) {
		System.out.println( message );
		return true;
	}
}
