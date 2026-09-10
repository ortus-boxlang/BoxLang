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

@BoxBIF( description = "Left-fold a Set with an accumulator function and an initial value." )
@BoxMember( type = BoxLangType.SET, name = "reduce" )
public class BoxSetReduce extends BIF {

	public BoxSetReduce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:BiFunction", Key.callback ),
		    new Argument( false, Argument.ANY, Key.initialValue )
		};
	}

	/**
	 * Left-fold a Set with an accumulator function, reducing it to a single value. The callback receives the
	 * current accumulator, the element value, its 1-based ordinal position, and the Set itself, and returns the
	 * new accumulator. Iteration follows the natural order of the underlying variant.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to reduce.
	 *
	 * @argument.callback Receives {@code (accumulator, value, ordinal, set)} and returns the new accumulator.
	 *
	 * @argument.initialValue The starting accumulator value. If omitted, the first element is used as the initial value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return SetUtil.reduce(
		    arguments.getAsSet( Key.set ),
		    arguments.getAsFunction( Key.callback ),
		    arguments.get( Key.initialValue ),
		    context
		);
	}

}
