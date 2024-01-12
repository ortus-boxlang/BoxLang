package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC, name = "Sin" )
public class Sin extends BIF {

	/**
	 * Constructor
	 */
	public Sin() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the sine of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the sine of, entered in radians.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		double value = arguments.getAsDouble( Key.number );
		return StrictMath.sin( value );
	}
}
