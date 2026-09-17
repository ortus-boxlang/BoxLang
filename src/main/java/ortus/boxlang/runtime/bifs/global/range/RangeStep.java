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
package ortus.boxlang.runtime.bifs.global.range;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Range;

@BoxBIF( description = "Iterate a range, advancing by a given step amount, invoking a callback for each value" )
@BoxMember( type = BoxLangType.RANGE )
public class RangeStep extends BIF {

	/**
	 * Constructor
	 */
	public RangeStep() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.range ),
		    new Argument( true, Argument.NUMERIC, Key.amount ),
		    new Argument( true, "function:Consumer", Key.callback )
		};
	}

	/**
	 * Iterate a range, re-stepping it by the given amount, and invoke the callback for each
	 * resulting value - e.g. {@code (1..10).step(2) { println(it) }}.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.range The range to iterate.
	 *
	 * @argument.amount The step amount to advance by.
	 *
	 * @argument.callback The function to invoke for each value in the stepped range.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Range<?>	range		= ( Range<?> ) arguments.get( Key.range );
		Number		amount		= arguments.getAsNumber( Key.amount );
		Function	callback	= arguments.getAsFunction( Key.callback );

		for ( Object value : range.step( amount ) ) {
			context.invokeFunction( callback, new Object[] { value } );
		}

		return range;
	}

}
