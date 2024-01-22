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

@BoxBIF
@BoxBIF( alias = "DateFormat" )
@BoxBIF( alias = "TimeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "format" )
public class DateTimeFormat extends BIF {

	private final static Key	FORMAT_EPOCH	= Key.of( "epoch" );
	private final static Key	FORMAT_EPOCHMS	= Key.of( "epochms" );

	/**
	 * Constructor
	 */
	public DateTimeFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.mask ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Formats a datetime, date or time
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date string or object
	 *
	 * @argument.mask Optional format mask, or common mask
	 *
	 * @argument.timezone Optional specific timezone to apply to the date ( if not present in the date string )
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		timezone		= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		DateTime	ref				= DateTimeCaster.cast( arguments.get( Key.date ), true, timezone );
		Key			bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		String		format			= arguments.getAsString( Key.mask );
		// LS Subclass locales
		Locale		locale			= LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );

		if ( format == null && bifMethodKey.equals( Key.dateFormat ) ) {
			return locale == null ? ref.format( DateTime.DEFAULT_DATE_FORMAT_MASK ) : ref.format( locale, DateTime.DEFAULT_DATE_FORMAT_MASK );
		} else if ( format == null && bifMethodKey.equals( Key.timeFormat ) ) {
			return locale == null ? ref.format( DateTime.DEFAULT_TIME_FORMAT_MASK ) : ref.format( locale, DateTime.DEFAULT_TIME_FORMAT_MASK );
		} else if ( format == null ) {
			return locale == null ? ref.format( DateTime.DEFAULT_DATETIME_FORMAT_MASK ) : ref.format( locale, DateTime.DEFAULT_DATETIME_FORMAT_MASK );
		} else {
			Key		formatKey		= Key.of( format );
			String	mode			= bifMethodKey.equals( Key.dateFormat )
			    ? DateTime.MODE_DATE
			    : bifMethodKey.equals( Key.timeFormat )
			        ? DateTime.MODE_TIME
			        : DateTime.MODE_DATETIME;
			// Create this key instance here so it doesn't get created twice on lookup and retrieval
			Key		commonFormatKey	= Key.of( format + mode );
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
