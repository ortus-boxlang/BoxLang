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
		Object value = arguments.get( Key.value );

		if ( value == null ) {
			return false;
		}

		// Even though CF will auto cast a string buffer to a string, isSimpleValue() still returns false. Go figure.
		if ( value instanceof StringBuffer || value instanceof StringBuilder ) {
			return false;
		}

		boolean	isObject		= IsObject.isObject( value );
		boolean	isBoxLangType	= value instanceof IType;
		boolean	isDate			= value instanceof DateTime;

		return isDate || ( !isBoxLangType && !isObject );
	}

}
