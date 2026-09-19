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
		    // Optional, and deliberately untyped (Argument.ANY): Range already had TWO existing
		    // overloads this single BIF must now stand in for, since registering it makes it the
		    // ONLY thing "step" resolves to - Range.step(Number) (no third argument) and
		    // Range.step(Number, String) (a calendar/custom unit name, e.g. "month", "chromatic"
		    // on an IRangeable). Typing this argument as "function:Consumer" would force-cast a
		    // unit string into a Function and fail - _invoke() below dispatches on its actual
		    // runtime type instead.
		    new Argument( false, Argument.ANY, Key.callback )
		};
	}

	/**
	 * Re-steps a range by the given amount, standing in for BOTH of {@code Range}'s own existing
	 * step overloads, plus a third, new form:
	 * <ul>
	 * <li>{@code range.step(amount)} - returns the re-stepped Range, unchanged from
	 * {@code Range.step(Number)}.</li>
	 * <li>{@code range.step(amount, unit)} - a calendar/custom unit name, unchanged from
	 * {@code Range.step(Number, String)}.</li>
	 * <li>{@code range.step(amount) { ... }} - new: also iterates the re-stepped range and
	 * invokes the callback for each value.</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.range The range to iterate.
	 *
	 * @argument.amount The step amount to advance by.
	 *
	 * @argument.callback Optional. Either a unit name (String) to re-step by, or a function to
	 *                    invoke for each value in the stepped range. If omitted, the stepped
	 *                    range is returned without iterating it.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Range<?>	range		= ( Range<?> ) arguments.get( Key.range );
		Number		amount		= arguments.getAsNumber( Key.amount );
		Object		callbackArg	= arguments.get( Key.callback );

		if ( callbackArg == null ) {
			return range.step( amount );
		}
		if ( callbackArg instanceof String unit ) {
			return range.step( amount, unit );
		}

		Function	callback	= arguments.getAsFunction( Key.callback );
		Range<?>	stepped		= range.step( amount );
		for ( Object value : stepped ) {
			context.invokeFunction( callback, new Object[] { value } );
		}

		return range;
	}

}
