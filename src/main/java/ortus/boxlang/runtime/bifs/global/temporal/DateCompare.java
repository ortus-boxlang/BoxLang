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
@BoxMember( type = BoxLangType.DATETIME, name = "compareTo" )
public class DateCompare extends BIF {

	HashMap<Key, ChronoUnit> datePartMap = new HashMap<Key, ChronoUnit>() {

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
	 * Compares the difference between two dates - returning 0 if equal, -1 if date2 is less than date1 and 1 if the inverse
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date1 The reference date object
	 *
	 * @argument.date2 The date which to compare against date1
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String		datePart	= arguments.getAsString( Key.datepart );
		ZoneId		timezone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	date1		= DateTimeCaster.cast( arguments.get( Key.date1 ), true, timezone, context );
		DateTime	date2		= DateTimeCaster.cast( arguments.get( Key.date2 ), true, timezone, context );

		if ( datePart == null ) {
			boolean	dateOneIsBefore	= date1.getWrapped().isBefore( date2.getWrapped() );
			boolean	datesAreEqual	= date1.getWrapped().isEqual( date2.getWrapped() );
			return datesAreEqual ? 0 : ( dateOneIsBefore ? -1 : 1 );
		} else {
			Key partKey = Key.of( datePart );
			if ( !datePartMap.containsKey( partKey ) ) {
				throw new BoxRuntimeException( "Invalid datepart: " + datePart );
			}
			ChronoUnit unit = datePartMap.get( partKey );

			switch ( unit ) {
				case NANOS, MICROS, MILLIS, SECONDS, MINUTES, DAYS -> {
					// For the smaller units, we can directly compare a truncated version
					return date1.getWrapped().truncatedTo( unit ).compareTo( date2.getWrapped().truncatedTo( unit ) );
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
