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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * This class represents a query and any parameters/bindings before being executed.
 * After calling {@link #execute()}, it returns an {@link ExecutedQuery} with a reference to this object.
 */
public class PendingQuery {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private static final Logger					logger				= LoggerFactory.getLogger( PendingQuery.class );

	/**
	 * The InterceptorService instance to use for announcing events.
	 */
	private static final InterceptorService		interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * A pattern to match named parameters in the SQL string.
	 */
	private static final Pattern				pattern				= Pattern.compile( ":\\w+" );

	/**
	 * Prefix for cache queries
	 */
	private static final String					CACHE_PREFIX		= "BL_QUERY";

	/**
	 * The SQL string to execute.
	 * <p>
	 * If this SQL has parameters, they should be represented either as question marks (`?`)
	 * or as named bindings, prefixed with a colon (`:`)
	 */
	private @Nonnull String						sql;

	/**
	 * The original SQL provided to the constructor. When constructing a `PendingQuery` with an
	 * {@link IStruct} instance, the named parameters — prefixed with a colon (`:`) in the SQL string —
	 * are replaced with question marks to work with the JDBC query. The SQL string with question marks
	 * is set as `sql` and the original SQL provided with named parameters is set as `originalSql`.
	 */
	private @Nonnull final String				originalSql;

	/**
	 * A List of QueryParameter instances. The store the value and SQL type of the parameters, in order.
	 *
	 * @see QueryParameter
	 */
	private @Nonnull final List<QueryParameter>	parameters;

	/**
	 * Query options from the original BoxLang code.
	 */
	private QueryOptions						queryOptions;

	/**
	 * The cache key for this query, determined from the combined hash of the SQL string and parameter values.
	 */
	private String								cacheKey;

	/**
	 * The cache provider to use for caching this query.
	 */
	private ICacheProvider						cacheProvider;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new PendingQuery instance from a SQL string, a list of parameters, and the original SQL string.
	 *
	 * @param sql         The SQL string to execute
	 * @param parameters  A list of {@link QueryParameter} to use as bindings.
	 * @param originalSql The original sql string. This will include named parameters if the `PendingQuery` was constructed using an {@link IStruct}.
	 */
	public PendingQuery( @Nonnull String sql, Object bindings, QueryOptions queryOptions ) {
		logger.debug( "Building new PendingQuery from SQL: [{}] and options: [{}]", sql, queryOptions.toStruct() );

		/**
		 * `onQueryBuild()` interception: Use this to modify query parameters or options before the query is executed.
		 *
		 * The event args will contain the following keys:
		 *
		 * - sql : The original SQL string
		 * - parameters : The parameters to be used in the query
		 * - pendingQuery : The BoxLang query class used to build and execute queries
		 * - options : The QueryOptions class populated with query options from `queryExecute()` or `<bx:query>`
		 */
		IStruct eventArgs = Struct.of(
		    "sql", sql.trim(),
		    "bindings", bindings,
		    "pendingQuery", this,
		    "options", queryOptions
		);
		interceptorService.announce( BoxEvent.ON_QUERY_BUILD, eventArgs );

		// We set instance data from the event args so interceptors can modify them.
		this.sql			= eventArgs.getAsString( Key.sql );
		this.originalSql	= eventArgs.getAsString( Key.sql );
		this.parameters		= processBindings( eventArgs.get( Key.of( "bindings" ) ) );
		this.queryOptions	= eventArgs.getAs( QueryOptions.class, Key.options );

		// Create a cache key with a default or via the passed options.
		this.cacheKey		= getOrComputeCacheKey();
		this.cacheProvider	= BoxRuntime.getInstance().getCacheService().getCache( this.queryOptions.cacheProvider );
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string and a list of parameters.
	 * This constructor uses the provided SQL string as the original SQL.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters A list of {@link QueryParameter} to use as bindings.
	 */
	public PendingQuery( @Nonnull String sql, @Nonnull List<QueryParameter> parameters ) {
		this( sql, parameters, new QueryOptions( new Struct() ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
	/**
	 * Returns the cache key for this query.
	 * <p>
	 * If a custom cache key was provided in the query options, it will be used. Otherwise, a cache key will be generated from a combined hash of the SQL string,parameter values, and relevant query options such as the `datasource`, `username`, and
	 * `password`.
	 */
	private String getOrComputeCacheKey() {
		if ( this.queryOptions.cacheKey != null ) {
			return this.queryOptions.cacheKey;
		}
		String key = CACHE_PREFIX + this.sql.hashCode() + this.getParameterValues().hashCode();
		if ( this.queryOptions.datasource != null ) {
			key += this.queryOptions.datasource.hashCode();
		}
		if ( this.queryOptions.username != null ) {
			key += this.queryOptions.username.hashCode();
		}
		if ( this.queryOptions.password != null ) {
			key += this.queryOptions.password.hashCode();
		}
		return key;
	}

	/**
	 * Processes the bindings provided to the constructor and returns a list of {@link QueryParameter} instances.
	 * Will also modify the SQL string to replace named parameters with positional placeholders.
	 *
	 * @param bindings The bindings to process. This can be an {@link Array} of values or a {@link IStruct} of named parameters.
	 *
	 * @return A list of {@link QueryParameter} instances.
	 */
	private List<QueryParameter> processBindings( Object bindings ) {
		if ( bindings == null ) {
			return new ArrayList<>();
		}
		CastAttempt<Array> castAsArray = ArrayCaster.attempt( bindings );
		if ( castAsArray.wasSuccessful() ) {
			return buildParameterList( castAsArray.getOrFail() );
		}

		CastAttempt<IStruct> castAsStruct = StructCaster.attempt( bindings );
		if ( castAsStruct.wasSuccessful() ) {
			return buildParameterList( castAsStruct.getOrFail() );
		}

		// We always have bindings, since we exit early if there are none
		String className = bindings.getClass().getName();
		throw new BoxRuntimeException( "Invalid type for params. Expected array or struct. Received: " + className );
	}

	/**
	 * Process an array of query bindings into a list of {@link QueryParameter} instances.
	 *
	 * @param parameters An {@link Array} of `queryparam` {@link IStruct} instances to convert to {@link QueryParameter} instances and use as bindings.
	 */
	private List<QueryParameter> buildParameterList( @Nonnull Array parameters ) {
		return parameters.stream().map( QueryParameter::fromAny ).collect( Collectors.toList() );
	}

	/**
	 * Process a struct of named query bindings into a list of {@link QueryParameter} instances.
	 * <p>
	 * Also performs SQL string replacement to convert named parameters to positional placeholders.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters An `IStruct` of `String` `name` to either an `Object` `value` or a `queryparam` `IStruct`.
	 */
	private List<QueryParameter> buildParameterList( @Nonnull IStruct parameters ) {
		List<QueryParameter>	params	= new ArrayList<>();
		Matcher					matcher	= pattern.matcher( sql );
		while ( matcher.find() ) {
			String paramName = matcher.group();
			paramName = paramName.substring( 1 );
			Object paramValue = parameters.get( paramName );
			if ( paramValue == null ) {
				throw new DatabaseException( "Missing param in query: [" + paramName + "]. SQL: " + sql );
			}
			params.add( QueryParameter.fromAny( paramValue ) );
		}
		this.sql = matcher.replaceAll( "?" );
		return params;
	}

	/**
	 * Returns the original sql for this PendingQuery
	 *
	 * @return The original sql string
	 */
	public @Nonnull String getOriginalSql() {
		return this.originalSql;
	}

	/**
	 * Returns a list of parameter `Object` values from the `List<QueryParameter>`.
	 *
	 * @return A list of parameter values as `Object`s.
	 */
	public @Nonnull List<Object> getParameterValues() {
		return this.parameters.stream().map( QueryParameter::getValue ).collect( Collectors.toList() );
	}

	/**
	 * Executes the PendingQuery using the provided ConnectionManager and returns the results in an {@link ExecutedQuery} instance.
	 *
	 * @param connectionManager The ConnectionManager instance to use for getting connections from the current context.
	 *
	 * @throws DatabaseException If a {@link SQLException} occurs, wraps it in a DatabaseException and throws.
	 *
	 * @return An ExecutedQuery instance with the results of this JDBC execution, as well as a link to this PendingQuery instance.
	 *
	 * @see ExecutedQuery
	 */
	public @Nonnull ExecutedQuery execute( ConnectionManager connectionManager ) {
		// We do an early cache check here to avoid the overhead of creating a connection if we already have a matching cached query.
		if ( isCacheable() ) {
			logger.debug( "Checking cache for query: {}", this.cacheKey );
			Attempt<Object> cachedQuery = cacheProvider.get( this.cacheKey );
			if ( cachedQuery.isPresent() ) {
				return respondWithCachedQuery( cachedQuery );
			}
			logger.debug( "Query is NOT present, continuing to execute query: {}", this.cacheKey );
		}

		Connection connection = connectionManager.getConnection( this.queryOptions );
		try {
			return execute( connection );
		} finally {
			if ( connection != null ) {
				connectionManager.releaseConnection( connection );
			}
		}
	}

	/**
	 * Executes the PendingQuery on a given {@link Connection} and returns the results in an {@link ExecutedQuery} instance.
	 *
	 * @param connection The Connection instance to use for executing the query. It is the responsibility of the caller to close the connection after this method returns.
	 *
	 * @throws DatabaseException If a {@link SQLException} occurs, wraps it in a DatabaseException and throws.
	 *
	 * @return An ExecutedQuery instance with the results of this JDBC execution, as well as a link to this PendingQuery instance.
	 *
	 * @see ExecutedQuery
	 */
	public @Nonnull ExecutedQuery execute( Connection connection ) {
		if ( isCacheable() ) {
			// we use separate get() and set() calls over a .getOrSet() so we can run `.setIsCached()` on discovered/cached results.
			Attempt<Object> cachedQuery = this.cacheProvider.get( this.cacheKey );
			if ( cachedQuery.isPresent() ) {
				return respondWithCachedQuery( cachedQuery );
			}

			ExecutedQuery executedQuery = executeStatement( connection );
			this.cacheProvider.set( this.cacheKey, executedQuery, this.queryOptions.cacheTimeout, this.queryOptions.cacheLastAccessTimeout );
			return executedQuery;
		}
		return executeStatement( connection );
	}

	/**
	 * Generate and execute a JDBC statement using the provided connection.
	 * <p>
	 * * If query parameters are present, a {@link PreparedStatement} will be utilized and populated with the paremeter bindings. Otherwise, a standard {@link Statement} object will be used.
	 * * Will announce a `PRE_QUERY_EXECUTE` event before executing the query.
	 */
	private ExecutedQuery executeStatement( Connection connection ) {
		try {
			ArrayList<ExecutedQuery> queries = new ArrayList<>();
			for ( String sqlStatement : this.sql.split( ";" ) ) {
				// @TODO: Consider refactoring this to use a try-with-resources block, as the ExecutedQuery
				// should not need the Statement object once the constructor completes and returns.
				Statement statement = this.parameters.isEmpty()
				    ? connection.createStatement()
				    : connection.prepareStatement( this.sql, Statement.RETURN_GENERATED_KEYS );

				applyParameters( statement );
				applyStatementOptions( statement );

				interceptorService.announce(
				    BoxEvent.PRE_QUERY_EXECUTE,
				    Struct.of(
				        "sql", this.sql,
				        "bindings", getParameterValues(),
				        "pendingQuery", this
				    )
				);

				long	startTick	= System.currentTimeMillis();
				boolean	hasResults	= statement instanceof PreparedStatement preparedStatement
				    ? preparedStatement.execute()
				    : statement.execute( sqlStatement, Statement.RETURN_GENERATED_KEYS );
				long	endTick		= System.currentTimeMillis();

				// @TODO: Close the statement to prevent resource leaks!
				queries.add( ExecutedQuery.fromPendingQuery(
				    this,
				    statement,
				    endTick - startTick,
				    hasResults
				) );
			}
			return queries.getFirst();
		} catch ( SQLException e ) {
			String detail = "";
			if ( e.getCause() != null ) {
				detail = e.getCause().getMessage();
			}
			throw new DatabaseException(
			    e.getMessage(),
			    detail,
			    String.valueOf( e.getErrorCode() ),
			    e.getSQLState(),
			    originalSql,
			    null, // queryError
			    ListUtil.asString( Array.fromList( this.getParameterValues() ), "," ), // where
			    e
			);
		}
	}

	/**
	 * Helper method to respond with an ExecutedQuery instance from the given query cache lookup.
	 * <p>
	 * This method assumes cachedQuery.isPresent() has already been checked, and populates the ExecutedQuery instance with the query cache metadata, such as cacheKey, cacheProvider, etc.
	 */
	private ExecutedQuery respondWithCachedQuery( Attempt<Object> cachedQuery ) {
		logger.debug( "Query is present, returning cached result: {}", this.cacheKey );
		IStruct cacheMeta = Struct.of(
		    "cached", true,
		    "cacheKey", this.cacheKey,
		    "cacheProvider", this.cacheProvider.getName().toString(),
		    "cacheTimeout", this.queryOptions.cacheTimeout,
		    "cacheLastAccessTimeout", this.queryOptions.cacheLastAccessTimeout
		);
		return ExecutedQuery.fromCachedQuery( ( ExecutedQuery ) cachedQuery.get(), cacheMeta );
	}

	/**
	 * Apply the parameter bindings to the provided {@link Statement} instance.
	 * <p>
	 * Will only take action if 1) there are parameters to apply, and 2) the Statement object is a PreparedStatement.
	 */
	private void applyParameters( Statement statement ) throws SQLException {
		if ( this.parameters.isEmpty() ) {
			return;
		}

		if ( statement instanceof PreparedStatement preparedStatement ) {
			// The param index starts from 1
			for ( int i = 1; i <= this.parameters.size(); i++ ) {
				QueryParameter	param			= this.parameters.get( i - 1 );
				Integer			scaleOrLength	= param.getScaleOrLength();
				if ( scaleOrLength == null ) {
					preparedStatement.setObject( i, param.getValue(), param.getSqlTypeAsInt() );
				} else {
					preparedStatement.setObject( i, param.getValue(), param.getSqlTypeAsInt(), scaleOrLength );
				}
			}
		}
	}

	/**
	 * Apply query options to the provided {@link Statement} instance.
	 * <p>
	 * Any query options which pass through to the JDBC Statement interface will be applied here. This includes `queryTimeout`, `maxRows`, and `fetchSize`.
	 */
	private void applyStatementOptions( Statement statement ) throws SQLException {
		IStruct options = this.queryOptions.toStruct();
		if ( options.containsKey( Key.queryTimeout ) ) {
			Integer queryTimeout = ( Integer ) options.getOrDefault( Key.queryTimeout, 0 );
			if ( queryTimeout > 0 ) {
				statement.setQueryTimeout( queryTimeout );
			}
		}

		if ( options.containsKey( Key.maxRows ) ) {
			Integer maxRows = ( Integer ) options.getOrDefault( Key.maxRows, 0 );
			if ( maxRows > 0 ) {
				statement.setLargeMaxRows( maxRows );
			}
		}
		if ( options.containsKey( Key.fetchSize ) ) {
			Integer fetchSize = ( Integer ) options.getOrDefault( Key.fetchSize, 0 );
			if ( fetchSize > 0 ) {
				statement.setFetchSize( fetchSize );
			}
		}
		/**
		 * TODO: Implement the following options:
		 * ormoptions
		 * dbtype : query of queries (In progress)
		 * username and password : To evaluate later due to security concerns of overriding datasources, not going to implement unless requested
		 * clientInfo : Part of the connection: get/setClientInfo()
		 */
	}

	/**
	 * Check the cacheable option to determine if the query should be cached.
	 */
	private boolean isCacheable() {
		return Boolean.TRUE.equals( this.queryOptions.cache );
	}
}
