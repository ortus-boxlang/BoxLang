package ortus.boxlang.runtime.functions.global.array;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.functions.BIF;
import ortus.boxlang.runtime.types.Array;

public class ArrayLen extends BIF {

	public static Object invoke( IBoxContext context, Object arr ) {
		Array actualArray = ArrayCaster.cast( arr );
		return actualArray.size();
	}
}
