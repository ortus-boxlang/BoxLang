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
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class IsStruct extends BIF {

	/**
	 * Constructor
	 */
	public IsStruct() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.variable )
		};
	}

	/**
	 * Determine whether a value is a struct
	 *
	 * @argument.variable The value to test for structi-ness.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope defining the value to test.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		CastAttempt<Struct> attempt = StructCaster.attempt( arguments.get( Key.variable ) );
		return attempt.wasSuccessful();
	}

}
