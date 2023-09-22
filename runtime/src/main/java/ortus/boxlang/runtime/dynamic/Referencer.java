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

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

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
		if ( safe && object == null ) {
			return null;
		}
		return getReferenceable( object ).dereference( key, safe );
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object              The object to dereference
	 * @param key                 The key to dereference
	 * @param positionalArguments The arguments to pass to the method
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The value that was assigned
	 */
	public static Object getAndInvoke( IBoxContext context, Object object, Key key, Object[] positionalArguments, Boolean safe ) {
		if ( safe && object == null ) {
			return null;
		}
		return getReferenceable( object ).dereferenceAndInvoke( context, key, positionalArguments, safe );
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object The object to dereference
	 * @param key    The key to dereference
	 * @param safe   Whether to throw an exception if the key is not found
	 *
	 * @return The value that was assigned
	 */
	public static Object getAndInvoke( IBoxContext context, Object object, Key key, Boolean safe ) {
		if ( safe && object == null ) {
			return null;
		}
		return getReferenceable( object ).dereferenceAndInvoke( context, key, new Object[] {}, safe );
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object         The object to dereference
	 * @param key            The key to dereference
	 * @param namedArguments The arguments to pass to the method
	 * @param safe           Whether to throw an exception if the key is not found
	 *
	 * @return The value that was assigned
	 */
	public static Object getAndInvoke( IBoxContext context, Object object, Key key, Map<Key, Object> namedArguments,
	    Boolean safe ) {
		if ( safe && object == null ) {
			return null;
		}
		return getReferenceable( object ).dereferenceAndInvoke( context, key, namedArguments, safe );
	}

	/**
	 * Used to implement any time an object is assigned to,
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

	/**
	 * Used to implement any time an object is assigned via deep keys like foo.bar.baz=1
	 * Missing keys will be created as needed as HashMaps
	 * An exception will be thrown if any intermediate keys exists, but are not a Map.
	 *
	 * @param object The object to dereference
	 * @param value  The value to assign
	 * @param keys   The keys to dereference
	 *
	 * @return The value that was assigned
	 */
	public static Object setDeep( Object object, Object value, Key... keys ) {
		IReferenceable obj = getReferenceable( object );

		for ( int i = 0; i <= keys.length - 1; i++ ) {
			Key key = keys[ i ];
			// At the final key, just assign our value and we're done
			if ( i == keys.length - 1 ) {
				obj.assign( key, value );
				return value;
			}

			// For all intermediate keys, check if they exist and are a Map
			Object next = obj.dereference( key, true );
			// If missing, create as a map
			// TODO: this needs to be a proper struct
			if ( next == null ) {
				next = new HashMap<Key, Object>();
				obj.assign( key, next );
				// If it's not null, it needs to be a Map
			} else if ( ! ( next instanceof Map ) ) {
				throw new ApplicationException(
				    String.format( "Cannot assign to key [%s] because it is a [%s] and not a Map", key,
				        next.getClass().getName() )
				);
			}
			obj = getReferenceable( next );
		}

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
