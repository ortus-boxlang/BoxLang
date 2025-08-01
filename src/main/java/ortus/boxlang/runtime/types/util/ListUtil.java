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
package ortus.boxlang.runtime.types.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.StringCompare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Utility class providing comprehensive list manipulation operations for BoxLang.
 *
 * This class handles conversion between string-based delimited lists and BoxLang Arrays,
 * providing functionality for searching, modifying, and processing list data. It supports
 * various delimiter configurations including single character, multi-character, and
 * whole delimiter matching.
 *
 * Key features:
 * - List to Array conversion with flexible delimiter handling
 * - Search operations (indexOf, contains) with case-sensitive and case-insensitive variants
 * - List modification (append, prepend, insert, delete, set)
 * - Functional programming operations (each, filter, map, reduce, some, every)
 * - Sorting with custom comparators and predefined sort directives
 * - Parallel processing support for performance-critical operations
 * - Deduplication and trimming utilities
 *
 * The class uses a default comma delimiter but supports any custom delimiter configuration.
 * All list operations maintain 1-based indexing to match BoxLang conventions.
 *
 * Thread Safety: This utility class is stateless and thread-safe for concurrent use.
 *
 * @since 1.2.0
 */
public class ListUtil {

	/**
	 * Default delimiter used for list operations.
	 * This is a comma (",") by default, but can be overridden in methods that accept a custom delimiter.
	 */
	public static final String	DEFAULT_DELIMITER	= ",";

	/**
	 * Regular expression pattern to escape special characters in a delimiter for regex operations.
	 * This is used to ensure that delimiters containing regex special characters are treated literally.
	 */
	public static final Pattern	SPECIAL_REGEX_CHARS	= Pattern.compile( "[{}()\\[\\].+*?^$\\\\|]" );

	/**
	 * Sort directives for sorting lists.
	 */
	public static final Struct	sortDirectives;

	static {
		sortDirectives = new Struct(
		    new HashMap<Key, Comparator<Object>>() {

			    {
				    put( Key.of( "numericAsc" ), ( a, b ) -> Compare.invoke( a, b, false ) );
				    put( Key.of( "numericDesc" ), ( b, a ) -> Compare.invoke( a, b, true ) );
				    put( Key.of( "textAsc" ), ( a, b ) -> StringCompare.invoke( StringCaster.cast( a ), StringCaster.cast( b ), true ) );
				    put( Key.of( "textDesc" ), ( b, a ) -> StringCompare.invoke( StringCaster.cast( a ), StringCaster.cast( b ), true ) );
				    put( Key.of( "textNoCaseAsc" ), ( a, b ) -> StringCompare.invoke( StringCaster.cast( a ), StringCaster.cast( b ), false ) );
				    put( Key.of( "textNoCaseDesc" ), ( b, a ) -> StringCompare.invoke( StringCaster.cast( a ), StringCaster.cast( b ), false ) );
			    }
		    }
		);
	}

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
		    // map nulls to empty string since the string caster won't do this
		    .map( s -> s == null ? "" : s )
		    .map( v -> v instanceof Array arrayValue ? "'" + arrayValue.toString() + "'" : v )
		    .map( StringCaster::cast )
		    .collect( Collectors.joining( list.containsDelimiters ? "" : delimiter ) );
	}

	/**
	 * Turns a string in to an Array
	 *
	 * @param list      The list to turn into a string
	 * @param delimiter The delimiter to use
	 *
	 * @return The Java List representation
	 */
	public static Array asList( String list, String delimiter ) {
		return asList( list, delimiter, false, false );
	}

	/**
	 * Creates an array from a delimited list. All items are trimmed.
	 *
	 * @param list           The string lists
	 * @param delimiter      The delimiter(s) of the list
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return A BoxLang array.
	 */
	public static Array asList(
	    String list,
	    String delimiter,
	    Boolean includeEmpty,
	    Boolean wholeDelimiter ) {
		return asList( list, delimiter, includeEmpty, wholeDelimiter, false );
	}

	/**
	 * Creates an array from a delimited list. All items are trimmed.
	 *
	 * @param list           The string lists
	 * @param delimiter      The delimiter(s) of the list
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 *
	 * @return A BoxLang array.
	 */
	public static Array asList(
	    String list,
	    String delimiter,
	    Boolean includeEmpty,
	    Boolean wholeDelimiter,
	    Boolean preserveDelimiters ) {

		if ( list == null || list.isEmpty() ) {
			return new Array();
		}

		String[] result = null;
		if ( delimiter.length() == 0 ) {
			result = list.split( "" );
		} else if ( wholeDelimiter ) {
			if ( includeEmpty ) {
				result = StringUtils.splitByWholeSeparatorPreserveAllTokens( list, delimiter );
			} else {
				result = StringUtils.splitByWholeSeparator( list, delimiter );
			}
		} else if ( delimiter.length() > 1 && !wholeDelimiter && preserveDelimiters ) {
			return new Array( list.splitWithDelimiters( "[" + escapeRegexSpecials( delimiter ) + "]", 0 ) ).withDelimiters();
		} else {
			if ( includeEmpty ) {
				result = StringUtils.splitPreserveAllTokens( list, delimiter );
			} else {
				result = StringUtils.split( list, delimiter );
			}
		}

		return Arrays.stream( result )
		    .collect( BLCollector.toArray() );
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
	 * Get an item at a specific (1-based) index, returning a default value if not found
	 *
	 * @param list           The list to search
	 * @param index          The index to get
	 * @param delimiter      The delimiter to use
	 * @param includeEmpty   Whether to include empty items in the result array
	 * @param wholeDelimiter Whether the delimiter contains multiple characters which should be matched. Otherwise all characters in the delimiter are
	 *                       treated as separate delimiters
	 * @param defaultValue   The value to return if the index is not found
	 *
	 * @return The value at the index if found
	 */
	public static String getAt( String list, int index, String delimiter, Boolean includeEmpty, Boolean wholeDelimiter, String defaultValue ) {
		Array array = asList( list, delimiter, includeEmpty, wholeDelimiter );
		if ( index < 0 || array.size() < index ) {
			return defaultValue;
		}
		return StringCaster.cast( array.getAt( index ) );
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
		return deleteAt( list, index, delimiter, false, true );
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
		    asList( list, delimiter, includeEmpty, wholeDelimiter, true ).deleteAt( index ),
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
	 */
	public static void each(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		// Parameter validation
		Objects.requireNonNull( array, "Array cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		IntConsumer consumer;
		if ( callback.requiresStrictArguments() ) {
			consumer = idx -> ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null } ) );
		} else {
			consumer = idx -> ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array } ) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		IntStream arrayStream = array.intStream();
		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				if ( ordered ) {
					arrayStream
					    .parallel()
					    .forEachOrdered( consumer );
				} else {
					arrayStream
					    .parallel()
					    .forEach( consumer );
				}
				return;
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			AsyncService.buildExecutor(
			    "ArrayEach_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				if ( ordered ) {
					arrayStream
					    .parallel()
					    .forEachOrdered( consumer );
				} else {
					arrayStream
					    .parallel()
					    .forEach( consumer );
				}
			} );
			return;
		}

		// If parallel is false, just use the regular stream
		if ( ordered ) {
			arrayStream
			    .forEachOrdered( consumer );
		} else {
			arrayStream
			    .forEach( consumer );
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
	public static boolean some(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( array, "Array cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		IntPredicate test;
		if ( callback.requiresStrictArguments() ) {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null } ) ) );
		} else {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array } ) ) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		IntStream arrayStream = array.intStream();

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return arrayStream
				    .parallel()
				    .anyMatch( test );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Boolean ) AsyncService.buildExecutor(
			    "ArraySome_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return arrayStream
				    .parallel()
				    .anyMatch( test );
			} );
		}

		// Non-parallel execution
		return arrayStream
		    .anyMatch( test );
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

		// Parameter validation
		Objects.requireNonNull( array, "Array cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		IntPredicate test;
		if ( callback.requiresStrictArguments() ) {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null } ) ) );
		} else {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array } ) ) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		IntStream arrayStream = array.intStream();

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return arrayStream
				    .parallel()
				    .allMatch( test );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Boolean ) AsyncService.buildExecutor(
			    "ArrayEvery_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return arrayStream
				    .parallel()
				    .allMatch( test );
			} );
		}

		// Non-parallel execution
		return arrayStream
		    .allMatch( test );
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

		// Parameter validation
		Objects.requireNonNull( array, "Array cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Build the test predicate based on the callback
		// If the callback requires strict arguments, we only pass the item (Usually Java Predicates)
		// Otherwise we pass the item, the index, and the array itself
		IntPredicate test;
		if ( callback.requiresStrictArguments() ) {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null } ) ) );
		} else {
			test = idx -> BooleanCaster.cast( ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction( callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array } ) ) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		Stream<Object> arrayStream = array
		    .intStream()
		    .filter( test )
		    .mapToObj( ( idx ) -> array.size() > idx ? array.get( idx ) : null );

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return arrayStream
				    .parallel()
				    .collect( BLCollector.toArray() );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Array ) AsyncService.buildExecutor(
			    "ArrayFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return arrayStream
				    .parallel()
				    .collect( BLCollector.toArray() );
			} );
		}

		// Non-parallel execution
		return arrayStream.collect( BLCollector.toArray() );
	}

	/**
	 * Method to filter an list with a function callback and context
	 *
	 * If parallel we create a fork join pool. If no max threads is specified it uses the {@link java.util.concurrent.ForkJoinPool#commonPool}
	 *
	 * @param array           The array object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
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
	 * @param array     The array object to filter
	 * @param sortType  The textual sort directive
	 * @param sortOrder The textual sort order
	 * @param locale    The locale to use for sorting
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
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Array map(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( array, "Array cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Build the mapper based on the callback
		// If the callback requires strict arguments, we only pass the item (Usually Java Predicates)
		// Otherwise we pass the item, the index, and the array itself
		java.util.function.IntFunction<Object> mapper;
		if ( callback.requiresStrictArguments() ) {
			mapper = idx -> ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction(
			    callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null }
			) );
		} else {
			mapper = idx -> ThreadBoxContext.runInContext( callbackContext, parallel, ctx -> ctx.invokeFunction(
			    callback,
			    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array }
			) );
		}

		Stream<Object> arrayStream = array
		    .intStream()
		    .mapToObj( mapper );

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return arrayStream
				    .parallel()
				    .collect( BLCollector.toArray() );
			}

			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Array ) AsyncService.buildExecutor(
			    "ArrayMap_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return arrayStream
				    .parallel()
				    .collect( BLCollector.toArray() );
			} );
		}

		// Non-parallel execution
		return arrayStream.collect( BLCollector.toArray() );
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
		BiFunction<Object, Integer, Object> reduction;
		if ( callback.requiresStrictArguments() ) {
			reduction = ( acc, idx ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, array.size() > idx ? array.get( idx ) : null } );
		} else {
			reduction = ( acc, idx ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, array.size() > idx ? array.get( idx ) : null, idx + 1, array } );
		}

		return array.intStream()
		    .boxed()
		    .reduce(
		        initialValue,
		        reduction,
		        ( acc, intermediate ) -> acc
		    );

	}

	/**
	 * Method reduce a delimited list
	 *
	 * @param list            The the delimited list to reduce
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param initialValue    the initial value
	 *
	 * @return the new object reduction
	 */
	public static Object reduce(
	    String list,
	    String delimiter,
	    boolean includeEmptyFields,
	    boolean multiCharacterDelimiter,
	    Function callback,
	    IBoxContext callbackContext,
	    Object initialValue ) {
		BiFunction<Object, Integer, Object>	reduction;
		Array								array	= asList( list, delimiter, includeEmptyFields, multiCharacterDelimiter );

		if ( callback.requiresStrictArguments() ) {
			reduction = ( acc, idx ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, array.size() > idx ? array.get( idx ) : null } );
		} else {
			reduction = ( acc, idx ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, array.size() > idx ? array.get( idx ) : null, idx + 1, list, delimiter } );
		}

		return array.intStream()
		    .boxed()
		    .reduce(
		        initialValue,
		        reduction,
		        ( acc, intermediate ) -> acc
		    );

	}

	/**
	 * Utility method to escape special regex characters
	 *
	 * @param str The string to escape
	 *
	 * @return The escaped string
	 */
	private static String escapeRegexSpecials( String str ) {
		return SPECIAL_REGEX_CHARS.matcher( str ).replaceAll( "\\\\$0" );
	}
}
