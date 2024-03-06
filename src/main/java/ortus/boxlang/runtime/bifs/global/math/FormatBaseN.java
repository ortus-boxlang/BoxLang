package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
public class FormatBaseN extends BIF {

	/**
	 * Constructor
	 */
	public FormatBaseN() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number ),
		    new Argument( true, "integer", Key.radix )
		};
	}

	/**
	 * Converts a number to a string representation in the specified base.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @return A string representation of the number in the specified base.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		double	number	= arguments.getAsDouble( Key.number );
		int		radix	= arguments.getAsInteger( Key.radix );

		// Validate radix
		if ( radix < Character.MIN_RADIX || radix > Character.MAX_RADIX ) {
			throw new BoxRuntimeException( "Radix out of valid range" );
		}

		return Long.toString( ( int ) number & 0xffffffffL, radix );
	}
}
