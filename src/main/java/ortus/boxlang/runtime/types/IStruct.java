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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public interface IStruct extends Map<Key, Object>, IType, IReferenceable {

	/**
	 * The Available types of structs
	 */
	public enum TYPES {

		CASE_SENSITIVE,
		DEFAULT,
		LINKED_CASE_SENSITIVE,
		LINKED,
		SOFT,
		SORTED,
		WEAK;

		/**
		 * Get the type of struct from a string
		 *
		 * @param type The string type to get
		 *
		 * @return The type of struct
		 */
		public static TYPES fromString( String type ) {
			String uType = type.toUpperCase();
			switch ( uType ) {
				case "CASESENSITIVE" :
					return CASE_SENSITIVE;
				case "DEFAULT" :
					return DEFAULT;
				case "ORDERED-CASESENSITIVE" :
					return LINKED_CASE_SENSITIVE;
				case "ORDERED" :
					return LINKED;
				case "SOFT" :
					return SOFT;
				case "SORTED" :
					return SORTED;
				case "WEAK" :
					return WEAK;
			}

			try {
				return TYPES.valueOf( uType );
			} catch ( IllegalArgumentException e ) {
				throw new BoxRuntimeException(
				    String.format(
				        "Could not create a struct with a type of [%s] as it is not a known type.",
				        uType
				    )
				);
			}
		}
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
	 * Copies all of the mappings from the specified map to this map (optional operation).
	 * This method will automatically convert the keys to Key objects
	 *
	 * @param map
	 */
	public void addAll( Map<? extends Object, ? extends Object> map );

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
	public Map<? extends Object, Object> getWrapped();

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

	/**
	 * Convenience method for getting cast as {@Link Key}
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Key getAsKey( Key key ) {
		return ( Key ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Array
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Array getAsArray( Key key ) {
		return ( Array ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Struct
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default IStruct getAsStruct( Key key ) {
		return ( IStruct ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as DateTime
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default DateTime getAsDateTime( Key key ) {
		return ( DateTime ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as String
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default String getAsString( Key key ) {
		return ( String ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Double
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Double getAsDouble( Key key ) {
		return ( Double ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Number
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Number getAsNumber( Key key ) {
		return ( Number ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Long
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Long getAsLong( Key key ) {
		return ( Long ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Integer
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Integer getAsInteger( Key key ) {
		return ( Integer ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Boolean
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Boolean getAsBoolean( Key key ) {
		return ( Boolean ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Function
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Function getAsFunction( Key key ) {
		return ( Function ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Query
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default Query getAsQuery( Key key ) {
		return ( Query ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as XML
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default XML getAsXML( Key key ) {
		return ( XML ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as Optional
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	@SuppressWarnings( "unchecked" )
	default Optional<Object> getAsOptional( Key key ) {
		return ( Optional<Object> ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as BoxLang Attempt
	 * If the value is not already an attempt, it will be wrapped in an Attempt
	 */
	@SuppressWarnings( "unchecked" )
	default Attempt<Object> getAsAttempt( Key key ) {
		Object result = DynamicObject.unWrap( get( key ) );
		// if it's already an Attempt, return it
		if ( result instanceof Attempt ar ) {
			return ar;
		}
		return Attempt.of( result );
	}

	/**
	 * Convenience method for getting cast as BoxRunnable
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default IClassRunnable getAsClassRunnable( Key key ) {
		return ( IClassRunnable ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting cast as BoxInterface
	 * Does NOT perform BoxLang casting, only Java cast so the object needs to actually be castable
	 */
	default BoxInterface getAsBoxInterface( Key key ) {
		return ( BoxInterface ) DynamicObject.unWrap( get( key ) );
	}

	default Stream<?> getAsStream( Key key ) {
		return ( Stream<?> ) DynamicObject.unWrap( get( key ) );
	}

	/**
	 * Convenience method for getting a key using the first param as the casting class
	 *
	 * @param clazz The class to cast the object to
	 * @param key   The key to get
	 *
	 * @return The object casted to the class
	 */
	default <T> T getAs( Class<T> clazz, Key key ) {
		return clazz.cast( DynamicObject.unWrap( get( key ) ) );
	}

}
