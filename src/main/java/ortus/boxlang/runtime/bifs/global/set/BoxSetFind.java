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

@BoxBIF( description = "Return the first element of a Set that matches the predicate, or null if none match." )
@BoxMember( type = BoxLangType.SET, name = "find" )
public class BoxSetFind extends BIF {

	public BoxSetFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Predicate", Key.callback )
		};
	}

	/**
	 * Return the first element of a Set for which the predicate returns true, or null if no element matches.
	 * Iteration follows the natural order of the underlying variant and stops at the first match. The predicate
	 * receives the element value, its 1-based ordinal position, and the Set itself.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to search.
	 *
	 * @argument.callback Predicate invoked for each element. Receives {@code (value, ordinal, set)}.
	 *                    Iteration stops at the first element for which this returns {@code true}.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return SetUtil.find( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
	}

}
