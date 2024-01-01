
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF
@BoxMember( type = BoxLangType.DATETIME )

public class CreateDateTime extends BIF {

	/**
	 * Constructor
	 */
	public CreateDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.of( "year" ) ),
		    new Argument( true, "integer", Key.of( "month" ) ),
		    new Argument( true, "integer", Key.of( "day" ) ),
		    new Argument( true, "integer", Key.of( "hour" ) ),
		    new Argument( true, "integer", Key.of( "minute" ) ),
		    new Argument( true, "integer", Key.of( "second" ) ),
		    new Argument( true, "integer", Key.of( "millisecond" ) ),
		    new Argument( false, "string", Key.of( "timezone" ) )
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
		    ( int ) arguments.get( Key.of( "year" ) ),
		    ( int ) arguments.get( Key.of( "month" ) ),
		    ( int ) arguments.get( Key.of( "day" ) ),
		    ( int ) arguments.get( Key.of( "hour" ) ),
		    ( int ) arguments.get( Key.of( "minute" ) ),
		    ( int ) arguments.get( Key.of( "second" ) ),
		    ( int ) arguments.get( Key.of( "millisecond" ) ),
		    ( String ) arguments.get( Key.of( "timezone" ) )
		);
	}

}
