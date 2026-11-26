
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
import ortus.boxlang.runtime.bifs.global.list.ListReduceRight;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Reduce string characters to a single value using a callback function starting from the right" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "StringReduceRight" )

public class StringReduceRight extends ListReduceRight {

	/**
	 * Constructor
	 */
	public StringReduceRight() {
		super();
	}

	/**
	 * Runs a callback over all the characters of a string, from right to left, to reduce them to a single value. The string is split into
	 * characters and the characters are processed in reverse order.
	 * This BIF shares its arguments with the corresponding list function: the <code>list</code> argument is the string, and the delimiter is
	 * always overridden with an empty string, so every character is one element. Pass the string positionally, as <code>list</code> or as
	 * <code>string</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The string to reduce.
	 *
	 * @argument.callback The function to invoke for each character. It receives the accumulator, the character and the 1-based index within
	 *                    the reversed string. A Java BiFunction receives only the accumulator and the character.
	 *
	 * @argument.initialValue The initial value of the accumulator. If omitted, the accumulator starts as null.
	 *
	 * @argument.delimiter Ignored. It is always set to an empty string so that every character of the string is an element.
	 *
	 * @argument.includeEmptyFields Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @return The final value of the accumulator.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.containsKey( Key.string ) ) {
			arguments.put( Key.list, arguments.get( Key.string ) );
		}
		arguments.put( Key.delimiter, "" );
		return super._invoke( context, arguments );
	}

}
