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
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;

public interface IStruct extends Map<Key, Object>, IType, IReferenceable {

	/**
	 * The Available types of structs
	 */
	public static enum TYPES {
		LINKED,
		SORTED,
		DEFAULT,
		CASE_SENSITIVE,
		LINKED_CASE_SENSITIVE,
		SOFT,
		WEAK
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified {@code Key}
	 *
	 * @param key key whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( Key key );

	/**
	 * Returns {@code true} if this map maps one or more keys using a String key
	 *
	 * @param key The string key to look for. Automatically converted to Key object
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( String key );

	/**
	 * Returns the value to which the specified Key is mapped
	 *
	 * @param key the key whose associated value is to be returned
	 *
	 * @return the value to which the specified key is mapped or null if not found
	 */
	public Object get( String key );

	/**
	 * Get key, with default value if not found
	 *
	 * @param key          The key to look for
	 * @param defaultValue The default value to return if the key is not found
	 *
	 * @return The value of the key
	 */
	public Object getOrDefault( Key key, Object defaultValue );

	/**
	 * Get key, with default value if not found
	 *
	 * @param key          The key to look for
	 * @param defaultValue The default value to return if the key is not found
	 *
	 * @return The value of the key
	 */
	public Object getOrDefault( String key, Object defaultValue );

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a NullValue still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a NullValue object, null means the key didn't exist *
	 */
	public Object getRaw( Key key );

	/**
	 * Set a value in the struct by a Key object
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	@Override
	public Object put( Key key, Object value );

	/**
	 * Set a value in the struct by a string key, which we auto-convert to a Key object
	 *
	 * @param key   The string key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	public Object put( String key, Object value );

	/**
	 * Put a value in the struct if the key doesn't exist
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	@Override
	public Object putIfAbsent( Key key, Object value );

	/**
	 * Put a value in the struct if the key doesn't exist
	 *
	 * @param key   The String key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	public Object putIfAbsent( String key, Object value );

	/**
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The String key to remove
	 */
	public Object remove( String key );

	/**
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The String key to remove
	 */
	public Object remove( Key key );

	/**
	 * Convert the struct to a human-readable string, usually great for debugging
	 * Remember structs have no order except their internal hash code
	 *
	 * @return The string representation of the struct using the format {key=value, key=value}
	 */
	public String toStringWithCase();

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<Key> getKeys();

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeysAsStrings();

	/**
	 * Get the wrapped map used in the implementation
	 */
	public Map<Key, Object> getWrapped();

	/**
	 * Convenience method for getting cast as {@Link Key}
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Key getAsKey( Key key );

	/**
	 * Convenience method for getting cast as Array
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Array getAsArray( Key key );

	/**
	 * Convenience method for getting cast as Struct
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public IStruct getAsStruct( Key key );

	/**
	 * Convenience method for getting cast as DateTime
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public DateTime getAsDateTime( Key key );

	/**
	 * Convenience method for getting cast as String
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public String getAsString( Key key );

	/**
	 * Convenience method for getting cast as Double
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Double getAsDouble( Key key );

	/**
	 * Convenience method for getting cast as Long
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Long getAsLong( Key key );

	/**
	 * Convenience method for getting cast as Integer
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Integer getAsInteger( Key key );

	/**
	 * Convenience method for getting cast as Boolean
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Boolean getAsBoolean( Key key );

	/**
	 * Convenience method for getting cast as Function
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Function getAsFunction( Key key );

	/**
	 * Convenience method for getting cast as Query
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public Query getAsQuery( Key key );

	/**
	 * Convenience method for getting cast as BoxRunnable
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	public IClassRunnable getClassRunnable( Key key );

	/**
	 * Get the type of struct
	 *
	 * @return The type of struct according to the {@Link Type} enum
	 */
	public TYPES getType();

	/**
	 * Returns a boolean as to whether the struct instance is case sensitive
	 */
	public Boolean isCaseSensitive();

	/**
	 * Returns a boolean as to whether the struct assignments are soft referenced
	 */
	public Boolean isSoftReferenced();

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 */
	public Set<Entry<Key, Object>> entrySet();

}
