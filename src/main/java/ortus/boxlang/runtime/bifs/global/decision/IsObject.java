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
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;

@BoxBIF
public class IsObject extends BIF {

	/**
	 * Constructor
	 */
	public IsObject() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Determines whether a value is an object.
	 * <p>
	 * True conditions are:
	 * <ul>
	 * <li>Box Class instances are "objects"</li>
	 * <li>All other classes that represent a Boxlang type are NOT "objects" (query, array, struct, XML, etc)</li>
	 * <li>These JDK classes which are used for "simple" BoxLang types are also not "objects" (String, Number, Boolean)</li>
	 * <li>Every other Java class is an "object"</li>
	 * </ul>
	 *
	 * @argument.value The value to test
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope defining the value to test.
	 *
	 * @return True if the value is an object, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return isObject( arguments.get( Key.value ) );
	}

	/**
	 * See above
	 *
	 * @param obj The value to test
	 *
	 * @return True if the value is an object, false otherwise.
	 */
	public static boolean isObject( Object obj ) {
		// Unwrap the object if it is a DynamicObject
		// This is needed because the DynamicObject is a wrapper around the object
		obj = DynamicObject.unWrap( obj );
		if ( obj == null ) {
			return false;
		}

		// Box Class instances are "objects"
		if ( obj instanceof IClassRunnable ) {
			return true;
		}

		// All other classes that represent a type are not "objects" (query, array, struct, XML, etc)
		if ( obj instanceof IType ) {
			return false;
		}

		// These JDK classes which are used for "simple" BoxLang types are also not "objects"
		if ( IsSimpleValue.isSimpleValue( obj ) ) {
			return false;
		}

		// Arrays are not "objects"
		if ( IsArray.isNativeArrayOrList( obj ) ) {
			return false;
		}

		// Maps are not "objects"
		if ( IsStruct.isMap( obj ) ) {
			return false;
		}

		// Every other Java class is an "object"
		return true;
	}

}
