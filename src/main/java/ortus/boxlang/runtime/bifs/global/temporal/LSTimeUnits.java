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

package ortus.boxlang.runtime.bifs.global.temporal;

import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF( alias = "LSWeek" )
@BoxBIF( alias = "LSDayOfWeek" )
@BoxMember( type = BoxLangType.DATETIME, name = "lsWeek" )
@BoxMember( type = BoxLangType.DATETIME, name = "lsDayOfWeek" )
public class LSTimeUnits extends TimeUnits {

	/**
	 * Map of ls functions to TimeUnits functions
	 */
	public final static Struct functionMap = new Struct(
	    new HashMap<String, Key>() {

		    {
			    put( "LSDayOfWeek", Key.of( "dayOfWeek" ) );
			    put( "LSWeek", Key.of( "weekOfYear" ) );
		    }
	    }
	);

	/**
	 * Constructor
	 */
	public LSTimeUnits() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.locale ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Provides the Localized BIF and member functions for time units ( e.g. different locales have different start days to the week )
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The DateTime object or datetime string representation
	 *
	 * @argument.locale The locale string to be parsed and applied to the final result
	 *
	 * @argument.timezone The timezone with which to cast the result
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put( BIF.__functionName, functionMap.get( arguments.getAsKey( BIF.__functionName ) ) );
		return super.invoke( context, arguments );
	}

}
