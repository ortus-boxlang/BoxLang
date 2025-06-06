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
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
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
	 * Tests whether a value is numeric
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to test
	 *
	 * @argument.locale Optional locale string, otherwise the context locale default is used when parsing string values
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