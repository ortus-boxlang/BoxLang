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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.types.meta.BoxMeta;

public class Struct implements Map<Key, Object>, IType, IReferenceable {

	public enum Type {
		LINKED,
		SORTED,
		DEFAULT
	}

	public static final Struct			EMPTY				= new ImmutableStruct();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	protected final Map<Key, Object>	wrapped;

	/**
	 * In general, a common approach is to choose an initial capacity that is a power of two.
	 * For example, 16, 32, 64, etc. This is because ConcurrentHashMap uses power-of-two-sized hash tables,
	 * and using a power-of-two capacity can lead to better distribution of elements in the table.
	 */
	protected static final int			INITIAL_CAPACITY	= 32;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param type The type of struct to create: DEFAULT, LINKED, SORTED
	 */
	public Struct( Type type ) {
		if ( type.equals( Type.DEFAULT ) ) {
			wrapped = new ConcurrentHashMap<>( INITIAL_CAPACITY );
			return;
		} else if ( type.equals( Type.LINKED ) ) {
			wrapped = Collections.synchronizedMap( new LinkedHashMap<Key, Object>( INITIAL_CAPACITY ) );
			return;
		} else if ( type.equals( Type.SORTED ) ) {
			wrapped = Collections.synchronizedMap( new TreeMap<Key, Object>() );
			return;
		}
		throw new ApplicationException( "Invalid struct type [" + type.name() + "]" );
	}

	/**
	 * Create a default struct
	 */
	public Struct() {
		this( Type.DEFAULT );
	}

	/**
	 * Construct a struct from a map. This wraps the original map.
	 * Use the Struct( Type type, Map<? extends Object, ? extends Object> map ) method and
	 * supply an explicit type to have this struct created with a copy of all the
	 * keys/values in your map.
	 *
	 * @param map The map to create the struct from
	 */
	protected Struct( Map<Key, Object> map, Boolean useMyMap ) {
		wrapped = map;
	}

	/**
	 * Construct a struct from the keys/values in your map.
	 *
	 * @param map The map to create the struct from
	 */
	public Struct( Map<? extends Object, ? extends Object> map ) {
		this( Type.DEFAULT, map );
	}

	/**
	 * Construct a struct of a specific type from a map
	 *
	 * @param map  The map to create the struct from
	 * @param type The type of struct to create: DEFAULT, LINKED, SORTED
	 */
	public Struct( Type type, Map<? extends Object, ? extends Object> map ) {
		this( type );
		addAll( map );
	}

	/**
	 * Create a struct from a map
	 *
	 * @param map The map to create the struct from
	 */
	public static Struct fromMap( Map<Object, Object> map ) {
		return new Struct( map );
	}

	/**
	 * Construct a struct of a specific type from a map
	 *
	 * @param map  The map to create the struct from
	 * @param type The type of struct to create: DEFAULT, LINKED, SORTED
	 */
	public static Struct fromMap( Type type, Map<Object, Object> map ) {
		return new Struct( type, map );
	}

	/**
	 * Create a struct from a list of values. The values must be in pairs, key, value, key, value, etc.
	 *
	 * @param values The values to create the struct from
	 *
	 * @return The struct
	 */
	public static Struct of( Object... values ) {
		if ( values.length % 2 != 0 ) {
			throw new ApplicationException( "Invalid number of arguments.  Must be an even number." );
		}
		Struct struct = new Struct();
		for ( int i = 0; i < values.length; i += 2 ) {
			struct.put( KeyCaster.cast( values[ i ] ), values[ i + 1 ] );
		}
		return struct;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Map Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the number of key-value mappings in this map. If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size() {
		return wrapped.size();
	}

	/**
	 * Returns {@code true} if this map contains no key-value mappings.
	 */
	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified {@code Key}
	 *
	 * @param key key whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	@Override
	public boolean containsKey( Object key ) {
		return wrapped.containsKey( key );
	}

	/**
	 * Returns {@code true} if this map maps one or more keys using a String key
	 *
	 * @param key The string key to look for. Automatically converted to Key object
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( String key ) {
		return containsKey( Key.of( key ) );
	}

	/**
	 * Returns {@code true} if this map maps has the specified value
	 *
	 * @param value value whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified value
	 */
	@Override
	public boolean containsValue( Object value ) {
		return wrapped.containsValue( value );
	}

	/**
	 * Returns the value to which the specified Key is mapped
	 *
	 * @param key the key whose associated value is to be returned
	 *
	 * @return the value to which the specified key is mapped or null if not found
	 */
	@Override
	public Object get( Object key ) {
		return unWrapNull( wrapped.get( key ) );
	}

	/**
	 * Returns the value to which the specified String key is mapped.
	 *
	 * @param key The string key to look for. Automatically converted to Key object
	 *
	 * @return The value of the key or null if not found
	 */
	public Object get( String key ) {
		return wrapped.get( Key.of( key ) );
	}

	/**
	 * Get key, with default value if not found
	 *
	 * @param key          The key to look for
	 * @param defaultValue The default value to return if the key is not found
	 *
	 * @return The value of the key
	 */
	public Object getOrDefault( Key key, Object defaultValue ) {
		return unWrapNull( wrapped.getOrDefault( key, defaultValue ) );
	}

	/**
	 * Get key, with default value if not found
	 *
	 * @param key          The key to look for
	 * @param defaultValue The default value to return if the key is not found
	 *
	 * @return The value of the key
	 */
	public Object getOrDefault( String key, Object defaultValue ) {
		return getOrDefault( Key.of( key ), defaultValue );
	}

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a NullValue still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a NullValue object, null means the key didn't exist *
	 */
	public Object getRaw( Key key ) {
		return wrapped.get( key );
	}

	/**
	 * Set a value in the struct by a Key object
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	@Override
	public Object put( Key key, Object value ) {
		return wrapped.put( key, wrapNull( value ) );
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
		return put( Key.of( key ), value );
	}

	/**
	 * Put a value in the struct if the key doesn't exist
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	@Override
	public Object putIfAbsent( Key key, Object value ) {
		return wrapped.putIfAbsent( key, wrapNull( value ) );
	}

	/**
	 * Put a value in the struct if the key doesn't exist
	 *
	 * @param key   The String key to set
	 * @param value The value to set
	 *
	 * @return The previous value of the key, or null if not found
	 */
	public Object putIfAbsent( String key, Object value ) {
		return putIfAbsent( Key.of( key ), value );
	}

	/**
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The key to remove
	 */
	@Override
	public Object remove( Object key ) {
		return wrapped.remove( key );
	}

	/**
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The String key to remove
	 */
	public Object remove( String key ) {
		return remove( Key.of( key ) );
	}

	/**
	 * Copies all of the mappings from the specified map to this map
	 * (optional operation). It expects the specific key and object generics.
	 */
	@Override
	public void putAll( Map<? extends Key, ? extends Object> map ) {
		wrapped.putAll( map );
	}

	/**
	 * Copies all of the mappings from the specified map to this map (optional operation).
	 * This method will automatically convert the keys to Key objects
	 *
	 * @param map
	 */
	public void addAll( Map<? extends Object, ? extends Object> map ) {
		map.entrySet()
		    .parallelStream()
		    .forEach( entry -> {
			    Key key;
			    if ( entry.getKey() instanceof Key entryKey ) {
				    key = entryKey;
			    } else {
				    key = Key.of( entry.getKey().toString() );
			    }
			    put( key, entry.getValue() );
		    } );
	}

	/**
	 * Removes all of the mappings from this map (optional operation).
	 */
	@Override
	public void clear() {
		wrapped.clear();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 */
	@Override
	public Set<Key> keySet() {
		return wrapped.keySet();
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 */
	@Override
	public Collection<Object> values() {
		return wrapped.values();
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 */
	@Override
	public Set<Entry<Key, Object>> entrySet() {
		return wrapped.entrySet();
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Super class
	 */
	@Override
	public boolean equals( Object obj ) {
		return wrapped.equals( obj );
	}

	/**
	 * Struct Hashcode
	 */
	@Override
	public int hashCode() {
		return wrapped.hashCode();
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
	 * --------------------------------------------------------------------------
	 * IType Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	@Override
	public String asString() {
		return wrapped.toString();
	}

	public BoxMeta getBoxMeta() {
		return null;
		/*
		 * /
		 * if ( this.$bx == null ) {
		 * this.$bx = new FunctionMeta( this );
		 * }
		 * return this.$bx;
		 */
	}

	/**
	 * --------------------------------------------------------------------------
	 * IReferenceable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( Key key, Object value ) {
		put( key, value );
		return value;
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( Key key, Boolean safe ) {
		Object value = getRaw( key );
		if ( value == null && !safe ) {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key %s was not found in the struct. Valid keys are (%s)", key.getName(), getKeys() ), this
			);
		}
		return unWrapNull( value );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {

		// Member functions here

		Object value = dereference( name, true );
		if ( value != null ) {

			if ( value instanceof Function function ) {
				return function.invoke(
				    Function.generateFunctionContext(
				        function,
				        context.getFunctionParentContext(),
				        name,
				        function.createArgumentsScope( positionalArguments )
				    )
				);
			} else {
				throw new ApplicationException(
				    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
			}
		}

		// If there is no member funtion, look for a native Java method of that name
		DynamicObject object = DynamicObject.of( this );

		if ( safe && !object.hasMethod( name.getName() ) ) {
			return null;
		}

		return object.invoke( name.getName(), positionalArguments ).orElse( null );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		// Member functions here

		Object value = dereference( name, safe );
		if ( value instanceof Function function ) {
			return function.invoke(
			    Function.generateFunctionContext(
			        function,
			        context.getFunctionParentContext(),
			        name,
			        function.createArgumentsScope( namedArguments )
			    )
			);
		} else {
			throw new ApplicationException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function "
			);
		}
	}

	/**
	 * Wrap null values in an instance of the NullValue class
	 *
	 * @param value The value to wrap
	 *
	 * @return The wrapped value
	 */
	public static Object wrapNull( Object value ) {
		if ( value == null ) {
			return new NullValue();
		}
		return value;
	}

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeys() {
		return keySet().stream().map( Key::getNameNoCase ).collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Unwrap null values from the NullValue class
	 *
	 * @param value The value to unwrap
	 *
	 * @return The unwrapped value which can be null
	 */
	public static Object unWrapNull( Object value ) {
		if ( value instanceof NullValue ) {
			return null;
		}
		return value;
	}

}
