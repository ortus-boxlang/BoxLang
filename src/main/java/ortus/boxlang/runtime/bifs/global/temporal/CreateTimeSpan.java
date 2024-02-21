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

import java.time.Duration;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class CreateTimeSpan extends BIF {

	/**
	 * Constructor
	 */
	public CreateTimeSpan() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "long", Key.days ),
		    new Argument( true, "long", Key.hours ),
		    new Argument( true, "long", Key.minutes ),
		    new Argument( true, "long", Key.seconds ),
		    new Argument( false, "long", Key.milliseconds, 0l )
		};
	}

	/**
	 * Creates a timespan {@link java.time.Duration}
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.days The number of days in the timespan
	 *
	 * @argument.hours The number of hours in the timespan
	 *
	 * @argument.minutes The number of minutes in the timespan
	 *
	 * @argument.seconds The number of seconds in the timespan
	 *
	 * @argument.milliseconds The number of milliseconds in the timespan
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Duration.ofDays( arguments.getAsLong( Key.days ) )
		    .plusHours( arguments.getAsLong( Key.hours ) )
		    .plusMinutes( arguments.getAsLong( Key.minutes ) )
		    .plusSeconds( arguments.getAsLong( Key.seconds ) )
		    .plusMillis( arguments.getAsLong( Key.milliseconds ) );
	}

}
