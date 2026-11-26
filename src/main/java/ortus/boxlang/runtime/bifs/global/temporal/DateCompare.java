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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Compare two date/time values" )
@BoxMember( type = BoxLangType.DATETIME, name = "compare" )
public class DateCompare extends BIF {

	private static final HashMap<Key, ChronoUnit> datePartMap = new HashMap<Key, ChronoUnit>() {

		{
			put( Key.of( "y" ), ChronoUnit.YEARS );
			put( Key.of( "yyyy" ), ChronoUnit.YEARS );
			put( Key.of( "m" ), ChronoUnit.MONTHS );
			put( Key.of( "d" ), ChronoUnit.DAYS );
			put( Key.of( "h" ), ChronoUnit.HOURS );
			put( Key.of( "n" ), ChronoUnit.MINUTES );
			put( Key.of( "s" ), ChronoUnit.SECONDS );
		}
	};

	/**
	 * Constructor
	 */
	public DateCompare() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date1 ),
		    new Argument( true, "any", Key.date2 ),
		    new Argument( false, "string", Key.datepart )
		};
	}

	/**
	 * Compares two dates, optionally only down to a given precision. Returns -1 if date1 is earlier than date2, 0 if they are equal at the
	 * requested precision, and 1 if date1 is later than date2. Also available as the member function {@code date.compare( date2, datepart )}.
	 * Note that for the {@code m} and {@code yyyy} dateparts the comparison is based on the number of whole months/years elapsed between the dates.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date1 The reference date object or date string
	 *
	 * @argument.date2 The date object or date string to compare against date1
	 *
	 * @argument.datepart The precision to compare down to. Accepts y, yyyy, m, d, h, n, s. When omitted the dates are compared to the second.
	 *                    Any other value throws an exception.
	 *
	 * @return -1, 0 or 1.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String		datePart	= arguments.getAsString( Key.datepart );
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	date1		= DateTimeCaster.cast( arguments.get( Key.date1 ), true, timezone, context );
		DateTime	date2		= DateTimeCaster.cast( arguments.get( Key.date2 ), true, timezone, context );

		if ( datePart == null ) {
			return date1.compare( date2, true );
		} else {
			Key partKey = Key.of( datePart );
			if ( !datePartMap.containsKey( partKey ) ) {
				throw new BoxRuntimeException( "Invalid datepart: " + datePart );
			}
			ChronoUnit unit = datePartMap.get( partKey );

			switch ( unit ) {
				case NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, DAYS -> {
					// For the smaller units, we can directly compare a truncated version
					int comparison = date1.getWrapped().truncatedTo( unit ).compareTo( date2.getWrapped().truncatedTo( unit ) );
					return comparison == 0 ? 0 : ( comparison < 0 ? -1 : 1 );
				}
				case MONTHS, YEARS -> {
					// For larger units, we need to convert to LocalDate to avoid errors attempting to truncate
					LocalDate	localDate1	= LocalDate.from( date1.getWrapped() );
					LocalDate	localDate2	= LocalDate.from( date2.getWrapped() );
					long		diffBetween	= unit.between( localDate1, localDate2 );
					return Long.compare( 0, diffBetween );
				}
				default -> {
					throw new BoxRuntimeException( "Unhandled datepart: [" + datePart + "]" );
				}
			}

		}
	}

}
