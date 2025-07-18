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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.StringCompare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.LocalizationUtil;

/**
 * StructUtil is a comprehensive utility class providing static methods for advanced struct operations in BoxLang.
 * This class offers functional programming capabilities, data manipulation, sorting, searching, and conversion utilities
 * for IStruct implementations.
 *
 * <h2>Core Features:</h2>
 * <ul>
 * <li><strong>Functional Operations:</strong> each, some, every, filter, map, reduce with parallel processing support</li>
 * <li><strong>Data Manipulation:</strong> deep merge, flatten/unflatten operations</li>
 * <li><strong>Sorting:</strong> Multi-type sorting (text, numeric, case-insensitive) with custom callback support</li>
 * <li><strong>Search Operations:</strong> Find keys and values with nested path support</li>
 * <li><strong>Conversion Utilities:</strong> Query string conversion, flat map operations</li>
 * </ul>
 *
 * <h2>Parallel Processing:</h2>
 * Many methods support parallel execution with configurable thread pools for improved performance
 * on large datasets. When parallel is enabled, operations can optionally maintain order for
 * LinkedHashMap-based structs.
 *
 * <h2>Thread Safety:</h2>
 * This utility class is stateless and thread-safe. However, the struct instances passed to methods
 * should be properly synchronized if accessed concurrently.
 *
 * <h2>Usage Examples:</h2>
 *
 * <pre>
 * // Functional operations
 * StructUtil.each( myStruct, callback, context, false, 0, true );
 * IStruct filtered = StructUtil.filter( myStruct, predicate, context, true, 4 );
 *
 * // Data manipulation
 * IStruct merged = StructUtil.deepMerge( struct1, struct2, true );
 * IStruct flattened = StructUtil.toFlatMap( nestedStruct );
 *
 * // Search operations
 * Stream&lt;IStruct&gt; keyResults = StructUtil.findKey( myStruct, "targetKey" );
 * Stream&lt;IStruct&gt; valueResults = StructUtil.findValue( myStruct, "targetValue" );
 *
 * // Conversion
 * String queryString = StructUtil.toQueryString( myStruct );
 * IStruct fromQuery = StructUtil.fromQueryString( "foo=bar&baz=qux" );
 * </pre>
 *
 * @author BoxLang Development Team
 *
 * @since 1.3.0
 */
public class StructUtil {

	public static final Key scopeAll = Key.of( "all" );

	/**
	 * Method to invoke a function for every item in a struct
	 *
	 * @param struct          The struct to iterate
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 */
	public static void each(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Build the consumer based on whether the callback requires strict arguments
		// or not. This is Java vs BoxLang predicate compatibility.
		Consumer<Map.Entry<Key, Object>> consumer;
		if ( callback.requiresStrictArguments() ) {
			consumer = item -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			);
		} else {
			consumer = item -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			);
		}

		Stream<Map.Entry<Key, Object>> entryStream = struct
		    .entrySet()
		    .stream();
		if ( !parallel ) {
			entryStream.forEach( consumer );
			return;
		}

		// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
		if ( maxThreads <= 0 ) {
			if ( ordered || struct.getType().equals( IStruct.TYPES.LINKED ) ) {
				entryStream.parallel().forEachOrdered( consumer );
			} else {
				entryStream.parallel().forEach( consumer );
			}
			return;
		}

		// Otherwise, create a new ForkJoinPool with the specified number of threads
		AsyncService.buildExecutor(
		    "StructEach_" + UUID.randomUUID().toString(),
		    AsyncService.ExecutorType.FORK_JOIN,
		    maxThreads
		).submitAndGet( () -> {
			if ( ordered || struct.getType().equals( IStruct.TYPES.LINKED ) ) {
				entryStream.parallel().forEachOrdered( consumer );
			} else {
				entryStream.parallel().forEach( consumer );
			}
		} );
	}

	/**
	 * Method to test if any item in the struct meets the criteria in the callback
	 *
	 * @param struct          The struct to test
	 * @param callback        The callback test to apply
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean some(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		Predicate<Map.Entry<Key, Object>> test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		Stream<Map.Entry<Key, Object>> entryStream = struct.entrySet().stream();

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return entryStream
				    .parallel()
				    .anyMatch( test );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Boolean ) AsyncService.buildExecutor(
			    "StructSome_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return entryStream
				    .parallel()
				    .anyMatch( test );
			} );
		}

		// Non-parallel execution
		return entryStream.anyMatch( test );
	}

	/**
	 * Method to test if any item in the struct meets the criteria in the callback
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean every(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		Predicate<Map.Entry<Key, Object>> test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		Stream<Map.Entry<Key, Object>> entryStream = struct.entrySet().stream();
		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return entryStream
				    .parallel()
				    .allMatch( test );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Boolean ) AsyncService.buildExecutor(
			    "StructEvery_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return entryStream
				    .parallel()
				    .allMatch( test );
			} );
		}

		// Non-parallel execution
		return entryStream.allMatch( test );
	}

	/**
	 * Method to filter a struct with a function callback and context
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	public static IStruct filter(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Build the test predicate based on whether the callback requires strict arguments
		// or not. This is Java vs BoxLang predicate compatibility.
		Predicate<Map.Entry<Key, Object>> test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		Stream<Map.Entry<Key, Object>> entryStream = struct
		    .entrySet()
		    .stream()
		    .filter( test );

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return entryStream.parallel().collect( BLCollector.toStruct( struct.getType() ) );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( IStruct ) AsyncService.buildExecutor(
			    "StructFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return entryStream
				    .parallel()
				    .collect( BLCollector.toStruct( struct.getType() ) );
			} );
		}

		// Non-parallel execution
		return entryStream.collect( BLCollector.toStruct( struct.getType() ) );
	}

	/**
	 * Method to map a struct to a new struct
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	public static Struct map(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Build out the result
		Struct								result	= new Struct( struct.getType() );
		Boolean								ordered	= struct.getType().equals( IStruct.TYPES.LINKED )
		    || struct.getType().equals( IStruct.TYPES.SORTED );

		// Build the test predicate based on whether the callback requires strict arguments
		// or not. This is Java vs BoxLang predicate compatibility.
		Consumer<Map.Entry<Key, Object>>	consumer;
		if ( callback.requiresStrictArguments() ) {
			consumer = item -> result.put(
			    item.getKey(),
			    callbackContext.invokeFunction(
			        callback,
			        new Object[] { item.getKey().getName(), item.getValue() }
			    )
			);
		} else {
			consumer = item -> result.put(
			    item.getKey(),
			    callbackContext.invokeFunction(
			        callback,
			        new Object[] { item.getKey().getName(), item.getValue(), struct }
			    )
			);
		}

		Stream<Map.Entry<Key, Object>> entryStream = struct
		    .entrySet()
		    .stream();

		if ( !parallel ) {
			entryStream.forEach( consumer );
			return result;
		}

		// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
		if ( maxThreads <= 0 ) {
			if ( ordered || struct.getType().equals( IStruct.TYPES.LINKED ) ) {
				entryStream.parallel().forEachOrdered( consumer );
			} else {
				entryStream.parallel().forEach( consumer );
			}
			return result;
		}

		// Otherwise, create a new ForkJoinPool with the specified number of threads
		AsyncService.buildExecutor(
		    "StructMap_" + UUID.randomUUID().toString(),
		    AsyncService.ExecutorType.FORK_JOIN,
		    maxThreads
		).submitAndGet( () -> {
			if ( ordered || struct.getType().equals( IStruct.TYPES.LINKED ) ) {
				entryStream.parallel().forEachOrdered( consumer );
			} else {
				entryStream.parallel().forEach( consumer );
			}
		} );

		return result;

	}

	/**
	 * Method to reduce a struct to an accumulated object
	 *
	 * @param struct          The struct object to reduce
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param initialValue    The initial value of the accumulation
	 *
	 * @return the new object reduction
	 */
	public static Object reduce(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Object initialValue ) {
		// Parameter validation
		Objects.requireNonNull( struct, "Struct cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );

		BiFunction<Object, Map.Entry<Key, Object>, Object> reduction;
		if ( callback.requiresStrictArguments() ) {
			reduction = ( acc, item ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, item.getKey().getName(), item.getValue() } );
		} else {
			reduction = ( acc, item ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, item.getKey().getName(), item.getValue(), struct } );
		}

		return struct.entrySet().stream()
		    .reduce(
		        initialValue,
		        reduction,
		        ( acc, intermediate ) -> acc
		    );

	}

	/**
	 * Performs a stort of a struct and returns the top-level keys in the sorted order
	 *
	 * @param struct    the struct to sort
	 * @param sortType  the textual sort directive ( numeric, text, textNoCase )
	 * @param sortOrder the sort order (asc, desc)
	 * @param path      An optional nested string path to perform the sort on
	 *
	 * @return an array containing the sorted keys
	 */
	public static Array sort(
	    IStruct struct,
	    String sortType,
	    String sortOrder,
	    String path ) {
		if ( path == null ) {
			Key typeKey = Key.of( sortType + sortOrder );
			if ( !getCommonKeyComparators().containsKey( typeKey ) ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The sort directive [%s,%s] is not a valid struct sorting directive",
				        sortType,
				        sortOrder
				    )
				);
			}

			return new Array(
			    struct.entrySet()
			        .stream()
			        .sorted( getCommonEntryComparators( Locale.US ).get( typeKey ) )
			        .map( k -> k.getKey().getName() )
			        .toArray()
			);
		} else {
			Boolean isDescending = Key.of( sortOrder ).equals( Key.of( "desc" ) );
			return new Array( struct.entrySet().stream().sorted(
			    ( a, b ) -> Compare.invoke(
			        StructUtil.getAtPath( StructCaster.cast( isDescending ? b.getValue() : a.getValue() ), path ),
			        StructUtil.getAtPath( StructCaster.cast( isDescending ? a.getValue() : b.getValue() ), path ),
			        sortType.toLowerCase().contains( "nocase" ) ? false : true
			    )
			).map( e -> e.getKey().getName() ).toArray()
			);
		}

	}

	/**
	 * Performs a stort of a struct with a callback funciton and returns the ordered struct keys
	 *
	 * @param struct          the struct to sort
	 * @param callback        the callback sort function which is passed the arguments (k1,k2) for comparison
	 * @param callbackContext the IBoxContext to execute the callback within
	 *
	 * @return an array containing the sorted keys
	 */
	public static Array sort(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext ) {

		return new Array(
		    struct.keySet()
		        .stream()
		        .map( k -> k.getName() )
		        .sorted(
		            ( a, b ) -> IntegerCaster.cast( callbackContext.invokeFunction(
		                callback,
		                new Object[] { a, b }
		            ) )
		        )
		        .toArray()
		);
	}

	/**
	 * Retreives the final value of a nested string path within a struct
	 *
	 * @param struct the struct to search within
	 * @param path   the string path representation ( e.g. foo.bar.baz )
	 *
	 * @return The found object or null
	 */
	public static Object getAtPath(
	    IStruct struct,
	    String path ) {
		String[]	parts	= path.split( "\\." );
		Object		ref		= null;
		Key			refName	= Key.of( parts[ 0 ] );
		if ( struct.containsKey( refName ) ) {
			ref = struct.get( refName );
			for ( int i = 1; i < parts.length - 1; i++ ) {
				ref = StructCaster.cast( ref ).get( Key.of( parts[ i ] ) );
				if ( ref == null )
					break;
			}
			return ref;
		} else {
			return null;
		}
	}

	/**
	 * Finds all instances of a key in a struct and returns a stream of structs
	 *
	 * @param struct the struct to get the keys from
	 * @param key    the key to search for
	 *
	 * @return a stream of structs containing the owner, path, and value of the found key
	 */
	public static Stream<IStruct> findKey( IStruct struct, String key ) {
		String[]			keyParts	= key.toLowerCase().split( "\\." );
		IStruct				flatMap		= toFlatMap( struct );
		ArrayList<IStruct>	results		= new ArrayList<IStruct>();

		// If we have a single key and find it in the root add that result first
		if ( keyParts.length == 1 && struct.containsKey( Key.of( key ) ) ) {
			results.add(
			    Struct.of(
			        Key.owner, struct,
			        Key.path, "." + key.toLowerCase(),
			        Key.value, struct.get( Key.of( key ) )
			    )
			);
		}

		// Now add any results that are nested
		results.addAll(
		    flatMap.entrySet()
		        .stream()
		        .filter( entry -> {
			        String[] splitParts = entry.getKey().getName().toLowerCase().split( "\\." );
			        String stringKey = entry.getKey().getName().toLowerCase();
			        return splitParts.length > 1
			            ? splitParts[ splitParts.length - 1 ].equals( key.toLowerCase() ) || stringKey.equals( key.toLowerCase() )
			            // For single keys make sure we check that it wasn't added above
			            : results.stream()
			                .filter(
			                    result -> {
				                    Object resultObj = result.get( Key.value );
				                    Object entryObj = entry.getValue();
				                    if ( resultObj == null ) {
					                    return entry.getValue() == null;
				                    } else {
					                    return entryObj != null ? resultObj.equals( entryObj ) : false;
				                    }
			                    }
			                )
			                .count() == 0
			                && stringKey.equals( key.toLowerCase() );
		        } )
		        .map( entry -> {
			        Struct returnStruct	= new Struct( Struct.TYPES.LINKED );
			        String keyName		= entry.getKey().getName();
			        String[] entryKeyParts = entry.getKey().getName().split( "\\." );
			        String flatMapParent = keyName.lastIndexOf( "." ) > -1 ? keyName.substring( 0, keyName.lastIndexOf( "." ) ) : "";
			        returnStruct.put(
			            Key.owner,
			            entryKeyParts.length > 1
			                ? unFlattenKeys(
			                    flatMap.entrySet().stream()
			                        .filter( mapEntry -> mapEntry.getKey().getName().length() >= flatMapParent.length()
			                            && mapEntry.getKey().getName().substring( 0, flatMapParent.length() ).equals( flatMapParent ) )
			                        .map( mapEntry -> {
				                        String keyname = mapEntry.getKey().getName();
				                        String resultKeyName = keyname.substring( flatMapParent.length() + 1 );
				                        return new AbstractMap.SimpleEntry<Key, Object>(
				                            Key.of( resultKeyName ), mapEntry.getValue()
				                        );
			                        }
			                        )
			                        .collect( BLCollector.toStruct() ),
			                    true,
			                    false
			                )
			                : struct
			        );
			        returnStruct.put(
			            Key.path,
			            "." + keyName
			        );
			        returnStruct.put(
			            Key.value,
			            entry.getValue()
			        );
			        return returnStruct;
		        } ).collect( Collectors.toList() )
		);

		return results.stream();

	}

	/**
	 * Finds all instances of a value in a struct and returns a stream of structs
	 * containing the owner struct, the path to the value, and the key of the value.
	 *
	 * @param struct the struct to search within
	 * @param value  the value to search for
	 *
	 * @return a stream of structs containing the owner, path, and key of the found value
	 */
	public static Stream<IStruct> findValue( IStruct struct, Object value ) {
		IStruct flatMap = toFlatMap( struct );
		return flatMap.entrySet()
		    .stream()
		    .filter( entry -> Compare.invoke( value, entry.getValue() ) == 0 )
		    .map( entry -> {
			    Struct	returnStruct	= new Struct( Struct.TYPES.LINKED );
			    String	keyName			= entry.getKey().getName();
			    String[] keyParts		= entry.getKey().getName().split( "\\." );
			    String	parentName		= keyName;
			    if ( keyParts.length > 1 ) {
				    parentName = keyName.substring( 0, keyName.lastIndexOf( "." ) );
			    }
			    final String finalParent = parentName;
			    returnStruct.put(
			        Key.owner,
			        keyParts.length > 1
			            ? unFlattenKeys(
			                flatMap.entrySet().stream()
			                    .filter( mapEntry -> mapEntry.getKey().getName().contains( finalParent )
			                    ).map(
			                        mapEntry -> new AbstractMap.SimpleEntry<Key, Object>(
			                            Key.of( mapEntry.getKey().getName().replace( finalParent + ".", "" ) ), mapEntry.getValue() )
			                    )
			                    .collect( BLCollector.toStruct() ),
			                true,
			                false
			            )
			            : struct
			    );
			    // TODO: This dot prefix is silly given the context this function operates in. Deprecate the dot prefix in a future release.
			    returnStruct.put(
			        Key.path,
			        "." + keyName
			    );
			    returnStruct.put(
			        Key.key,
			        keyParts[ keyParts.length - 1 ]
			    );
			    return returnStruct;
		    } );

	}

	/**
	 * Performs a deep merge on two structs and will only add top level and deep values not present in the recipient
	 *
	 * @param recipient The struct to merge into
	 * @param merge     The struct to merge from
	 *
	 * @return the recipient struct merged
	 */
	public static IStruct deepMerge( IStruct recipient, IStruct merge ) {
		return deepMerge( recipient, merge, false );
	}

	/**
	 * Performs a deep merge on two structs. If override is set to true, it will override all keys in the recipeient with keys from the merge struct.
	 * If set to false, it will only add top level and deep values not present in the recipient
	 *
	 * @param recipient The struct to merge into
	 * @param merge     The struct to merge from
	 * @param override  Whether to override the recipient keys with the merge keys
	 *
	 * @return the recipient struct merged
	 */
	public static IStruct deepMerge( IStruct recipient, IStruct merge, boolean override ) {
		merge.entrySet().forEach(
		    entry -> {
			    if ( entry.getValue() instanceof IStruct mergeStruct && recipient.get( entry.getKey() ) instanceof IStruct recipStruct ) {
				    StructUtil.deepMerge( recipStruct, mergeStruct, override );
			    } else if ( entry.getValue() instanceof Array mergeArray && recipient.get( entry.getKey() ) instanceof Array recipArray ) {
				    mergeArray.stream().forEach( item -> {
					    if ( !recipArray.contains( item ) ) {
						    recipArray.add( item );
					    }
				    } );
			    } else {
				    if ( override ) {
					    recipient.put( entry.getKey(), entry.getValue() );
				    } else {
					    recipient.putIfAbsent( entry.getKey(), entry.getValue() );
				    }
			    }
		    }
		);
		return recipient;
	}

	/**
	 * Flattens a struct in to a struct containing dot-delmited keys for nested structs
	 *
	 * @param struct the struct to flatten
	 *
	 * @return a flattened map of the struct
	 */
	public static IStruct toFlatMap( IStruct struct ) {
		return new Struct(
		    struct.getType(),
		    struct.entrySet().stream()
		        .filter( entry -> entry.getValue() != null )
		        .flatMap( StructUtil::flattenEntry )
		        .collect( LinkedHashMap<Key, Object>::new, ( m, entry ) -> m.put( entry.getKey(), entry.getValue() ), LinkedHashMap::putAll )
		);
	}

	/**
	 * Method to recursively flatten an entry set containing structs
	 *
	 * @param entry the individual entry set from the stream
	 *
	 * @return the stream object for further operations
	 */
	public static Stream<Map.Entry<Key, Object>> flattenEntry( Map.Entry<Key, Object> entry ) {

		java.util.function.Function<Map.Entry<Key, Object>, AbstractMap.SimpleEntry<Key, Object>> flattener = e -> new AbstractMap.SimpleEntry<Key, Object>(
		    Key.of(
		        entry.getKey().getName() + "." + e.getKey().getName()
		    ),
		    e.getValue()
		);

		if ( entry.getValue() instanceof Map ) {
			IStruct nested = StructCaster.cast( entry.getValue() );
			return nested.entrySet().stream()
			    .map( flattener )
			    .flatMap( StructUtil::flattenEntry );
		}
		return Stream.of( entry );
	}

	/**
	 * Translates a struct with dot-delimited keys in to nested struct
	 *
	 * @param struct     The struct to de-flatten
	 * @param deep       Whether to recurse in to nested keys to unflatten
	 * @param retainKeys Whether to retain the original keys
	 *
	 * @return a flattened map of the struct
	 */
	public static IStruct unFlattenKeys( IStruct struct, boolean deep, boolean retainKeys ) {
		String	key;
		Object	value;
		int		index;
		for ( Key k : struct.getKeys() ) {
			key		= k.getName();
			value	= struct.get( k );
			if ( deep && value instanceof IStruct )
				unFlattenKeys( StructCaster.cast( value ), deep, retainKeys );
			if ( ( index = key.indexOf( '.' ) ) != -1 ) {
				unFlattenKey( index, k, key, struct, retainKeys );
			}
		}

		return struct;

	}

	/**
	 * Method to recursively un-flatten a struct with keys in dot-notation
	 *
	 * @param index      the initial key index
	 * @param key        the struct key to de-flatten
	 * @param keyValue   the string representation of the key
	 * @param original   the original struct to start the deflattening
	 * @param retainKeys whether to retain the original flattened keys
	 */
	public static void unFlattenKey( int index, Key key, String keyValue, IStruct original, boolean retainKeys ) {
		String	left;
		Object	value		= original.get( key );
		IStruct	destination	= original;
		if ( !retainKeys )
			original.remove( key );
		do {
			left		= keyValue.substring( 0, index );
			keyValue	= keyValue.substring( index + 1 );
			Key destinationKey = Key.of( left );
			if ( !destination.containsKey( destinationKey ) ) {
				destination.put( destinationKey, new Struct() );
			}
			destination = destination.getAsStruct( destinationKey );
		} while ( ( index = keyValue.indexOf( '.' ) ) != -1 );
		// final put of the last key in the delimited string
		destination.put( Key.of( keyValue ), value );
	}

	/**
	 * Convert a struct to a query string
	 * Example:
	 *
	 * <pre>
	 * { foo: "bar", baz: "qux" } -> "foo=bar&amp;baz=qux"
	 * </pre>
	 *
	 * @param struct    The struct to convert
	 * @param delimiter The delimiter to use between key-value pairs
	 *
	 * @return The query string
	 */
	public static String toQueryString( IStruct struct, String delimiter ) {
		return struct.entrySet()
		    .stream()
		    .map( entry -> EncryptionUtil.urlEncode( entry.getKey().getName().trim() ) + "=" + EncryptionUtil.urlEncode( entry.getValue().toString().trim() ) )
		    .collect( Collectors.joining( delimiter ) );
	}

	/**
	 * Convert a struct to a query string using the default delimiter of {@code "&"}
	 *
	 * @param struct The struct to convert
	 *
	 * @return The query string
	 */
	public static String toQueryString( IStruct struct ) {
		return toQueryString( struct, "&" );
	}

	/**
	 * Convert a query string to a struct
	 * Example:
	 *
	 * <pre>
	 * "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
	 * </pre>
	 *
	 * @param target    The query string to convert
	 * @param delimiter The delimiter to use between key-value pairs
	 *
	 * @return The struct
	 */
	public static IStruct fromQueryString( String target, String delimiter ) {
		target = target.trim();

		// Empty string should return an empty struct
		if ( target.length() == 0 ) {
			return new Struct( Struct.TYPES.LINKED );
		}

		// If the string starts with ? remove it
		if ( target.startsWith( "?" ) ) {
			target = target.substring( 1 );
		}

		// parse the string into a struct: Example: "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
		return new Struct(
		    Struct.TYPES.LINKED,
		    Stream.of( target.split( delimiter ) )
		        .map( pair -> pair.split( "=" ) )
		        .collect(
		            Collectors.toMap(
		                pair -> Key.of( EncryptionUtil.urlDecode( pair[ 0 ] ).trim() ),
		                pair -> pair.length > 1 ? EncryptionUtil.urlDecode( pair[ 1 ] ).trim() : ""
		            )
		        )
		);
	}

	/**
	 * Convert a query string to a struct using the default delimiter of {@code "&"}.
	 * Example:
	 *
	 * <pre>
	 * "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
	 * </pre>
	 *
	 * @param target The query string to convert.
	 *
	 * @return The struct representing the parsed query string.
	 */
	public static IStruct fromQueryString( String target ) {
		return fromQueryString( target, "&" );
	}

	/**
	 * Get a map of common comparators for sorting structs.
	 * This map contains comparators for text, numeric, and case-insensitive text sorting.
	 *
	 * @return A HashMap of Key to Comparator for common sorting operations.
	 */
	public static HashMap<Key, Comparator<Key>> getCommonKeyComparators() {
		return getCommonKeyComparators( LocalizationUtil.COMMON_LOCALES.get( Key.of( "US" ) ) );
	}

	/**
	 * Get a map of common comparators for sorting structs with a specific locale.
	 * This map contains comparators for text, numeric, and case-insensitive text sorting,
	 * localized according to the provided Locale.
	 *
	 * @param locale The Locale to use for text comparisons.
	 *
	 * @return A HashMap of Key to Comparator for common sorting operations.
	 */
	public static HashMap<Key, Comparator<Key>> getCommonKeyComparators( Locale locale ) {
		return new HashMap<Key, Comparator<Key>>() {

			{
				put( Key.of( "textAsc" ), ( a, b ) -> StringCompare
				    .invoke( StringCaster.cast( a ), StringCaster.cast( b ), true, locale ) );
				put( Key.of( "textDesc" ), ( b, a ) -> StringCompare
				    .invoke( StringCaster.cast( a ), StringCaster.cast( b ), true, locale ) );
				put( Key.of( "textNoCaseAsc" ),
				    ( a, b ) -> StringCompare.invoke( StringCaster.cast( a ),
				        StringCaster.cast( b ), false, locale ) );
				put( Key.of( "textNoCaseDesc" ),
				    ( b, a ) -> StringCompare.invoke( StringCaster.cast( a ),
				        StringCaster.cast( b ), false, locale ) );
				put( Key.of( "numericAsc" ),
				    ( a, b ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getOriginalValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getOriginalValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
				put( Key.of( "numericDesc" ),
				    ( b, a ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getOriginalValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getOriginalValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
			}
		};
	}

	/**
	 * Get a map of common comparators for sorting structs with a specific locale.
	 * This map contains comparators for text, numeric, and case-insensitive text sorting,
	 * localized according to the provided Locale.
	 * This method sorts on a struct's entries, which are Map.Entry<Key, Object> pairs.
	 *
	 * @param locale The Locale to use for text comparisons.
	 *
	 * @return A HashMap of Key to Comparator for common sorting operations.
	 */
	public static HashMap<Key, Comparator<Map.Entry<Key, Object>>> getCommonEntryComparators( Locale locale ) {
		return new HashMap<Key, Comparator<Map.Entry<Key, Object>>>() {

			{
				put( Key.of( "textAsc" ), ( a, b ) -> StringCompare
				    .invoke( StringCaster.cast( a.getValue() ), StringCaster.cast( b.getValue() ), true, locale ) );
				put( Key.of( "textDesc" ), ( b, a ) -> StringCompare
				    .invoke( StringCaster.cast( a.getValue() ), StringCaster.cast( b.getValue() ), true, locale ) );
				put( Key.of( "textNoCaseAsc" ),
				    ( a, b ) -> StringCompare.invoke( StringCaster.cast( a.getValue() ),
				        StringCaster.cast( b.getValue() ), false, locale ) );
				put( Key.of( "textNoCaseDesc" ),
				    ( b, a ) -> StringCompare.invoke( StringCaster.cast( a.getValue() ),
				        StringCaster.cast( b.getValue() ), false, locale ) );
				put( Key.of( "numericAsc" ),
				    ( a, b ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
				put( Key.of( "numericDesc" ),
				    ( b, a ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
			}
		};
	}

}
