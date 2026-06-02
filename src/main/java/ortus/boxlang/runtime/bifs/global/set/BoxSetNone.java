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

@BoxBIF( description = "Return true if no element of a Set matches the predicate." )
@BoxMember( type = BoxLangType.SET, name = "none" )
public class BoxSetNone extends BIF {

	public BoxSetNone() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Predicate", Key.callback )
		};
	}

	/**
	 * Test whether no element of a Set satisfies a predicate. Iteration short-circuits on the first element for
	 * which the predicate returns true. Returns true for an empty Set. The predicate receives the element value,
	 * its 1-based ordinal position, and the Set itself.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to test.
	 *
	 * @argument.callback Predicate invoked for each element. Receives {@code (value, ordinal, set)}.
	 *                    Short-circuits on the first {@code true} result.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return !SetUtil.some( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
	}

}
