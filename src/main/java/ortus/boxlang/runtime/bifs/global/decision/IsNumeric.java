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
package ortus.boxlang.runtime.bifs.global.decision;

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Check if value is numeric" )
@BoxBIF( alias = "LSIsNumeric" )
public class IsNumeric extends BIF {

	/**
	 * Constructor
	 */
	public IsNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.string ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Determines whether a value is numeric. Numbers, numeric strings (including grouped numbers such as `1,000` and exponent notation such as
	 * `1e3`) and any value castable to a number return true. Booleans, boolean strings (`true`, `false`, `yes`, `no`), empty strings and null
	 * return false. If the value is not castable directly, string values are also parsed using the given (or context) locale, so `1.321,00` is
	 * numeric in a German locale.
	 *
	 * This BIF is also registered as `LSIsNumeric`.
	 *
	 * @argument.string The value to test.
	 *
	 * @argument.locale Optional. Locale string (for example `de_DE`) used to parse string values. Defaults to the locale of the current context.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return True if the value is numeric, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object value = arguments.get( Key.string );
		if ( value == null ) {
			return false;
		}
		// We can't use the number caster on booleans when the booleansAreNumbers setting is set to true
		if ( value instanceof Boolean ) {
			return false;
		}
		// If we have a string which can be cast to boolean strings, it's not numeric
		if ( value instanceof String str && BooleanCaster.parseBooleanString( str ) != null ) {
			return false;
		}
		if ( GenericCaster.attempt( context, value, "numeric" ).wasSuccessful() ) {
			return true;
		} else {
			CastAttempt<String> stringAttempt = StringCaster.attempt( value );
			if ( stringAttempt.wasSuccessful() ) {
				Locale locale;
				if ( arguments.containsKey( Key.locale ) ) {
					locale = LocalizationUtil.getParsedLocale( arguments.getAsString( Key.locale ) );
				} else {
					locale = LocalizationUtil.parseLocaleFromContext( context, arguments );
				}
				return LocalizationUtil.parseLocalizedNumber( stringAttempt.get(), locale ) != null;
			} else {
				return false;
			}
		}

	}

}