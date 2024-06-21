package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/lang/Runnable.html
 */
public class Runnable extends BaseProxy implements java.lang.Runnable {

	public Runnable( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( Runnable.class );
	}

	@Override
	public void run() {
		try {
			invoke();
		} catch ( Exception e ) {
			getLogger().error( "Error invoking Runnable", e );
			throw new BoxRuntimeException( "Error invoking Runnable", e );
		}
	}

}
