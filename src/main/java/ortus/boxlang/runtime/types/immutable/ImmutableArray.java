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
package ortus.boxlang.runtime.types.immutable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

/**
 * Represents an immutable Array. All data you want needs to be passed in the constructor or
 * provided to a static creation method. Once instantiated, the Array cannot be modified. An
 * exception will be thrown if you invoke any mutator method.
 */
public class ImmutableArray extends Array implements IImmutable {

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create default array
	 */
	public ImmutableArray() {
		super( 10 );
	}

	/**
	 * Constructor to create array with an initial capacity
	 *
	 * @param initialCapactity The initialCapactity of Array to create
	 */
	public ImmutableArray( int initialCapactity ) {
		// Immutable array does not use syncronized list
		super( new ArrayList<Object>( initialCapactity ) );
	}

	/**
	 * Constructor to create a Array from a Java array
	 *
	 * @param arr The array to create the Array from
	 */
	public ImmutableArray( Object[] arr ) {
		super( arr );
	}

	/**
	 * Constructor to create a Array from a List
	 *
	 * @param list The List to create the Array from
	 */
	public ImmutableArray( List<? extends Object> list ) {
		this( list.size() );
		// add each item to this array
		for ( Object item : list ) {
			_add( item );
		}
	}

	/**
	 * To Mutable
	 *
	 * @return The mutable type
	 */
	@Override
	public Array toMutable() {
		return new Array( this.wrapped );
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
	public static ImmutableArray fromList( List<? extends Object> list ) {
		return new ImmutableArray( list );
	}

	/**
	 * Create a Array from a Java array
	 *
	 * @param arr The array to create the Array from
	 */
	public static ImmutableArray fromArray( Object[] arr ) {
		return new ImmutableArray( arr );
	}

	/**
	 * Create a Array from a list of values.
	 *
	 * @param values The values to create the Array from
	 *
	 * @return The Array
	 */
	public static ImmutableArray of( Object... values ) {
		return fromArray( values );
	}

	/**
	 * --------------------------------------------------------------------------
	 * List Interface Methods
	 * --------------------------------------------------------------------------
	 */

	private boolean _add( Object e ) {
		return wrapped.add( e );
	}

	public boolean add( Object e ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public boolean remove( Object o ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public boolean addAll( Collection<? extends Object> c ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public boolean addAll( int index, Collection<? extends Object> c ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public boolean removeAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public boolean retainAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public void clear() {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public Object remove( int index ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

	public int append( Object e ) {
		throw new UnmodifiableException( "Cannot modify immutable Array" );
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
		throw new UnmodifiableException( "Cannot modify immutable Array" );
	}

}
