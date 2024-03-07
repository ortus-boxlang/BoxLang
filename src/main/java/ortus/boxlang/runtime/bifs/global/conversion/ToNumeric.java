package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class ToNumeric extends BIF {

	public ToNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "any", Key.radix )
		};
	}

	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	inputValue	= arguments.get( Key.value );
		Object	radixValue	= arguments.get( Key.radix );

		if ( radixValue == null ) {
			CastAttempt<Double> doubleCastAttempt = DoubleCaster.attempt( inputValue );
			return doubleCastAttempt.wasSuccessful() ? doubleCastAttempt.get() : 0;
		} else {
			String	inputValueAsString	= StringCaster.cast( inputValue );
			String	radixValueAsString	= StringCaster.cast( radixValue );
			int		numericRadix		= convertStringToRadix( radixValueAsString );
			return Integer.parseInt( inputValueAsString, numericRadix );
		}
	}

	private int convertStringToRadix( String radixString ) {
		switch ( radixString ) {
			case "bin" :
				return 2;
			case "oct" :
				return 8;
			case "dec" :
				return 10;
			case "hex" :
				return 16;
			default :
				return 10; // Default to decimal if unspecified or unrecognized
		}
	}
}