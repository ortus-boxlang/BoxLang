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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.util.JSONUtil;

@BoxBIF
public class IsJSON extends BIF {

	/**
	 * Constructor
	 */
	public IsJSON() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.var )
		};
	}

	/**
	 * Evaluates whether a string is in valid JSON (JavaScript Object Notation) data interchange format.
	 *
	 * @argument.var The value to test for JSON
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope defining the value to test.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object				oInput			= arguments.get( Key.var );
		CastAttempt<String>	stringAttempt	= StringCaster.attempt( oInput );
		if ( !stringAttempt.wasSuccessful() ) {
			return false;
		}
		// TODO: Make a JSON caster for this.
		// I don't like catching the exception, but our JSON lib doesn't give us another option
		try {
			JSONUtil.getJSONBuilder().anyFrom( stringAttempt.get() );
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}

}
