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
package ortus.boxlang.runtime.dynamic;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.interop.DynamicObject;

/**
 * I handle dereferencing of objects
 */
public class Referencer {

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object The object to dereference
	 * @param key    The key to dereference
	 * @param safe   Whether to throw an exception if the key is not found
	 *
	 * @return The value that was dereferenced
	 */
	public static Object get( Object object, Key key, Boolean safe ) {
		return getReferenceable( object ).dereference( key, safe );
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object    The object to dereference
	 * @param key       The key to dereference
	 * @param arguments The arguments to pass to the method
	 * @param safe      Whether to throw an exception if the key is not found
	 *
	 * @return The value that was assigned
	 */
	public static Object getAndInvoke( Object object, Key key, Object[] arguments, Boolean safe ) {
		return getReferenceable( object ).dereferenceAndInvoke( key, arguments, safe );
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object The object to dereference
	 * @param key    The key to dereference
	 * @param value  The value to assign
	 *
	 * @return The value that was assigned
	 */
	public static Object set( Object object, Key key, Object value ) {
		getReferenceable( object ).assign( key, value );
		return value;
	}

	private static IReferenceable getReferenceable( Object object ) {
		if ( object instanceof IReferenceable ) {
			return ( IReferenceable ) object;
		} else {
			return DynamicObject.of( object );
		}
	}
}
