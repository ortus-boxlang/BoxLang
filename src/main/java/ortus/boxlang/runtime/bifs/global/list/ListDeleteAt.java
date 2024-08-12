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
@BoxMember( type = BoxLangType.STRING, name = "listDeleteAt" )

public class ListDeleteAt extends BIF {

	/**
	 * Constructor
	 */
	public ListDeleteAt() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "integer", Key.position ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false ),
		};
	}

	/**
	 * Deletes an element from a list. Returns a copy of the list, without the
	 * specified element.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to delete from.
	 *
	 * @argument.position The one-based index position of the element to delete.
	 *
	 * @argument.delimiter The delimiter used in the list.
	 *
	 * @argument.includeEmptyFields Whether to include empty fields in the list.
	 *
	 * @argument.multiCharacterDelimiter Whether the delimiter is a multi-character
	 *                                   delimiter.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.deleteAt(
		    arguments.getAsString( Key.list ),
		    arguments.getAsInteger( Key.position ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    arguments.getAsBoolean( Key.multiCharacterDelimiter ) );
	}

}
