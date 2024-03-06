
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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "ListCompact" )
/**
 * @Deprecated
 */
@BoxBIF( alias = "ListTrim" )
@BoxMember( type = BoxLangType.STRING, name = "ListTrim" )

public class ListCompact extends BIF {

	/**
	 * Constructor
	 */
	public ListCompact() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false )
		};
	}

	/**
	 * Compacts a list by removing empty items from the start and end of the list
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to compact
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is multi-character
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array ref = ListUtil.asList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    true,
		    arguments.getAsBoolean( Key.multiCharacterDelimiter )
		);
		trimLeadingEmpties( ref );
		trimLeadingEmpties( ref.reverse() );
		return ListUtil.asString( ref.reverse(), arguments.getAsString( Key.delimiter ) );
	}

	/**
	 * Trims the leading empty items from the passed array
	 *
	 * @param ref The array to trim leading items
	 */
	private void trimLeadingEmpties( Array ref ) {
		Boolean trimmed = false;
		while ( !trimmed ) {
			if ( ref.size() >= 1 && StringCaster.cast( ref.get( 0 ) ).length() == 0 ) {
				ref.remove( 0 );
			} else {
				trimmed = true;
			}
		}
		;
	}

}
