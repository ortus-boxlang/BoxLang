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
import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Parse a date/time string into a datetime object" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "parseDateTime" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "toDateTime" )
public class ParseDateTime extends BIF {

	/**
	 * Constructor
	 */
	public ParseDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.format ),
		    new Argument( false, "string", Key.timezone ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Parses a datetime string or object into a DateTime. Also available as the string member functions {@code parseDateTime()} and
	 * {@code toDateTime()}. If a format mask is given, the string must match it exactly; otherwise a number of common formats are tried.
	 * A value which cannot be parsed throws an exception.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date the date, datetime string or an object
	 *
	 * @argument.format optional format mask to use in parsing, using java.time.format.DateTimeFormatter pattern characters
	 *
	 * @argument.timezone optional timezone to apply to the parsed datetime. Defaults to the request/runtime timezone.
	 *
	 * @argument.locale optional ISO locale string ( e.g. en-US, en_US, es-SA, es_ES, ru-RU, etc ) used to parse localized formats
	 *
	 * @return A {@link DateTime} object.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	dateRef		= arguments.get( Key.date );
		String	format		= arguments.getAsString( Key.format );
		ZoneId	timezone	= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		Locale	locale		= LocalizationUtil.parseLocaleFromContext( context, arguments );
		if ( dateRef instanceof DateTime ) {
			DateTime dateObj = DateTimeCaster.cast( dateRef, context );
			if ( format != null ) {
				dateObj.setFormat( format );
			} else if ( locale != null ) {
				dateObj.setFormat( DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale( locale ) );
			}
			return dateObj;
		}
		if ( format != null ) {
			// Should this use Locale?
			return new DateTime( StringCaster.cast( dateRef ), format, timezone );
		} else {
			return DateTimeCaster.cast( StringCaster.cast( dateRef ), true, timezone, false, context, locale );
		}
	}

}
