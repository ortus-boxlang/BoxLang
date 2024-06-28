/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.jdbc;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * This class represents a query that has been executed and contains the results of executing that query.
 * It contains a reference to the {@link PendingQuery} that was executed to create this.
 */
public final class ExecutedQuery {

	private static final InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * The {@link PendingQuery} executed.
	 */
	private @Nonnull final PendingQuery		pendingQuery;

	/**
	 * The execution time of the query.
	 */
	private final long						executionTime;

	/**
	 * A Query object holding the results of the query.
	 * If there were no results, the Query object will have no rows.
	 *
	 * @see Query
	 */
	private @Nonnull final Query			results;

	/**
	 * The generated key of the request, if any.
	 */
	private @Nullable Object				generatedKey;

	/**
	 * If the query was cached.
	 */
	private Boolean							isCached;

	/**
	 * String name of the cache provider used to cache the query.
	 */
	private String							cacheProvider;

	/**
	 * Cache key used to store the query in the cache.
	 * <p>
	 * This key can be used in manual query cache manipulations, i.e. for invalidation:
	 * 
	 * <pre>
	 * // Execute a query and cache the results
	 * var myQuery = queryExecute( "SELECT * FROM table", {}, { cache: true, result : "myQueryResult" } );
	 * // Clear the cache for this query from the default cache
	 * getBoxCache().clear( myQueryResult.cacheKey );
	 * // or
	 * </pre>
	 */
	private String							cacheKey;

	/**
	 * Max time the query will be cached for.
	 * <p>
	 * This must be populated with a timespan value using `createTimespan()`.
	 */
	private Duration							cacheTimeout;

	/**
	 * Max time to wait for a cache to be accessed before it is considered stale and automatically removed from the BoxLang cache.
	 * <p>
	 * This must be populated with a timespan value using `createTimespan()`.
	 * <p>
	 * Consider a query with the following query options: `{ cache: true, cacheTimeout: createTimespan( 0, 0, 10, 0 ), cacheLastAccessTimeout: createTimespan( 0, 0, 1, 0 ) }`. This query has a 10-minute cache timeout, so after 10 minutes of intermittent use it will be removed from the cache. The `cacheLastAccessTimeout` is set to 1 minute, so if the query is not accessed for 1 minute, it will be removed from the cache.
	 */
	private Duration							cacheLastAccessTimeout;

	/**
	 * Creates an ExecutedQuery instance.
	 *
	 * @param pendingQuery  The {@link PendingQuery} executed.
	 * @param statement     The {@link Statement} instance executed.
	 * @param executionTime The execution time the query took.
	 * @param hasResults    Boolean flag from {@link PreparedStatement#execute()} designating if the execution returned any results.
	 */
	public ExecutedQuery( @Nonnull PendingQuery pendingQuery, @Nonnull Statement statement, long executionTime, boolean hasResults ) {
		this.isCached		= false;
		this.pendingQuery	= pendingQuery;
		this.executionTime	= executionTime;

		try ( ResultSet rs = statement.getResultSet() ) {
			this.results = Query.fromResultSet( rs );
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}

		// Capture generated keys, if any.
		try {
			try ( ResultSet keys = statement.getGeneratedKeys() ) {
				if ( keys != null && keys.next() ) {
					this.generatedKey = keys.getObject( 1 );
				}
			} catch ( SQLException e ) {
				// @TODO Add in more info to this
				throw new DatabaseException( e.getMessage(), e );
			}
		} catch ( NullPointerException e ) {
			// This is likely due to Hikari wrapping a null ResultSet.
			// There should not be a null ResultSet returned from getGeneratedKeys
			// (https://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html#getGeneratedKeys--)
			// but some JDBC drivers do anyway.
			// Since Hikari wraps the null value, we can't get access to it,
			// so instead we have to catch it here and ignore it.
			// We do check the message to try to be very particular about what NullPointerExceptions we are catching
			if ( !e.getMessage().equals( "Cannot invoke \"java.sql.ResultSet.next()\" because \"this.delegate\" is null" ) ) {
				throw e;
			}
		}

		interceptorService.announce(
		    BoxEvent.POST_QUERY_EXECUTE,
		    Struct.of(
		        "sql", this.pendingQuery.getOriginalSql(),
		        "bindings", this.pendingQuery.getParameterValues(),
		        "executionTime", executionTime,
		        "data", results,
		        "result", getResultStruct(),
		        "pendingQuery", this.pendingQuery,
		        "executedQuery", this
		    )
		);
	}

	/**
	 * Get the query results as the configured return type.
	 *
	 * @param query The executed query
	 *
	 * @return The query results as the configured return type - either a query, array, or struct
	 */
	// public Object getResult( ExecutedQuery query ) {

	// IType results = switch ( this.pendingQuery.getQueryOptions().returnType ) {
	// case "query" -> query.getResults();
	// case "array" -> query.getResultsAsArray();
	// case "struct" -> query.getResultsAsStruct( this.columnKey );
	// default -> throw new BoxRuntimeException( "Unknown return type: " + returnType );
	// };

	// // add in the metadata
	// results.getBoxMeta().getMeta().put( "debug", this.getResultStruct() );

	// // then return it
	// return results;
	// }

	/**
	 * Returns the Query object of results of the query.
	 *
	 * @return A Query object of results.
	 */
	public @Nonnull Query getResults() {
		return this.results;
	}

	/**
	 * Returns an {@link Array} of {@link Struct} instances representing the {@link Query} results.
	 *
	 * @return An Array of Structs representing the Query
	 */
	public @Nonnull Array getResultsAsArray() {
		return this.results.toStructArray();
	}

	/**
	 * Returns a {@link Struct} instance grouping the results by the given key.
	 *
	 * @param key The column to group the results by.
	 *
	 * @return A struct of String to Struct instances representing the Query results.
	 */
	public @Nonnull IStruct getResultsAsStruct( @Nonnull String key ) {
		// @TODO get brad to make this better
		Map<Object, List<IStruct>>	groupedResults	= this.results.stream().collect( groupingBy( r -> r.get( key ) ) );
		Map<Object, Object>			groupedArray	= groupedResults.entrySet().stream().collect( toMap( Map.Entry::getKey, e -> new Array( e.getValue() ) ) );
		return Struct.fromMap(
		    IStruct.TYPES.LINKED,
		    groupedArray
		);
	}

	/**
	 * Returns the total count of records returned by the query.
	 *
	 * @return The total count of records.
	 */
	public int getRecordCount() {
		return this.results.size();
	}

	/**
	 * Sets the query as cached.
	 * <p>
	 * This is used to indicate that the query was cached - it does not actually cache the query. Use after retrieval from cache.
	 */
	public ExecutedQuery setIsCached() {
		this.isCached = true;
		return this;
	}

	/**
	 * Set the cache provider used to cache the query.
	 */
	public ExecutedQuery setCacheProvider( String cacheProvider ) {
		this.cacheProvider = cacheProvider;
		return this;
	}

	/**
	 * Set the cache key which uniquely identifies this query in the cache.
	 */
	public ExecutedQuery setCacheKey( String cacheKey ) {
		this.cacheKey = cacheKey;
		return this;
	}

	/**
	 * Set the cache timeout for the query.
	 */
	public ExecutedQuery setCacheTimeout( Duration cacheTimeout ) {
		this.cacheTimeout = cacheTimeout;
		return this;
	}

	/**
	 * Set the cache last access timeout for the query.
	 */
	public ExecutedQuery setCacheLastAccessTimeout( Duration cacheLastAccessTimeout ) {
		this.cacheLastAccessTimeout = cacheLastAccessTimeout;
		return this;
	}

	/**
	 * Returns the `result` struct returned from `queryExecute` and `query`.
	 * <p>
	 * The struct contains the following keys:
	 * 
	 * <ul>
	 * <li>SQL: The SQL statement that was executed. (string)
	 * <li>SqlParameters: An ordered Array of queryparam values. (array)
	 * <li>RecordCount: Total number of records in the query. (numeric)
	 * <li>ColumnList: Column list, comma separated. (string)
	 * <li>ExecutionTime: Execution time for the SQL request. (numeric)
	 * <li>GENERATEDKEY: If the query was an INSERT with an identity or auto-increment value the value of that ID is placed in this variable.
	 * <li>Cached: If the query was cached. (boolean)
	 * </ul>
	 * 
	 * @return A struct of query metadata, like original SQL, parameters, size, and cache info.
	 */
	public @Nonnull Struct getResultStruct() {
		Struct result = new Struct();
		result.put( "sql", this.pendingQuery.getOriginalSql() );
		result.put( "sqlParameters", Array.fromList( this.pendingQuery.getParameterValues() ) );
		result.put( "recordCount", getRecordCount() );
		result.put( "columnList", this.results.getColumnList() );
		result.put( "executionTime", this.executionTime );
		if ( this.generatedKey != null ) {
			result.put( "generatedKey", this.generatedKey );
		}

		// cache info
		result.put( "cached", this.isCached );
		result.put( "cacheProvider", this.cacheProvider );
		result.put( "cacheKey", this.cacheKey );
		result.put( "cacheTimeout", this.cacheTimeout );
		result.put( "cacheLastAccessTimeout", this.cacheLastAccessTimeout );

		return result;
	}

	/**
	 * Returns the generated key of the query, if any
	 *
	 * @return The generated key of the query.
	 */
	public @Nullable Object getGeneratedKey() {
		return this.generatedKey;
	}
}
