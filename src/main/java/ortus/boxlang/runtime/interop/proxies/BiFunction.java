package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BiFunction extends BaseProxy {

	public BiFunction( Function target ) {
		super( target );
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
