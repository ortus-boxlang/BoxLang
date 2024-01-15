package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class ParseNumber extends BIF {

	/**
	 * Constructor
	 */
	public ParseNumber() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.number ),
		    new Argument( false, "string", Key.radix )
		};
	}

	/**
	 * Converts a string to a number in the specified numeral system
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The string to convert to a number.
	 * 
	 * @argument.radix The numeral system to use for conversion (e.g., "bin", "oct", "dec", "hex").
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	number	= arguments.getAsString( Key.number );
		String	radix	= arguments.getAsString( Key.radix );

		switch ( radix ) {
			case "bin" :
				return Integer.parseInt( number, 2 );
			case "oct" :
				return Integer.parseInt( number, 8 );
			case "dec" :
				return Double.parseDouble( number ); // Parses as a double for decimal values
			case "hex" :
				return Integer.parseInt( number, 16 );
			default :
				throw new BoxRuntimeException( "Invalid radix: " + radix );
		}
	}
}
