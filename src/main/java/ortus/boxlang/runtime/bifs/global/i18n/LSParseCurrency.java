
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class LSParseCurrency extends BIF {

	/**
	 * Constructor
	 */
	public LSParseCurrency() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.locale )
		};
	}

	/**
	 * Parses a currency value in to a numeric using the specified or context locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string the value to be parsed
	 *
	 * @argument.locale the optional locale to apply in parsing
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	value	= arguments.getAsString( Key.string );
		Locale	locale	= LocalizationUtil.parseLocaleOrDefault(
		    arguments.getAsString( Key.locale ),
		    ( Locale ) context.getConfigItem( Key.locale, Locale.getDefault() )
		);

		Double	parsed	= NumberUtils.isCreatable( value )
		    ? DoubleCaster.cast( value )
		    : LocalizationUtil.parseLocalizedCurrency( arguments.get( Key.string ), locale );
		if ( parsed == null ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The value [%s] could not be parsed using the locale [%s]",
			        value,
			        locale.getDisplayName()
			    )
			);
		}
		return parsed;
	}

}
