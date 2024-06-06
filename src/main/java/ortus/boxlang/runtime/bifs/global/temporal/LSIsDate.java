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

package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class LSIsDate extends BIF {

	/**
	 * Constructor
	 */
	public LSIsDate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Determines whether a string is avalid date/time string with either a specific locale or within the current system/application locale
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date/time string to check.
	 *
	 * @argument.locale The locale to use for parsing the date/time string.
	 *
	 * @argument.timezone Optional timezone to use for parsing the date/time string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	dateRef		= arguments.get( Key.date );
		String	timezone	= arguments.getAsString( Key.timezone );
		String	locale		= arguments.getAsString( Key.locale );
		Locale	localeObj	= LocalizationUtil.getParsedLocale( locale );
		ZoneId	zoneId		= null;

		if ( dateRef instanceof DateTime ) {
			return true;
		}
		try {
			zoneId = timezone != null ? ZoneId.of( timezone ) : ZoneId.systemDefault();
		} catch ( ZoneRulesException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The value [%s] is not a valid timezone.",
			        timezone
			    ),
			    e
			);
		}

		try {
			new DateTime( StringCaster.cast( dateRef ), localeObj, zoneId );
			return true;
		} catch ( Exception e ) {
			return false;
		}

	}

}
