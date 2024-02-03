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

public class LSIsNumeric extends BIF {

	/**
	 * Constructor
	 */
	public LSIsNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.number ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Tests whether a value is numeric, according to a given or the default
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to test
	 *
	 * @argument.locale Optional locale string, otherwise the context default is used
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	value	= arguments.getAsString( Key.number );
		Locale	locale	= LocalizationUtil.parseLocaleFromContext( context, arguments );

		return NumberUtils.isCreatable( value )
		    ? true
		    : LocalizationUtil.parseLocalizedNumber( arguments.get( Key.number ), locale ) != null;
	}

}
