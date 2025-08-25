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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.jdbc.qoq.QoQStatement;
import ortus.boxlang.runtime.logging.BoxLangLogger;
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
public final class ExecutedQuery implements Serializable {

	private static final InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();
	private static final BoxLangLogger		logger				= BoxRuntime.getInstance().getLoggingService().DATASOURCE_LOGGER;

	/**
	 * A Query object holding the results of the query.
	 * If there were no results, the Query object will have no rows.
	 *
	 * @see Query
	 */
	private @NonNull final Query			results;

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
	 */
	public ExecutedQuery( @NonNull Query results, @Nullable Object generatedKey ) {
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
	public static void dumpResultSet( ResultSet rs ) throws SQLException {
		if ( rs == null ) {
			System.out.println( "ResultSet is null." );
			return;
		}

		try {
			ResultSetMetaData	meta		= rs.getMetaData();
			int					columnCount	= meta.getColumnCount();
			System.out.println( "ResultSet:" );

			while ( rs.next() ) {
				for ( int i = 1; i <= columnCount; i++ ) {
					String	name	= meta.getColumnLabel( i );
					Object	value	= rs.getObject( i );
					System.out.print( name + "=" + value + "\t" );
				}
				System.out.println();
			}
		} catch ( NullPointerException e ) {
			System.out.println( "ResultSet is null 2." );
			return;
		}
	}

	/**
	 * Detect if a result set is generated keys. This is needed because some JDBC drivers will INCORRECTLY return a
	 * "normal" result set as the generated keys after an update. (Looking at you MSSQL :/ )
	 *
	 * @param rs The ResultSet to check.
	 * 
	 * @return True if the ResultSet is for generated keys, false otherwise.
	 */
	private static boolean isResultSetGeneratedKeys( ResultSet rs ) {
		if ( rs == null ) {
			return false;
		}
		try {
			ResultSetMetaData	meta		= rs.getMetaData();
			int					columnCount	= meta.getColumnCount();
			if ( columnCount != 1 ) {
				return false;
			}
			// This can be tricked, but it's the best we can do
			String columnName = meta.getColumnLabel( 1 );
			// System.out.println( "Checking if result set is generated keys: " + columnName );
			// Possible others, not sure. IDENTITYCOL, GENERATEDKEYS
			return columnName.equalsIgnoreCase( "GENERATED_KEYS" ) || columnName.equalsIgnoreCase( "GENERATED_KEY" );
		} catch ( SQLException | NullPointerException e ) {
			return false;
		}
	}

	public static ExecutedQuery fromPendingQuery( @NonNull PendingQuery pendingQuery, @NonNull Statement statement, long executionTime, boolean hasResults,
	    SQLException initialSqlException ) {
		boolean		generatedKeysComeAsResultSet	= false;
		Object		generatedKey					= null;
		Throwable	raisedError						= initialSqlException;
		Query		results							= null;
		int			recordCount						= 0;
		int			totalUpdateCount				= 0;
		Array		updateCounts					= new Array();
		int			affectedCount					= -1;
		Array		allGeneratedKeys				= new Array();
		// System.out.println( "****************************** process query result. hasResults: " + hasResults );

		if ( statement instanceof QoQStatement qs ) {
			results = qs.getQueryResult();
		} else {
			try {
				// TODO: Move this into a generic flag that we set on the MSSQL driver.
				generatedKeysComeAsResultSet = statement.getConnection().getMetaData().getDriverName().toLowerCase().contains( "microsoft" );
			} catch ( SQLException e ) {
				logger.error( "Error getting JDBC driver name", e );
			}

			// Loop over results until we find a result set, or run out of results
			while ( true ) {
				if ( hasResults ) {
					try ( ResultSet rs = statement.getResultSet() ) {
						// Surprise, MSSQL sometimes returns generated keys as a result set. Because it hates us.
						if ( generatedKeysComeAsResultSet && isResultSetGeneratedKeys( rs ) ) {
							generatedKey = processGeneratedKeys( rs, allGeneratedKeys, generatedKey );
						} else {
							// Only take first result set. We don't break here though because we need to keep looping in case a later result raised an exception
							if ( results == null ) {
								results		= Query.fromResultSet( rs );
								recordCount	= results.size();
								// System.out.println( "acquired query result. recordCount: " + recordCount );
							}
						}
					} catch ( SQLException e ) {
						// e.printStackTrace();
						throw new DatabaseException( e );
					}
				} else {

					// Capture generated keys, if any.
					try {
						affectedCount = statement.getUpdateCount();
						// System.out.println( "affectedCount: " + affectedCount );
						if ( affectedCount > -1 ) {
							updateCounts.add( affectedCount );
							totalUpdateCount += affectedCount;

							if ( !generatedKeysComeAsResultSet ) {
								ResultSet keys = statement.getGeneratedKeys();
								// System.out.println( "retrieving generated keys" );
								if ( keys != null ) {
									generatedKey = processGeneratedKeys( keys, allGeneratedKeys, generatedKey );
								}
								keys.close();
							}
						}
					} catch ( SQLException | NullPointerException t ) {
						// throw new DatabaseException( "Error getting update count", t );
						// t.printStackTrace();
						// System.out.println( "Error getting generatedKeys: " + t.getMessage() );
						logger.error( "Error getting update count", t );
					}
				}
				// If we have no results and no affected count, we're done.
				if ( !hasResults && affectedCount == -1 ) {
					// System.out.println( "no results or affected count, breaking" );
					break;
				}

				// Otherwise, look for another result set or update count.
				try {
					// System.out.println( "checking for more results" );
					hasResults = statement.getMoreResults();
					// System.out.println( "hasResults: " + hasResults );
				} catch ( SQLException e ) {
					// e.printStackTrace();
					// Keep nesting our raised errors. We'll throw them all at the end.
					if ( raisedError == null || !raisedError.equals( e ) ) {
						if ( raisedError == null || e.getCause() != null ) {
							raisedError = e;
						} else {
							raisedError = e.initCause( raisedError );
						}
					}
					try {
						// It's possible that the SQLException was that the statement was closed
						// If so, break to avoid endless looping.
						// Otherwise, we want to continue as MSSQL can thrown many exceptions and we gotta' catch them all!
						if ( statement.isClosed() ) {
							break;
						}
					} catch ( SQLException e1 ) {
						// stupid isCClosed() can throw SQLException
						break;
					}
				}

			} // /while

			// If there are one or more errors raised, we throw them now.
			if ( raisedError != null ) {
				throw new DatabaseException( raisedError.getMessage(), raisedError );
			}

		}

		// I'm not sure what the precedent is here, but if there were multiple updates or inserts and no selects,
		// then make our record count the total number of updated rows.
		if ( results == null ) {
			recordCount = totalUpdateCount;
		}

		IStruct queryMeta = Struct.of(
		    Key.cached, false,
		    Key.cacheKey, pendingQuery.getCacheKey(),
		    Key.sql, pendingQuery.getSQLWithParamValues(),
		    Key.sqlParameters, Array.fromList( pendingQuery.getParameterValues() ),
		    Key.executionTime, executionTime,
		    Key.recordCount, recordCount
		);

		if ( generatedKey != null ) {
			// The first/last generated key (depends on driver)
			queryMeta.put( Key.generatedKey, generatedKey );
			// array of arrays of generated keys (depends on driver)
			queryMeta.put( Key.generatedKeys, allGeneratedKeys );
		}

		if ( !updateCounts.isEmpty() ) {
			// The total number of updated rows across all statements
			queryMeta.put( Key.updateCount, totalUpdateCount );
			// Array of update counts for each statement
			queryMeta.put( Key.updateCounts, updateCounts );
		}

		// If we only had an update or insert, we need an empty query object to return
		if ( results == null ) {
			results = new Query();
		}

		// important that we set the metadata on the Query object for later getBoxMeta(), i.e. $bx.meta calls.
		results.setMetadata( queryMeta );
		ExecutedQuery executedQuery = new ExecutedQuery( results, generatedKey );

		interceptorService.announce( BoxEvent.POST_QUERY_EXECUTE,
		    Struct.of(
		        Key.sql, queryMeta.getAsString( Key.sql ),
		        Key.bindings, pendingQuery.getParameterValues(),
		        Key.executionTime, executionTime,
		        Key.data, results,
		        Key.result, queryMeta,
		        Key.pendingQuery, pendingQuery,
		        Key.executedQuery, executedQuery
		    )
		);
		return executedQuery;
	}

	/**
	 * Processes the generated keys from the result set.
	 *
	 * @param rs               The result set containing the generated keys.
	 * @param allGeneratedKeys An array to store all generated keys.
	 * @param generatedKey     The generated key for the current insert/update operation.
	 * 
	 * @return The processed generated key.
	 * 
	 * @throws SQLException If an SQL error occurs.
	 */
	private static Object processGeneratedKeys( ResultSet rs, Array allGeneratedKeys, Object generatedKey ) throws SQLException {
		// System.out.println( "retrieving generated keys posing as a result set" );
		Array theseKeys = new Array();
		while ( rs.next() ) {
			theseKeys.add( rs.getObject( 1 ) );
		}
		allGeneratedKeys.add( theseKeys );
		if ( generatedKey == null && !theseKeys.isEmpty() ) {
			generatedKey = theseKeys.get( 0 );
			// System.out.println( "acquired generated key: " + generatedKey );
		}
		return generatedKey;
	}

	/**
	 * Returns the Query object of results of the query.
	 *
	 * @return A Query object of results.
	 */
	public @NonNull Query getResults() {
		return this.results;
	}

	/**
	 * Returns an {@link Array} of {@link Struct} instances representing the {@link Query} results.
	 *
	 * @return An Array of Structs representing the Query
	 */
	public @NonNull Array getResultsAsArray() {
		return this.results.toArrayOfStructs();
	}

	/**
	 * Returns a {@link Struct} instance grouping the results by the given key.
	 *
	 * @param key The column to group the results by.
	 *
	 * @return A struct of String to Struct instances representing the Query results.
	 */
	public @NonNull IStruct getResultsAsStruct( @NonNull String key ) {
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
	public @NonNull IStruct getQueryMeta() {
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
