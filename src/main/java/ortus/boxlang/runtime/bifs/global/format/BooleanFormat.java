
package ortus.boxlang.runtime.bifs.global.format;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
@BoxMember( type = BoxLangType.STRING )

public class BooleanFormat extends BIF {

	/**
	 * Constructor
	 */
	public BooleanFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Returns the value formatted as a boolean string
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The value to cast as a boolean and return the string value
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return BooleanCaster.cast( arguments.get( Key.value ), true ).toString();
	}

}
