
package ortus.boxlang.runtime.bifs.global.format;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class NumberFormat extends BIF {

	/**
	 * Constructor
	 */
	public NumberFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.mask )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Replace this example function body with your own implementation;
		return null;
	}

}
