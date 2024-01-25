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
package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.InstanceOf;

/**
 * This exception is thrown when a cast can't be done on any type
 */
public class ExceptionUtil {

	/**
	 * Checks if an exception is of a given type
	 *
	 * @param context The context
	 * @param e       The exception
	 * @param type    The type
	 *
	 * @return True if the exception is of the given type
	 */
	public static Boolean exceptionIsOfType( IBoxContext context, Throwable e, String type ) {
		// BoxLangExceptions check the type
		if ( e instanceof BoxLangException ble ) {
			// Either direct match to type, or "foo.bar" matches "foo.bar.baz
			if ( ble.type.equalsIgnoreCase( type ) || ble.type.toLowerCase().startsWith( type + "." ) )
				return true;
		}

		// Native exceptions just check the class hierarchy
		if ( InstanceOf.invoke( context, e, type ) ) {
			return true;
		}
		return false;
	}

	/**
	 * Throws a BoxLang exception or a passed in exception
	 *
	 * @param exception The exception to throw
	 */
	public static void throwException( Object exception ) {
		Object ex = DynamicObject.unWrap( exception );

		if ( ex instanceof RuntimeException runtimeException ) {
			throw runtimeException;
		} else if ( ex instanceof Throwable throwable ) {
			throw new CustomException( throwable.getMessage(), throwable );
		}

		if ( ex instanceof String string ) {
			throw new CustomException( string );
		} else {
			throw new BoxRuntimeException( "Cannot throw object of type [" + ex.getClass().getName() + "].  Must be a Throwable." );
		}
	}
}
