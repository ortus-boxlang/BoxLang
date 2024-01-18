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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF

public class CreateDateTime extends BIF {

	/**
	 * Constructor
	 */
	public CreateDateTime() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.year ),
		    new Argument( true, "integer", Key.month ),
		    new Argument( true, "integer", Key.day ),
		    new Argument( true, "integer", Key.hour ),
		    new Argument( true, "integer", Key.minute ),
		    new Argument( true, "integer", Key.second ),
		    new Argument( true, "integer", Key.millisecond ),
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
		String timezone = arguments.getAsString( Key.timezone );
		if ( timezone == null ) {
			timezone = StringCaster.cast( context.getConfigItem( Key.timezone, ZoneId.systemDefault().toString() ) );
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
