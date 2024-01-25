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
import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle basic list operations. I assume 1-based indexes. All casting to strings must be done prior to calling these methods.
 */
public class ListUtil {

	/**
	 * Turns a list into a string
	 * 
	 * @param list      The list to turn into a string
	 * @param delimiter The delimiter to use
	 *
	 * @return The string representation
	 */
	public static String asString( List<String> list, String delimiter ) {
		return list.stream().collect( Collectors.joining( delimiter ) );
	}

	/**
	 * Turns a string into a Java List
	 * 
	 * @param string    The string to turn into a list
	 * @param delimiter The delimiter to use
	 *
	 * @return The Java List representation
	 */
	public static List<String> asList( String string, String delimiter ) {
		// TODO: include empty elements
		// TODO: Test multi-char delimiters
		return new ArrayList<>( List.of( string.split( delimiter ) ) );
	}

	/**
	 * Find the index of a value in a list
	 * 
	 * @param list      The list to search
	 * @param value     The value to search for
	 * @param delimiter The delimiter to use
	 *
	 * @return The (1-based) index of the value or 0 if not found
	 */
	public static int indexOf( String list, String value, String delimiter ) {
		return asList( list, delimiter ).indexOf( value ) + 1;
	}

	/**
	 * Find the index of a value in a list case insensitive
	 * 
	 * @param list      The list to search
	 * @param value     The value to search for
	 * @param delimiter The delimiter to use
	 *
	 * @return The (1-based) index of the value or 0 if not found
	 */
	public static int indexOfNoCase( String list, String value, String delimiter ) {
		return asList( list.toLowerCase(), delimiter ).indexOf( value.toLowerCase() ) + 1;
	}

	/**
	 * Determine if a value is in a list
	 * 
	 * @param list      The list to search
	 * @param value     The value to search for
	 * @param delimiter The delimiter to use
	 * 
	 * @return True if the value is in the list
	 */
	public static Boolean contains( String list, String value, String delimiter ) {
		return indexOf( list, value, delimiter ) > 0;
	}

	/**
	 * Determine if a value is in a list
	 * 
	 * @param list      The list to search
	 * @param value     The value to search for
	 * @param delimiter The delimiter to use
	 * 
	 * @return True if the value is in the list
	 */
	public static Boolean containsNoCase( String list, String value, String delimiter ) {
		return indexOfNoCase( list, value, delimiter ) > 0;
	}

	/**
	 * Get an item at a specific (1-based) index
	 * 
	 * @param list      The list to search
	 * @param index     The index to get
	 * @param delimiter The delimiter to use
	 * 
	 * @return The value at the index if found
	 */
	public static String getAt( String list, int index, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		// Throw if index is out of bounds
		if ( index < 1 || index > jList.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + jList.size() + " elements." );
		}
		return jList.get( index - 1 );
	}

	/**
	 * Set an item at a specific (1-based) index
	 * 
	 * @param list      The list to set into
	 * @param index     The index to set
	 * @param delimiter The delimiter to use
	 * 
	 * @return The new list
	 */
	public static String setAt( String list, int index, String value, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		// Throw if index is out of bounds
		if ( index < 1 || index > jList.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + jList.size() + " elements." );
		}
		jList.set( index - 1, value );
		return asString( jList, delimiter );
	}

	/**
	 * Append an item to the end of a list
	 * 
	 * @param list      The list to append to
	 * @param value     The value to append
	 * @param delimiter The delimiter to use
	 * 
	 * @return The new list
	 */
	public static String append( String list, String value, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		jList.add( value );
		return asString( jList, delimiter );
	}

	/**
	 * Prepend an item to the beginning of a list
	 * 
	 * @param list      The list to prepend to
	 * @param value     The value to prepend
	 * @param delimiter The delimiter to use
	 * 
	 * @return The new list
	 */
	public static String prepend( String list, String value, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		jList.add( 0, value );
		return asString( jList, delimiter );
	}

	/**
	 * Insert an item at a specific (1-based) index
	 * 
	 * @param list      The list to insert into
	 * @param index     The index to insert at
	 * @param value     The value to insert
	 * @param delimiter The delimiter to use
	 * 
	 * @return The new list
	 */
	public static String insertAt( String list, int index, String value, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		// Throw if index is out of bounds
		if ( index < 1 || index > jList.size() + 1 ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + jList.size() + " elements." );
		}
		jList.add( index - 1, value );
		return asString( jList, delimiter );
	}

	/**
	 * Remove an item at a specific (1-based) index
	 * 
	 * @param list      The list to remove from
	 * @param index     The index to remove
	 * @param delimiter The delimiter to use
	 * 
	 * @return The new list
	 */
	public static String deleteAt( String list, int index, String delimiter ) {
		List<String> jList = asList( list, delimiter );
		// Throw if index is out of bounds
		if ( index < 1 || index > jList.size() ) {
			throw new BoxRuntimeException( "Index out of bounds for list with " + jList.size() + " elements." );
		}
		jList.remove( index - 1 );
		return asString( jList, delimiter );
	}

}