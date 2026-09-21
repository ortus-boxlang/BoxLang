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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Range;

/**
 * Check if a value is a Range type
 */
@BoxBIF( description = "Check if value is a range" )
public class IsRange extends BIF {

	/**
	 * Constructor
	 */
	public IsRange() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Determine whether a value is a range.
	 *
	 * @argument.value The value to test for range-ness.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope defining the value to test.
	 *
	 * @return True if the value is a Range, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return isRange( arguments.get( Key.value ) );
	}

	/**
	 * Verify that this is a range.
	 *
	 * @param object The object to test
	 *
	 * @return True if the object is a Range, false otherwise
	 */
	public static boolean isRange( Object object ) {
		if ( object == null ) {
			return false;
		}

		// Unwrap DynamicObject to get the underlying object
		object = DynamicObject.unWrap( object );

		return object instanceof Range;
	}

}