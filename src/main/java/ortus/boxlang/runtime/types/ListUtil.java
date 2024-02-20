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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle basic list operations. I assume 1-based indexes. All casting to strings must be done prior to calling these methods.
 */
public class ListUtil {

	public static final String	DEFAULT_DELIMITER	= ",";

	public static final Struct	sortDirectives		= new Struct(
	    new HashMap<Key, Comparator<Object>>() {

		    {
			    put( Key.of( "numericAsc" ), ( a, b ) -> Compare.invoke( a, b, false ) );
			    put( Key.of( "numericDesc" ), ( b, a ) -> Compare.invoke( a, b, true ) );
			    put( Key.of( "textAsc" ), ( a, b ) -> Compare.invoke( a, b, true ) );
			    put( Key.of( "textDesc" ), ( b, a ) -> Compare.invoke( a, b, true ) );
			    put( Key.of( "textNoCaseAsc" ), ( a, b ) -> Compare.invoke( a, b, false ) );
			    put( Key.of( "textNoCaseDesc" ), ( b, a ) -> Compare.invoke( a, b, false ) );
		    }
	    }
	);

	/**
	 * Turns a list into a string
	 *
	 * @param list      The list to turn into a string
	 * @param delimiter The delimiter to use
	 *
	 * @return The string representation
	 */
	public static String asString( Array list, String delimiter ) {
		return list.stream()
		    .map( StringCaster::cast )
		    .collect( Collectors.joining( delimiter ) );
	}

	/**
	 * Turns a string in to an Array
	 *
	 * @param string    The string to turn into a list
	 * @param delimiter The delimiter to use
	 *
	 * @return The Java List representation
	 */
	public static Array asList( String list, String delimiter ) {
		return asList( list, delimiter, false, false );
	}

	/**
	 * Creates an array from a delimited list
	 *
	 * @param list           The string lists
	 * @param delimiter      The delimiter(s) of the list
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return
	 */
	public static Array asList(
	    String list,
	    String delimiter,
	    Boolean includeEmpty,
	    Boolean wholeDelimiter ) {

		String[] result = null;
		if ( delimiter.length() == 0 ) {
			result = list.split( "" );
		} else if ( wholeDelimiter ) {
			if ( includeEmpty ) {
				result = StringUtils.splitByWholeSeparatorPreserveAllTokens( list, delimiter );
			} else {
				result = StringUtils.splitByWholeSeparator( list, delimiter );
			}
		} else if ( includeEmpty ) {
			result = StringUtils.splitPreserveAllTokens( list, delimiter );
		} else {
			result = StringUtils.split( list, delimiter );
		}
		return new Array( result );
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
		return indexOf( list, value, delimiter, false, false );
	}

	/**
	 * Find the index of a value in a list
	 *
	 * @param list           The list to search
	 * @param value          The value to search for
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The (1-based) index of the value or 0 if not found
	 */
	public static int indexOf( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return asList( list, delimiter, includeEmpty, wholeDelimiter ).findIndex( value, true );
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
		return indexOfNoCase( list, value, delimiter, false, false );
	}

	/**
	 * Find the index of a value in a list case insensitive
	 *
	 * @param list           The list to search
	 * @param value          The value to search for
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The (1-based) index of the value or 0 if not found
	 */
	public static int indexOfNoCase( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return asList( list, delimiter, includeEmpty, wholeDelimiter ).findIndex( value, false );
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
		return contains( list, value, delimiter, false, false );
	}

	/**
	 * Determine if a value is in a list
	 *
	 * @param list           The list to search
	 * @param value          The value to search for
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return True if the value is in the list
	 */
	public static Boolean contains( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return indexOf( list, value, delimiter, includeEmpty, wholeDelimiter ) > 0;
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
		return containsNoCase( list, value, delimiter, false, false );
	}

	/**
	 * Determine if a value is in a list
	 *
	 * @param list           The list to search
	 * @param value          The value to search for
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return True if the value is in the list
	 */
	public static Boolean containsNoCase( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return asList( list, delimiter, includeEmpty, wholeDelimiter ).findIndex( value, false ) > 0;
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
		return getAt( list, index, delimiter, false, false );
	}

	/**
	 * Get an item at a specific (1-based) index
	 *
	 * @param list           The list to search
	 * @param index          The index to get
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The value at the index if found
	 */
	public static String getAt( String list, int index, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return StringCaster.cast( asList( list, delimiter, includeEmpty, wholeDelimiter ).getAt( index ) );
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
		return setAt( list, index, value, delimiter, false, false );
	}

	/**
	 * Set an item at a specific (1-based) index
	 *
	 * @param list           The list to set into
	 * @param index          The index to set
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The new list
	 */
	public static String setAt( String list, int index, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return asString( asList( list, delimiter, includeEmpty, wholeDelimiter ).setAt( index, value ), delimiter );
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
		return append( list, value, delimiter, false, false );
	}

	/**
	 * Append an item to the end of a list
	 *
	 * @param list           The list to append to
	 * @param value          The value to append
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The new list
	 */
	public static String append( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		Array jList = asList( list, delimiter, includeEmpty, wholeDelimiter );
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
		return prepend( list, value, delimiter, false, false );
	}

	/**
	 * Prepend an item to the beginning of a list
	 *
	 * @param list           The list to prepend to
	 * @param value          The value to prepend
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The new list
	 */
	public static String prepend( String list, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		Array jList = asList( list, delimiter, includeEmpty, wholeDelimiter );
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
		return insertAt( list, index, value, delimiter, false, false );
	}

	/**
	 * Insert an item at a specific (1-based) index
	 *
	 * @param list           The list to insert into
	 * @param index          The index to insert at
	 * @param value          The value to insert
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The new list
	 */
	public static String insertAt( String list, int index, String value, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		Array jList = asList( list, delimiter, includeEmpty, wholeDelimiter );
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
		return deleteAt( list, index, delimiter, false, false );
	}

	/**
	 * Remove an item at a specific (1-based) index
	 *
	 * @param list           The list to remove from
	 * @param index          The index to remove
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return The new list
	 */
	public static String deleteAt( String list, int index, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter ) {
		return asString(
		    asList( list, delimiter, includeEmpty, wholeDelimiter ).deleteAt( index ),
		    delimiter
		);
	}

	/**
	 * De-duplicates a list
	 *
	 * @param list          The list to remove from
	 * @param delimiter     The delimiter to use
	 * @param caseSensitive Whether the perform the deduplication case-insenstively
	 *
	 * @return The new list
	 */
	public static String removeDuplicates( String list, String delimiter, Boolean caseSensitive ) {
		return asString(
		    asList( list, delimiter ).removeDuplicates( caseSensitive ),
		    delimiter
		);
	}

	/**
	 * Method to invoke a function for every iteration of the array
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static void each(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		IntConsumer	exec		= idx -> callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		IntStream	intStream	= array.intStream();
		if ( !parallel ) {
			intStream.forEach( exec );
		} else if ( ordered ) {
			AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> array.intStream().parallel().forEachOrdered( exec ) );
		} else {
			AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> array.intStream().parallel().forEach( exec ) );
		}

	}

	/**
	 * Method to test if any item in the array meets the criteria in the callback
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean some(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		IntPredicate	test		= idx -> ( boolean ) callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		IntStream		intStream	= array.intStream();

		return !parallel
		    ? ( Boolean ) intStream.anyMatch( test )
		    : ( Boolean ) AsyncService.buildExecutor(
		        "ArraySome_" + UUID.randomUUID().toString(),
		        AsyncService.ExecutorType.FORK_JOIN,
		        maxThreads
		    ).submitAndGet( () -> array.intStream().parallel().anyMatch( test ) );

	}

	/**
	 * Method to test if any item in the array meets the criteria in the callback
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean every(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		IntPredicate	test		= idx -> ( boolean ) callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		IntStream		intStream	= array.intStream();

		return !parallel
		    ? intStream.dropWhile( test ).toArray().length == 0
		    : BooleanCaster.cast(
		        AsyncService.buildExecutor(
		            "ArrayEvery_" + UUID.randomUUID().toString(),
		            AsyncService.ExecutorType.FORK_JOIN,
		            maxThreads
		        ).submitAndGet( () -> array.intStream().parallel().dropWhile( test ).toArray().length == 0 )
		    );

	}

	/**
	 * Method to filter an list with a function callback and context
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	public static Array filter(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		IntPredicate	test		= idx -> ( boolean ) callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		IntStream		intStream	= array.intStream();

		return ArrayCaster.cast(
		    !parallel
		        ? intStream
		            .filter( test )
		            .mapToObj( array::get )
		            .toArray()

		        : AsyncService.buildExecutor(
		            "ArrayFilter_" + UUID.randomUUID().toString(),
		            AsyncService.ExecutorType.FORK_JOIN,
		            maxThreads
		        ).submitAndGet( () -> array.intStream().parallel().filter( test ).mapToObj( array::get ).toArray() )
		);

	}

	/**
	 * Method to filter an list with a function callback and context
	 *
	 * If parallel we create a fork join pool. If no max threads is specified it uses the {@link java.util.concurrent.ForkJoinPool#commonPool}
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	public static Array sort(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext ) {

		array.sort(
		    ( a, b ) -> IntegerCaster.cast( callbackContext.invokeFunction( callback, new Object[] { a, b } ) )
		);
		return array;
	}

	/**
	 * Method to filter an list with a function callback and context
	 *
	 * If parallel we create a fork join pool. If no max threads is specified it uses the {@link java.util.concurrent.ForkJoinPool#commonPool}
	 *
	 * @param array           The array object to filter
	 * @param sortType        The textual sort directive
	 * @param localeSensitive Whether to use locale-specific comparisons
	 *
	 * @return A filtered array
	 */
	@SuppressWarnings( "unchecked" )
	public static Array sort(
	    Array array,
	    String sortType,
	    String sortOrder,
	    Locale locale ) {

		Key sortKey = Key.of( sortType + sortOrder );

		if ( !sortDirectives.containsKey( sortKey ) ) {
			throw new BoxRuntimeException( "You must supply either a sortOrder or callback" );
		}

		array.sort( ( Comparator<Object> ) sortDirectives.get( sortKey ) );

		return array;

	}

	public static Array trim( Array array ) {
		int fromIndex = 0;
		for ( int i = 0; i < array.size(); i++ ) {
			fromIndex = i;
			if ( StringCaster.cast( array.get( i ) ).length() > 0 )
				break;
		}

		int toIndex = 0;
		for ( int i = array.size() - 1; i >= 0; i-- ) {
			toIndex = i;
			if ( StringCaster.cast( array.get( i ) ).length() > 0 )
				break;
		}

		return Array.copyOf( array.subList( fromIndex, toIndex + 1 ) );
	}

	/**
	 * Maps an existing array to a new array
	 *
	 * @param array           The array object to map
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the map in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Array map(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		java.util.function.IntFunction<Object>	mapper		= idx -> ( Object ) callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		IntStream								intStream	= array.intStream();
		if ( !parallel ) {
			return new Array( intStream.mapToObj( mapper ).toArray() );
		} else {
			return ArrayCaster.cast( AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> new Array( array.intStream().parallel().mapToObj( mapper ).toArray() ) )
			);
		}

	}

	/**
	 * Method reduce an array
	 *
	 * @param array           The array to reduce
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param initialValue    the initial value
	 *
	 * @return the new object reduction
	 */
	public static Object reduce(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Object initialValue ) {

		BiFunction<Object, Integer, Object> reduction = ( acc, idx ) -> callbackContext.invokeFunction( callback,
		    new Object[] { acc, array.get( idx ), idx + 1, array } );

		return array.intStream()
		    .boxed()
		    .reduce(
		        initialValue,
		        reduction,
		        ( acc, intermediate ) -> acc
		    );

	}
}
