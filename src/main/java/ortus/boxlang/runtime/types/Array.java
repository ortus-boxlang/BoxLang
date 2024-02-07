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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.meta.IListenable;

public class Array implements List<Object>, IType, IReferenceable, IListenable {

	public static final Array			EMPTY			= new ImmutableArray();
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
	 * Function service
	 */
	private static FunctionService		functionService	= BoxRuntime.getInstance().getFunctionService();

	DynamicObject						dynamicObject	= null;

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
		wrapped = Collections.synchronizedList( new ArrayList<Object>( Arrays.asList( arr ) ) );
	}

	/**
	 * Constructor to create a Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	@SuppressWarnings( "unchecked" )
	public Array( List<? extends Object> list ) {
		wrapped = ( List<Object> ) list;
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
	public static Array fromList( List<? extends Object> list ) {
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
	 * Create a new Array from a list of values.
	 *
	 * @param arr List of values to copy into a new Array
	 *
	 * @return The Array
	 */
	public static Array copyOf( List<?> arr ) {
		Array newArr = new Array();
		// loop over list and add all elements
		for ( Object o : arr ) {
			newArr.add( o );
		}
		return newArr;
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
			ListIterator<Object> iterator = wrapped.listIterator();
			while ( iterator.hasNext() ) {
				Object element = iterator.next();
				if ( element.equals( o ) ) {
					iterator.remove();
					return true;
				}
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
		synchronized ( wrapped ) {
			return wrapped.removeAll( c );
		}
	}

	public boolean retainAll( Collection<?> c ) {
		// TODO: deal with listeners
		synchronized ( wrapped ) {
			return wrapped.retainAll( c );
		}
	}

	public void clear() {
		// TODO: deal with listeners
		synchronized ( wrapped ) {
			wrapped.clear();
		}
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
		synchronized ( wrapped ) {
			ListIterator<Object>	iterator	= wrapped.listIterator();
			int						i			= 0;
			while ( iterator.hasNext() ) {
				Object element = iterator.next();
				if ( i == index ) {
					iterator.remove();
					return element;
				}
				i++;
			}
			return null;
		}
	}

	public Object removeAt( Number index ) {
		return remove( index.intValue() );
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

	public void sort( Comparator compareFunc ) {
		wrapped.sort( compareFunc );
	}

	/*
	 * Returns a stream of the array
	 */
	public Stream<Object> stream() {
		return wrapped.stream();
	}

	/*
	 * Returns a IntStream of the indexes
	 */
	public IntStream intStream() {
		return IntStream.range( 0, size() );
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
		StringBuilder sb = new StringBuilder();
		sb.append( "[\n  " );
		sb.append( wrapped.stream()
		    .map( value -> ( value instanceof IType t ? t.asString() : ( value == null ? "[null]" : value.toString() ) ) )
		    .collect( java.util.stream.Collectors.joining( ",\n  " ) ) );
		sb.append( "\n]" );
		return sb.toString();
	}

	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;

	}

	/**
	 * --------------------------------------------------------------------------
	 * One-based index methods for getters/setters and filtering
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Inserts an object at a specified position using the one-based index value
	 *
	 * @param index
	 *
	 * @return
	 */
	public Array insertAt( int index, Object element ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + wrapped.size() + " elements." );
		}
		add( index - 1, element );
		return this;
	}

	/**
	 * Appends a new object to the array
	 *
	 * @param element The object to insert
	 *
	 * @return
	 */
	public Array push( Object element ) {
		add( element );
		return this;
	}

	/**
	 * Retrieves and object using the one-based index value
	 *
	 * @param index
	 *
	 * @return
	 */
	public Object getAt( int index ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + wrapped.size() + " elements." );
		}
		return get( index - 1 );
	}

	/**
	 * Sets an object using the one-based index value
	 *
	 * @param index
	 * @param element the object to set at the specified position
	 *
	 * @return
	 */
	public Array setAt( int index, Object element ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + wrapped.size() + " elements." );
		}
		set( index - 1, element );
		return this;
	}

	/**
	 * Deletes an object from the array using the one-based index value
	 *
	 * @param index The one-based index value to use for deletion
	 *
	 * @return
	 */
	public Array deleteAt( int index ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + wrapped.size() + " elements." );
		}
		remove( index - 1 );
		return this;
	}

	/**
	 * Finds the first one-based index of an string - either case sensitively or not
	 *
	 * @param value         The value to be searched
	 * @param caseSensitive Whether the test should be case sensitive or not
	 *
	 * @return The one-based index value or zero if not found
	 */
	public int findIndex( Object value, Boolean caseSensitive ) {
		return intStream()
		    .filter(
		        i -> EqualsEquals.invoke( get( i ), value, caseSensitive ) || get( i ).equals( value )
		    )
		    .findFirst()
		    .orElse( -1 ) + 1;
	}

	/**
	 * Finds the first one-based index of an array item with a specified value
	 *
	 * @param value The value to be searched
	 *
	 * @return The one-based index value or zero if not found
	 */
	public int findIndex( Object value ) {
		return findIndex( value, true );
	}

	/**
	 * Finds the first index of an array item using a function filter
	 *
	 * @param test
	 * @param context
	 *
	 * @return
	 */
	public int findIndex( Function test, IBoxContext context ) {
		return intStream()
		    .filter( i -> ( boolean ) context.invokeFunction( test, new Object[] { get( i ) } ) )
		    .findFirst()
		    .orElse( -1 ) + 1;
	}

	/**
	 * Returns a new array removing all of the duplicates caseSenstively
	 *
	 * @return
	 */
	public Array removeDuplicates() {
		return removeDuplicates( true );
	}

	/**
	 * Returns a new array removing all of the duplicates - either caseSenstively or not
	 *
	 * @param caseSensitive whether to perform the deduplication caseSenstively
	 *
	 * @return
	 */
	public Array removeDuplicates( Boolean caseSensitive ) {
		Array	ref			= this;
		Array	distinct	= new Array( ref.stream()
		    .collect( Collectors.groupingBy( item -> caseSensitive ? item : Key.of( item ), Collectors.counting() ) )
		    .keySet()
		    .stream()
		    .map( item -> StringCaster.cast( item ) )
		    .toArray()
		);
		// Our collector HashMap didn't maintain order so we need to restore it
		distinct.sort( ( a, b ) -> Compare.invoke( ref.findIndex( a ), ref.findIndex( b ) ) );
		return distinct;
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
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {

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

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.ARRAY );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		return DynamicInteropService.invoke( this, name.getName(), safe, positionalArguments );
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

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.ARRAY );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicInteropService.invoke( this, name.getName(), safe, namedArguments );
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
			throw new BoxRuntimeException( String.format(
			    "Array cannot be indexed by a number smaller than 1"
			) );
		}
		// Disallow out of bounds indexes foo[5]
		if ( index > size ) {
			throw new BoxRuntimeException( String.format(
			    "Array index [%s] is out of bounds for an array of length [%s]", index, size
			) );
		}
		return index;
	}

	public static int validateAndGetIntForAssign( Key key, int size, boolean isNative ) {
		Integer index = getIntFromKey( key, false );

		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			throw new BoxRuntimeException( String.format(
			    "Array cannot be assigned by a number smaller than 1"
			) );
		}

		if ( isNative ) {
			// Disallow out of bounds indexes foo[5]
			if ( index > size ) {
				throw new BoxRuntimeException( String.format(
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
				throw new BoxRuntimeException( String.format(
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
				throw new BoxRuntimeException( String.format(
				    "Array index [%s] is invalid.  Index must be an integer.", dIndex
				) );
			}
		}
		return index;
	}

}
