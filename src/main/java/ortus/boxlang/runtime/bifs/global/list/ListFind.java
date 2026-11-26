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
import ortus.boxlang.runtime.bifs.global.array.ArrayFind;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Find the position of a value in a list" )
@BoxBIF( alias = "ListFindNoCase" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listFind" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listFindNoCase" )
@BoxBIF( alias = "ListContains" )
@BoxBIF( alias = "ListContainsNoCase" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listContains" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listContainsNoCase" )
public class ListFind extends ArrayFind {

	private static final Key	listContainsKey			= new Key( "listContains" );
	private static final Key	listContainsNoCaseKey	= new Key( "listContainsNoCase" );

	/**
	 * Constructor
	 */
	public ListFind() {
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
	 * Finds the position of a value in a list. The function is registered under four names:
	 * <ul>
	 * <li><code>listFind</code> - case-sensitive, returns the one-based index or 0 if not found</li>
	 * <li><code>listFindNoCase</code> - case-insensitive, returns the one-based index or 0 if not found</li>
	 * <li><code>listContains</code> - case-sensitive substring match; returns a boolean</li>
	 * <li><code>listContainsNoCase</code> - case-insensitive substring match; returns a boolean</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to be searched.
	 *
	 * @argument.value The string value to locate in the list. For listContains and listContainsNoCase it is matched as a substring of each element.
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty elements are counted when computing positions. Defaults to false.
	 *
	 * @argument.multiCharacterDelimiter When true, the delimiter is treated as one multi-character separator instead of a set of
	 *     single-character delimiters. Defaults to false.
	 *
	 * @return The one-based index of the first match (0 if none) for listFind and listFindNoCase; a boolean for listContains and
	 *     listContainsNoCase.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put(
		    Key.array,
		    ListUtil.asList(
		        arguments.getAsString( Key.list ),
		        arguments.getAsString( Key.delimiter ),
		        arguments.getAsBoolean( Key.includeEmptyFields ),
		        arguments.getAsBoolean( Key.multiCharacterDelimiter )
		    )
		);
		Key calledName = arguments.getAsKey( BIF.__functionName );
		arguments.put( Key.substringMatch, calledName.equals( listContainsKey ) || calledName.equals( listContainsNoCaseKey ) ? true : false );
		return super._invoke( context, arguments );
	}

}
