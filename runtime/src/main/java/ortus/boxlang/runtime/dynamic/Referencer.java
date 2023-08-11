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

/**
 * I handle de
 */
public class Referencer {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	 /**
	  * Used to implement any time an object is derefernced,
	  * @param object
	  * @param key
	  * @return
	  */
	 public static Object get( Object object, Key key ) {
		// If this object is referenable,
		if( object instanceof IReferenceable ) {
			// ask it to do the work
			return ((IReferenceable)object).__dereference( key );
		// TODO: handle other scenarios of unknown objects
		} else {
			throw new RuntimeException(
				String.format( "Unable to dereference object [%s] by key [%s]", object.getClass().getName(), key.getName() )
			 );
		}
	 }

	 public static Object set( Object object, Key key, Object value ) {
		// If this object is referenable,
		if( object instanceof IReferenceable ) {
			// ask it to do the work
			((IReferenceable)object).__assign( key, value );
			return value;
		// TODO: handle other scenarios of unknown objects
		} else {
			throw new RuntimeException(
				String.format( "Unable to assign object [%s] a key [%s]", object.getClass().getName(), key.getName() )
			 );
		}
	 }
}
