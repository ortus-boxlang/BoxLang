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
public class Now extends BIF {

	/**
	 * Constructor
	 */
	public Now() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Returns the current DateTimeObject representing the current zoned instance
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.timezone A timezone to use for the DateTime object, defaults to the system default
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String timezone = arguments.getAsString( Key.timezone );
		if ( timezone == null ) {
			timezone = StringCaster.cast( context.getConfig().getOrDefault( Key.timezone, ZoneId.systemDefault().toString() ) );
		}
		return new DateTime( ZoneId.of( timezone ) );
	}

}
