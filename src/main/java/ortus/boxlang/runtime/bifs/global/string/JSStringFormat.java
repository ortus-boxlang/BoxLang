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

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "jsFormat" )
public class JSStringFormat extends BIF {

	/**
	 * Constructor
	 */
	public JSStringFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.string )
		};
	}

	/**
	 *
	 * Escapes special JavaScript characters, such as single quotation mark, double quotation mark, and newline
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to escape.
	 *
	 * @return The escaped string.
	 */
	public String _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			target	= arguments.getAsString( Key.string );
		int				len		= target.length();
		StringBuilder	result	= new StringBuilder( len + 10 );
		char			c;

		for ( int i = 0; i < len; i++ ) {
			c = target.charAt( i );
			switch ( c ) {
				case '\\' :
					result.append( "\\\\" );
					break;
				case '\n' :
					result.append( "\\n" );
					break;
				case '\r' :
					result.append( "\\r" );
					break;
				case '\f' :
					result.append( "\\f" );
					break;
				case '\b' :
					result.append( "\\b" );
					break;
				case '\t' :
					result.append( "\\t" );
					break;
				case '"' :
					result.append( "\\\"" );
					break;
				case '\'' :
					result.append( "\\\'" );
					break;
				default :
					result.append( c );
					break;
			}
		}
		return result.toString();
	}

}
