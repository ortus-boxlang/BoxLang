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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Extract a specific part from a date/time value" )

public class DatePart extends TimeUnits {

	private static final IStruct parts = Struct.of(
	    Key.of( "d" ), BIFMethods.day,
	    Key.of( "yyyy" ), Key.of( "Year" ),
	    Key.of( "q" ), BIFMethods.quarter,
	    Key.of( "m" ), BIFMethods.month,
	    Key.of( "y" ), Key.of( "DayOfYear" ),
	    Key.of( "w" ), BIFMethods.dayOfWeek,
	    Key.of( "ww" ), BIFMethods.week,
	    Key.of( "h" ), Key.of( "Hour" ),
	    Key.of( "n" ), Key.of( "Minute" ),
	    Key.of( "s" ), Key.of( "Second" ),
	    Key.of( "l" ), BIFMethods.millis
	);

	/**
	 * Constructor
	 */
	public DatePart() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.datepart ),
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Extracts a part from a datetime value as a numeric.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.datepart The part of the date to extract.
	 *
	 * @argument.date The date to extract the part from.
	 *
	 * @argument.timezone An optional, explicit timezone to apply to the date.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key datePart = Key.of( arguments.getAsString( Key.datepart ) );
		if ( !parts.containsKey( datePart ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The key [%s] is not supported for the DatePart method",
			        datePart
			    )
			);
		}
		arguments.put( BIF.__functionName, parts.get( datePart ) );
		return super._invoke( context, arguments );
	}

}
