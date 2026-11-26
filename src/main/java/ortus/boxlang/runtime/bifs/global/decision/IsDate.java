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
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Check if value is a valid date" )
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
	 * Determines whether a value is a date object or a string which can be parsed as a date/time. Null, blank strings and plain numbers return
	 * false, as numbers are not treated as dates in this decision context.
	 *
	 * This BIF is also registered as `IsNumericDate`. When called by that name it instead returns true if the value can be cast to a number (a
	 * numeric date representation) and the `locale` and `timezone` arguments are not used.
	 *
	 * @argument.date The value to test for date-ness.
	 *
	 * @argument.locale Optional. ISO locale string (for example `en_US`) used to parse date strings. Defaults to the locale of the current
	 *                  context.
	 *
	 * @argument.timezone Optional. Timezone ID (for example `America/New_York`) used when parsing date strings. An invalid timezone throws a
	 *                    BoxRuntimeException. Defaults to the timezone of the current context.
	 *
	 * @function.IsNumericDate Tests whether the given value is a numeric representation of a date
	 *
	 * @function.IsNumericDate.arguments.exclude locale,timezone
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return True if the value is a date or a parseable date string, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Object	dateRef			= arguments.get( Key.date );
		String	timezone		= arguments.getAsString( Key.timezone );

		if ( dateRef == null || dateRef instanceof String stringDate && stringDate.isBlank() ) {
			return false;
		}

		if ( bifMethodKey.equals( numericDateFunction ) ) {
			return DoubleCaster.attempt( dateRef ).wasSuccessful();
		} else if ( dateRef instanceof DateTime ) {
			return true;
		}

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

		// Exclude numbers here - even though the DateTimeCaster would parse them, we don't want to treat them as dates in the decision context
		if ( NumberCaster.attempt( dateRef, false ).wasSuccessful() ) {
			return false;
		}
		Locale locale = LocalizationUtil.parseLocaleFromContext( context, arguments );
		return DateTimeCaster.cast( dateRef, false, zoneId, false, context, locale ) != null;
	}

}