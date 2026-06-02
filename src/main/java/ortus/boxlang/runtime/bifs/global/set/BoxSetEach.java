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
@BoxMember( type = BoxLangType.SET, name = "each" )
public class BoxSetEach extends BIF {

	public BoxSetEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Consumer", Key.callback )
		};
	}

	/**
	 * Invoke a callback for every element of a Set. The callback receives the element value, its 1-based ordinal
	 * position, and the Set itself; single-argument callbacks receive only the value. Iteration follows the natural
	 * order of the underlying variant. Use setMap() if you need a transformed result.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to iterate.
	 *
	 * @argument.callback Invoked for each element. Receives {@code (value, ordinal, set)}; single-argument callbacks
	 *                    receive only the value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		SetUtil.each( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
		return null;
	}

}
