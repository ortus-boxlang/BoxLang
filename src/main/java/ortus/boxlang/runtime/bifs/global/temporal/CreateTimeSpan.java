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

import java.math.BigDecimal;
import java.time.Duration;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
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
		    new Argument( true, "numeric", Key.days ),
		    new Argument( true, "numeric", Key.hours ),
		    new Argument( true, "numeric", Key.minutes ),
		    new Argument( true, "numeric", Key.seconds ),
		    new Argument( false, "numeric", Key.milliseconds, 0l )
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
		// Check to see if any of the inbound arguments are decimals. If so, demote them
		// for assignment
		if ( arguments.get( Key.days ) instanceof BigDecimal castDecimal ) {
			BigDecimal timeoutMinutes = castDecimal.multiply( BigDecimalCaster.cast( 1440 ) );
			arguments.put( Key.days, 0l );
			arguments.put( Key.minutes, timeoutMinutes.longValue() );
		}
		if ( arguments.get( Key.hours ) instanceof BigDecimal castDecimal ) {
			BigDecimal timeoutMinutes = castDecimal.multiply( BigDecimalCaster.cast( 60 ) );
			arguments.put( Key.hours, 0l );
			arguments.put( Key.minutes, timeoutMinutes.longValue() );
		}
		if ( arguments.get( Key.minutes ) instanceof BigDecimal castDecimal ) {
			BigDecimal timeoutSeconds = castDecimal.multiply( BigDecimalCaster.cast( 60 ) );
			arguments.put( Key.minutes, 0l );
			arguments.put( Key.seconds, timeoutSeconds.longValue() );
		}
		if ( arguments.get( Key.seconds ) instanceof BigDecimal castDecimal ) {
			BigDecimal timeoutMilliseconds = castDecimal.multiply( BigDecimalCaster.cast( 1000 ) );
			arguments.put( Key.seconds, 0l );
			arguments.put( Key.milliseconds, timeoutMilliseconds.longValue() );
		}

		return Duration.ofDays( LongCaster.cast( arguments.get( Key.days ) ) )
		    .plusHours( LongCaster.cast( arguments.get( Key.hours ) ) )
		    .plusMinutes( LongCaster.cast( arguments.get( Key.minutes ) ) )
		    .plusSeconds( LongCaster.cast( arguments.get( Key.seconds ) ) )
		    .plusMillis( LongCaster.cast( arguments.get( Key.milliseconds ) ) );
	}

}
