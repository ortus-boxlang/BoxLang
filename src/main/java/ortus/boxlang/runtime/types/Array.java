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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class Array implements List<Object>, IType, IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private final List<Object> wrapped;

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
		return wrapped.add( e );
	}

	public boolean remove( Object o ) {
		synchronized ( wrapped ) {
			return wrapped.remove( o );
		}
	}

	public boolean containsAll( Collection<?> c ) {
		return wrapped.containsAll( c );
	}

	public boolean addAll( Collection<? extends Object> c ) {

		synchronized ( wrapped ) {
			return wrapped.addAll( c );
		}
	}

	public boolean addAll( int index, Collection<? extends Object> c ) {
		synchronized ( wrapped ) {
			return wrapped.addAll( index, c );
		}
	}

	public boolean removeAll( Collection<?> c ) {
		return wrapped.removeAll( c );
	}

	public boolean retainAll( Collection<?> c ) {
		return wrapped.retainAll( c );
	}

	public void clear() {
		wrapped.clear();
	}

	public Object get( int index ) {
		return wrapped.get( index );
	}

	public Object set( int index, Object element ) {
		return wrapped.set( index, element );
	}

	public void add( int index, Object element ) {
		synchronized ( wrapped ) {
			wrapped.add( index, element );
		}
	}

	public Object remove( int index ) {
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
		CastAttempt<Double> indexAtt = DoubleCaster.attempt( key.getName() );
		if ( !indexAtt.wasSuccessful() ) {
			throw new ExpressionException( String.format(
			    "Array cannot be assigned with key %s", key.getName()
			) );
		}
		Double	dIndex	= indexAtt.get();
		Integer	index	= dIndex.intValue();
		// Dissallow non-integer indexes foo[1.5]
		if ( index.doubleValue() != dIndex ) {
			throw new ExpressionException( String.format(
			    "Array index [%s] is invalid.  Index must be an integer.", dIndex
			) );
		}
		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			throw new ExpressionException( String.format(
			    "Array cannot be assigned by a number smaller than 1"
			) );
		}
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

		CastAttempt<Double> indexAtt = DoubleCaster.attempt( key.getName() );
		if ( !indexAtt.wasSuccessful() ) {
			if ( safe ) {
				return null;
			}
			throw new ExpressionException( String.format(
			    "Array cannot be deferenced by key %s", key.getName()
			) );
		}
		Double	dIndex	= indexAtt.get();
		Integer	index	= dIndex.intValue();
		// Dissallow non-integer indexes foo[1.5]
		if ( index.doubleValue() != dIndex ) {
			if ( safe ) {
				return null;
			}
			throw new ExpressionException( String.format(
			    "Array index [%s] is invalid.  Index must be an integer.", dIndex
			) );
		}
		// Dissallow negative indexes foo[-1]
		if ( index < 1 ) {
			if ( safe ) {
				return null;
			}
			throw new ExpressionException( String.format(
			    "Array cannot be indexed by a number smaller than 1"
			) );
		}
		// Disallow out of bounds indexes foo[5]
		if ( index > wrapped.size() ) {
			if ( safe ) {
				return null;
			}
			throw new ExpressionException( String.format(
			    "Array index [%s] is out of bounds for an array of length [%s]", index, wrapped.size()
			) );
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

}
