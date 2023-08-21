package ortus.boxlang.runtime.functions.global;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.functions.BIF;

public class Println extends BIF {

	public static void invoke( IBoxContext context, Object message ) throws RuntimeException {
		System.out.println( StringCaster.cast( message ) );
	}
}
