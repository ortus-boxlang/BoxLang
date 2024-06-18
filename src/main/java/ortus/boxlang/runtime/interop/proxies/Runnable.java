package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/lang/Runnable.html
 */
public class Runnable extends BaseProxy implements java.lang.Runnable {

	public Runnable( Function target, IBoxContext context ) {
		super( target, context );
		prepLogger( Runnable.class );
	}

	@Override
	public void run() {
		try {
			this.context.invokeFunction(
			    this.target,
			    new Object[] {}
			);
		} catch ( Exception e ) {
			getLogger().error( "Error invoking Runnable", e );
			throw new BoxRuntimeException( "Error invoking Runnable", e );
		}
	}

}
