
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class GetLocaleDisplayName extends GetLocaleInfo {

	/**
	 * Constructor
	 */
	public GetLocaleDisplayName() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.dspLocale )
		};
	}

	/**
	 * Returns the {@link java.util.Locale} display name with an optional display language/locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.locale Optional locale to target - either a common format ( "German" ), or an ISO Directive
	 *
	 * @argument.dspLocale Optional display language locale
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	dspLocale		= arguments.getAsString( Key.dspLocale );

		Locale	locale			= new Locale.Builder()
		    .setLocale( LocalizationUtil.parseLocaleFromContext( context, arguments ) )
		    .build();

		Locale	displayLocale	= new Locale.Builder()
		    .setLocale( LocalizationUtil.parseLocaleOrDefault( dspLocale, locale ) )
		    .build();

		return locale.getDisplayName( displayLocale );
	}

}
