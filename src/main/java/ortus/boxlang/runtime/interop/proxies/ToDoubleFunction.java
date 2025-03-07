package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/ToDoubleFunction.html
 */
public class ToDoubleFunction<T> extends BaseProxy implements java.util.function.ToDoubleFunction<T> {

	public ToDoubleFunction( Object target, IBoxContext context, String method ) {
		super( target, context, method );
	}

	@Override
	public double applyAsDouble( T value ) {
		try {
			return DoubleCaster.cast( invoke( value ) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking ToDoubleFunction", e );
			throw new BoxRuntimeException( "Error invoking ToDoubleFunction", e );
		}
	}

}
