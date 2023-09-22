package ortus.boxlang.runtime.functions.namespaces.math;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.functions.BIF;

public class Abs extends BIF {

	public static Object invoke( IBoxContext context, Object value ) {
		return Math.abs( DoubleCaster.cast( value ) );
	}

}
