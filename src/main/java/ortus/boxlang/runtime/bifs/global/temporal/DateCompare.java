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

@BoxBIF
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
			return date1.getWrapped().compareTo( date2.getWrapped() );
		} else {
			Key partKey = Key.of( datePart );
			if ( !datePartMap.containsKey( partKey ) ) {
				throw new BoxRuntimeException( "Invalid datepart: " + datePart );
			}
			// Between is reversed so it will return negative numbers if date1 is before date2
			long diff = datePartMap.get( partKey ).between( date2.getWrapped(), date1.getWrapped() );

			return diff == 0 ? 0 : ( diff > 0 ? 1 : -1 );
		}
	}

}
