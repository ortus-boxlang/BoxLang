package ortus.boxlang.runtime.bifs.namespaces.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;

public class Abs extends BIF {

	public static Object invoke( IBoxContext context, Object value ) {
		return Math.abs( DoubleCaster.cast( value ) );
	}

}
