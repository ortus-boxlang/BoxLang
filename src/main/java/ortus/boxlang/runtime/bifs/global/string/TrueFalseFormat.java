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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
public class TrueFalseFormat extends BIF {

	/**
	 * Constructor
	 */
	public TrueFalseFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 *
	 * Return Yes/No based on whether the input is true/false
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The value to check for true/false and return Yes/No
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {

		// Edge case, test if incoming is a string and if it is empty, return false
		if ( arguments.get( Key.value ) instanceof String castedString && ( castedString.length() == 0 ) ) {
			return "false";
		}

		return BooleanCaster.cast( arguments.get( Key.value ) ) ? "true" : "false";
	}

}
