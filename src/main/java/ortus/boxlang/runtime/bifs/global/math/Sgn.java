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
package ortus.boxlang.runtime.bifs.global.math;

import java.math.BigDecimal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC, name = "sgn" )
public class Sgn extends BIF {

	/**
	 * Constructor
	 */
	public Sgn() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Determine the sign of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to determine the sign of.
	 */
	public Integer _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number number = arguments.getAsNumber( Key.number );
		if ( number instanceof BigDecimal bd ) {
			return bd.signum();
		}
		if ( number.doubleValue() > 0 ) {
			return 1;
		} else if ( number.doubleValue() < 0 ) {
			return -1;
		} else {
			return 0;
		}
	}
}
