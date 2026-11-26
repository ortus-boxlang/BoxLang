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
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF( description = "Check if value is a struct" )
public class IsStruct extends BIF {

	/**
	 * Constructor
	 */
	public IsStruct() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.variable )
		};
	}

	/**
	 * Determines whether a value is a struct. This is true for BoxLang Structs and any value which can be cast to a struct, such as a Java Map.
	 * Arrays, strings and other types return false.
	 *
	 * @argument.variable The value to test for struct-ness.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return True if the value is, or can be cast to, a struct; false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return isStruct( arguments.get( Key.variable ) );
	}

	/**
	 * Verify that this is a struct
	 *
	 * @param object The object to test
	 *
	 * @return True if the object is a struct, false otherwise
	 */
	public static boolean isStruct( Object object ) {
		CastAttempt<IStruct> attempt = StructCaster.attempt( object );
		return attempt.wasSuccessful();
	}

	/**
	 * Check if we are a native Map
	 */
	public static boolean isMap( Object object ) {
		object = DynamicObject.unWrap( object );
		if ( object == null ) {
			return false;
		}
		return object instanceof java.util.Map;
	}

}
