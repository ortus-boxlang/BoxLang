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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;

@BoxBIF( description = "Print a line to the console or response" )
public class Println extends BIF {

	/**
	 * Constructor
	 */
	public Println() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "any", Key.message, "" )
		};
	}

	/**
	 * Print a message with line break to the console
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.message The message to print
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object obj = DynamicObject.unWrap( arguments.get( Key.message ) );

		// Force to string representation
		obj = Println.forceToString( obj );

		// If we have a request context, let's use that context's out
		RequestBoxContext rCon = context.getRequestContext();
		if ( rCon != null ) {
			rCon.getOut().println( obj );
			return null;
		}

		// Fallback in case we're not in a request
		System.out.println( obj );

		return null;
	}

	public static String forceToString( Object obj ) {
		CastAttempt<String>	strAttempt;
		String				result;

		// If it's a BoxLang type, let's use the string representation
		if ( obj instanceof IType t ) {
			result = t.asString();
		}
		// For native arrays use the Arrays.toString for better formatting
		else if ( obj != null && obj.getClass().isArray() ) {
			result = java.util.Arrays.toString( ArrayCaster.cast( obj ).toArray() );
			// Let our StringCaster take a whack at it
		} else if ( ( strAttempt = StringCaster.attempt( obj ) ).wasSuccessful() ) {
			result = strAttempt.get();
		} else {
			// Fallback to default toString
			result = String.valueOf( obj );
		}
		return result;
	}
}
