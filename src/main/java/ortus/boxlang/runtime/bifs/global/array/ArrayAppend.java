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
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Append a value to an array" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayAppend extends BIF {

	/**
	 * Constructor
	 */
	public ArrayAppend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "boolean", Key.merge, false )
		};
	}

	/**
	 * Appends a value to the end of an array. The array is modified in place.
	 * When merge is true and the value is an array (or can be cast to one), each of its elements is appended individually; otherwise the value
	 * is appended as a single element.
	 * Also available as the <code>append()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to which the element should be appended.
	 *
	 * @argument.value The element to append. Can be any type.
	 *
	 * @argument.merge If true and the value is an array, its elements are appended individually. If false (default), the value is appended as a
	 *                 single element.
	 *
	 * @return The same array, with the value(s) appended.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array				actualArray	= arguments.getAsArray( Key.array );
		Object				value		= arguments.get( Key.value );
		CastAttempt<Array>	attempt		= ArrayCaster.attempt( value );
		if ( arguments.getAsBoolean( Key.merge ) && attempt.wasSuccessful() ) {
			actualArray.addAll( attempt.get() );
		} else {
			actualArray.add( value );
		}
		return actualArray;
	}

}
