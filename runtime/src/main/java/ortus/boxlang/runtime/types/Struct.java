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

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class Struct implements Map<Key, Object>, IType, IReferenceable {

	public enum Type {
		LINKED,
		SORTED,
		DEFAULT
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private final Map<Key, Object>	wrapped;

	/**
	 * In general, a common approach is to choose an initial capacity that is a power of two.
	 * For example, 16, 32, 64, etc. This is because ConcurrentHashMap uses power-of-two-sized hash tables,
	 * and using a power-of-two capacity can lead to better distribution of elements in the table.
	 */
	private static final int		INITIAL_CAPACITY	= 32;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public Struct( Type type ) {
		if ( type.equals( Type.DEFAULT ) ) {
			wrapped = new ConcurrentHashMap<Key, Object>( INITIAL_CAPACITY );
			return;
		} else if ( type.equals( Type.LINKED ) ) {
			wrapped = Collections.synchronizedMap( new LinkedHashMap<Key, Object>( INITIAL_CAPACITY ) );
			return;
		} else if ( type.equals( Type.SORTED ) ) {
			wrapped = Collections.synchronizedMap( new TreeMap<Key, Object>() );
			return;
		}
		throw new RuntimeException( "Invalid struct type" );
	}

	public Struct() {
		this( Type.DEFAULT );
	}

	public Struct( Map<Object, Object> map ) {
		this( Type.DEFAULT );
		addAll( map );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Map Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	@Override
	public boolean containsKey( Object key ) {
		return wrapped.containsKey( key );
	}

	@Override
	public boolean containsValue( Object value ) {
		return wrapped.containsValue( value );
	}

	@Override
	public Object get( Object key ) {
		return unWrapNull( wrapped.get( key ) );
	}

	public Object get( String key ) {
		return wrapped.get( Key.of( key ) );
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

	@Override
	public Object putIfAbsent( Key key, Object value ) {
		return wrapped.putIfAbsent( key, wrapNull( value ) );
	}

	@Override
	public Object remove( Object key ) {
		return wrapped.remove( key );
	}

	@Override
	public void putAll( Map<? extends Key, ? extends Object> map ) {
		wrapped.putAll( map );
	}

	public void addAll( Map<Object, Object> map ) {
		for ( Map.Entry<Object, Object> entry : map.entrySet() ) {
			Key key;
			if ( entry.getKey() instanceof Key ) {
				key = ( Key ) entry.getKey();
			} else {
				key = Key.of( entry.getKey().toString() );
			}
			Object value = entry.getValue();
			put( key, value );
		}
	}

	@Override
	public void clear() {
		wrapped.clear();
	}

	@Override
	public Set<Key> keySet() {
		return wrapped.keySet();
	}

	@Override
	public Collection<Object> values() {
		return wrapped.values();
	}

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

	@Override
	public String asString() {
		return wrapped.toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * IReferenceable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void assign( Key key, Object value ) {
		put( key, value );
	}

	@Override
	public Object dereference( Key key, Boolean safe ) throws KeyNotFoundException {
		Object value = get( key );
		if ( value == null && !safe ) {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key %s was not found in the struct. Valid keys are (%s)", key.getName(), getKeys() ), this
			);
		}
		return value;
	}

	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe )
	    throws KeyNotFoundException {
		Object value = dereference( name, safe );
		if ( value instanceof Function ) {
			Function			function	= ( Function ) value;
			FunctionBoxContext	fContext	= new FunctionBoxContext(
			    context,
			    function,
			    name,
			    function.createArgumentsScope( positionalArguments )
			);
			return function.invoke( fContext );
		} else {
			throw new RuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}
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
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe )
	    throws KeyNotFoundException {
		Object value = dereference( name, safe );
		if ( value instanceof Function ) {
			Function			function	= ( Function ) value;
			FunctionBoxContext	fContext	= new FunctionBoxContext(
			    context,
			    function,
			    name,
			    function.createArgumentsScope( namedArguments )
			);
			return function.invoke( fContext );
		} else {
			throw new RuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function "
			);
		}
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

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeys() {
		return keySet().stream().map( Key::getNameNoCase ).collect( java.util.stream.Collectors.toList() );
	}

	public static Object unWrapNull( Object value ) {
		if ( value instanceof NullValue ) {
			return null;
		}
		return value;
	}

	public static Struct of( Object... values ) {
		if ( values.length % 2 != 0 ) {
			throw new RuntimeException( "Invalid number of arguments.  Must be an even number." );
		}
		Struct struct = new Struct();
		for ( int i = 0; i < values.length; i += 2 ) {
			if ( values[ i ] == null ) {
				throw new RuntimeException( "Invalid key type.  Cannot be null." );
			}
			Key key;
			if ( values[ i ] instanceof Key ) {
				key = ( Key ) values[ i ];
			} else if ( values[ i ] instanceof String ) {
				key = Key.of( ( String ) values[ i ] );
			} else {
				CastAttempt<String> castAttempt = StringCaster.attempt( values[ i ] );
				if ( castAttempt.wasSuccessful() ) {
					key = Key.of( castAttempt.get() );
				} else {
					throw new RuntimeException( "Invalid key type.  Must be String or Key instance." );
				}
			}

			struct.put( key, values[ i + 1 ] );
		}
		return struct;
	}

}