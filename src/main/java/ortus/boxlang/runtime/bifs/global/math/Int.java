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
import java.math.RoundingMode;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Return the integer portion of a number" )
@BoxMember( type = BoxLangType.NUMERIC )
public class Int extends BIF {

	/**
	 * Constructor
	 */
	public Int() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the closest integer that is less than or equal to the number (it rounds down, so -1.5 becomes -2). Also available as
	 *     <code>number.int()</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to round down to an integer.
	 *
	 * @return The integer value of the number.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number number = arguments.getAsNumber( Key.number );
		if ( number instanceof BigDecimal bd ) {
			return bd.setScale( 0, RoundingMode.FLOOR );
		}
		return ( int ) StrictMath.floor( number.doubleValue() );
	}

}
