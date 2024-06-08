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

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxBIF( alias = "LSIsCurrency" )

public class IsCurrency extends BIF {

	/**
	 * Constructor
	 */
	public IsCurrency() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Determines whether a value can be parsed to numeric in the given or default locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The value to be parsed
	 *
	 * @argument.locale The locale to apply to parsing - uses the context config value if not specified
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	value	= arguments.getAsString( Key.number );

		Locale	locale	= LocalizationUtil.parseLocaleFromContext( context, arguments );

		return NumberUtils.isCreatable( value )
		    ? true
		    : LocalizationUtil.parseLocalizedCurrency( arguments.get( Key.number ), locale ) != null;
	}

}
