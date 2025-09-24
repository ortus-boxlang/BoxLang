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
package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF( description = "Convert data to script representation" )
public class ToScript extends BIF {

	/**
	 * Constructor
	 */
	public ToScript() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.cfvar ),
		    new Argument( true, "string", Key.javascriptvar )
		};
	}

	/**
	 * Creates a JavaScript expression that assigns the input BoxLang value to a JavaScript variable.
	 * This is useful for converting BoxLang values to JavaScript variables in a script.
	 * It can convert:
	 * <ul>
	 * <li>Booleans</li>
	 * <li>DateTimes</li>
	 * <li>Numbers</li>
	 * <li>Arrays</li>
	 * <li>Queries</li>
	 * <li>Strings</li>
	 * <li>Structures</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.cfvar The value to convert to a script line; String, DateTimes, Number, Array, Structure or Query
	 *
	 * @argument.javascriptvar The name of the JavaScript variable to assign the result to.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	runtimeVar	= arguments.get( Key.cfvar );
		String	jsVar		= arguments.getAsString( Key.javascriptvar );

		// Null handling
		if ( runtimeVar == null ) {
			return jsVar + " = null;";
		}

		// String empty handling
		if ( runtimeVar instanceof String castedVar && castedVar.isEmpty() ) {
			return jsVar + " = '';";
		}

		// Try DateTimes
		CastAttempt<DateTime> dateCastAttempt = DateTimeCaster.attempt( runtimeVar, context );
		if ( dateCastAttempt.wasSuccessful() ) {
			return new StringBuilder( jsVar.length() + 20 )
			    .append( jsVar )
			    .append( " = new Date('" )
			    .append( dateCastAttempt.get().toISOString() )
			    .append( "');" )
			    .toString();
		}

		// For other types, we use JSON serialization to convert to a script line
		Object result = context.invokeFunction( Key.JSONSerialize, new Object[] { runtimeVar } );
		return new StringBuilder( jsVar.length() + 20 + String.valueOf( result ).length() )
		    .append( jsVar )
		    .append( " = " )
		    .append( result )
		    .append( ";" )
		    .toString();
	}
}