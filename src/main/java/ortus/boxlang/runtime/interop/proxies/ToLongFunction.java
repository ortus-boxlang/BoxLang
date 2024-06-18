package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/ToLongFunction.html
 */
public class ToLongFunction<T> extends BaseProxy implements java.util.function.ToLongFunction<T> {

	public ToLongFunction( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( ToLongFunction.class );
	}

	@Override
	public long applyAsLong( T value ) {
		try {
			return LongCaster.cast( invoke( value ) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking ToLongFunction", e );
			throw new BoxRuntimeException( "Error invoking ToLongFunction", e );
		}
	}

}
