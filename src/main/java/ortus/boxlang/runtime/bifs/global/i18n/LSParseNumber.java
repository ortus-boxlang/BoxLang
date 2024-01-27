
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

public class LSParseNumber extends BIF {

	/**
	 * Constructor
	 */
	public LSParseNumber() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.number ),
		    new Argument( true, "string", Key.locale )
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
		String	value	= arguments.getAsString( Key.number );
		Locale	locale	= LocalizationUtil.parseLocaleOrDefault(
		    arguments.getAsString( Key.locale ),
		    ( Locale ) context.getConfigItem( Key.locale, Locale.getDefault() )
		);

		Double	parsed	= NumberUtils.isCreatable( value )
		    ? DoubleCaster.cast( value )
		    : LocalizationUtil.parseLocalizedNumber( arguments.get( Key.number ), locale );
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
