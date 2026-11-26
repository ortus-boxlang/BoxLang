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
package ortus.boxlang.runtime.bifs.global.decision;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF( description = "Check if value is a date object" )
public class IsDateObject extends BIF {

	/**
	 * Constructor
	 */
	public IsDateObject() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		};
	}

	/**
	 * Determines whether a value is a BoxLang DateTime object.
	 *
	 * Note that date strings, such as `2021-01-01`, and timespans returned from `createTimespan()` are NOT date objects. The former are strings
	 * and the latter are numeric values.
	 *
	 * @argument.value The value to test for date object-ness.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return True if the value is a DateTime instance, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.get( Key.value ) instanceof DateTime;
	}

}
