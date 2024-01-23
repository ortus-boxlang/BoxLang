package ortus.boxlang.runtime.bifs.global.binary;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class BitShln extends BIF {

	/**
	 * Constructor
	 */
	public BitShln() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Performs a bitwise shift-left, no-rotation operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number Numeric value to shift left.
	 *
	 * @argument.count Number of bits to shift to the left (Integer in the range 0-31, inclusive).
	 *
	 * @return Returns the result of the bitwise shift-left operation.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		int	number	= arguments.getAsInteger( Key.number );
		int	count	= arguments.getAsInteger( Key.count );

		// Ensure count is within the valid range (0-31)
		count = Math.max( 0, Math.min( count, 31 ) );

		return number << count;
	}
}
