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
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxBIF( alias = "LSDateFormat" )
@BoxBIF( alias = "LSTimeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "LSDateTimeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "LSDateFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "LSTimeFormat" )
public class LSDateTimeFormat extends DateTimeFormat {

	/**
	 * Constructor
	 */
	public LSDateTimeFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.mask ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Formats a date in a locale-specific format
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date string or object
	 *
	 * @argument.mask Optional format mask, or common mask
	 *
	 * @argument.locale Optional locale designation of the output ( e.g. es-SA )
	 *
	 * @argument.timezone Optional specific timezone to apply to the date ( if not present in the date string )
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	functionName	= arguments.getAsKey( BIF.__functionName ).getName();
		Key		dtFunctionName	= Key.of( functionName.substring( 2 ) );
		arguments.put( BIF.__functionName, dtFunctionName );
		return super.invoke( context, arguments );
	}

}
