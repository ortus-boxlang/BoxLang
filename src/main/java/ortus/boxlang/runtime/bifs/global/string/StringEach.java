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
import ortus.boxlang.runtime.bifs.global.list.ListEach;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

@BoxBIF( description = "Execute a callback function for each character in a string" )

public class StringEach extends ListEach {

	/**
	 * Constructor
	 */
	public StringEach() {
		super();
	}

	/**
	 * Iterates all the characters of a string and runs the passed callback on each character.
	 * This BIF shares its arguments with the corresponding list function: the <code>list</code> argument is the string, and the delimiter is
	 * always overridden with an empty string, so every character is one element. Pass the string positionally or as <code>list</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The string to iterate.
	 *
	 * @argument.callback The function to invoke for each character. It receives the character, its 1-based index and the original string. A
	 *                    Java Consumer receives only the character.
	 *
	 * @argument.delimiter Ignored. It is always set to an empty string so that every character of the string is an element.
	 *
	 * @argument.includeEmptyFields Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter Not used for strings, since there are no delimiters. Defaults to false.
	 *
	 * @argument.parallel Whether to run the callback in parallel. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when running in parallel. If omitted, the default ForkJoinPool parallelism is
	 *                      used. Ignored if parallel is false. If a boolean is passed it is assigned to the virtual argument instead.
	 *
	 * @argument.ordered Whether parallel execution should maintain element order. Defaults to false. Ignored if parallel is false.
	 *
	 * @argument.virtual If true, the callback is invoked using virtual threads. Defaults to false. Ignored if parallel is false.
	 *
	 * @return Always null. The callback is executed for its side effects.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put( Key.delimiter, "" );
		return super._invoke( context, arguments );
	}

}
