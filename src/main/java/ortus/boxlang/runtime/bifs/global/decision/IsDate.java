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

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxBIF( alias = "IsNumericDate" )
public class IsDate extends BIF {

	private static final Key numericDateFunction = Key.of( "isNumericDate" );

	/**
	 * Constructor
	 */
	public IsDate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Determine whether a given value is a date object or a date string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date Value to test for date-ness
	 *
	 * @argument.locale Optional ISO locale string to use for parsing the date/time string.
	 *
	 * @argument.timezone Optional timezone to use for parsing the date/time string.
	 *
	 * @function.IsNumericDate Tests whether the given value is a numeric representation of a date
	 *
	 * @function.IsNumericDate.arguments.exclude locale,timezone
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Object	dateRef			= arguments.get( Key.date );
		String	timezone		= arguments.getAsString( Key.timezone );
		String	localeString	= arguments.getAsString( Key.locale );

		if ( bifMethodKey.equals( numericDateFunction ) ) {
			return DoubleCaster.attempt( dateRef ).wasSuccessful();
		} else if ( dateRef instanceof DateTime ) {
			return true;
		}
		// localized handling
		if ( localeString != null || timezone != null ) {
			ZoneId zoneId = null;
			try {
				zoneId = timezone != null ? ZoneId.of( timezone ) : LocalizationUtil.parseZoneId( timezone, context );
			} catch ( ZoneRulesException e ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The value [%s] is not a valid timezone.",
				        timezone
				    ),
				    e
				);
			}
			Locale locale = LocalizationUtil.getParsedLocale( localeString );
			try {
				new DateTime( StringCaster.cast( dateRef ), locale, zoneId );
				return true;
			} catch ( Exception e ) {
				return false;
			}
			// Caster handling
		} else {
			return DateTimeCaster.attempt( dateRef, context ).wasSuccessful();
		}

	}

}