
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

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

public class SetLocale extends BIF {

	/**
	 * Constructor
	 */
	public SetLocale() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.locale )
		};
	}

	/**
	 * Sets the current request-level locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.locale The locale ISO directive, common name or alias
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Locale locale = LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );
		if ( locale == null || !LocalizationUtil.isValidLocale( locale ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The Locale requested, [%s], is not a valid Locale for this runtime.",
			        locale
			    )
			);
		}
		context.getParentOfType( RequestBoxContext.class ).setLocale( locale );
		return null;
	}

}
