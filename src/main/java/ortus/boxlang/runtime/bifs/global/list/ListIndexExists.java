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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Check if an index exists in a list" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listIndexExists" )
public class ListIndexExists extends BIF {

	/**
	 * Constructor
	 */
	public ListIndexExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "integer", Key.index ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false )
		};
	}

	/**
	 * Checks whether a one-based position exists in a list. Positions less than 1 always return false.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to check.
	 *
	 * @argument.index The one-based position to check for.
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *                     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty elements (for example those created by consecutive delimiters) are kept. Defaults
	 *                              to false.
	 *
	 * @argument.multiCharacterDelimiter When true, the delimiter is treated as one multi-character separator instead of a set of
	 *                                   single-character delimiters. Defaults to false.
	 *
	 * @return True if the list has an element at the position, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		int index = arguments.getAsInteger( Key.index );
		return index > 0 && ListUtil.asList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    arguments.getAsBoolean( Key.multiCharacterDelimiter )
		).size() >= index;
	}
}
