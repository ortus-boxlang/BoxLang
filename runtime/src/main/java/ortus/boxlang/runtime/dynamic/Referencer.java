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
import ortus.boxlang.runtime.interop.ClassInvoker;

/**
 * I handle de
 */
public class Referencer {

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object
	 * @param key
	 *
	 * @return
	 */
	public static Object get( Object object, Key key ) {
		// If this object is referenable,
		if ( object instanceof IReferenceable ) {

			// ask it to do the work
			return ( ( IReferenceable ) object ).dereference( key );

			// Treat it like a Java object and generically look for a field
		} else {
			return ClassInvoker.of( object ).dereference( key );

			// Do we ever throw here, or do we always delagate the Java objet, letting the ClassInvoker throw?
			// throw new RuntimeException(
			// String.format( "Unable to dereference object [%s] by key [%s]", object.getClass().getName(), key.getName() )
			// );
		}
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object
	 * @param key
	 *
	 * @return
	 */
	public static Object getAndInvoke( Object object, Key key, Object[] arguments ) {
		// If this object is referenable,
		if ( object instanceof IReferenceable ) {

			// ask it to do the work
			return ( ( IReferenceable ) object ).dereferenceAndInvoke( key, arguments );

			// Treat it like a Java object and generically invoke a method
		} else {
			return ClassInvoker.of( object ).dereferenceAndInvoke( key, arguments );

			// Do we ever throw here, or do we always delagate the Java objet, letting the ClassInvoker throw?
			// throw new RuntimeException(
			// String.format( "Unable to dereference object [%s] by key [%s]", object.getClass().getName(), key.getName() )
			// );
		}
	}

	public static Object set( Object object, Key key, Object value ) {
		// If this object is referenable,
		if ( object instanceof IReferenceable ) {

			// ask it to do the work
			( ( IReferenceable ) object ).assign( key, value );

			// Treat it like a Java object and generically look for a field
		} else {
			ClassInvoker.of( object ).assign( key, value );

			// Do we ever throw here, or do we always delagate the Java objet, letting the ClassInvoker throw?
			// throw new RuntimeException(
			// String.format( "Unable to assign object [%s] a key [%s]", object.getClass().getName(), key.getName() )
			// );
		}

		return value;
	}
}
