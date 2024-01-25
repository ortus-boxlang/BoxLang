
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class Sleep extends BIF {

	/**
	 * Constructor
	 */
	public Sleep() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "long", Key.duration ),
		};
	}

	/**
	 * Sleeps the current thread for the specified duration in millisecons
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.duration The amount of time, in milliseconds to sleep the thread
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Long duration = arguments.getAsLong( Key.duration );
		try {
			Thread.sleep( duration );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An unexpected error occurred while attempting to sleep the thread",
			    e
			);
		}
		return null;
	}

}
