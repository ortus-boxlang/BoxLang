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

@BoxBIF
@BoxBIF( alias = "ListFindNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "listFind" )
@BoxMember( type = BoxLangType.STRING, name = "listFindNoCase" )
@BoxBIF( alias = "ListContains" )
@BoxBIF( alias = "ListContainsNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "listContains" )
@BoxMember( type = BoxLangType.STRING, name = "listContainsNoCase" )
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
	 * Return int position of value in delimited list, case sensitive or case-insenstive variations
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to be searched.
	 *
	 * @argument.value The value to locate in the list or a function to filter the list
	 *
	 * @argument.delimiter The list delimiter(s)
	 *
	 * @argument.includeEmptyFields Whether to include empty fields in the search
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
