package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/ToDoubleFunction.html
 */
public class UnaryOperator<T> extends BaseProxy implements java.util.function.UnaryOperator<T> {

	public UnaryOperator( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( UnaryOperator.class );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public T apply( T t ) {
		try {
			return ( T ) invoke( t );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking UnaryOperator", e );
			throw new BoxRuntimeException( "Error invoking UnaryOperator", e );
		}
	}

}
