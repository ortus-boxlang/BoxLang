/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.format;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
public class DollarFormat extends NumberFormat {

	/**
	 * Constructor
	 */
	public DollarFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number )
		};
	}

	/**
	 * Formats a number as a U.S. Dollar string with two decimal places, thousands separator, and a dollar sign.
	 * If the number is negative, the return value is enclosed in parentheses.
	 * If the number is an empty string, the function returns "0.00".
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to format as a U.S. Dollar string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	number	= arguments.get( Key.number );
		Double	value	= null;

		if ( number == null || ( number instanceof String && StringCaster.cast( number ).isEmpty() ) ) {
			value = 0D;
		} else {
			value = DoubleCaster.cast( number, true );
		}
		arguments.put( Key.number, value );
		arguments.put( Key.mask, "dollarFormat" );
		arguments.put( Key.locale, "US" );
		return super._invoke( context, arguments );
	}

}
