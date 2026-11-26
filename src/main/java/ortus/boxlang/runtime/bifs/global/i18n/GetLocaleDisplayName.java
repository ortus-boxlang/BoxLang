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

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Get the display name for a locale" )

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
	 * Returns the display name of a locale, such as `Deutsch (Deutschland)`, optionally displayed in the language of another locale.
	 *
	 * @argument.locale Optional. The locale to describe: either a common name (e.g. `German (Standard)`) or an ISO locale such as `de_DE`.
	 *                  Defaults to the current locale.
	 *
	 * @argument.dspLocale Optional. The locale to display the name in, e.g. `fr_FR` returns `allemand (Allemagne)` for `de_DE`. Defaults to the
	 *                     locale being described.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The display name of the locale as a string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
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
