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

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.format.NumberFormat;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Format a number as currency" )
@BoxBIF( alias = "LSCurrencyFormat" )
@BoxMember( type = BoxLangType.NUMERIC )

public class CurrencyFormat extends NumberFormat {

	/**
	 * Formats a number as a currency value using the symbol and separators of the given or current locale, for example `$1,234.50` for `en_US`. The
	 * formatting logic is inherited from {@link NumberFormat#_invoke}. Non-numeric values throw an exception, and an empty string is treated as 0.
	 *
	 * This BIF is also registered as `LSCurrencyFormat` and as a member function on numbers, e.g. `(1234.5).currencyFormat()`.
	 *
	 * @argument.number The number to format, or an empty string which will be treated as 0.
	 *
	 * @argument.type Optional. How the currency symbol is displayed: `local` (the locale's symbol, the default), `international` (the ISO currency
	 *                code, for example `USD`) or `none` (no symbol). Any other value throws an exception.
	 *
	 * @argument.locale Optional. The locale (a common name or ISO code) used to format the number. Defaults to the locale of the current context.
	 *
	 * @return The formatted currency string.
	 */
	public CurrencyFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.type, "local" ),
		    new Argument( false, "string", Key.locale )
		};
	}

}
