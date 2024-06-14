package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class Callable extends BaseProxy {

	public Callable( Function target, IBoxContext context ) {
		super( target, context );
	}

	public Object call() {
		try {
			// Call the target function
			return null;
		} catch ( Exception e ) {
			logger.error( "Error invoking call method on target function", e );
			throw new BoxRuntimeException( "Error invoking call method on target function", e );
		}
	}

}
