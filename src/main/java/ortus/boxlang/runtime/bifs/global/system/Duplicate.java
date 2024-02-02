
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.DuplicationUtil;

@BoxBIF

public class Duplicate extends BIF {

	/**
	 * Constructor
	 */
	public Duplicate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( false, "boolean", Key.deep, true )
		};
	}

	/**
	 * Duplicates an object - either shallow or deep
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.object Any object to duplicate
	 *
	 * @argument.deep Whether to deep copy the object or make a shallow copy (e.g. only the top level keys in a struct)
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return DuplicationUtil.duplicate( arguments.get( Key.object ), arguments.getAsBoolean( Key.deep ) );
	}

}
