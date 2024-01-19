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
package ortus.boxlang.runtime.bifs.global.binary;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class BitAnd extends BIF {

	/**
	 * Constructor
	 */
	public BitAnd() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "integer", Key.number1 ),
		    new Argument( true, "integer", Key.number2 )
		};
	}

	/**
	 * Performs a bitwise logical AND operation.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number1 Numeric value for bitwise AND.
	 * 
	 * @argument.number2 Numeric value for bitwise AND.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		int	number1	= arguments.getAsInteger( Key.number1 );
		int	number2	= arguments.getAsInteger( Key.number2 );

		return number1 & number2;
	}
}
