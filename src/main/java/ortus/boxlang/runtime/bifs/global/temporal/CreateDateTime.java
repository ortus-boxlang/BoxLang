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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxBIF( alias = "CreateDate" )
public class CreateDateTime extends BIF {

	/**
	 * Constructor
	 */
	public CreateDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "integer", Key.year, 0 ),
		    new Argument( false, "integer", Key.month, 1 ),
		    new Argument( false, "integer", Key.day, 1 ),
		    new Argument( false, "integer", Key.hour, 0 ),
		    new Argument( false, "integer", Key.minute, 0 ),
		    new Argument( false, "integer", Key.second, 0 ),
		    new Argument( false, "integer", Key.millisecond, 0 ),
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
		ZoneId timezone = LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		if ( arguments.isEmpty() ) {
			return new DateTime( timezone );
		} else {
			if ( arguments.getAsInteger( Key.year ).equals( 0 ) ) {
				throw new BoxRuntimeException( "The year argument passed to this method must be greater than zero" );
			}
			if ( arguments.getAsInteger( Key.month ).equals( 0 ) ) {
				throw new BoxRuntimeException( "The month argument passed to this method must be greater than zero" );
			}
			if ( arguments.getAsInteger( Key.day ).equals( 0 ) ) {
				throw new BoxRuntimeException( "The day argument passed to this method must be greater than zero" );
			}
			return new DateTime(
			    arguments.getAsInteger( Key.year ),
			    arguments.getAsInteger( Key.month ),
			    arguments.getAsInteger( Key.day ),
			    arguments.getAsInteger( Key.hour ),
			    arguments.getAsInteger( Key.minute ),
			    arguments.getAsInteger( Key.second ),
			    arguments.getAsInteger( Key.millisecond ),
			    timezone
			);
		}

	}

}
