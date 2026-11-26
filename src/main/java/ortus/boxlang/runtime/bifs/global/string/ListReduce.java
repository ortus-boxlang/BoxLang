
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

package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Reduce list items to a single value using a callback function" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listReduce" )
public class ListReduce extends BIF {

	/**
	 * Constructor
	 */
	public ListReduce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "function:BiFunction", Key.callback ),
		    new Argument( Key.initialValue ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, true )
		};
	}

	/**
	 * Reduce the items of a delimited list to a single value by running a callback for each item.
	 * The callback is invoked with <code>(accumulator, value, index, list, delimiter)</code>, where index is 1-based, and its result becomes
	 * the next accumulator.
	 * Strict-argument functions (such as Java BiFunctions) only receive <code>(accumulator, value)</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The delimited list to reduce.
	 *
	 * @argument.callback The function to invoke for each item. It receives the accumulator, the item, the 1-based index, the original list and
	 *                    the delimiter, and returns the new accumulator.
	 *
	 * @argument.initialValue The initial value of the accumulator. If omitted, the accumulator starts as null.
	 *
	 * @argument.delimiter The list delimiter(s). Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty list elements are included in the iteration. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter Whether the delimiter is treated as a multi-character delimiter (the whole string) instead of a set of
	 *                                   single-character delimiters. Defaults to true.
	 *
	 * @return The final value of the accumulator.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.reduce(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    arguments.getAsBoolean( Key.multiCharacterDelimiter ),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.get( Key.initialValue )
		);
	}

}
