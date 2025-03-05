package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/Supplier.html
 */
public class Supplier<T> extends BaseProxy implements java.util.function.Supplier<T> {

	public Supplier( Object target, IBoxContext context, String method ) {
		super( target, context, method );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public T get() {
		try {
			return ( T ) invoke();
		} catch ( BoxRuntimeException e ) {
			throw e;
		} catch ( Exception e ) {
			getLogger().error( "An error occurred while invoking the Proxy Supplier: " + e.getMessage(), e );
			throw new BoxRuntimeException( "An error occurred while invoking the Proxy Supplier: " + e.getMessage(), e );
		}
	}

}
