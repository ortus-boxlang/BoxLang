/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF

public class GetTimezoneInfo extends BIF {

	/**
	 * Constructor
	 */
	public GetTimezoneInfo() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.timezone ),
		    new Argument( false, "string", Key.locale )
		};
	}

	/**
	 * Retrieves a struct of information about the timezone
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.timezone optional, a specific timezone to retrieve information on
	 *
	 * @argument.locale optional, a specific locale for language output of the timezone name fields
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId		zone	= LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		TimeZone	tz		= TimeZone.getTimeZone( zone );
		Locale		locale	= LocalizationUtil.parseLocale( arguments.getAsString( Key.locale ) );

		if ( locale == null ) {
			locale = Locale.getDefault();
		}

		int	hourOffset		= ( int ) ( tz.getRawOffset() / 3600000 );
		// The expectation for our minute offset is that it is the remainder of the hour offset
		int	minuteOffset	= ( tz.getRawOffset() / 60000 ) - ( hourOffset * 60 );

		System.out.println( "Offset: " + tz.getOffset( tz.getRawOffset() ) );

		return ImmutableStruct.of(
		    "DSTOffset", tz.getDSTSavings(),
		    "id", tz.getID(),
		    "isDSTon", tz.inDaylightTime( new Date() ),
		    "name", tz.getDisplayName( false, TimeZone.LONG, locale ),
		    "nameDST", tz.getDisplayName( true, TimeZone.LONG, locale ),
		    "offset", tz.getRawOffset() / 1000,
		    "shortName", tz.getDisplayName( false, TimeZone.SHORT, locale ),
		    "shortNameDST", tz.getDisplayName( true, TimeZone.SHORT, locale ),
		    "timezone", tz.getID(),
		    "utcHourOffset", hourOffset,
		    "utcMinuteOffset", minuteOffset,
		    "utcTotalOffset", Math.abs( tz.getRawOffset() / 1000 )
		);
	}

}
