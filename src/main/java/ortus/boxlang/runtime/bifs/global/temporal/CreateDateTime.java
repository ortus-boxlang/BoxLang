
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF

public class CreateDateTime extends BIF {

	/**
	 * Constructor
	 */
	public CreateDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.year ),
		    new Argument( true, "integer", Key.month ),
		    new Argument( true, "integer", Key.day ),
		    new Argument( true, "integer", Key.hour ),
		    new Argument( true, "integer", Key.minute ),
		    new Argument( true, "integer", Key.second ),
		    new Argument( true, "integer", Key.millisecond ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new DateTime(
		    ( int ) arguments.get( Key.year ),
		    ( int ) arguments.get( Key.month ),
		    ( int ) arguments.get( Key.day ),
		    ( int ) arguments.get( Key.hour ),
		    ( int ) arguments.get( Key.minute ),
		    ( int ) arguments.get( Key.second ),
		    ( int ) arguments.get( Key.millisecond ),
		    ( String ) arguments.get( Key.timezone )
		);
	}

}
