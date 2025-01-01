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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.jdbc.qoq.QoQStatement;
import ortus.boxlang.runtime.scopes.Key;
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

	private static final Logger				logger				= LoggerFactory.getLogger( ExecutedQuery.class );

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
	 * Struct of query metadata, such as original SQL, parameters, size, and cache info.
	 */
	private IStruct							queryMeta;

	/**
	 * Constructor
	 *
	 * @param results      The results of the query, i.e. the actual Query object.
	 * @param generatedKey The generated key of the query, if any.
	 * @param queryMeta    Struct of query metadata, such as original SQL, parameters, size, and cache info.
	 */
	public ExecutedQuery( @Nonnull Query results, @Nullable Object generatedKey ) {
		this.results		= results;
		this.generatedKey	= generatedKey;
		this.queryMeta		= results.getMetaData();
	}

	/**
	 * Creates an ExecutedQuery instance from a PendingQuery instance and a JDBC Statement.
	 *
	 * @param pendingQuery  The {@link PendingQuery} executed.
	 * @param statement     The {@link Statement} instance executed.
	 * @param executionTime The execution time the query took.
	 * @param hasResults    Boolean flag from {@link PreparedStatement#execute()} designating if the execution returned any results.
	 */
	public static ExecutedQuery fromPendingQuery( @Nonnull PendingQuery pendingQuery, @Nonnull Statement statement, long executionTime, boolean hasResults ) {
		Object	generatedKey	= null;
		Query	results			= null;
		int		recordCount		= 0;
		int		affectedCount	= -1;

		if ( statement instanceof QoQStatement qs ) {
			results = qs.getQueryResult();
		} else {
			// Loop over results until we find a result set, or run out of results
			while ( true ) {
				if ( hasResults ) {
					try ( ResultSet rs = statement.getResultSet() ) {
						results		= Query.fromResultSet( rs );
						recordCount	= results.size();
						break;
					} catch ( SQLException e ) {
						throw new DatabaseException( e.getMessage(), e );
					}
				} else {

					// Capture generated keys, if any.
					try {
						try {
							affectedCount = statement.getUpdateCount();
							if ( affectedCount > -1 ) {
								recordCount = affectedCount;
								try ( ResultSet keys = statement.getGeneratedKeys() ) {
									if ( keys != null && keys.next() ) {
										generatedKey = keys.getObject( 1 );
									}
								} catch ( SQLException e ) {
									// @TODO: drop the message check, since it doesn't support alternate languages.
									if ( e.getMessage().contains( "The statement must be executed before any results can be obtained." ) ) {
										logger.info(
										    "SQL Server threw an error when attempting to retrieve generated keys. Am ignoring the error - no action is required. Error : [{}]",
										    e.getMessage() );
									} else {
										logger.warn( "Error getting generated keys", e );
									}
								}
							}
						} catch ( SQLException t ) {
							logger.error( "Error getting update count", t );
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
				}
				// If we have no results and no affected count, we're done.
				if ( !hasResults && affectedCount == -1 ) {
					break;
				}

				// Otherwise, look for another result set or update count.
				try {
					hasResults = statement.getMoreResults();
				} catch ( SQLException e ) {
					break;
				}

			} // /while
		}

		IStruct queryMeta = Struct.of(
		    "cached", false,
		    "cacheKey", pendingQuery.getCacheKey(),
		    "sql", pendingQuery.getSQLWithParamValues(),
		    "sqlParameters", Array.fromList( pendingQuery.getParameterValues() ),
		    "executionTime", executionTime,
		    "recordCount", recordCount
		);

		if ( generatedKey != null ) {
			queryMeta.put( "generatedKey", generatedKey );
		}

		// If we only had an update or insert, we need an empty query object to return
		if ( results == null ) {
			results = new Query();
		}

		// important that we set the metadata on the Query object for later getBoxMeta(), i.e. $bx.meta calls.
		results.setMetadata( queryMeta );
		ExecutedQuery executedQuery = new ExecutedQuery( results, generatedKey );

		interceptorService.announce(
		    BoxEvent.POST_QUERY_EXECUTE,
		    Struct.of(
		        "sql", queryMeta.getAsString( Key.sql ),
		        "bindings", pendingQuery.getParameterValues(),
		        "executionTime", executionTime,
		        "data", results,
		        "result", queryMeta,
		        "pendingQuery", pendingQuery,
		        "executedQuery", executedQuery
		    )
		);
		return executedQuery;
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
	// results.getBoxMeta().getMeta().put( "debug", this.getQueryMeta() );

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
	 * Retrieve query metadata.
	 * <p>
	 * The struct contains the following keys:
	 *
	 * <ul>
	 * <li>SQL: The SQL statement that was executed. (string)
	 * <li>SqlParameters: An ordered Array of queryparam values. (array)
	 * <li>ExecutionTime: Execution time for the SQL request. (numeric)
	 * <li>GENERATEDKEY: If the query was an INSERT with an identity or auto-increment value the value of that ID is placed in this variable.
	 * <li>Cached: If the query was cached. (boolean)
	 * <li>CacheProvider: The cache provider used to cache the query. (string)
	 * <li>CacheKey: The cache key used to store the query in the cache. (string)
	 * <li>CacheTimeout: The max time the query will be cached for. (timespan)
	 * <li>CacheLastAccessTimeout: Max time to wait for a cache to be accessed before it is considered stale and automatically removed from the BoxLang cache. (timespan)
	 * </ul>
	 *
	 * @return A struct of query metadata, like original SQL, parameters, size, and cache info.
	 */
	public @Nonnull IStruct getQueryMeta() {
		return this.queryMeta;
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
