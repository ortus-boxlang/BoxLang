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

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.ClassBoxContext;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.KeyCased;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.meta.IListenable;
import ortus.boxlang.runtime.types.meta.StructMeta;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;

/**
 * I wrap a Map to allow it to be used as a Struct, but without needing to make a copy of the original Map.
 * Changes to this struct will be reflected in the original map and vice versa
 *
 */
public class StructMapWrapper implements IStruct, IListenable, Serializable {

	/**
	 * This is to help prevent endless recursion when converting a struct to a string. Technically, this approach only applies to structs
	 * and would not prevent two arrays with circular references. If we run into that, we can probably move this static field to the
	 * IType interface and add the same logic to the Array class and any other type that might have circular references like query.
	 */
	private static final ThreadLocal<Set<Integer>>	toStringObjects		= ThreadLocal.withInitial( HashSet::new );

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Metadata object
	 */
	public BoxMeta									$bx;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Serial version UID
	 */
	private static final long						serialVersionUID	= 1L;

	/**
	 * The wrapped map used in the implementation
	 */
	protected final Map<Object, Object>				wrapped;

	/**
	 * Used to track change listeners. Intitialized on-demand
	 */
	private Map<Key, IChangeListener>				listeners;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param type The type of struct to create: DEFAULT, LINKED, SORTED
	 *
	 * @throws BoxRuntimeException If an invalid type is specified: DEFAULT, LINKED, SORTED
	 */
	public StructMapWrapper( Map<Object, Object> map ) {
		this.wrapped = map;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Creation from Maps
	 * --------------------------------------------------------------------------
	 * These methods are used to create structs from existing maps
	 */

	/**
	 * Static helper to create a struct from an existing map
	 *
	 * @param map The map to create the struct from
	 *
	 * @return The struct created from the map
	 */
	public static IStruct of( Map<Object, Object> map ) {
		return new StructMapWrapper( map );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static builders from name value pairs
	 * --------------------------------------------------------------------------
	 * These methods are used to create structs from name value pairs
	 */

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
	public boolean containsKey( String key ) {
		return wrapped.containsKey( key );
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified {@code Key}
	 *
	 * @param key key whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( Key key ) {
		return wrapped.containsKey( key.getOriginalValue() );
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified {@code Key}
	 *
	 * @param key key whose presence in this map is to be tested
	 *
	 * @return {@code true} if this map contains a mapping for the specified
	 */
	public boolean containsKey( Object key ) {
		if ( key instanceof Key keyKey ) {
			return containsKey( keyKey );
		}
		return wrapped.containsKey( key );
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
	public Object get( Key key ) {
		return wrapped.get( key.getOriginalValue() );
	}

	/**
	 * Returns the value to which the specified Key is mapped
	 *
	 * @param key the key whose associated value is to be returned
	 *
	 * @return the value to which the specified key is mapped or null if not found
	 */
	@Override
	public Object get( String key ) {
		return wrapped.get( Key.of( key ) );
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
		if ( key instanceof Key keyKey ) {
			return wrapped.get( keyKey.getOriginalValue() );
		}
		return wrapped.get( key );
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
		return getOrDefault( key.getOriginalValue(), defaultValue );
	}

	/**
	 * Get key, with default value if not found
	 *
	 * @param key          The key to look for
	 * @param defaultValue The default value to return if the key is not found
	 *
	 * @return The value of the key
	 */
	public Object getOrDefault( Object key, Object defaultValue ) {
		return wrapped.getOrDefault( key, defaultValue );
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
		return wrapped.getOrDefault( key, defaultValue );
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
		return wrapped.put(
		    key.getOriginalValue(),
		    notifyListeners( key, value )
		);
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
	public Object put( String key, Object value ) {
		return wrapped.put(
		    key,
		    notifyListeners( Key.of( key ), value )
		);
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
		if ( !containsKey( key ) ) {
			return wrapped.putIfAbsent(
			    isCaseSensitive() && ! ( key instanceof KeyCased ) ? new KeyCased( key.getName() ) : key,
			    notifyListeners( key, value )
			);
		}
		return null;
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
		if ( key instanceof Key keyKey ) {
			return remove( keyKey );
		}
		if ( key instanceof String stringKey ) {
			return remove( stringKey );
		}
		return remove( Key.of( StringCaster.cast( key ) ) );
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
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The String key to remove
	 */
	public Object remove( Key key ) {
		notifyListeners( key, null );
		return isCaseSensitive()
		    ? wrapped.remove( keySet().stream().filter( k -> KeyCaster.cast( k ).equalsWithCase( key ) ).findFirst().orElse( Key.EMPTY ) )
		    : wrapped.remove( key );
	}

	/**
	 * Copies all of the mappings from the specified map to this map
	 * (optional operation). It expects the specific key and object generics.
	 */
	@Override
	public void putAll( Map<? extends Key, ? extends Object> map ) {
		Stream<Map.Entry<? extends Key, ?>> entryStream;
		// Parallel streams are actually slower for small data sets!
		// 1000 may even be to small. Some resoruces say to not bnother unless you have over 10,000 items! Need to test more.
		if ( map.size() > 1000 ) {
			entryStream = map.entrySet().parallelStream().map( entry -> entry );
		} else {
			entryStream = map.entrySet().stream().map( entry -> entry );
		}
		// With a linked hashmap we need to maintain order - which is a tiny bit slower
		entryStream.forEach( entry -> {
			wrapped.put( entry.getKey(), entry.getValue() );
		} );
	}

	/**
	 * Copies all of the mappings from the specified map to this map (optional operation).
	 * This method will automatically convert the keys to Key objects
	 *
	 * @param map
	 */
	public void addAll( Map<? extends Object, ? extends Object> map ) {
		Stream<Map.Entry<?, ?>> entryStream;
		// Parallel streams are actually slower for small data sets!
		// 1000 may even be to small. Some resoruces say to not bnother unless you have over 10,000 items! Need to test more.
		if ( map.size() > 1000 ) {
			entryStream = map.entrySet().parallelStream().map( entry -> entry );
		} else {
			entryStream = map.entrySet().stream().map( entry -> entry );
		}
		// With a linked hashmap we need to maintain order - which is a tiny bit slower

		entryStream.forEach( entry -> {
			Object key;
			if ( entry.getKey() instanceof Key entryKey ) {
				key = entryKey.getOriginalValue();
			} else {
				key = entry.getKey();
			}
			wrapped.put( key, entry.getValue() );
		} );
	}

	/**
	 * Removes all of the mappings from this map (optional operation).
	 */
	@Override
	public void clear() {
		// TODO: handle listeners
		wrapped.clear();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 */
	@Override
	public Set<Key> keySet() {
		return wrapped.keySet().stream().map( Key::of ).collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 */
	@Override
	public Collection<Object> values() {
		return wrapped.values().stream()
		    .collect( Collectors.toList() );
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 */
	@Override
	public Set<Entry<Key, Object>> entrySet() {
		return wrapped.entrySet().stream()
		    .map( entry -> new SimpleEntry<>( Key.of( entry.getKey() ), entry.getValue() ) )
		    .collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Super class
	 */
	@Override
	public boolean equals( Object obj ) {
		return obj instanceof IStruct struct
		    ? wrapped.equals( struct.getWrapped() )
		    : wrapped.equals( obj );
	}

	/**
	 * Struct Hashcode
	 */
	@Override
	public int hashCode() {
		return computeHashCode( IType.createIdentitySetForType() );
	}

	@Override
	public int computeHashCode( Set<IType> visited ) {
		if ( visited.contains( this ) ) {
			return 0;
		}
		visited.add( this );
		int result = 1;
		for ( Map.Entry<Object, Object> entry : wrapped.entrySet() ) {
			result = 31 * result + ( entry.getKey() == null ? 0 : entry.getKey().hashCode() );
			Object value = entry.getValue();
			if ( value instanceof IType ) {
				result = 31 * result + ( ( IType ) value ).computeHashCode( visited );
			} else {
				result = 31 * result + ( value == null ? 0 : value.hashCode() );
			}
		}
		return result;
	}

	/**
	 * Convert the struct to a human-readable string, usually great for debugging
	 * Remember structs have no order except their internal hash code
	 *
	 * @return The string representation of the struct using the format {key=value, key=value}
	 */
	@Override
	public String toString() {
		return asString();
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
		// Get the set of stringed structs for this thread, so it doesn't recurse forever
		Set<Integer>	stringed		= toStringObjects.get();
		Integer			thisHashCode	= System.identityHashCode( this );
		if ( !stringed.add( thisHashCode ) ) {
			// The target object has already been stringed in this thread, so return to prevent recursion
			return "<recursive reference " + thisHashCode + ">";
		}
		try {
			StringBuilder sb = new StringBuilder();
			sb.append( size() > 0 ? "{\n" : "{" );
			sb.append( wrapped.entrySet().stream()
			    .map( entry -> {
				    String line = entry.getKey().toString() + " : ";
				    if ( entry.getValue() instanceof IType t ) {
					    line += t.asString();
				    } else {
					    if ( entry.getValue() instanceof String s ) {
						    line += "\"" + s.replace( "\"", "\\\"" ) + "\"";
					    } else {
						    line += entry.getValue().toString();
					    }
				    }
				    return line;
			    } )
			    .map( line -> line.replaceAll( "(?m)^", "  " ) ) // Add an indent to the start of each line
			    .collect( java.util.stream.Collectors.joining( ",\n" ) ) );
			sb.append( size() > 0 ? "\n}" : "}" );
			return sb.toString();
		} finally {
			stringed.remove( thisHashCode );
		}
	}

	/**
	 * Get the BoxMetadata object for this struct
	 *
	 * @return The {@Link BoxMeta} object for this struct
	 */
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new StructMeta( this );
		}
		return this.$bx;
	}

	/**
	 * Get the type of struct
	 *
	 * @return The type of struct according to the {@Link Type} enum
	 */
	public TYPES getType() {
		// TODO: Guess a valid type based on the class of the wrapped map?
		return TYPES.DEFAULT;
	}

	/**
	 * Returns a boolean as to whether the struct instance is case sensitive
	 */
	public Boolean isCaseSensitive() {
		return false;
	}

	/**
	 * Returns a boolean as to whether this is a soft-referenced struct
	 */
	public Boolean isSoftReferenced() {
		return false;
	}

	/**
	 * Helper to make the struct Unmodifiable
	 */
	public UnmodifiableStruct toUnmodifiable() {
		return new UnmodifiableStruct( this );
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
	public Object assign( IBoxContext context, Key key, Object value ) {
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
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {
		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		Object value = getRaw( key );
		if ( value == null && !safe ) {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", key.getName(), getKeysAsStrings() ), this
			);
		}
		return value;
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

		MemberDescriptor	memberDescriptor	= BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.STRUCT );

		Object				value				= get( name );
		if ( value != null ) {

			if ( value instanceof Function function ) {
				FunctionBoxContext fContext = Function.generateFunctionContext(
				    function,
				    context.getFunctionParentContext(),
				    name,
				    positionalArguments,
				    getFunctionContextThisClassForInvoke( context ),
				    getFunctionContextThisInterfaceForInvoke()
				);
				return function.invoke( fContext );
			} else if ( memberDescriptor == null ) {
				throw new BoxRuntimeException(
				    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
			}
		}

		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
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

		MemberDescriptor	memberDescriptor	= BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.STRUCT );

		Object				value				= get( name );
		if ( value != null ) {
			if ( value instanceof Function function ) {
				FunctionBoxContext fContext = Function.generateFunctionContext(
				    function,
				    context.getFunctionParentContext(),
				    name,
				    namedArguments,
				    getFunctionContextThisClassForInvoke( context ),
				    getFunctionContextThisInterfaceForInvoke()
				);
				return function.invoke( fContext );
			} else if ( memberDescriptor == null ) {
				throw new BoxRuntimeException(
				    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function "
				);
			}
		}
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	public IClassRunnable getFunctionContextThisClassForInvoke( IBoxContext context ) {
		if ( context instanceof ClassBoxContext cContext ) {
			return cContext.getThisClass();
		} else if ( context instanceof FunctionBoxContext fContext ) {
			return fContext.getThisClass();
		}
		return null;
	}

	public BoxInterface getFunctionContextThisInterfaceForInvoke() {
		return null;
	}

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<Key> getKeys() {
		return keySet().stream().collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Get an array list of all the keys in the struct
	 *
	 * @return An array list of all the keys in the struct
	 */
	public List<String> getKeysAsStrings() {
		return wrapped.keySet().stream().map( String::valueOf ).collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Get the wrapped map used in the implementation
	 */
	public Map<? extends Object, Object> getWrapped() {
		return wrapped;
	}

	/**
	 * --------------------------------------------------------------------------
	 * IListenable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void registerChangeListener( IChangeListener listener ) {
		initListeners();
		listeners.put( IListenable.ALL_KEYS, listener );
	}

	@Override
	public void registerChangeListener( Key key, IChangeListener listener ) {
		initListeners();
		listeners.put( key, listener );
	}

	@Override
	public void removeChangeListener( Key key ) {
		initListeners();
		listeners.remove( key );
	}

	private Object notifyListeners( Key key, Object value ) {
		if ( listeners == null ) {
			return value;
		}
		IChangeListener listener = listeners.get( key );
		if ( listener == null ) {
			listener = listeners.get( IListenable.ALL_KEYS );
		}
		if ( listener == null ) {
			return value;
		}
		return listener.notify( key, value, wrapped.get( key ) );

	}

	private void initListeners() {
		if ( listeners == null ) {
			listeners = new ConcurrentHashMap<Key, IChangeListener>();
		}
	}

}
