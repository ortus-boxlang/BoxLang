
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.HashMap;
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class GetLocaleInfo extends BIF {

	/**
	 * Constructor
	 */
	public GetLocaleInfo() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.locale ),
		    new Argument( true, "string", Key.dspLocale )
		};
	}

	/**
	 * Retrieves a struct containin info on a locale, with an optional display locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.locale The locale to retrieve information on - either a common format ( "German" ), or an ISO Directive
	 * 
	 * @argument.dspLocale The display language locale
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	localeArg		= arguments.getAsString( Key.locale );
		String	dspLocale		= arguments.getAsString( Key.dspLocale );

		Locale	locale			= new Locale.Builder()
		    .setLocale( LocalizationUtil.parseLocaleOrDefault( localeArg, Locale.getDefault() ) )
		    .build();

		Locale	displayLocale	= new Locale.Builder()
		    .setLocale( LocalizationUtil.parseLocaleOrDefault( dspLocale, locale ) )
		    .build();

		return new ImmutableStruct(
		    new HashMap<Key, Object>() {

			    {
				    put( Key.country, locale.getCountry() );
				    put( Key.display, new ImmutableStruct(
				        new HashMap<Key, Object>() {

					        {
						        put( Key.country, locale.getDisplayCountry( displayLocale ) );
						        put( Key.language, locale.getDisplayLanguage( displayLocale ) );
					        }
				        }
				    ) );
				    put( Key.iso, new ImmutableStruct(
				        new HashMap<Key, Object>() {

					        {
						        put( Key.country, locale.getISO3Country() );
						        // Note this replicates the Lucee behavior where the ISO object returns the non-iso3 language
						        // and the top level language key returns the iso3 version
						        put( Key.language, locale.getLanguage() );
					        }
				        }
				    ) );
				    put( Key.language, locale.getISO3Language() );
				    put(
				        Key.of( "name" ),
				        String.format(
				            "%s (%s)",
				            locale.getDisplayLanguage( displayLocale ),
				            locale.getDisplayCountry( displayLocale )
				        )
				    );
				    put( Key.variant, locale.getVariant() );
			    }
		    }

		);
	}

}
