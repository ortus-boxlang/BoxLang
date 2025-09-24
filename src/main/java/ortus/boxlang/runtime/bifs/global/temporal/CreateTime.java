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

@BoxBIF( description = "Create a time object" )
public class CreateTime extends BIF {

	/**
	 * Constructor
	 */
	public CreateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "integer", Key.hour, 0 ),
		    new Argument( false, "integer", Key.minute, 0 ),
		    new Argument( false, "integer", Key.second, 0 ),
		    new Argument( false, "integer", Key.millisecond, 0 ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Creates a time-only datetime object using the epoch date ( 1970-1-1 ) as the date reference.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 *
	 * @argument.hour The hour of the date-time object.
	 *
	 * @argument.minute The minute of the date-time object.
	 *
	 * @argument.second The second of the date-time object.
	 * 
	 * @argument.millisecond The millisecond of the date-time object.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ZoneId timezone = LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context );
		if ( arguments.isEmpty() ) {
			return new DateTime( timezone );
		} else {
			// Note: ACF uses the legacy Microsoft epoch for its createTime method. We use the unix epoch.
			return new DateTime(
			    1970,
			    1,
			    1,
			    arguments.getAsInteger( Key.hour ),
			    arguments.getAsInteger( Key.minute ),
			    arguments.getAsInteger( Key.second ),
			    arguments.getAsInteger( Key.millisecond ),
			    timezone
			);
		}

	}

}
