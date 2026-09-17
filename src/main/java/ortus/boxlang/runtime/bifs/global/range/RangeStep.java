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
		    // Optional: Range already has its own step(Number) method (registered here as a
		    // fallback-free member, this BIF is now the ONLY thing "step" resolves to - see
		    // below), which just returns a new, re-stepped Range with no iteration at all. That
		    // call shape ("(1..10).step(2)" with nothing further) must keep working exactly as
		    // before; the callback is what turns this into "iterate and invoke" instead.
		    new Argument( false, "function:Consumer", Key.callback )
		};
	}

	/**
	 * Re-steps a range by the given amount - same as {@code Range.step(Number)} - and, if a
	 * callback is provided, also iterates the result and invokes the callback for each value:
	 * {@code (1..10).step(2) { println(it) } }. Without a callback, behaves exactly like the
	 * range's own {@code step(Number)} method: {@code (1..10).step(2)} returns the re-stepped
	 * Range itself, without iterating it.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.range The range to iterate.
	 *
	 * @argument.amount The step amount to advance by.
	 *
	 * @argument.callback Optional. The function to invoke for each value in the stepped range.
	 *                    If omitted, the stepped range is returned without iterating it.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Range<?>	range		= ( Range<?> ) arguments.get( Key.range );
		Number		amount		= arguments.getAsNumber( Key.amount );
		Range<?>	stepped		= range.step( amount );

		Function	callback	= arguments.getAsFunction( Key.callback );
		if ( callback == null ) {
			return stepped;
		}

		for ( Object value : stepped ) {
			context.invokeFunction( callback, new Object[] { value } );
		}

		return range;
	}

}
