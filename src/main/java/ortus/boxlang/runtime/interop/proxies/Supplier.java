package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/Supplier.html
 */
public class Supplier<T> extends BaseProxy implements java.util.function.Supplier<T> {

	public Supplier( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( Supplier.class );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public T get() {
		try {
			return ( T ) invoke();
		} catch ( Exception e ) {
			getLogger().error( "Error invoking Supplier", e );
			throw new BoxRuntimeException( "Error invoking Supplier", e );
		}
	}

}
