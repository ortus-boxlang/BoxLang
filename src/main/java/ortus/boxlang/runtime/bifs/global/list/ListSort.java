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
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArraySort;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DelimitedArray;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Sort the items in a list" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listSort" )

public class ListSort extends ArraySort {

	/**
	 * Constructor
	 */
	public ListSort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( false, "any", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false ),
		    new Argument( false, "boolean", Key.localeSensitive, false ),
		    new Argument( false, "any", Key.callback )
		};
	}

	/**
	 * Sorts a delimited list and returns the sorted list as a string. Provide either a sortType (text, textnocase or numeric) with a
	 *     sortOrder, or a comparator callback. Note that this BIF declares no default for sortType, so it should always be supplied
	 *     unless a callback is used.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to sort.
	 *
	 * @argument.sortType The sort type: "text", "textnocase" or "numeric". You can also pass a comparator function here.
	 *
	 * @argument.sortOrder The sort direction: "asc" (the default) or "desc".
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty elements (for example those created by consecutive delimiters) are kept. Defaults
	 *     to false.
	 *
	 * @argument.multiCharacterDelimiter When true, the delimiter is treated as one multi-character separator instead of a set of
	 *     single-character delimiters. Defaults to false.
	 *
	 * @argument.localeSensitive Whether to sort using locale-specific rules. Defaults to false.
	 *
	 * @argument.callback An optional comparator function that receives two elements and returns a negative number, zero or a
	 *     positive number. When supplied it takes precedence over sortType and sortOrder.
	 *
	 * @return The sorted list.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array listArray = ListUtil.asDelimitedList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    arguments.getAsBoolean( Key.multiCharacterDelimiter )
		);

		arguments.put( Key.array, listArray );

		return ( ( DelimitedArray ) super._invoke( context, arguments ) ).asString();
	}

}
