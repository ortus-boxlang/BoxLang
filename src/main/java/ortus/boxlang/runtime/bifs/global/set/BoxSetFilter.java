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

@BoxBIF( description = "Return a new Set containing only the elements for which the predicate returns true." )
@BoxMember( type = BoxLangType.SET, name = "filter" )
public class BoxSetFilter extends BIF {

	public BoxSetFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Predicate", Key.callback )
		};
	}

	/**
	 * Return a new Set containing only the elements of the source Set for which the predicate returns true. The
	 * result is a new Set of the same variant as the source; the source is not modified. The predicate receives
	 * the element value, its 1-based ordinal position, and the Set itself.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to filter.
	 *
	 * @argument.callback Predicate invoked for each element. Receives {@code (value, ordinal, set)}.
	 *                    Elements for which this returns {@code true} are included in the result.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return SetUtil.filter( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
	}

}
