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
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
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
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the default locale or the currently set locale
		Locale					locale	= context.getConfig().getAs( Locale.class, Key.locale );
		HashMap<Key, Locale>	aliases	= LocalizationUtil.LOCALE_ALIASES;
		Object					alias	= aliases.keySet()
		    .stream().filter( key -> locale.equals( aliases.get( key ) ) )
		    .findFirst()
		    .orElse( null );

		if ( alias != null ) {
			return KeyCaster.cast( alias ).getName();
		} else {
			return LocalizationUtil.getLocaleDisplayName( locale );
		}
	}

}
