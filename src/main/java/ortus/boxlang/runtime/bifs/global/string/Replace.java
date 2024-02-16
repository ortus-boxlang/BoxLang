/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Replace" )
public class Replace extends BIF {

	/**
	 * Constructor
	 */
	public Replace() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.substring1 ),
		    new Argument( true, "string", Key.obj ),
		    new Argument( true, "string", Key.scope, "once" )
		};
	}

	/**
	 * 
	 * Replaces occurrences of substring1 in a string with obj, in a specified scope. The search is case-sensitive. Function returns original string with
	 * replacements made
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.string The string to search
	 * 
	 * @argument.substring1 The substring to search for
	 * 
	 * @argument.obj The string to replace substring1 with
	 * 
	 * @argument.scope The scope to search in
	 * 
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	string		= arguments.getAsString( Key.string );
		String	substring1	= arguments.getAsString( Key.substring1 );
		String	obj			= arguments.getAsString( Key.obj );
		String	scope		= arguments.getAsString( Key.scope ).toLowerCase();
		if ( scope.equals( "once" ) ) {
			int idx = string.indexOf( substring1 );
			if ( idx != -1 ) {
				return string.substring( 0, idx ) + obj + string.substring( idx + substring1.length() );
			} else {
				return string;
			}
		} else if ( scope.equals( "all" ) ) {
			return string.replace( substring1, obj );
		} else {
			throw new BoxRuntimeException( "Invalid replacement scope: [" + scope + "]. Valid options are 'once' or 'all'." );
		}
	}

}
