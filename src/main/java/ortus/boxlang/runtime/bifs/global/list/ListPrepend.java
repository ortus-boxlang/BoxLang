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

@BoxBIF( description = "Prepend a value to the beginning of a list" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listPrepend" )

public class ListPrepend extends BIF {

	/**
	 * Constructor
	 */
	public ListPrepend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "string", Key.value ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false )
		};
	}

	/**
	 * Prepends a value to the beginning of a list and returns the new list as a string. The original list is not modified. A value
	 * containing delimiters is inserted as multiple elements.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The delimited list to operate on.
	 *
	 * @argument.value The value to prepend.
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
	 * @return The new list with the value at the front.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Boolean	isMultiChar		= arguments.getAsBoolean( Key.multiCharacterDelimiter );
		String	delimiter		= arguments.getAsString( Key.delimiter );
		Boolean	includeEmpty	= arguments.getAsBoolean( Key.includeEmptyFields );

		// Tokenize both the incoming list and the value being prepended with the same delimiter
		// and empty-field policy so the two are treated consistently.
		var		list			= ListUtil.asDelimitedList(
		    arguments.getAsString( Key.list ),
		    delimiter,
		    includeEmpty,
		    isMultiChar
		).withDelimiter( delimiter, isMultiChar );

		list.addAll(
		    0,
		    ListUtil.asList( arguments.getAsString( Key.value ), delimiter, includeEmpty, isMultiChar )
		);

		return list.asString();
	}

}
