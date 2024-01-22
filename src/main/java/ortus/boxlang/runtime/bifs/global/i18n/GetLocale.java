
package ortus.boxlang.runtime.bifs.global.i18n;

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class GetLocale extends BIF {

	/**
	 * Constructor
	 */
	public GetLocale() {
		super();
	}

	/**
	 * Retrieves the the string representation of the current locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Locale			locale	= ( Locale ) context.getConfigItem( Key.locale, Locale.getDefault() );
		ImmutableStruct	aliases	= LocalizationUtil.localeAliases;
		Object			alias	= aliases.keySet().stream().filter( key -> locale.equals( aliases.get( key ) ) ).findFirst()
		    .orElse( null );
		if ( alias != null ) {
			return KeyCaster.cast( alias ).getName();
		} else {
			return String.format(
			    "%s (%s)",
			    locale.getDisplayLanguage( ( Locale ) LocalizationUtil.commonLocales.get( "US" ) ),
			    BooleanCaster.cast( locale.getVariant().length() ) ? locale.getVariant()
			        : locale.getDisplayCountry( ( Locale ) LocalizationUtil.commonLocales.get( "US" ) )
			);
		}
	}

}
