
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

@BoxBIF

public class ClearTimezone extends BIF {

	/**
	 * Constructor
	 */
	public ClearTimezone() {
		super();
	}

	/**
	 * Clears the current timezone in the request
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		context.getParentOfType( RequestBoxContext.class ).setTimezone( null );
		context.getConfig().put( Key.timezone, null );
		return null;
	}

}
