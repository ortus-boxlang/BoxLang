
package ortus.boxlang.runtime.bifs.global.date;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF
@BoxMember( type = BoxLangType.DATETIME )

public class Now extends BIF {

	/**
	 * Constructor
	 */
	public Now() {
		super();
	}

	/**
	 * Returns the current DateTimeObject representing the current zoned instance
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new DateTime();
	}

}
