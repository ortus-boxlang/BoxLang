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

@BoxBIF( description = "Extract a token from a delimited string" )
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
	 * Returns the token at the given one-based index of a delimited string, as a string. Empty tokens are skipped (consecutive
	 * delimiters count as one). If the index is greater than the number of tokens, an empty string is returned. An index of zero
	 * throws an exception.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The delimited string to extract the token from.
	 *
	 * @argument.index The one-based position of the token to return.
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *                     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @return The token at the given position, or an empty string if the index is past the last token.
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
