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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * A special array which also tracks the delimiters between each value form the list which was parsed to create it
 * All modifications to the array, will also modify the delimiters to keep them in sync.
 * 
 * When re-arranging the array, the values will generally be associated with the delimiter which precedes them.
 * 
 * The first item in the array will never have a delimiter associated with it.
 * 
 * There are new add() methods to add a value with a delimiter, but if adding values via the standard `List.add()` method,
 * the current delimiter will be used, which will be the last delimiter used OR can be set via the `.withDelimiter( "|" )` method.
 * 
 * The current delimiter that the withDelimiter() method sets, will always be considered a whole delimiter, so if it has more than one character,
 * the entire string will be used. Even if multiple delimiter chars were used to parse the original string, you can only set a single current delimiter
 * to be used for adding additional items.
 *
 * To get a stream of values with the corresponding delimiters, use the `toElementDelimiterPairs()` method. This will allow you to
 * sort, or otherwise manipulate the values while keeping the delimiters in sync.
 * 
 * The `add()` `addAll()` methods will also accept an `ElementDelimiterPair` which contains the element and the delimiter to use.
 * You can also collect a stream of `ElementDelimiterPair` objects using the `BLCollector.toArray( DelimitedArray.class )` method
 * and the stream of `ElementDelimiterPair` objects will be reassmebled into a new `DelimitedArray` with the delimiters intact.
 */
public class DelimitedArray extends Array {

	/**
	 * Serialization ID
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * A list of delimiters which will always contain this.size()-1 items
	 */
	private List<String>		delimiters			= new ArrayList<>();

	/**
	 * The current delimiter to use when an item is added via the `List.add()` method.
	 */
	private String				currentDelimiter	= ListUtil.DEFAULT_DELIMITER;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create default array
	 */
	public DelimitedArray() {
		this( new String[] {} );
	}

	/**
	 * Constructor
	 */
	public DelimitedArray( String[] dataAndDelimiters ) {
		super();
		if ( dataAndDelimiters.length == 0 ) {
			return; // No data, no delimiters
		}
		if ( dataAndDelimiters.length == 2 ) {
			throw new BoxRuntimeException( "DelimitedArray cannot be created with only two elements." );
		}
		if ( dataAndDelimiters.length % 2 == 0 ) {
			throw new BoxRuntimeException( "DelimitedArray must have an odd number of elements, with values and delimiters alternating." );
		}
		// Odd items go into array, even items go into delimiters
		for ( int i = 0; i < dataAndDelimiters.length; i++ ) {
			if ( i % 2 == 0 ) {
				// Even index, add to array
				super.add( dataAndDelimiters[ i ] );
			} else {
				// Odd index, add to delimiters
				delimiters.add( dataAndDelimiters[ i ] );
				currentDelimiter = dataAndDelimiters[ i ];
			}
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Static convenience methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create an Array from a list of values and delimiters. Each value is passed in as a separate argument
	 *
	 * @param values The values to create the Array from
	 *
	 * @return The Array
	 */
	public static DelimitedArray of( String... values ) {
		return new DelimitedArray( values );
	}

	/**
	 * --------------------------------------------------------------------------
	 * List Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public boolean add( Object e ) {
		if ( e instanceof ElementDelimiterPair edp ) {
			return add( edp );
		}
		return add( e, currentDelimiter );
	}

	public boolean add( ElementDelimiterPair e ) {
		return add( e.element, e.delimiter );
	}

	/*
	 * Get the element at the specified index wrapped as some sort of data which
	 * the add() methods can use when re-adding. This allows subclasses of the Array
	 * such as the DelimitedArray to wrap the data in a delimiter
	 */
	@Override
	public Object getData( int index ) {
		// First element has no delimiter
		return new ElementDelimiterPair( wrapped.get( index ), index > 0 ? delimiters.get( index - 1 ) : null );
	}

	/**
	 * Given a new value, copy any additional data from the old value at that index
	 * This is used by the DelimitedArray to copy the delimiter from the old value
	 * 
	 * @param index    The index to copy the data from
	 * @param newValue The new value to set at the index
	 * 
	 * @return The new value to set at the index, possibly modified with additional data
	 */
	public Object copyData( int index, Object newValue ) {
		return new ElementDelimiterPair(
		    newValue,
		    ( ( ElementDelimiterPair ) getData( index ) ).delimiter
		);
	}

	/**
	 * Adds an element to the end of the list with the specified delimiter
	 * 
	 * @param e         The element to add
	 * @param delimiter The delimiter to use
	 * 
	 * @return true if the element was added, false otherwise
	 */
	public boolean add( Object e, String delimiter ) {
		if ( delimiter == null ) {
			delimiter = currentDelimiter;
		}
		synchronized ( wrapped ) {
			if ( size() > 0 ) {
				delimiters.add( delimiter );
			}
			currentDelimiter = delimiter;
			return super.add( e );
		}
	}

	@Override
	public void add( int index, Object element ) {
		if ( element instanceof ElementDelimiterPair edp ) {
			add( index, edp );
			return;
		}
		add( index, element, currentDelimiter );
	}

	public void add( int index, ElementDelimiterPair element ) {
		add( index, element.element, element.delimiter );
	}

	/**
	 * Adds an element at the specified index with the specified delimiter
	 * 
	 * @param index     The index at which to add the element
	 * @param element   The element to add
	 * @param delimiter The delimiter to use
	 */
	public void add( int index, Object element, String delimiter ) {
		if ( delimiter == null ) {
			delimiter = currentDelimiter;
		}
		synchronized ( wrapped ) {
			super.add( index, element );
			if ( size() > 1 ) {
				if ( index == 0 ) {
					delimiters.add( 0, delimiter );
				} else {
					delimiters.add( index - 1, delimiter );
				}
			}
			currentDelimiter = delimiter;
		}
	}

	@Override
	public boolean remove( Object o ) {
		synchronized ( wrapped ) {
			ListIterator<Object>	iterator	= wrapped.listIterator();
			int						index		= 0;
			while ( iterator.hasNext() ) {
				Object element = iterator.next();
				if ( element.equals( o ) ) {
					iterator.remove();
					// Remove the delimiter at the same index
					if ( size() > 0 ) {
						if ( index < delimiters.size() ) {
							// If the index is within the delimiters list, remove the delimiter
							delimiters.remove( index );
						} else if ( super.size() > 0 ) {
							// If the index is out of bounds, just remove the last delimiter
							delimiters.remove( index - 1 );
						}
					}
					return true;
				}
				index++;
			}
			return false;
		}
	}

	@Override
	public boolean addAll( Collection<? extends Object> c ) {
		return addAll( size(), c );
	}

	@Override
	public boolean addAll( int index, Collection<? extends Object> c ) {
		synchronized ( wrapped ) {
			boolean result = false;
			for ( Object item : c ) {
				add( index, item );
				index++;
				result = true;
			}
			return result;
		}
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		boolean result = false;
		synchronized ( wrapped ) {
			for ( Object item : c ) {
				boolean thisRemove = remove( item );
				result |= thisRemove;
				while ( thisRemove ) {
					thisRemove = remove( item );
				}
			}
		}
		return result;
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		synchronized ( wrapped ) {
			boolean result = false;
			for ( Object item : wrapped ) {
				if ( !c.contains( item ) ) {
					result |= remove( item );
				}
			}
			return result;
		}
	}

	@Override
	public void clear() {
		synchronized ( wrapped ) {
			wrapped.clear();
			delimiters.clear();
		}
	}

	@Override
	public Object remove( int index ) {
		synchronized ( wrapped ) {
			ListIterator<Object>	iterator	= wrapped.listIterator();
			int						i			= 0;
			while ( iterator.hasNext() ) {
				Object element = iterator.next();
				if ( i == index ) {
					iterator.remove();

					// Remove the delimiter at the same index
					if ( size() > 0 ) {
						if ( index < delimiters.size() ) {
							// If the index is within the delimiters list, remove the delimiter
							delimiters.remove( index );
						} else {
							// If the index is out of bounds, just remove the last delimiter
							delimiters.remove( index - 1 );
						}
					}
					return element;
				}
				i++;
			}
			return null;
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void sort( Comparator compareFunc ) {
		synchronized ( wrapped ) {
			if ( wrapped.size() <= 1 ) {
				return; // Nothing to sort
			}

			// Create pairs of (element, delimiter) to maintain relationship during sorting
			List<ElementDelimiterPair> pairs = Arrays.asList( toElementDelimiterPairs() );

			// Sort the pairs using the comparator on the elements
			pairs.sort( ( pair1, pair2 ) -> compareFunc.compare( pair1.element, pair2.element ) );

			// Extract sorted elements and delimiters back into their respective lists
			wrapped.clear();
			delimiters.clear();

			for ( int i = 0; i < pairs.size(); i++ ) {
				ElementDelimiterPair pair = pairs.get( i );
				wrapped.add( pair.element );
				String delimiter = pair.delimiter != null ? pair.delimiter : currentDelimiter;

				// Add delimiter if it exists
				if ( size() > 1 ) {
					delimiters.add( delimiter );
				}
			}

		}
	}

	/**
	 * Helper class to maintain the relationship between an element and its delimiter during sorting
	 */
	public static record ElementDelimiterPair( Object element, String delimiter ) {
	}

	/**
	 * Reverses the elements in the underlying list
	 */
	public DelimitedArray reverse() {
		super.reverse();
		Collections.reverse( delimiters );
		return this;
	}

	@Override
	public int computeHashCode( Set<IType> visited ) {
		if ( visited.contains( this ) ) {
			return 0;
		}
		visited.add( this );
		int	result	= 1;
		int	index	= 0;
		for ( Object value : wrapped.toArray() ) {
			if ( value instanceof IType ) {
				result = 31 * result + ( ( IType ) value ).computeHashCode( visited );
			} else {
				result = 31 * result + ( value == null ? 0 : value.hashCode() );
			}
			// incorporate the delimiter into the hash code
			if ( index < delimiters.size() ) {
				result = 31 * result + delimiters.get( index ).hashCode();
			}
			index++;
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
		return Arrays.stream( toDataAndDelimiters() )
		    .map( item -> item == null ? "" : item.toString() )
		    .collect( Collectors.joining( "" ) );

	}

	/**
	 * Convert the Array to an array of strings, alternating between data and delimiters
	 * The first element will be the data, the second will be the delimiter, and so on
	 * 
	 * @return An array of strings containing the data and delimiters
	 */
	public String[] toDataAndDelimiters() {
		String[]	result	= new String[ wrapped.size() + delimiters.size() ];
		int			i		= 0;
		for ( int j = 0; j < wrapped.size(); j++ ) {
			result[ i++ ] = wrapped.get( j ).toString();
			if ( j < delimiters.size() ) {
				result[ i++ ] = delimiters.get( j );
			}
		}
		return result;
	}

	/**
	 * Convert the Array to an array of ElementDelimiterPair objects, which contain the element and its delimiter
	 * All elements but the first will have a delimiter, the first element will have a null delimiter
	 * 
	 * @return An array of ElementDelimiterPair objects
	 */
	public ElementDelimiterPair[] toElementDelimiterPairs() {
		ElementDelimiterPair[] result = new ElementDelimiterPair[ wrapped.size() ];
		for ( int i = 0; i < wrapped.size(); i++ ) {
			String delimiter = i != 0 ? delimiters.get( i - 1 ) : null;
			result[ i ] = new ElementDelimiterPair( wrapped.get( i ), delimiter );
		}
		return result;
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
		return toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * One-based index methods for getters/setters and filtering
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns a new array removing all of the duplicates - either caseSenstively or not
	 *
	 * @param caseSensitive whether to perform the deduplication caseSenstively
	 *
	 * @return The new array
	 */
	public DelimitedArray removeDuplicates( Boolean caseSensitive ) {
		DelimitedArray								result	= new DelimitedArray();
		LinkedHashMap<String, ElementDelimiterPair>	seen	= new LinkedHashMap<>();
		Arrays.stream( toElementDelimiterPairs() )
		    .forEach( pair -> {
			    String key = caseSensitive ? pair.element.toString() : pair.element.toString().toLowerCase();
			    if ( !seen.containsKey( key ) ) {
				    seen.put( key, pair );
			    }
		    } );

		seen.values().forEach( pair -> result.add( pair.element, pair.delimiter ) );
		return result;
	}

	/**
	 * Sets the current delimiter to use when adding new elements
	 * 
	 * @param delimiter The delimiter to use for new elements
	 * 
	 * @return The current DelimitedArray instance for method chaining
	 */
	public DelimitedArray withDelimiter( String delimiter ) {
		return withDelimiter( delimiter, true );
	}

	/**
	 * Sets the current delimiter to use when adding new elements
	 * If the delimiter is not multi-character, it will only use the first character of the delimiter
	 * 
	 * @param delimiter               The delimiter to use for new elements
	 * @param multiCharacterDelimiter Whether the delimiter is multi-character
	 * 
	 * @return The current DelimitedArray instance for method chaining
	 */
	public DelimitedArray withDelimiter( String delimiter, boolean multiCharacterDelimiter ) {
		if ( !multiCharacterDelimiter && delimiter.length() > 1 ) {
			// Just take first character of the delimiter
			delimiter = delimiter.substring( 0, 1 );
		}
		currentDelimiter = delimiter;
		return this;
	}

}
