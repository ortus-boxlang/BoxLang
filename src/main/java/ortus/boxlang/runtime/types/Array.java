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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.meta.IListenable;

public class Array implements List<Object>, IType, IReferenceable, IListenable {

	public static final Array			EMPTY	= new ImmutableArray();
	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	protected final List<Object>		wrapped;

	/**
	 * Metadata object
	 */
	public BoxMeta						$bx;

	/**
	 * Used to track change listeners. Intitialized on-demand
	 */
	private Map<Key, IChangeListener>	listeners;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create default array
	 */
	public Array() {
		this( 10 );
	}

	/**
	 * Constructor to create array with an initial capacity
	 *
	 * @param initialCapactity The initialCapactity of Array to create
	 */
	public Array( int initialCapactity ) {
		wrapped = Collections.synchronizedList( new ArrayList<Object>( initialCapactity ) );
	}

	/**
	 * Constructor to create a Array from a Java array
	 *
	 * @param arr The array to create the Array from
	 */
	public Array( Object[] arr ) {
		wrapped = Collections.synchronizedList( Arrays.asList( arr ) );
	}

	/**
	 * Constructor to create a Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	public Array( List<Object> list ) {
		wrapped = list;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static convenience methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	public static Array fromList( List<Object> list ) {
		return new Array( list );
	}

	/**
	 * Create a Array from a Java array
	 *
	 * @param arr The array to create the Array from
	 */
	public static Array fromArray( Object[] arr ) {
		return new Array( arr );
	}

	/**
	 * Create a Array from a list of values.
	 *
	 * @param values The values to create the Array from
	 *
	 * @return The Array
	 */
	public static Array of( Object... values ) {
		return fromArray( values );
	}

	/**
	 * --------------------------------------------------------------------------
	 * List Interface Methods
	 * --------------------------------------------------------------------------
	 */

	public int size() {
		return wrapped.size();
	}

	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	public boolean contains( Object o ) {
		return wrapped.contains( o );
	}

	public Iterator<Object> iterator() {
		return wrapped.iterator();
	}

	public Object[] toArray() {
		return wrapped.toArray();
	}

	public <T> T[] toArray( T[] a ) {
		return wrapped.toArray( a );
	}

	public boolean add( Object e ) {
		synchronized ( wrapped ) {
			return wrapped.add( notifyListeners( wrapped.size(), e ) );
		}
	}

	public boolean remove( Object o ) {
		synchronized ( wrapped ) {
			int indexOf = wrapped.indexOf( o );
			if ( indexOf > -1 ) {
				notifyListeners( indexOf, null );
				return wrapped.remove( o );
			}
			return false;
		}
	}

	public boolean containsAll( Collection<?> c ) {
		return wrapped.containsAll( c );
	}

	public boolean addAll( Collection<? extends Object> c ) {

		synchronized ( wrapped ) {
			// TODO: deal with listeners
			return wrapped.addAll( c );
		}
	}

	public boolean addAll( int index, Collection<? extends Object> c ) {
		synchronized ( wrapped ) {
			// TODO: deal with listeners
			return wrapped.addAll( index, c );
		}
	}

	public boolean removeAll( Collection<?> c ) {
		// TODO: deal with listeners
		return wrapped.removeAll( c );
	}

	public boolean retainAll( Collection<?> c ) {
		// TODO: deal with listeners
		return wrapped.retainAll( c );
	}

	public void clear() {
		// TODO: deal with listeners
		wrapped.clear();
	}

	public Object get( int index ) {
		return wrapped.get( index );
	}

	public Object set( int index, Object element ) {
		return wrapped.set(
		    index,
		    notifyListeners( index, element )
		);
	}

	public void add( int index, Object element ) {
		synchronized ( wrapped ) {
			wrapped.add( index, notifyListeners( wrapped.size(), element ) );
		}
	}

	public Object remove( int index ) {
		notifyListeners( index, null );
		return wrapped.remove( index );
	}

	public int indexOf( Object o ) {
		return wrapped.indexOf( o );
	}

	public int lastIndexOf( Object o ) {
		return wrapped.lastIndexOf( o );
	}

	public ListIterator<Object> listIterator() {
		return wrapped.listIterator();
	}

	public ListIterator<Object> listIterator( int index ) {
		return wrapped.listIterator( index );
	}

	public List<Object> subList( int fromIndex, int toIndex ) {
		return wrapped.subList( fromIndex, toIndex );
	}

	public int append( Object e ) {
		synchronized ( wrapped ) {
			add( e );
			return wrapped.size();
		}
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
	 * Array Hashcode
	 */
	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}

	/**
	 * Convert the Array to a human-readable string, usually great for debugging
	 *
	 * @return The string representation of the Array
	 */
	@Override
	public String toString() {
		return wrapped.toString();
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
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;

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

		Integer index = Array.validateAndGetIntForAssign( key, wrapped.size(), false );
		if ( index > wrapped.size() ) {
			synchronized ( wrapped ) {
				// If the index is larger than the array, pad the array with nulls
				for ( int i = wrapped.size(); i < index; i++ ) {
					wrapped.add( null );
				}
			}
		}
		wrapped.set( index - 1, value );
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

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		Integer index = Array.validateAndGetIntForDerefernce( key, wrapped.size(), safe );
		// non-existant indexes return null when dereferncing safely
		if ( safe && ( index < 1 || index > wrapped.size() ) ) {
			return null;
		}
		return wrapped.get( index - 1 );
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

		// If there is no member funtion, look for a native Java method of that name
		DynamicObject object = DynamicObject.of( this );

		if ( safe && !object.hasMethod( name.getName() ) ) {
			return null;
		}

		return object.invoke( name.getName(), namedArguments ).orElse( null );

		// Native java methods can't be called with named params so we don't even try
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

	private Object notifyListeners( int i, Object value ) {
		if ( listeners == null ) {
			return value;
		}
		Key				key			= Key.of( String.valueOf( i + 1 ) );
		IChangeListener	listener	= listeners.get( key );
		if ( listener == null ) {
			listener = listeners.get( IListenable.ALL_KEYS );
		}
		if ( listener == null ) {
			return value;
		}
		return listener.notify( key, value, i < wrapped.size() ? wrapped.get( i ) : null );

	}

	private void initListeners() {
		if ( listeners == null ) {
			listeners = new ConcurrentHashMap<Key, IChangeListener>();
		}
	}

	public static int validateAndGetIntForDerefernce( Key key, int size, boolean safe ) {
		Integer index = getIntFromKey( key, safe );
		// If we're dereferencing safely, anything goes.
		if ( safe ) {
			return index;
		}
		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			throw new ExpressionException( String.format(
			    "Array cannot be indexed by a number smaller than 1"
			) );
		}
		// Disallow out of bounds indexes foo[5]
		if ( index > size ) {
			throw new ExpressionException( String.format(
			    "Array index [%s] is out of bounds for an array of length [%s]", index, size
			) );
		}
		return index;
	}

	public static int validateAndGetIntForAssign( Key key, int size, boolean isNative ) {
		Integer index = getIntFromKey( key, false );

		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			throw new ExpressionException( String.format(
			    "Array cannot be assigned by a number smaller than 1"
			) );
		}

		if ( isNative ) {
			// Disallow out of bounds indexes foo[5]
			if ( index > size ) {
				throw new ExpressionException( String.format(
				    "Invalid index [%s] for Native Array, can't expand Native Arrays.  Current array length is [%s]", index,
				    size
				) );
			}
		}
		return index;
	}

	public static int getIntFromKey( Key key, boolean safe ) {
		Integer index;

		// If key is int, use it directly
		if ( key instanceof IntKey intKey ) {
			index = intKey.getIntValue();
		} else {
			// If key is not an int, we must attempt to cast it
			CastAttempt<Double> indexAtt = DoubleCaster.attempt( key.getName() );
			if ( !indexAtt.wasSuccessful() ) {
				if ( safe ) {
					return -1;
				}
				throw new ExpressionException( String.format(
				    "Array cannot be assigned with key %s", key.getName()
				) );
			}
			Double dIndex = indexAtt.get();
			index = dIndex.intValue();
			// Dissallow non-integer indexes foo[1.5]
			if ( index.doubleValue() != dIndex ) {
				if ( safe ) {
					return -1;
				}
				throw new ExpressionException( String.format(
				    "Array index [%s] is invalid.  Index must be an integer.", dIndex
				) );
			}
		}
		return index;
	}

}
