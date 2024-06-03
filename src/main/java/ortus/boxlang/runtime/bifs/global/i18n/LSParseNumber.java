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
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Converts a string that is a valid numeric representation in the current locale into a formatted number.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to parse.
	 *
	 * @argument.locale The locale to use when parsing the number. If not provided, the system or application-configured locale is used.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	value	= arguments.getAsString( Key.number );
		Locale	locale	= LocalizationUtil.parseLocaleFromContext( context, arguments );

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
