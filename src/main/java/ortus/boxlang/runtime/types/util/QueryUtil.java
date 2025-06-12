package ortus.boxlang.runtime.types.util;

import java.util.Objects;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

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

}
