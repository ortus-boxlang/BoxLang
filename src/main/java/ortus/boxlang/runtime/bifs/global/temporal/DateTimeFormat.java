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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Format a date/time value as a string" )
@BoxBIF( alias = "DateFormat" )
@BoxBIF( alias = "TimeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "format" )
@BoxMember( type = BoxLangType.DATETIME, name = "dateFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "timeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "dateTimeFormat" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "dateFormat" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "timeFormat" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "dateTimeFormat" )
public class DateTimeFormat extends BIF {

	private static final Key	FORMAT_EPOCH	= Key.of( "epoch" );
	private static final Key	FORMAT_EPOCHMS	= Key.of( "epochms" );

	/**
	 * Constructor
	 */
	public DateTimeFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.date ),
		    new Argument( false, Argument.STRING, Key.mask ),
		    new Argument( false, Argument.STRING, Key.timezone ),
		    new Argument( false, Argument.STRING, Key.locale )
		};
	}

	/**
	 * Formats a datetime, date or time as a string. This BIF is registered as {@code dateTimeFormat()}, {@code dateFormat()} and {@code timeFormat()};
	 * the name used determines the default mask when none is passed: {@code dd-MMM-yyyy HH:mm:ss} for dateTimeFormat, {@code dd-MMM-yy} for
	 * dateFormat and {@code hh:mm a} for timeFormat. It is also available as the member functions {@code format()}, {@code dateFormat()},
	 * {@code timeFormat()} and {@code dateTimeFormat()} on dates, and (except {@code format()}) on strings.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date object or date string to format
	 *
	 * @argument.mask Optional format mask, or common mask name. If an explicit mask is used, it should use the mask characters specified in the
	 *                [java.time.format.DateTimeFormatter](https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/time/format/DateTimeFormatter.html) class.
	 *                The following common masks are supported (the date, time or datetime variant is chosen from the function name used):
	 *                - short, medium, long, full: localized styles using the java.time FormatStyle of the same name (output depends on the locale)
	 *                - ISO: equivalent to "yyyy-MM-dd'T'HH:mm:ssXXX" (datetime)
	 *                - ISO8601: ISO_OFFSET_DATE_TIME for datetimes, ISO_DATE for dates and ISO_TIME for times
	 *                - ODBC: the ODBC literal formats, e.g. {ts '2024-03-15 10:30:00'}
	 *                - javascript: the format of a JavaScript Date toString (datetime only)
	 *                - epoch: Total seconds of a given date (Example:1567517664)
	 *                - epochms: Total milliseconds of a given date (Example:1567517664000)
	 *
	 * @argument.timezone Optional specific timezone to apply to the date ( if not present in the date string )
	 *
	 * @argument.locale Optional ISO locale string which will be used to localize the resulting date/time string
	 *
	 * @return The formatted string, or a number when the mask is epoch or epochms.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		timezone		= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		DateTime	ref				= DateTimeCaster.cast( arguments.get( Key.date ), true, timezone, context );
		Key			bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		String		format			= arguments.getAsString( Key.mask );

		// Alternate named argument - ACFvsLucee
		if ( format == null ) {
			format = arguments.getAsString( Key.format );
		}

		// LS Subclass locales
		Locale locale = LocalizationUtil.parseLocaleFromContext( context, arguments );
		// Update to use a copy with the current zone only if a timezone was passed in, or if the ref has not been manually fixed to a timezone
		if ( arguments.getAsString( Key.timezone ) != null || !ref.fixedTimezone ) {
			ref = new DateTime( ref.getWrapped().withZoneSameInstant( timezone ) );
		}

		if ( format == null && bifMethodKey.equals( Key.dateFormat ) ) {
			return locale == null ? ref.format( DateTime.DEFAULT_DATE_FORMATTER ) : ref.format( locale, DateTime.DEFAULT_DATE_FORMATTER );
		} else if ( format == null && bifMethodKey.equals( Key.timeFormat ) ) {
			return locale == null ? ref.format( DateTime.DEFAULT_TIME_FORMATTER ) : ref.format( locale, DateTime.DEFAULT_TIME_FORMATTER );
		} else if ( format == null ) {
			return locale == null ? ref.format( DateTime.DEFAULT_DATETIME_FORMATTER ) : ref.format( locale, DateTime.DEFAULT_DATETIME_FORMATTER );
		} else {
			Key		formatKey		= Key.of( format );
			String	mode			= bifMethodKey.equals( Key.dateFormat )
			    ? DateTime.MODE_DATE
			    : bifMethodKey.equals( Key.timeFormat )
			        ? DateTime.MODE_TIME
			        : DateTime.MODE_DATETIME;
			// Create this key instance here so it doesn't get created twice on lookup and retrieval
			Key		commonFormatKey	= Key.of( format.trim() + mode );
			if ( formatKey.equals( FORMAT_EPOCH ) ) {
				return ref.toEpoch();
			} else if ( formatKey.equals( FORMAT_EPOCHMS ) ) {
				return ref.toEpochMillis();
			} else if ( DateTime.COMMON_FORMATTERS.containsKey( commonFormatKey ) ) {
				DateTimeFormatter formatter = ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( commonFormatKey );
				return locale == null
				    ? ref.format( formatter )
				    : ref.format( formatter.withLocale( locale ) );
			} else {
				return locale == null
				    ? ref.format( format )
				    : ref.format( locale, format );
			}
		}

	}

}
