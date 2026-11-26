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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

// TODO: Move to compat module
@BoxBIF( description = "Inline conditional evaluation" )
public class IIF extends BIF {

	/**
	 * Constructor
	 */
	public IIF() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.BOOLEAN, Key.condition ),
		    new Argument( true, Argument.STRING, Key.expression1 ),
		    new Argument( true, Argument.STRING, Key.expression2 )
		};
	}

	/**
	 * Inline conditional evaluation. If the condition is true the second argument is evaluated and returned, otherwise the third argument is evaluated
	 * and returned. The expressions are passed as strings and evaluated as a variable or literal reference, so use de() to pass a literal string.
	 * The ternary operator is preferred in modern code.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.condition The boolean condition to evaluate.
	 *
	 * @argument.expression1 The expression to evaluate and return if the condition is true.
	 *
	 * @argument.expression2 The expression to evaluate and return if the condition is false.
	 *
	 * @return The result of evaluating the chosen expression.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsBoolean( Key.condition ) ? ExpressionInterpreter.getVariable( context, arguments.getAsString( Key.expression1 ), false )
		    : ExpressionInterpreter.getVariable( context, arguments.getAsString( Key.expression2 ), false );
	}
}
