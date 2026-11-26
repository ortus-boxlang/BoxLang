
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
import ortus.boxlang.runtime.bifs.global.list.ListSome;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Test whether at least one character in string matches the callback function" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "StringSome" )

public class StringSome extends ListSome {

	/**
	 * Constructor
	 */
	public StringSome() {
		super();
	}

	/**
	 * Tests whether at least one character of a string meets the criteria of a callback. It stops at the first character for which the
	 * callback returns true.
	 * This BIF shares its arguments with the corresponding list function: the <code>list</code> argument is the string, and the delimiter is
	 * always overridden with an empty string, so every character is one element. Pass the string positionally, as <code>list</code> or as
	 * <code>string</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The string to test.
	 *
	 * @argument.callback The test function. It receives the character and its 1-based index, and must return a boolean. A Java Predicate
	 *                    receives only the character.
	 *
	 * @argument.delimiter Ignored. It is always set to an empty string so that every character of the string is an element.
	 *
	 * @argument.includeEmptyFields Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. Ignored if parallel is false.
	 *
	 * @argument.virtual If true, the callback is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return True if the callback returned true for at least one character, false otherwise (including an empty string).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.containsKey( Key.string ) ) {
			arguments.put( Key.list, arguments.get( Key.string ) );
		}
		arguments.put( Key.delimiter, "" );
		return super._invoke( context, arguments );
	}

}
