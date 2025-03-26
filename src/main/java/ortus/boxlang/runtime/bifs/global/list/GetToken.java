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

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "GetToken" )

public class GetToken extends BIF {

	/**
	 * Constructor
	 */
	public GetToken() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.index ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER )
		};
	}

	/**
	 * Determines whether a token of the list in the delimiters parameter is present in a string.
	 * Returns the token found at position index of the string, as a string.
	 * If index is greater than the number of tokens in the string, returns an empty string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string string list to filter entries from
	 *
	 * @argument.index numeric the one-based index position to retrieve the value at
	 *
	 * @argument.delimiter string the list delimiter
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.getAt(
		    arguments.getAsString( Key.string ),
		    arguments.getAsInteger( Key.index ),
		    arguments.getAsString( Key.delimiter ),
		    false,
		    false,
		    ""
		);
	}
}
