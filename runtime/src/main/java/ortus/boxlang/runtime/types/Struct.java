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
	 * Checks the struct for a key using a string which is auto-converted to a Key object
	 *
	 * @param key The string key to check
	 *
	 * @return True if the key exists, false otherwise
	 */
	public Boolean containsKey( String key ) {
		return super.containsKey( Key.of( key ) );
	}

	/**
	 * Set a value in the struct by a string key, which we auto-convert to a Key object
	 *
	 * @param key   The string key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	public Object put( String key, Object value ) {
		return super.put( Key.of( key ), value );
	}

	/**
	 * Set a value in the struct by a string key, which we auto-convert to a Key object
	 *
	 * @param key   The string key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	public Object put( Key key, Object value ) {
		return super.put( key, wrapNull( value ) );
	}

	/**
	 * If the specified key is not already associated with a value, associates it with the given value
	 *
	 * @param key   The string key to set
	 * @param value The value to set
	 *
	 * @return The previous value associated with the specified key, or null if there was no mapping for the key
	 */
	public Object putIfAbsent( String key, Object value ) {
		return super.putIfAbsent( Key.of( key ), wrapNull( value ) );
	}

	/**
	 * Get helper using a string key, which we auto-convert to a Key object
	 *
	 * @param key The string key to look for
	 *
	 * @return The value of the key
	 *
	 * @throws KeyNotFoundException If the key is not found
	 */
	public Object get( String key ) {
		return get( Key.of( key ) );
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
		return get( key, false );
	}

	/**
	 * Returns the value of the key if found, throwing an exception if not.
	 *
	 * When safe=true, missing keys return null sa well so there is no differnce between a missing key and a legitimately null value
	 *
	 * @param key  The key to look for
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The value of the key, or null if not found and safe=true
	 *
	 * @throws KeyNotFoundException If the key is not found
	 */
	public Object get( Key key, Boolean safe ) {
		Object target = getRaw( key );

		// Revisit for full CFML null support
		if ( target != null || safe ) {
			return unWrapNull( target );
		}

		throw new KeyNotFoundException(
		    String.format( "The key %s was not found in the struct. Valid keys are (%s)", key.getName(), getKeys() ), this
		);

	}

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a NullValue still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a NullValue object, null means the key didn't exist *
	 */
	public Object getRaw( Key key ) {
		return super.get( key );
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
	public Object dereference( Key name, Boolean safe ) throws KeyNotFoundException {
		return get( name, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( Key name, Object[] arguments, Boolean safe ) throws KeyNotFoundException {
		Object object = dereference( name, safe );
		// Test if the object is invokable (a UDF or java call site) and invoke it or throw exception if not invokable
		// Ideally, the invoker logic is not here, but in a helper
		throw new RuntimeException( "not implemented yet" );
	}

	/**
	 * Derefence by assignment (x = y)
	 *
	 * @param name  The key to assign to
	 * @param value The value to assign
	 */
	public void assign( Key name, Object value ) {
		put( name, value );
	}

	/**
	 * Wrap null values in an instance of the NullValue class
	 *
	 * @param value
	 *
	 * @return
	 */
	public static Object wrapNull( Object value ) {
		if ( value == null ) {
			return new NullValue();
		}
		return value;
	}

	public static Object unWrapNull( Object value ) {
		if ( value instanceof NullValue ) {
			return null;
		}
		return value;
	}
}
