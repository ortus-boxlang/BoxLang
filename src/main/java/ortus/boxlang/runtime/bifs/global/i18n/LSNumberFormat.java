
package ortus.boxlang.runtime.bifs.global.i18n;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.format.NumberFormat;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )

public class LSNumberFormat extends NumberFormat {

	/**
	 * Constructor
	 */
	public LSNumberFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.mask ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Localized Number formatting
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to be formatted
	 *
	 * @argument.mask The formatting mask to apply
	 *
	 * @argument.locale The locale string to apply to the format
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return super.invoke( context, arguments );
	}

}
