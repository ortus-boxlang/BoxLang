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

@BoxBIF
public class Attempt extends BIF {

	/**
	 * Constructor
	 */
	public Attempt() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "any", Key.value )
		};
	}

	/**
	 * Create an Attempt object with or without a given value so you can do fluent operations on the
	 * registered attempt value.
	 *
	 * @argument.value The value to store as the attempt. This can be a value, or a closure/lambda that will be executed to get the value.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope defining the value to test.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ortus.boxlang.runtime.dynamic.Attempt.of( arguments.get( Key.value ) );
	}

}
