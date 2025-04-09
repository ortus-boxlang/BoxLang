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
package ortus.boxlang.runtime.bifs.global.decision;

import java.util.Date;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IType;

@BoxBIF
public class IsSimpleValue extends BIF {

	/**
	 * Constructor
	 */
	public IsSimpleValue() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "any", Key.value ),
		};
	}

	/**
	 * Determine whether the given value is a string, boolean, numeric, or date value.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value Value to test for simple-ness.
	 *
	 * @return True if the value is a simple value, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return isSimpleValue( arguments.get( Key.value ) );
	}

	/**
	 * Determine whether the given value is a string, boolean, numeric, or date value.
	 *
	 * @param value The value to test for simple-ness.
	 *
	 * @return True if the value is a simple value, false otherwise.
	 */
	public static boolean isSimpleValue( Object value ) {
		if ( value == null ) {
			return false;
		}

		// Exclude StringBuffer and StringBuilder explicitly
		if ( value instanceof StringBuffer || value instanceof StringBuilder ) {
			return false;
		}

		// Check for "simple" types
		if ( value instanceof String || value instanceof Boolean || value instanceof Number || value instanceof Character ) {
			return true;
		}

		// Check for date/time-related types
		if ( value instanceof Date || value instanceof DateTime ) {
			return true;
		}

		// Check for locale/timezone
		if ( value instanceof java.util.Locale || value instanceof java.util.TimeZone ) {
			return true;
		}

		// Exclude objects and boxed language types
		if ( value instanceof IClassRunnable || value instanceof IType ) {
			return false;
		}

		return false; // Default case
	}

}
