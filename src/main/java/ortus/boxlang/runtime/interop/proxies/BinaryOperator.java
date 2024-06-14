package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BinaryOperator extends BaseProxy {

	public BinaryOperator( Function target, IBoxContext context ) {
		super( target, context );
	}

	public Object apply( Object t, Object u ) {
		try {
			// Call the target function
			return null;
		} catch ( Exception e ) {
			logger.error( "Error invoking apply method on target function", e );
			throw new BoxRuntimeException( "Error invoking apply method on target function", e );
		}
	}

}
