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

import java.util.Objects;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

/**
 * Utility class for Query operations.
 *
 * This class provides various helper methods for working with Query objects in BoxLang,
 * including column existence checking and query filtering with callback functions.
 * It supports both sequential and parallel processing for filter operations.
 */
public class QueryUtil {

	/**
	 * Checks if a column exists in a query.
	 *
	 * @param query  the query to check
	 * @param column the name of the column to check
	 *
	 * @return true if the column exists, false otherwise
	 */
	public static Boolean columnExists( Query query, String column ) {
		return query.hasColumn( Key.of( column ) );
	}

	/**
	 * Method to filter a query with a function callback and context
	 *
	 * @param query           The query object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered query
	 */
	public static Query filter(
	    Query query,
	    Function callback,
	    IBoxContext callbackContext,
	    boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( query, "Query cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		// Check if the callback requires strict arguments.
		// This is Java vs BoxLang predicate compatibility.
		IntPredicate test;
		if ( callback.requiresStrictArguments() ) {
			test = idx -> BooleanCaster.cast(
			    callbackContext.invokeFunction( callback, new Object[] { query.getRowAsStruct( idx ) } )
			);
		} else {
			test = idx -> BooleanCaster.cast(
			    callbackContext.invokeFunction( callback, new Object[] { query.getRowAsStruct( idx ), idx + 1, query } )
			);
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		Stream<IStruct> queryStream = query
		    .intStream()
		    .filter( test )
		    .mapToObj( query::getRowAsStruct );

		// Let's do it!
		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return queryStream.parallel().collect( BLCollector.toQuery( query ) );
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Query ) AsyncService.buildExecutor(
			    "QueryFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> queryStream.parallel().collect( BLCollector.toQuery( query ) ) );
		}

		// If parallel is false, just use the regular stream
		return queryStream.collect( BLCollector.toQuery( query ) );
	}

	/**
	 * Method to invoke a function for every iteration of the query
	 *
	 * @param query           The query object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 */
	public static void each(
	    Query query,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		// Parameter validation
		Objects.requireNonNull( query, "Query cannot be null" );
		Objects.requireNonNull( callback, "Callback cannot be null" );
		Objects.requireNonNull( callbackContext, "Callback context cannot be null" );
		if ( maxThreads == null ) {
			maxThreads = 0; // Default to 0 if not provided
		}

		IntConsumer consumer;
		if ( callback.requiresStrictArguments() ) {
			consumer = idx -> callbackContext.invokeFunction( callback,
			    new Object[] { query.size() > idx ? query.getRowAsStruct( idx ) : null } );
		} else {
			consumer = idx -> callbackContext.invokeFunction( callback,
			    new Object[] { query.size() > idx ? query.getRowAsStruct( idx ) : null, idx + 1, query } );
		}

		// Create a stream of what we want, usage is determined internally by the terminators
		IntStream queryStream = query.intStream();
		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				if ( ordered ) {
					queryStream
					    .parallel()
					    .forEachOrdered( consumer );
				} else {
					queryStream
					    .parallel()
					    .forEach( consumer );
				}
				return;
			}
			// Otherwise, create a new ForkJoinPool with the specified number of threads
			AsyncService.buildExecutor(
			    "QueryEach_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				if ( ordered ) {
					queryStream
					    .parallel()
					    .forEachOrdered( consumer );
				} else {
					queryStream
					    .parallel()
					    .forEach( consumer );
				}
			} );
			return;
		}

		// If parallel is false, just use the regular stream
		if ( ordered ) {
			queryStream
			    .forEachOrdered( consumer );
		} else {
			queryStream
			    .forEach( consumer );
		}
	}

	/**
	 * Maps an existing query to a new query
	 *
	 * @param query           The query object to map
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the map in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Query map(
	    Query query,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		// Parameter validation
		Objects.requireNonNull( query, "Query cannot be null" );
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
			mapper = idx -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { query.size() > idx ? query.getRowAsStruct( idx ) : null }
			);
		} else {
			mapper = idx -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { query.size() > idx ? query.getRowAsStruct( idx ) : null, idx + 1, query }
			);
		}

		Stream<IStruct> queryStream = query
		    .intStream()
		    .mapToObj( idx -> ( IStruct ) mapper.apply( idx ) );

		if ( parallel ) {
			// If maxThreads is null or 0, then use just the ForkJoinPool default parallelism level
			if ( maxThreads <= 0 ) {
				return queryStream
				    .parallel()
				    .collect( BLCollector.toQuery( query ) );
			}

			// Otherwise, create a new ForkJoinPool with the specified number of threads
			return ( Query ) AsyncService.buildExecutor(
			    "QueryMap_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> {
				return queryStream
				    .parallel()
				    .collect( BLCollector.toQuery( query ) );
			} );
		}

		// Non-parallel execution
		return queryStream.collect( BLCollector.toQuery( query ) );
	}

}
