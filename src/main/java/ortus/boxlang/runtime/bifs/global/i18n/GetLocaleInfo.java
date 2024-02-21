/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.dspLocale )
		};
	}

	/**
	 * Retrieves a struct containin info on a locale, with an optional display locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.locale Optional locale to retrieve information on - either a common format ( "German" ), or an ISO Directive
	 *
	 * @argument.dspLocale Optional display language locale
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	dspLocale		= arguments.getAsString( Key.dspLocale );

		Locale	locale			= new Locale.Builder()
		    .setLocale( LocalizationUtil.parseLocaleFromContext( context, arguments ) )
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
