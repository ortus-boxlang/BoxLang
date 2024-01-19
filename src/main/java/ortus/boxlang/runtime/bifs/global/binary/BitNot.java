package ortus.boxlang.runtime.bifs.global.binary;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class BitNot extends BIF {

	/**
	 * Constructor
	 */
	public BitNot() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number )
		};
	}

	/**
	 * Performs a bitwise logical NOT operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number Numeric value for bitwise NOT.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		int number = arguments.getAsInteger( Key.number );

		return ~number;
	}
}
