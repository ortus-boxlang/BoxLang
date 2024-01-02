
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
@BoxMember( type = BoxLangType.DATETIME, name = "add", objectArgument = "date" )

public class DateAdd extends BIF {

	/**
	 * Constructor
	 */
	public DateAdd() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.datepart ),
		    new Argument( true, "long", Key.number ),
		    new Argument( true, "any", Key.date )
		};
	}

	/**
	 * Modifies a date object by date part and integer time unit
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime ref = null;
		try {
			ref = ( DateTime ) arguments.get( Key.date );
		} catch ( java.lang.ClassCastException e ) {
			ref = new DateTime( ( String ) arguments.get( Key.date ) );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		return ref.modify(
		    ( String ) arguments.get( Key.datepart ),
		    ( Long ) arguments.get( Key.number )
		);
	}

}
