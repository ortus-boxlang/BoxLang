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
package ortus.boxlang.runtime.types;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.CastException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * A struct is a collection of key-value pairs, where the key is unique and case insensitive
 */
public class Struct extends ConcurrentHashMap<Key, Object> implements IType, IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * In general, a common approach is to choose an initial capacity that is a power of two.
	 * For example, 16, 32, 64, etc. This is because ConcurrentHashMap uses power-of-two-sized hash tables,
	 * and using a power-of-two capacity can lead to better distribution of elements in the table.
	 */
	private static final int INITIAL_CAPACITY = 32;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public Struct() {
		super( INITIAL_CAPACITY );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Super class
	 */
	@Override
	public boolean equals( Object obj ) {
		// Same object
		if ( this == obj ) {
			return true;
		}
		// Null and class checks
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		// Super
		return super.equals( obj );
	}

	/**
	 * Struct Hashcode
	 */
	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode() );
	}

	/**
	 * Convert the struct to a human-readable string, usually great for debugging
	 * Remember structs have no order except their internal hash code
	 *
	 * @return The string representation of the struct using the format {key=value, key=value}
	 */
	@Override
	public String toString() {
		return entrySet().stream().map( entry -> entry.getKey().getNameNoCase() + "=" + entry.getValue() )
		        .collect( java.util.stream.Collectors.joining( ", ", "{", "}" ) );
	}

	/**
	 * Convert the struct to a human-readable string, usually great for debugging
	 * Remember structs have no order except their internal hash code
	 *
	 * @return The string representation of the struct using the format {key=value, key=value}
	 */
	public String toStringWithCase() {
		return entrySet().stream().map( entry -> entry.getKey().getName() + "=" + entry.getValue() )
		        .collect( java.util.stream.Collectors.joining( ", ", "{", "}" ) );
	}

	/**
	 * Represent as string, or throw exception if not possible
	 */
	@Override
	public String asString() {
		throw new CastException( "Can't cast a struct to a string. Try serializing it" );
	}

	/**
	 * Returns the value of the key if found.
	 * We override in order to present nicer exception messages
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key
	 *
	 * @throws KeyNotFoundException If the key is not found
	 */
	public Object get( Key key ) {
		Object target = super.get( key );
		if ( target != null ) {
			return target;
		}
		throw new KeyNotFoundException(
		        String.format( "The key %s was not found in the struct. Valid keys are (%s)", key.getName(), getKeys() ), this
		);
	}

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeys() {
		return super.keySet().stream().map( Key::getNameNoCase ).collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Get an array list of all the keys in the struct with case-sensitivity
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeysWithCase() {
		return super.keySet().stream().map( Key::getName ).collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @return The requested obect
	 */
	public Object dereference( Key name ) throws KeyNotFoundException {
		return get( name );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( Key name, Object[] arguments ) throws KeyNotFoundException {
		Object object = dereference( name );
		// Test if the object is invokable (a UDF or java call site) and invoke it or throw exception if not invokable
		// Ideally, the invoker logic is not here, but in a helper
		throw new RuntimeException( "not implemeneted yet" );
	}

	/**
	 * Safely dereference this object by a key and return the value, or null if not found
	 *
	 * @return The requested object or null
	 */
	public Object safeDereference( Key name ) {
		return super.get( name );
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	public void assign( Key name, Object value ) {
		put( name, value );
	}
}
