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

import java.sql.Statement;
import java.time.Duration;

import javax.annotation.Nullable;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A class to hold the options for a query, such as the datasource, return type, and result variable name.
 * <p>
 * The following options are supported:
 * <ul>
 * <li><code>datasource</code> - The name of the datasource to use. If not provided, the default datasource will be used.
 * <li><code>returnType</code> - The type to return the query results as. Can be <code>query</code>, <code>array</code>, or <code>struct</code>.
 * <li><code>result</code> - The name of the variable to store the query results in.
 * <li><code>columnKey</code> - The name of the column to use as the key in the result struct when <code>returnType</code> is <code>struct</code>.
 * This is only used, but <strong>required</strong>, when <code>returnType</code> is <code>struct</code>.
 * <li><code>username</code> - The username to use when connecting to the datasource.
 * <li><code>password</code> - The password to use when connecting to the datasource.
 * <li><code>timeout</code> - The number of seconds to wait for the query to execute before timing out.
 * <li><code>maxRows</code> - The maximum number of rows to return from the query.
 * </ul>
 */
public class QueryOptions {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The query options struct
	 */
	private IStruct					options;

	/**
	 * The datasource setting - purposely left as an Object to allow to support both datasource string names and on-the-fly datasource struct configurations.
	 */
	public final Object				datasource;

	/**
	 * The result variable name
	 */
	public final @Nullable String	resultVariableName;

	/**
	 * The return type of the query. Available options are "query", "array", or "struct".
	 */
	private String					returnType;

	/**
	 * The column key to use when returning a struct.
	 */
	private String					columnKey;

	/**
	 * The datasource username to use for the connection, if any
	 */
	public final String				username;

	/**
	 * The datasource password to use for the connection, if any
	 */
	public final String				password;

	/**
	 * The query timeout in seconds
	 */
	public final Integer			queryTimeout;

	/**
	 * The maximum number of rows to return from the query, defaults to all
	 */
	public final Long				maxRows;

	/**
	 * The fetch size for the query. Should be preferred over `maxRows` for large result sets, as `maxrows` will only truncate further rows from the
	 * result, whereas `fetchsize` will prevent the retrieval of those rows in the first place.
	 *
	 * @see Statement#setFetchSize(int)
	 */
	public final Integer			fetchSize;

	/**
	 * Whether or not the query results should be cached.
	 */
	public final Boolean			cache;

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
	public final String				cacheKey;

	/**
	 * String name of the cache provider used to cache the query.
	 */
	public final String				cacheProvider;

	/**
	 * Max time the query will be cached for.
	 * <p>
	 * This must be populated with a timespan value using `createTimespan()`.
	 */
	public final Duration			cacheTimeout;

	/**
	 * Max time to wait for a cache to be accessed before it is considered stale and automatically removed from the BoxLang cache.
	 * <p>
	 * This must be populated with a timespan value using `createTimespan()`.
	 * <p>
	 * Consider a query with the following query options: `{ cache: true, cacheTimeout: createTimespan( 0, 0, 10, 0 ), cacheLastAccessTimeout: createTimespan( 0, 0, 1, 0 ) }`. This query has a 10-minute cache timeout, so after 10 minutes of intermittent
	 * use it will be removed from the cache. The `cacheLastAccessTimeout` is set to 1 minute, so if the query is not accessed for 1 minute, it will be removed from the cache.
	 */
	public final Duration			cacheLastAccessTimeout;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Read in the provided query options and set private fields accordingly.
	 * <p>
	 * Will throw BoxRuntimeExceptions if certain options are not valid, such as an unknown <code>datasource</code> or <code>returnType</code>.
	 *
	 * @param options Struct of query options. Backwards-compatible with the old-style <code>&lt;query&gt;</code> from BL.
	 */
	public QueryOptions( IStruct options ) {
		CacheService cacheService = BoxRuntime.getInstance().getCacheService();

		this.options				= options;
		this.resultVariableName		= options.getAsString( Key.result );
		this.username				= options.getAsString( Key.username );
		this.password				= options.getAsString( Key.password );
		this.queryTimeout			= options.getAsInteger( Key.timeout );
		this.datasource				= options.get( Key.datasource );
		this.fetchSize				= ( Integer ) options.getOrDefault( Key.fetchSize, 0 );

		// Caching options
		this.cache					= BooleanCaster.attempt( options.get( Key.cache ) ).getOrDefault( false );
		this.cacheKey				= options.getAsString( Key.cacheKey );
		this.cacheTimeout			= ( Duration ) options.getOrDefault( Key.cacheTimeout, Duration.ZERO );
		this.cacheLastAccessTimeout	= ( Duration ) options.getOrDefault( Key.cacheLastAccessTimeout, Duration.ZERO );
		this.cacheProvider			= ( String ) options.getOrDefault( Key.cacheProvider, cacheService.getDefaultCache().getName().toString() );

		Integer intMaxRows = options.getAsInteger( Key.maxRows );
		this.maxRows = Long.valueOf( intMaxRows != null ? intMaxRows : -1 );

		determineReturnType();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Do we want a result struct
	 *
	 * @return True if the query should return a struct, false otherwise.
	 */
	public boolean wantsResultStruct() {
		return this.resultVariableName != null;
	}

	/*
	 * Get the `returnType` query option.
	 */
	public String getReturnType() {
		return this.returnType;
	}

	/**
	 * Get the query results as the configured return type.
	 *
	 * @param query The executed query
	 *
	 * @return The query results as the configured return type - either a query, array, or struct
	 */
	public Object castAsReturnType( ExecutedQuery query ) {
		return switch ( this.returnType ) {
			case "query" -> query.getResults();
			case "array" -> query.getResultsAsArray();
			case "struct" -> query.getResultsAsStruct( this.columnKey );
			default -> throw new BoxRuntimeException( "Unknown return type: " + returnType );
		};
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * If the query options contain a `username` field, then the query should use the provided username and password to connect to the datasource.
	 *
	 * @return True if the query should use a username and password to connect to the datasource, false otherwise.
	 */
	public boolean wantsUsernameAndPassword() {
		return this.username != null;
	}

	/**
	 * Parse the `returnType` query option and set the `returnType` and `columnKey` fields.
	 *
	 * Performs validation upon the configured `returnType` option.
	 */
	private void determineReturnType() {
		Object				returnTypeObject	= options.get( Key.returnType );
		CastAttempt<String>	returnTypeAsString	= StringCaster.attempt( returnTypeObject );
		String				returnTypeString	= returnTypeAsString.getOrDefault( "query" );

		switch ( returnTypeString ) {
			case "query", "array" -> this.returnType = returnTypeString;
			case "struct" -> {
				this.columnKey = options.getAsString( Key.columnKey );
				if ( this.columnKey == null ) {
					throw new BoxRuntimeException( "You must define a `columnKey` option when using `returnType: struct`." );
				}
				this.returnType = "struct";
			}
			default -> throw new BoxRuntimeException( "Unknown return type: " + returnTypeString );
		}
	}

	/**
	 * Acquire the query options as a struct.
	 */
	public IStruct toStruct() {
		IStruct result = new Struct( this.options );
		// Overwrite any options that were set in the constructor, as we want to return the actual values used
		result.put( "fetchSize", this.fetchSize );
		result.put( "setQueryTimeout", this.queryTimeout );
		result.put( "setMaxRows", this.maxRows );
		return result;
	}

}
