
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class SetTimezone extends BIF {

	/**
	 * Constructor
	 */
	public SetTimezone() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.timezone )
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
		String	timezone	= arguments.getAsString( Key.timezone );
		ZoneId	assigned	= LocalizationUtil.parseZoneId( timezone );
		if ( assigned == null ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The timezone requested, [%s], is not a valid timezone identifier.",
			        timezone
			    )
			);
		}
		context.getParentOfType( RequestBoxContext.class ).setTimezone( assigned );
		return null;
	}

}
