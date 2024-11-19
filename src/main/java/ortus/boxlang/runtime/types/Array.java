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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BoxMemberExpose;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.meta.IListenable;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * The primary array class in BoxLang. This class wraps a Java List and provides additional functionality for BoxLang.
 *
 * BoxLang indices are one-based, so the first element is at index 1, not 0.
 */
public class Array implements List<Object>, IType, IReferenceable, IListenable, Serializable {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Array					EMPTY				= new UnmodifiableArray();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This is the array we are wrapping and enhancing for BoxLang
	 */
	protected final List<Object>				wrapped;

	/**
	 * Metadata object
	 */
	public transient BoxMeta					$bx;

	/**
	 * Used to track change listeners. Intitialized on-demand
	 */
	private transient Map<Key, IChangeListener>	listeners;

	/**
	 * Function service
	 */
	private static FunctionService				functionService		= BoxRuntime.getInstance().getFunctionService();

	/**
	 * Serialization ID
	 */
	private static final long					serialVersionUID	= 1L;

	/**
	 * Public property to determine if the parse array contains delimiters
	 */
	public boolean								containsDelimiters	= false;

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
		this.wrapped = Collections.synchronizedList( new ArrayList<Object>( initialCapactity ) );
	}

	/**
	 * Constructor to create an Array from a Java array
	 *
	 * @param arr The array to create the Array from
	 */
	public Array( Object[] arr ) {
		this.wrapped = Collections.synchronizedList( new ArrayList<Object>( Arrays.asList( arr ) ) );
	}

	/**
	 * Constructor to create an Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	@SuppressWarnings( "unchecked" )
	public Array( List<? extends Object> list ) {
		this.wrapped = ( List<Object> ) list;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static convenience methods
	 * --------------------------------------------------------------------------
	 * These are mostly builders from other types
	 */

	/**
	 * Convert a list to an array, using , as the delimiter, making sure each element is trimmed
	 *
	 * @param list The list to convert
	 *
	 * @return The array
	 */
	public static Array fromString( String list ) {
		return fromString( list, "," );
	}

	/**
	 * Convert a list to an array, making sure each element is trimmed
	 *
	 * @param list      The list to convert
	 * @param delimiter The delimiter to use, comma by default
	 *
	 * @return The array
	 */
	public static Array fromString( String list, String delimiter ) {
		if ( delimiter == null ) {
			delimiter = ",";
		}

		// Split the string by comma and trim the values
		return Arrays.stream( list.split( delimiter ) )
		    .map( String::trim )
		    .collect( BLCollector.toArray() );
	}

	/**
	 * Create an Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	public static Array fromList( List<? extends Object> list ) {
		return new Array( list );
	}

	/**
	 * Create an array from a Set<String>
	 *
	 * @param set The set to create the Array from
	 *
	 * @return The array
	 */
	public static Array fromSet( Set<? extends Object> set ) {
		return new Array( new ArrayList<>( set ) );
	}

	/**
	 * Create an Array from a Java array of boxed objects
	 *
	 * @param arr The array to create the Array from
	 */
	public static Array fromArray( Object[] arr ) {
		return new Array( arr );
	}

	/**
	 * Create an Array from a list of values. Each value is passed in as a separate argument
	 *
	 * @param values The values to create the Array from
	 *
	 * @return The Array
	 */
	@BoxMemberExpose
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
	@BoxMemberExpose
	public static Array copyOf( List<?> arr ) {
		Array newArr = new Array();
		// loop over list and add all elements
		for ( Object o : arr ) {
			newArr.add( o );
		}
		return newArr;
	}

	public Object toVarArgsArray( Class<?> varArgType ) {
		Object array = java.lang.reflect.Array.newInstance( varArgType, wrapped.size() );
		System.arraycopy( wrapped.toArray(), 0, array, 0, wrapped.size() );
		return array;
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

	public List<Object> toList() {
		return wrapped;
	}

	/**
	 * Because toList() can't be called from BL code due to the arrayToList() BIF
	 * 
	 * @return
	 */
	public List<Object> asList() {
		return wrapped;
	}

	public boolean add( Object e ) {
		synchronized ( wrapped ) {
			return wrapped.add( notifyListeners( wrapped.size(), e ) );
		}
	}

	public void add( int index, Object element ) {
		synchronized ( wrapped ) {
			wrapped.add( index, notifyListeners( index, element ) );
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

	/**
	 * Clears the contents contents of the array
	 */
	public void clear() {
		// TODO: deal with listeners
		synchronized ( wrapped ) {
			wrapped.clear();
		}
	}

	/*
	 * Get the element at the specified index
	 */
	public Object get( int index ) {
		return wrapped.get( index );
	}

	/**
	 * Set the element at the specified index
	 */
	public Object set( int index, Object element ) {
		return wrapped.set(
		    index,
		    notifyListeners( index, element )
		);
	}

	/**
	 * Remove an element at a specified index
	 */
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

	/**
	 * Sort the array using a comparator function
	 *
	 * @param compareFunc The object to be compared for equality with this list
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void sort( Comparator compareFunc ) {
		wrapped.sort( compareFunc );
	}

	/**
	 * Returns a stream of the array
	 *
	 * @return The stream
	 */
	@Override
	@BoxMemberExpose
	public Stream<Object> stream() {
		return wrapped.stream();
	}

	/**
	 * Returns a parallel stream of the array
	 *
	 * @return The parallel stream
	 */
	@Override
	@BoxMemberExpose
	public Stream<Object> parallelStream() {
		return wrapped.parallelStream();
	}

	/**
	 * Returns a IntStream representing the indexes of the array: 0 -> size()
	 */
	public IntStream intStream() {
		return IntStream.range( 0, size() );
	}

	/**
	 * Reverses the elements in the underlying list
	 */
	public Array reverse() {
		Collections.reverse( this.wrapped );
		return this;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Super class
	 *
	 * @param obj The object to compare
	 *
	 * @return Whether the objects are equal
	 */
	@Override
	@BoxMemberExpose
	public boolean equals( Object obj ) {
		return wrapped.equals( obj );
	}

	/**
	 * Array Hashcode
	 *
	 * @return The hashcode of the array
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
		for ( Object value : wrapped.toArray() ) {
			if ( value instanceof IType ) {
				result = 31 * result + ( ( IType ) value ).computeHashCode( visited );
			} else {
				result = 31 * result + ( value == null ? 0 : value.hashCode() );
			}
		}
		return result;
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
		    .map( line -> line.replaceAll( "(?m)^", "  " ) ) // Add an indent to the start of each line
		    .collect( java.util.stream.Collectors.joining( ",\n" ) ) );
		sb.append( "\n]" );
		return sb.toString();
	}

	/**
	 * Get the metadata object for this array
	 *
	 * @return The metadata object for the array
	 */
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
	 * @param index   The one-based index value to use for insertion
	 * @param element The object to insert
	 *
	 * @return The array
	 */
	public Array insertAt( int index, Object element ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index [" + index + "] out of bounds for list with " + wrapped.size() + " elements." );
		}
		add( index - 1, element );
		return this;
	}

	/**
	 * Appends a new object to the array
	 *
	 * @param element The object to insert
	 *
	 * @return The array
	 */
	public Array push( Object element ) {
		add( element );
		return this;
	}

	/**
	 * Retrieves and object using the one-based index value
	 *
	 * @param index The one-based index value to use for retrieval
	 *
	 * @return The object at the specified position
	 */
	public Object getAt( int index ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index [" + index + "] out of bounds for list with " + wrapped.size() + " elements." );
		}
		return get( index - 1 );
	}

	/**
	 * Sets an object using the one-based index value
	 *
	 * @param index   The one-based index value to use for setting
	 * @param element The object to set at the specified position
	 *
	 * @return The array
	 */
	public Array setAt( int index, Object element ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index [" + index + "] out of bounds for list with " + wrapped.size() + " elements." );
		}
		set( index - 1, element );
		return this;
	}

	/**
	 * Deletes an object from the array using the one-based index value
	 *
	 * @param index The one-based index value to use for deletion
	 *
	 * @return The array
	 */
	public Array deleteAt( int index ) {
		if ( index < 1 || index > wrapped.size() ) {
			throw new BoxRuntimeException( "Index [" + index + "] out of bounds for list with " + wrapped.size() + " elements." );
		}

		if ( containsDelimiters ) {
			index = index * 2;
		}

		synchronized ( wrapped ) {
			remove( index - 1 );
			if ( containsDelimiters && size() >= index - 1 ) {
				remove( index - 1 );
			}
			notifyListeners( index - 1, null );
		}
		return this;
	}

	/**
	 * Finds the first one-based index of a substring - either case sensitively or not.
	 *
	 * @param value         The value to be searched
	 * @param caseSensitive Whether the test should be case sensitive or not
	 *
	 * @return The one-based index value or zero if not found
	 */
	public int findIndexWithSubstring( Object value, Boolean caseSensitive ) {
		return intStream()
		    .filter(
		        i -> ( ( !caseSensitive
		            &&
		            StringUtils.containsAnyIgnoreCase( get( i ).toString(), value.toString() ) )
		            ||
		            ( caseSensitive
		                &&
		                get( i ).toString().contains( value.toString() ) ) )
		    )
		    .findFirst()
		    .orElse( -1 ) + 1;
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
	 * @param test    The function to use for filtering
	 * @param context The context in which the function is being executed
	 *
	 * @return The one-based index value or zero if not found
	 */
	public int findIndex( Function test, IBoxContext context ) {
		return intStream()
		    .filter( i -> BooleanCaster.cast(
		        test.requiresStrictArguments()
		            // Java Lambdas
		            ? context.invokeFunction( test, new Object[] { get( i ) } )
		            // BoxLang Functions, more args!!=
		            : context.invokeFunction( test, new Object[] { get( i ), i, this } )
		    ) )
		    .findFirst()
		    .orElse( -1 ) + 1;
	}

	/**
	 * Returns a new array removing all of the duplicates caseSenstively
	 *
	 * @return The new array
	 */
	public Array removeDuplicates() {
		return removeDuplicates( true );
	}

	/**
	 * Returns a new array removing all of the duplicates - either caseSenstively or not
	 *
	 * @param caseSensitive whether to perform the deduplication caseSenstively
	 *
	 * @return The new array
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
	 * Flags the array as containing delimiters - which may be used for list re-assembly
	 *
	 * @return
	 */
	public Array withDelimiters() {
		containsDelimiters = true;
		return this;

	}

	/**
	 * Make Unmodifiable
	 */
	public UnmodifiableArray toUnmodifiable() {
		return new UnmodifiableArray( this );
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
	 *
	 * @return The assigned value
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

		Integer index = Array.validateAndGetIntForDereference( key, wrapped.size(), safe );
		// non-existant indexes or keys which could not be turned into an int return null when dereferencing safely
		if ( safe && ( index == null || Math.abs( index ) > wrapped.size() || index == 0 ) ) {
			return null;
		}
		if ( index < 0 ) {
			return wrapped.get( wrapped.size() + index );
		}
		return wrapped.get( index - 1 );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param context             The context in which the dereference is being performed
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

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param context        The context in which the dereference is being performed
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

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
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

	/**
	 * Notify listeners of a change
	 *
	 * @param i     The index of the change
	 * @param value The value of the change
	 *
	 * @return The value after notifying listeners
	 */
	private Object notifyListeners( int i, Object value ) {
		if ( listeners == null ) {
			return value;
		}
		Key				key			= Key.of( i + 1 );
		IChangeListener	listener	= listeners.get( key );
		if ( listener == null ) {
			listener = listeners.get( IListenable.ALL_KEYS );
		}
		if ( listener == null ) {
			return value;
		}
		return listener.notify( key, value, i < wrapped.size() ? wrapped.get( i ) : null );
	}

	/**
	 * Initialize listeners map
	 */
	private void initListeners() {
		if ( listeners == null ) {
			listeners = new ConcurrentHashMap<>();
		}
	}

	/**
	 * Validate and get an integer for dereferencing
	 *
	 * @param key  The key to validate
	 * @param size The size of the array
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The index
	 */
	public static Integer validateAndGetIntForDereference( Key key, int size, boolean safe ) {
		Integer index = getIntFromKey( key, safe );

		// If we're dereferencing safely, anything goes.
		if ( safe ) {
			return index;
		}

		// assert: if safe was false, then index cannot be null here

		// negative indexes are allowed, and offset from the right had side of the array

		if ( index == 0 ) {
			throw new BoxRuntimeException( String.format(
			    "Arrays cannot be accesse by an index of 0.", index, size
			) );
		}

		// Disallow out of bounds indexes foo[5] or foo[-5]
		if ( Math.abs( index ) > size ) {
			throw new BoxRuntimeException( String.format(
			    "Array index [%s] is out of bounds for an array of length [%s]", index, size
			) );
		}
		return index;
	}

	public static int validateAndGetIntForAssign( Key key, int size, boolean isNative ) {
		// Since safe is false, we don't need to deal with null since getIntFromKey will throw an exception
		Integer index = getIntFromKey( key, false );

		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			throw new BoxRuntimeException(
			    "Array cannot be assigned by a number smaller than 1"
			);
		}

		// Disallow out of bounds indexes foo[5]
		if ( isNative && index > size ) {
			throw new BoxRuntimeException( String.format(
			    "Invalid index [%s] for Native Array, can't expand Native Arrays.  Current array length is [%s]", index,
			    size
			) );
		}

		return index;
	}

	/**
	 * Get an integer from a key, returning null if the key is not an integer unless safe is false.
	 * 
	 * @param key  The key to get the integer from
	 * @param safe Whether to return null if the key is not an integer
	 * 
	 * @return The integer or null
	 */
	public static Integer getIntFromKey( Key key, boolean safe ) {
		Integer index;

		// If key is int, use it directly
		if ( key instanceof IntKey intKey ) {
			index = intKey.getIntValue();
		} else {
			// If key is not an int, we must attempt to cast it
			CastAttempt<Number> indexAtt = NumberCaster.attempt( key.getName() );
			if ( !indexAtt.wasSuccessful() ) {
				if ( safe ) {
					return null;
				}
				throw new BoxRuntimeException( String.format(
				    "Array cannot be assigned with key %s", key.getName()
				) );
			}
			Number dIndex = indexAtt.get();
			index = dIndex.intValue();
			// Dissallow non-integer indexes foo[1.5]
			if ( index.doubleValue() != dIndex.doubleValue() ) {
				if ( safe ) {
					return null;
				}
				throw new BoxRuntimeException( String.format(
				    "Array index [%s] is invalid.  Index must be an integer.", dIndex
				) );
			}
		}
		return index;
	}

}
