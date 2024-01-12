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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class Max extends BIF {

	/**
	 * Constructor
	 */
	public Max() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number1 ),
		    new Argument( true, "numeric", Key.number2 )
		};
	}

	/**
	 * 
	 * Return larger of two numbers
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.number1 The first number
	 * 
	 * @argument.number2 The second number
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		double	number1	= arguments.getAsDouble( Key.number1 );
		double	number2	= arguments.getAsDouble( Key.number2 );
		return Max._invoke( number1, number2 );
	}

	/**
	 * 
	 * Return larger of two numbers
	 * 
	 * @param context
	 */
	public static Object _invoke( double number1, double number2 ) {
		return StrictMath.max( number1, number2 );
	}
}
