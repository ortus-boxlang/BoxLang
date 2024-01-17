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
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class LSParseDateTime extends BIF {

	/**
	 * Constructor
	 */
	public LSParseDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone ),
		    new Argument( false, "string", Key.format )
		};
	}

	/**
	 * Parses a locale-specific datetime string or object
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date the date, datetime string or an object
	 *
	 * @argument.the ISO locale string ( e.g. en-US, en_US, es-SA, es_ES, ru-RU, etc )
	 *
	 * @argument.format the format mask to use in parsing
	 *
	 * @argument.timezone the timezone to apply to the parsed datetime
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	dateRef		= arguments.get( Key.date );
		String	locale		= arguments.getAsString( Key.locale );
		String	timezone	= arguments.getAsString( Key.timezone );
		String	format		= arguments.getAsString( Key.format );
		Locale	localeObj	= null;
		if ( locale != null ) {
			var		localeParts	= locale.split( "-|_| " );
			String	ISOLang		= localeParts[ 0 ];
			String	ISOCountry	= null;
			if ( localeParts.length > 1 ) {
				ISOCountry = localeParts[ 1 ];
			}
			localeObj = ISOCountry == null ? new Locale( ISOLang ) : new Locale( ISOLang, ISOCountry );
		} else {
			localeObj = Locale.getDefault();
		}

		ZoneId zoneId = null;
		try {
			zoneId = timezone != null ? ZoneId.of( timezone ) : ZoneId.systemDefault();
		} catch ( ZoneRulesException e ) {
			// determine whether this is a format argument
			if ( IntegerCaster.attempt( timezone.substring( 0, 1 ) ) != null ) {
				format		= timezone;
				zoneId		= ZoneId.systemDefault();
				timezone	= null;
			} else {
				throw new BoxRuntimeException(
				    "The value [%s] is not a valid locale.",
				    e
				);
			}
		}
		DateTime dateObj = null;
		if ( dateRef instanceof DateTime ) {
			dateObj = DateTimeCaster.cast( dateRef );
			dateObj.setFormat( DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale( localeObj ) );
			if ( format != null ) {
				dateObj.setFormat( format );
			}
			if ( timezone != null ) {
				dateObj.setTimezone( timezone );
			}
		}
		if ( format != null ) {
			// If we have specified format then use that to parse
			dateObj = new DateTime( StringCaster.cast( dateRef ), format );
			dateObj.setFormat( DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale( localeObj ) );
			return timezone != null ? dateObj.setTimezone( timezone ) : dateObj;
		} else {
			// Otherwise attempt to auto-parse
			dateObj = new DateTime( StringCaster.cast( dateRef ), localeObj, zoneId );
		}

		return dateObj;
	}

}
