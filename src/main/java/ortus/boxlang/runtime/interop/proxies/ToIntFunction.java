package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/ToIntFunction.html
 */
public class ToIntFunction<T> extends BaseProxy implements java.util.function.ToIntFunction<T> {

	public ToIntFunction( Function target, IBoxContext context ) {
		super( target, context );
		prepLogger( ToIntFunction.class );
	}

	@Override
	public int applyAsInt( T value ) {
		try {
			return IntegerCaster.cast( this.context.invokeFunction(
			    this.target,
			    new Object[] { value }
			) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking ToIntFunction", e );
			throw new BoxRuntimeException( "Error invoking ToIntFunction", e );
		}
	}

}
