
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

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.list.ListSort;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Sort the characters in a string" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "StringSort" )

public class StringSort extends ListSort {

	/**
	 * Constructor
	 */
	public StringSort() {
		super();
	}

	/**
	 * Sorts the characters of a string and returns the result as a new string. Characters are compared as text, ascending by default.
	 * This BIF shares its arguments with the corresponding list function: the <code>list</code> argument is the string, and the delimiter is
	 * always overridden with an empty string, so every character is one element. Pass the string positionally, as <code>list</code> or as
	 * <code>string</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The string whose characters are sorted.
	 *
	 * @argument.sortType Ignored. It is always set to "text".
	 *
	 * @argument.sortOrder The sort direction: "asc" or "desc". Defaults to "asc".
	 *
	 * @argument.delimiter Ignored. It is always set to an empty string so that every character of the string is an element.
	 *
	 * @argument.includeEmptyFields Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.localeSensitive Whether the text sort uses locale-aware comparison. Defaults to false.
	 *
	 * @argument.callback An optional comparator function used instead of the built-in text sort. It receives two characters and returns a
	 *                    negative number, zero or a positive number.
	 *
	 * @return The string with its characters sorted.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.containsKey( Key.string ) ) {
			arguments.put( Key.list, arguments.get( Key.string ) );
		}
		arguments.put( Key.delimiter, "" );
		arguments.put( Key.sortType, "text" );
		return super._invoke( context, arguments );
	}

}
