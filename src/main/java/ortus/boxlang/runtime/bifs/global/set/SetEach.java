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
package ortus.boxlang.runtime.bifs.global.set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.SetUtil;

@BoxBIF( description = "Invoke a callback for each element of a Set." )
@BoxMember( type = BoxLangType.SET )
public class SetEach extends BIF {

	public SetEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Consumer", Key.callback )
		};
	}

	/**
	 * @argument.set The set to iterate.
	 *
	 * @argument.callback Invoked for each element. Receives {@code (value, ordinal, set)} —
	 *                    single-arg functions only receive the value.
	 *
	 * @return null
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		SetUtil.each( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
		return null;
	}

}
