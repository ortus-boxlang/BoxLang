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
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
@BoxBIF( alias = "IsNumericDate" )
public class IsDate extends BIF {

	private static final Key numericDateFunction = Key.of( "isNumericDate" );

	/**
	 * Constructor
	 */
	public IsDate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		};
	}

	/**
	 * Determine whether a given value is a date object or a date string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value Value to test for date-ness
	 *
	 * @function.IsNumericDate Tests whether the given value is a numeric representation of a date
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key bifMethodKey = arguments.getAsKey( BIF.__functionName );
		return bifMethodKey.equals( numericDateFunction )
		    ? DoubleCaster.attempt( arguments.get( Key.value ) ).wasSuccessful()
		    : DateTimeCaster.attempt( arguments.get( Key.value ) ).wasSuccessful();
	}

}