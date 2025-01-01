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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * This class represents a query and any parameters/bindings before being
 * executed.
 * After calling {@link #execute(ConnectionManager,IBoxContext)}, it returns an
 * {@link ExecutedQuery} with a reference to this object.
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
	 * Prefix for cache queries
	 */
	private static final String					CACHE_PREFIX		= "BL_QUERY";

	/**
	 * The SQL string to execute.
	 * <p>
	 * If this SQL has parameters, they should be represented either as question
	 * marks (`?`)
	 * or as named bindings, prefixed with a colon (`:`)
	 */
	private @Nonnull String						sql;

	private List<String>						SQLWithParamTokens	= new ArrayList<>();
	/**
	 * SQL string broken up into segments so we can build a fully
	 */
	private @Nonnull String						SQLWithParamValues;

	/**
	 * The original SQL provided to the constructor. When constructing a
	 * `PendingQuery` with an
	 * {@link IStruct} instance, the named parameters — prefixed with a colon (`:`)
	 * in the SQL string —
	 * are replaced with question marks to work with the JDBC query. The SQL string
	 * with question marks
	 * is set as `sql` and the original SQL provided with named parameters is set as
	 * `originalSql`.
	 */
	private @Nonnull final String				originalSql;

	/**
	 * A List of QueryParameter instances. The store the value and SQL type of the
	 * parameters, in order.
	 *
	 * @see QueryParameter
	 */
	private @Nonnull final List<QueryParameter>	parameters;

	/**
	 * Query options from the original BoxLang code.
	 */
	private QueryOptions						queryOptions;

	/**
	 * The cache key for this query, determined from the combined hash of the SQL
	 * string and parameter values.
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
	 * Creates a new PendingQuery instance from a SQL string, a list of parameters,
	 * and the original SQL string.
	 *
	 * @param sql          The SQL string to execute
	 * @param bindings     An array or struct of {@link QueryParameter} to use as
	 *                     bindings.
	 * @param queryOptions QueryOptions object denoting the options for this query.
	 */
	public PendingQuery( @Nonnull String sql, Object bindings, QueryOptions queryOptions ) {
		logger.debug( "Building new PendingQuery from SQL: [{}] and options: [{}]", sql, queryOptions.toStruct() );

		/**
		 * `onQueryBuild()` interception: Use this to modify query parameters or options
		 * before the query is executed.
		 *
		 * The event args will contain the following keys:
		 *
		 * - sql : The original SQL string
		 * - parameters : The parameters to be used in the query
		 * - pendingQuery : The BoxLang query class used to build and execute queries
		 * - options : The QueryOptions class populated with query options from
		 * `queryExecute()` or `<bx:query>`
		 */
		IStruct eventArgs = Struct.of(
		    "sql", sql.trim(),
		    "bindings", bindings,
		    "pendingQuery", this,
		    "options", queryOptions );
		interceptorService.announce( BoxEvent.ON_QUERY_BUILD, eventArgs );

		// We set instance data from the event args so interceptors can modify them.
		this.sql				= eventArgs.getAsString( Key.sql );
		this.SQLWithParamValues	= this.sql;
		this.originalSql		= eventArgs.getAsString( Key.sql );
		this.parameters			= processBindings( eventArgs.get( Key.of( "bindings" ) ) );
		this.queryOptions		= eventArgs.getAs( QueryOptions.class, Key.options );

		// Create a cache key with a default or via the passed options.
		this.cacheKey			= getOrComputeCacheKey();
		this.cacheProvider		= BoxRuntime.getInstance().getCacheService().getCache( this.queryOptions.cacheProvider );
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string and a list of
	 * parameters.
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
	 * If a custom cache key was provided in the query options, it will be used.
	 * Otherwise, a cache key will be generated from a combined hash of the SQL
	 * string,parameter values, and relevant query options such as the `datasource`,
	 * `username`, and
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
	 * Retrieve the generated or provided-in-query-options cache key for this query.
	 */
	public String getCacheKey() {
		return this.cacheKey;
	}

	/**
	 * Processes the bindings provided to the constructor and returns a list of
	 * {@link QueryParameter} instances.
	 * Will also modify the SQL string to replace named parameters with positional
	 * placeholders.
	 *
	 * @param bindings The bindings to process. This can be an {@link Array} of
	 *                 values or a {@link IStruct} of named parameters.
	 *
	 * @return A list of {@link QueryParameter} instances.
	 */
	private List<QueryParameter> processBindings( Object bindings ) {
		if ( bindings == null ) {
			return new ArrayList<>();
		}
		CastAttempt<Array> castAsArray = ArrayCaster.attempt( bindings );
		if ( castAsArray.wasSuccessful() ) {
			Array castedArray = castAsArray.get();
			// Short circuit for empty arrays to simplify our checks below
			if ( castedArray.isEmpty() ) {
				return new ArrayList<>();
			}
			// An array could be an array of structs where a name is specified in each struct, which we massage back into a struct
			int		foundNames		= 0;
			IStruct	possibleStruct	= Struct.of();
			for ( Object item : castedArray ) {
				CastAttempt<IStruct> itemStructAttempt = StructCaster.attempt( item );
				if ( itemStructAttempt.wasSuccessful() ) {
					IStruct itemStruct = itemStructAttempt.get();
					if ( itemStruct.containsKey( Key._NAME ) ) {
						foundNames++;
						possibleStruct.put( Key.of( itemStruct.get( Key._NAME ) ), itemStruct );
					}
				}
			}
			// All items in the array are structs with names
			if ( foundNames == castedArray.size() ) {
				return buildParameterList( null, possibleStruct );
			} else if ( foundNames > 0 ) {
				throw new DatabaseException( "Invalid query params passed as array of structs. Some structs have a name, some do not." );
			}
			// No structs with names were found, or possbly no structs were found at all!
			return buildParameterList( castedArray, null );
		}

		CastAttempt<IStruct> castAsStruct = StructCaster.attempt( bindings );
		if ( castAsStruct.wasSuccessful() ) {
			return buildParameterList( null, castAsStruct.get() );
		}

		// We always have bindings, since we exit early if there are none
		String className = bindings.getClass().getName();
		throw new DatabaseException( "Invalid type for query params. Expected array or struct. Received: " + className );
	}

	/**
	 * Process a struct of named query bindings into a list of
	 * {@link QueryParameter} instances.
	 * <p>
	 * Also performs SQL string replacement to convert named parameters to
	 * positional placeholders.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters An `IStruct` of `String` `name` to either an `Object`
	 *                   `value` or a `queryparam` `IStruct`.
	 */
	@SuppressWarnings( { "null", "unchecked" } )
	private List<QueryParameter> buildParameterList( Array positionalParameters, IStruct namedParameters ) {
		List<QueryParameter> params = new ArrayList<>();
		// Short circuit for no parameters
		if ( positionalParameters == null && namedParameters == null ) {
			return params;
		} else if ( positionalParameters != null && positionalParameters.isEmpty() ) {
			return params;
		} else if ( namedParameters != null && namedParameters.isEmpty() ) {
			return params;
		}

		boolean			isPositional		= positionalParameters != null;
		String			SQL					= this.sql;
		// This is the SQL string with the named parameters replaced with positional placeholders
		StringBuilder	newSQL				= new StringBuilder();
		// This is the name of the current named parameter being processed
		StringBuilder	paramName			= new StringBuilder();
		// This is the current SQL token being processed. We'll save these for later when we apply the parameters.
		// We could techincally finalize this string now, but we'd end up casting all the values twice which seems inefficient.
		StringBuilder	SQLWithParamToken	= new StringBuilder();
		// Track the named params we encounter for validation below
		Set<Key>		foundNamedParams	= new HashSet<>();

		// 0 = Default state, processing SQL
		// 1 = Inside a string literal
		// 2 = Inside a single line comment
		// 3 = Inside a multi-line comment
		// 4 = Inside a named parameter
		int				state				= 0;

		// This should always match params.size(), but is a little easier to use
		int				paramsEncountered	= 0;
		// Pop this into a lambda so we can re-use it for the last named parameter
		Runnable		processNamed		= () -> {
												SQLWithParamTokens.add( SQLWithParamToken.toString() );
												SQLWithParamToken.setLength( 0 );
												Key finalParamName = Key.of( paramName.toString() );
												if ( isPositional ) {
													throw new DatabaseException(
													    "Named parameter [:" + finalParamName.getName() + "] found in query with positional parameters." );
												} else {
													if ( namedParameters.containsKey( finalParamName ) ) {
														QueryParameter newParam = QueryParameter.fromAny( namedParameters.get( finalParamName ) );
														foundNamedParams.add( finalParamName );
														params.add( newParam );
														// List params add ?, ?, ? etc. to the SQL string
														if ( newParam.isListParam() ) {
															List<Object> values = ( List<Object> ) newParam.getValue();
															newSQL.append( values.stream().map( v -> "?" ).collect( Collectors.joining( ", " ) ) );
														} else {
															newSQL.append( "?" );
														}
													} else {
														throw new DatabaseException(
														    "Named parameter [:" + finalParamName.getName() + "] not provided to query." );
													}
												}
											};

		for ( int i = 0; i < SQL.length(); i++ ) {
			char c = SQL.charAt( i );

			switch ( state ) {
				// Default state, processing SQL
				case 0 : {
					if ( c == '\'' ) {
						// If we've reached a ' then we're inside a string literal
						state = 1;
					} else if ( c == '-' && i < SQL.length() - 1 && SQL.charAt( i + 1 ) == '-' ) {
						// If we've reached a -- then we're inside a single line comment
						state = 2;
					} else if ( c == '/' && i < SQL.length() - 1 && SQL.charAt( i + 1 ) == '*' ) {
						// If we've reached a /* then we're inside a multi-line comment
						state = 3;
					} else if ( c == '?' ) {
						// We've encountered a positional parameter
						paramsEncountered++;
						if ( isPositional ) {
							if ( paramsEncountered > positionalParameters.size() ) {
								throw new DatabaseException( "Too few positional parameters [" + positionalParameters.size()
								    + "] provided for query having at least [" + paramsEncountered + "] '?' char(s)." );
							}

							SQLWithParamTokens.add( SQLWithParamToken.toString() );
							SQLWithParamToken.setLength( 0 );
							var				newParam	= QueryParameter.fromAny( positionalParameters.get( paramsEncountered - 1 ) );
							List<Object>	values;
							// List params add ?, ?, ? etc. to the SQL string
							if ( newParam.isListParam() && ( values = ( List<Object> ) newParam.getValue() ).size() > 1 ) {
								newSQL.append( "?, ".repeat( values.size() - 1 ) );
							}
							params.add( newParam );
							// append here and break so the ? doesn't go into the SQLWithParamToken
							newSQL.append( c );
							break;
						} else {
							throw new DatabaseException( "Positional parameter [?] found in query with named parameters." );
						}
					} else if ( c == ':' ) {
						// We've encountered a named parameter
						state = 4;
						// Do not append anything
						break;
					}
					newSQL.append( c );
					SQLWithParamToken.append( c );
					break;
				}
				// Inside a string literal
				case 1 : {
					// If we've reached the ending ' and it wasn't escaped as \' then we're done
					if ( c == '\'' && ( i == SQL.length() - 1 || SQL.charAt( i + 1 ) != '\'' ) ) {
						state = 0;
						// if we reached ' but the next char is also ' then this is just an escaped ''
						// Append them both and move on
					} else if ( c == '\'' && i < SQL.length() - 1 && SQL.charAt( i + 1 ) == '\'' ) {
						newSQL.append( c ); // Append the first single quote
						SQLWithParamToken.append( c );
						c = SQL.charAt( ++i ); // Skip the next single quote
					}
					newSQL.append( c );
					SQLWithParamToken.append( c );
					break;
				}
				// Inside a single line comment
				case 2 : {
					if ( c == '\n' || c == '\r' ) {
						state = 0;
					}
					newSQL.append( c );
					SQLWithParamToken.append( c );
					break;
				}
				// Inside a multi-line comment
				case 3 : {
					if ( c == '*' && i < SQL.length() - 1 && SQL.charAt( i + 1 ) == '/' ) {
						state = 0;
						newSQL.append( c );
						SQLWithParamToken.append( c );
						c = SQL.charAt( ++i );
					}
					newSQL.append( c );
					SQLWithParamToken.append( c );
					break;
				}
				// Inside a named parameter
				case 4 : {
					if ( ! ( Character.isLetterOrDigit( c ) || c == '_' ) ) {
						processNamed.run();
						paramName.setLength( 0 );
						// reset the state and backup to re-precess the next char again
						state = 0;
						i--;
						break;
					}
					paramName.append( c );
					break;
				}
			}
		}

		// If named param is the last thing in the query
		if ( state == 4 ) {
			processNamed.run();
		}

		// Make sure positional params were all used
		if ( isPositional && positionalParameters.size() > paramsEncountered ) {
			throw new DatabaseException( "Too many positional parameters [" + positionalParameters.size()
			    + "] provided for query having only [" + paramsEncountered + "] '?' char(s)." );
		} else if ( !isPositional && namedParameters.keySet().size() != foundNamedParams.size() ) {
			// Make sure all named params were used
			Set<Key> missingParams = new HashSet<>( namedParameters.keySet() );
			missingParams.removeAll( foundNamedParams );
			throw new DatabaseException( "Named parameter(s) [" + missingParams + "] provided to query were not used." );
		}

		SQLWithParamTokens.add( SQLWithParamToken.toString() );
		this.sql = newSQL.toString();
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
	 * Return final SQL with paramter values
	 * 
	 * @return
	 */
	public @Nonnull String getSQLWithParamValues() {
		return this.SQLWithParamValues;
	}

	/**
	 * Returns a list of parameter `Object` values from the `List<QueryParameter>`.
	 * If a parameter is a list type, its values are expanded into the main list.
	 *
	 * @return A list of parameter values as `Object`s.
	 */
	public @Nonnull List<Object> getParameterValues() {
		List<Object> values = new ArrayList<>();
		for ( QueryParameter param : this.parameters ) {
			if ( param.isListParam() ) {
				values.addAll( ( List<?> ) param.getValue() );
			} else {
				values.add( param.getValue() );
			}
		}
		return values;
	}

	/**
	 * Executes the PendingQuery using the provided ConnectionManager and returns
	 * the results in an {@link ExecutedQuery} instance.
	 *
	 * @param connectionManager The ConnectionManager instance to use for getting
	 *                          connections from the current context.
	 *
	 * @throws DatabaseException If a {@link SQLException} occurs, wraps it in a
	 *                           DatabaseException and throws.
	 *
	 * @return An ExecutedQuery instance with the results of this JDBC execution, as
	 *         well as a link to this PendingQuery instance.
	 *
	 * @see ExecutedQuery
	 */
	public @Nonnull ExecutedQuery execute( ConnectionManager connectionManager, IBoxContext context ) {
		// We do an early cache check here to avoid the overhead of creating a
		// connection if we already have a matching cached query.
		if ( isCacheable() ) {
			logger.debug( "Checking cache for query: {}", this.cacheKey );
			Attempt<Object> cachedQuery = cacheProvider.get( this.cacheKey );
			if ( cachedQuery.isPresent() ) {
				return respondWithCachedQuery( ( ExecutedQuery ) cachedQuery.get() );
			}
			logger.debug( "Query is NOT present, continuing to execute query: {}", this.cacheKey );
		}

		Connection connection = connectionManager.getConnection( this.queryOptions );
		try {
			return execute( connection, context );
		} finally {
			if ( connection != null ) {
				connectionManager.releaseConnection( connection );
			}
		}
	}

	/**
	 * Executes the PendingQuery on a given {@link Connection} and returns the
	 * results in an {@link ExecutedQuery} instance.
	 *
	 * @param connection The Connection instance to use for executing the query. It
	 *                   is the responsibility of the caller to close the connection
	 *                   after this method returns.
	 *
	 * @throws DatabaseException If a {@link SQLException} occurs, wraps it in a
	 *                           DatabaseException and throws.
	 *
	 * @return An ExecutedQuery instance with the results of this JDBC execution, as
	 *         well as a link to this PendingQuery instance.
	 *
	 * @see ExecutedQuery
	 */
	public @Nonnull ExecutedQuery execute( Connection connection, IBoxContext context ) {
		if ( isCacheable() ) {
			// we use separate get() and set() calls over a .getOrSet() so we can run
			// `.setIsCached()` on discovered/cached results.
			Attempt<Object> cachedQuery = this.cacheProvider.get( this.cacheKey );
			if ( cachedQuery.isPresent() ) {
				return respondWithCachedQuery( ( ExecutedQuery ) cachedQuery.get() );
			}

			ExecutedQuery executedQuery = executeStatement( connection, context );
			this.cacheProvider.set( this.cacheKey, executedQuery, this.queryOptions.cacheTimeout,
			    this.queryOptions.cacheLastAccessTimeout );
			return executedQuery;
		}
		return executeStatement( connection, context );
	}

	/**
	 * Generate and execute a JDBC statement using the provided connection.
	 * <p>
	 * * If query parameters are present, a {@link PreparedStatement} will be
	 * utilized and populated with the paremeter bindings. Otherwise, a standard
	 * {@link Statement} object will be used.
	 * * Will announce a `PRE_QUERY_EXECUTE` event before executing the query.
	 */
	private ExecutedQuery executeStatement( Connection connection, IBoxContext context ) {
		try {

			String sqlStatement = this.sql;
			try (
			    Statement statement = this.parameters.isEmpty()
			        ? connection.createStatement()
			        : connection.prepareStatement( sqlStatement, Statement.RETURN_GENERATED_KEYS ); ) {

				applyParameters( statement, context );
				applyStatementOptions( statement );

				interceptorService.announce(
				    BoxEvent.PRE_QUERY_EXECUTE,
				    Struct.of(
				        "sql", sqlStatement,
				        "bindings", getParameterValues(),
				        "pendingQuery", this ) );

				long	startTick	= System.currentTimeMillis();
				boolean	hasResults	= statement instanceof PreparedStatement preparedStatement
				    ? preparedStatement.execute()
				    : statement.execute( sqlStatement, Statement.RETURN_GENERATED_KEYS );
				long	endTick		= System.currentTimeMillis();

				return ExecutedQuery.fromPendingQuery(
				    this,
				    statement,
				    endTick - startTick,
				    hasResults );
			}
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
			    e );
		}
	}

	/**
	 * Helper method to respond with an ExecutedQuery instance from the given query
	 * cache lookup.
	 * <p>
	 * This method assumes cachedQuery.isPresent() has already been checked, and
	 * populates the ExecutedQuery instance with the query cache metadata, such as
	 * cacheKey, cacheProvider, etc.
	 */
	private ExecutedQuery respondWithCachedQuery( ExecutedQuery cachedQuery ) {
		logger.debug( "Query is present, returning cached result: {}", this.cacheKey );
		IStruct	cacheMeta	= Struct.of(
		    "cached", true,
		    "cacheKey", this.cacheKey,
		    "cacheProvider", this.cacheProvider.getName().toString(),
		    "cacheTimeout", this.queryOptions.cacheTimeout,
		    "cacheLastAccessTimeout", this.queryOptions.cacheLastAccessTimeout );
		Query	results		= cachedQuery.getResults();
		Struct	queryMeta	= new Struct( cachedQuery.getQueryMeta() );
		queryMeta.addAll( cacheMeta );
		results.setMetadata( queryMeta );
		return new ExecutedQuery( results, cachedQuery.getGeneratedKey() );
	}

	/**
	 * If this is a paramaterized query, apply the parameters to the provided statement.
	 * We will also take this opportunity to finalize the list of SQL tokens with the
	 * final param values to build the effective SQL string.
	 * 
	 * @throws SQLException
	 */
	private void applyParameters( Statement statement, IBoxContext context ) throws SQLException {
		if ( this.parameters.isEmpty() ) {
			return;
		}

		if ( statement instanceof PreparedStatement preparedStatement ) {
			StringBuilder	SQLWithParamValues	= new StringBuilder();
			// The param index starts from 1
			int				parameterIndex		= 1;
			int				SQLParamIndex		= 0;
			for ( QueryParameter param : this.parameters ) {
				SQLWithParamValues.append( SQLWithParamTokens.get( SQLParamIndex ) );
				Integer scaleOrLength = param.getScaleOrLength();
				if ( param.isListParam() ) {
					var		i		= 1;
					Array	list	= ( Array ) param.getValue();
					for ( Object value : list ) {
						Object casted = QueryColumnType.toSQLType( param.getType(), value, context );
						emitValueToSQL( SQLWithParamValues, casted, param.getType() );
						if ( i < list.size() ) {
							SQLWithParamValues.append( ", " );
						}
						if ( scaleOrLength == null ) {
							preparedStatement.setObject( parameterIndex, casted, param.getSqlTypeAsInt() );
						} else {
							preparedStatement.setObject( parameterIndex, casted, param.getSqlTypeAsInt(),
							    scaleOrLength );
						}
						parameterIndex++;
						i++;
					}
				} else {
					Object value = param.toSQLType( context );
					emitValueToSQL( SQLWithParamValues, value, param.getType() );
					if ( scaleOrLength == null ) {
						preparedStatement.setObject( parameterIndex, value, param.getSqlTypeAsInt() );
					} else {
						preparedStatement.setObject( parameterIndex, value, param.getSqlTypeAsInt(),
						    scaleOrLength );
					}
					parameterIndex++;
				}
				SQLParamIndex++;
			}
			SQLWithParamValues.append( SQLWithParamTokens.get( SQLParamIndex ) );
			this.SQLWithParamValues = SQLWithParamValues.toString();
			SQLWithParamTokens.clear();
		}
	}

	/**
	 * Emit a value to the SQL string, quoting it if necessary.
	 * 
	 * @param SQLWithParamValues The SQL string to append the value to.
	 * @param value              The value to append.
	 * @param type               The type of the value.
	 */
	private void emitValueToSQL( StringBuilder SQLWithParamValues, Object value, QueryColumnType type ) {
		boolean quoted = type == QueryColumnType.VARCHAR || type == QueryColumnType.CHAR || type == QueryColumnType.TIME || type == QueryColumnType.DATE
		    || type == QueryColumnType.TIMESTAMP || QueryColumnType.OBJECT == type || QueryColumnType.OTHER == type;
		if ( quoted ) {
			SQLWithParamValues.append( "'" );
		}
		SQLWithParamValues.append( StringCaster.attempt( value ).getOrSupply( () -> String.valueOf( value ) ) );
		if ( quoted ) {
			SQLWithParamValues.append( "'" );
		}
	}

	/**
	 * Apply query options to the provided {@link Statement} instance.
	 * <p>
	 * Any query options which pass through to the JDBC Statement interface will be
	 * applied here. This includes `queryTimeout`, `maxRows`, and `fetchSize`.
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
		 * username and password : To evaluate later due to security concerns of
		 * overriding datasources, not going to implement unless requested
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
