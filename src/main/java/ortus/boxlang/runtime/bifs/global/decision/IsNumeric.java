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
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.FloatCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class IsNumeric extends BIF {

	/**
	 * Constructor
	 */
	public IsNumeric() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.string ),
		};
	}

	/**
	 * Determine whether a given value can be casted to numeric.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string Value to test for date-ness
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object value = arguments.get( Key.string );
		if ( value == null ) {
			return false;
		}
		return IntegerCaster.attempt( value ).wasSuccessful()
		    || FloatCaster.attempt( value ).wasSuccessful()
		    || DoubleCaster.attempt( value ).wasSuccessful()
		    || LongCaster.attempt( value ).wasSuccessful();
	}

}