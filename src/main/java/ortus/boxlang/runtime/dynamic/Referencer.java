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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle dereferencing of objects
 */
public class Referencer {

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param context The context we're executing inside of
	 * @param object  The object to dereference
	 * @param key     The key to dereference
	 * @param safe    Whether to throw an exception if the key is not found
	 *
	 * @return The value that was dereferenced
	 */
	public static Object get( IBoxContext context, Object object, Key key, Boolean safe ) {
		if ( object == null ) {
			if ( safe ) {
				return null;
			} else {
				throw new BoxRuntimeException( "Cannot dereference key [" + key.getName() + "] on a null object" );
			}
		}
		if ( object instanceof DynamicObject dob ) {
			return dob.dereference( context, key, safe );
		} else if ( object instanceof Class clazz ) {
			return DynamicInteropService.dereference( context, clazz, null, key, safe );
		} else {
			return DynamicInteropService.dereference( context, object.getClass(), object, key, safe );
		}
	}

	/**
	 * Used to implement any time an object is dereferenced,
	 *
	 * @param object              The object to dereference
	 * @param key                 The key to dereference
	 * @param positionalArguments The arguments to pass to the method
	 * @param safe                Whether to throw an exception if the key is not
	 *                            found
	 *
	 * @return The value that was assigned
	 */
	public static Object getAndInvoke( IBoxContext context, Object object, Key key, Object[] positionalArguments, Boolean safe ) {
		if ( object == null ) {
			if ( safe ) {
				return null;
			} else {
				throw new BoxRuntimeException( "Cannot invoke method [" + key.getName() + "()] on a null object" );
			}
		}
		if ( object instanceof DynamicObject dob ) {
			return dob.dereferenceAndInvoke( context, key, positionalArguments, safe );
		}
		return DynamicInteropService.dereferenceAndInvoke( object, context, key, positionalArguments, safe );
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
		if ( object == null ) {
			if ( safe ) {
				return null;
			} else {
				throw new BoxRuntimeException( "Cannot invoke method [" + key.getName() + "()] on a null object" );
			}
		}
		if ( object instanceof DynamicObject dob ) {
			return dob.dereferenceAndInvoke( context, key, new Object[] {}, safe );
		}
		return DynamicInteropService.dereferenceAndInvoke( object, context, key, new Object[] {}, safe );
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
	public static Object getAndInvoke( IBoxContext context, Object object, Key key, Map<Key, Object> namedArguments, Boolean safe ) {
		if ( object == null ) {
			if ( safe ) {
				return null;
			} else {
				throw new BoxRuntimeException( "Cannot invoke method [" + key.getName() + "()] on a null object" );
			}
		}
		if ( object instanceof DynamicObject dob ) {
			return dob.dereferenceAndInvoke( context, key, namedArguments, safe );
		}
		return DynamicInteropService.dereferenceAndInvoke( object, context, key, namedArguments, safe );
	}

	/**
	 * Used to implement any time an object is assigned to,
	 *
	 * @param context The context we're executing inside of
	 * @param isFinal Whether the assignment is final
	 * @param object  The object to dereference
	 * @param key     The key to dereference
	 * @param value   The value to assign
	 *
	 * @return The value that was assigned
	 */
	public static Object set( IBoxContext context, boolean isFinal, Object object, Key key, Object value ) {
		if ( isFinal ) {
			// this.foo = "bar" references the class, but it's really hitting the this scope, so swap that out here to pass our instanceof check below
			if ( object instanceof IClassRunnable icr ) {
				object = icr.getThisScope();
			}
			if ( object instanceof IScope scope ) {
				return scope.assignFinal( context, key, value );
			} else {
				throw new BoxRuntimeException(
				    "Cannot assign final key [" + key.getName() + "] to an object other than a scope.  Base object was [" + object.getClass().getName() + "]" );
			}
		}

		if ( object instanceof DynamicObject dob ) {
			return dob.assign( context, key, value );
		} else if ( object instanceof Class clazz ) {
			return DynamicInteropService.assign( context, clazz, null, key, value );
		} else {
			return DynamicInteropService.assign( context, object.getClass(), object, key, value );
		}
	}

	/**
	 * Used to implement any time an object is assigned to,
	 *
	 * @param context The context we're executing inside of
	 * @param object  The object to dereference
	 * @param key     The key to dereference
	 * @param value   The value to assign
	 *
	 * @return The value that was assigned
	 */
	public static Object set( IBoxContext context, Object object, Key key, Object value ) {
		return set( context, false, object, key, value );
	}

	/**
	 * Used to implement any time an object is assigned via deep keys like
	 * foo.bar.baz=1
	 * Missing keys will be created as needed as HashMaps
	 * An exception will be thrown if any intermediate keys exists, but are not a
	 * Map.
	 *
	 * @param context The context we're executing inside of
	 * @param object  The object to dereference
	 * @param value   The value to assign
	 * @param keys    The keys to dereference
	 *
	 * @return The value that was assigned
	 */
	public static Object setDeep( IBoxContext context, boolean isFinal, Key mustBeScopeName, Object object, Object value, Key... keys ) {
		if ( mustBeScopeName != null && ! ( object instanceof IScope s && s.getName().equals( mustBeScopeName ) ) ) {
			throw new BoxRuntimeException( "Scope [" + mustBeScopeName.getName() + "] is not available in this context." );
		}

		for ( int i = 0; i <= keys.length - 1; i++ ) {
			Key key = keys[ i ];
			// At the final key, just assign our value and we're done
			if ( i == keys.length - 1 ) {
				set( context, isFinal, object, key, value );
				return value;
			}

			// For all intermediate keys, check if they exist and are a Struct or Array
			Object next = DynamicObject.unWrap( get( context, object, key, true ) );
			// If missing, create as a Struct
			if ( next == null ) {

				next = new Struct();
				set( context, isFinal, object, key, next );
				// If it's not null, it needs to be a Map
			} else if ( ! ( next instanceof Map || next instanceof List || next instanceof Query || next instanceof QueryColumn ) ) {
				throw new BoxRuntimeException(
				    String.format( "Cannot assign to key [%s] because it is a [%s] and not a Struct,  Array, or Query",
				        key.getName(),
				        next.getClass().getName() ) );
			}
			object	= next;
			// Only counts the first time through
			isFinal	= false;
		}

		return value;
	}

	public static Object setDeep( IBoxContext context, Object object, Object value, Key... keys ) {
		return setDeep( context, false, null, object, value, keys );
	}

	/**
	 * Used to implement any time an object is assigned via deep keys like
	 * foo.bar.baz=1
	 * Missing keys will be created as needed as HashMaps
	 * An exception will be thrown if any intermediate keys exists, but are not a
	 * Map.
	 *
	 * @param context           The context we're executing inside of
	 * @param scopeSearchResult The scope search result
	 * @param value             The value to assign
	 * @param keys              The keys to dereference
	 *
	 * @return The value that was assigned
	 */
	public static Object setDeep( IBoxContext context, ScopeSearchResult scopeSearchResult, Object value, Key... keys ) {
		return setDeep( context, false, null, scopeSearchResult.scope(), value, scopeSearchResult.getAssignmentKeys( keys ) );
	}

	public static Object setDeep( IBoxContext context, boolean isFinal, Key mustBeScopeName, ScopeSearchResult scopeSearchResult, Object value, Key... keys ) {
		return setDeep( context, isFinal, mustBeScopeName, scopeSearchResult.scope(), value, scopeSearchResult.getAssignmentKeys( keys ) );
	}

}
