package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/ToIntFunction.html
 */
public class ToIntFunction<T> extends BaseProxy implements java.util.function.ToIntFunction<T> {

	public ToIntFunction( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( ToIntFunction.class );
	}

	@Override
	public int applyAsInt( T value ) {
		try {
			return IntegerCaster.cast( invoke( value ) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking ToIntFunction", e );
			throw new BoxRuntimeException( "Error invoking ToIntFunction", e );
		}
	}

}
