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

import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class DatePart extends TimeUnits {

	private static final Struct parts = new Struct(
	    new HashMap<String, Object>() {

		    {
			    put( "d", BIFMethods.day );
			    put( "yyyy", Key.of( "Year" ) );
			    put( "q", BIFMethods.quarter );
			    put( "m", BIFMethods.month );
			    put( "y", Key.of( "DayOfYear" ) );
			    put( "w", BIFMethods.dayOfWeek );
			    put( "ww", BIFMethods.weekOfYear );
			    put( "h", Key.of( "Hour" ) );
			    put( "n", Key.of( "Minute" ) );
			    put( "s", Key.of( "Second" ) );
			    put( "l", BIFMethods.millis );
		    }
	    }
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
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
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
		return super.invoke( context, arguments );
	}

}
