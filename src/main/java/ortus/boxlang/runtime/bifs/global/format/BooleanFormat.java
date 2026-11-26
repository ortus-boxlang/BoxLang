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
package ortus.boxlang.runtime.bifs.global.format;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Format a boolean value using a specified format" )
// Deprecated alias: remove TrueFalseFormat in the 2.0 release.
@BoxBIF( alias = "TrueFalseFormat" )
@BoxMember( type = BoxLangType.NUMERIC )
@BoxMember( type = BoxLangType.STRING_STRICT )
// Deprecated member alias: remove TrueFalseFormat in the 2.0 release.
@BoxMember( type = BoxLangType.STRING_STRICT, name = "TrueFalseFormat" )

public class BooleanFormat extends BIF {

	/**
	 * Constructor
	 */
	public BooleanFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Casts a value to a boolean and returns it as the string `true` or `false`. Numbers are `false` when zero and `true` otherwise, and the
	 * strings `true`, `false`, `yes` and `no` (case-insensitive) are recognised. An empty string returns `false`. Values which cannot be cast to
	 * a boolean throw an exception.
	 *
	 * This BIF is also registered as the deprecated `TrueFalseFormat` BIF and member function (scheduled for removal in 2.0), and as a member
	 * function on numbers and strings, e.g. `(1).booleanFormat()`.
	 *
	 * @argument.value The value to cast as a boolean and return as a string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The string `true` or `false`.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.get( Key.value ) instanceof String castedString && castedString.isEmpty() ) {
			return "false";
		}

		return BooleanCaster.cast( arguments.get( Key.value ), true ).toString();
	}

}
