
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class LSIsNumeric extends BIF {

	/**
	 * Constructor
	 */
	public LSIsNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.number ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Tests whether a value is numeric, according to a given or the default
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to test
	 * 
	 * @argument.locale Optional locale string, otherwise the context default is used
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	value	= arguments.getAsString( Key.number );
		Locale	locale	= LocalizationUtil.parseLocaleFromContext( context, arguments );

		return NumberUtils.isCreatable( value )
		    ? true
		    : LocalizationUtil.parseLocalizedNumber( arguments.get( Key.number ), locale ) != null;
	}

}
