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
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class IsDefined extends BIF {

	/**
	 * Constructor
	 */
	public IsDefined() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.variable )
		};
	}

	/**
	 * Determine whether a given variable reference exists.
	 * <p>
	 * For example:
	 * <ul>
	 * <li><code>isDefined( "luis" )</code> will test for the existence of an <code>lmajano</code> variable in any accessible scope.</li>
	 * <li><code>isDefined( "variables.foo" )</code> will test for the existence of a <code>foo</code> variable in the <code>variables</code> scope.</li>
	 * <li><code>isDefined( "brad.age" )</code> will test for the existence of an <code>age</code> key in the <code>brad</code> struct, in any accessible
	 * scope</li>
	 * </ul>
	 * </p>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.variable The variable reference to test for existence. For security reasons, only dot-notation is supported. Struct/array bracket
	 *                    notation
	 *                    is not supported, nor is function invocation, etc.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ExpressionInterpreter.getVariable( context, arguments.getAsString( Key.variable ), true ) != null;
	}

}