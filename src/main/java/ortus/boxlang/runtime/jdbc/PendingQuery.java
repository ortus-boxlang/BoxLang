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

import ortus.boxlang.runtime.BoxRuntime;
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
 * After calling {@link #execute(Connection)}, it returns an {@link ExecutedQuery} with a reference to this object.
 */
public class PendingQuery {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The InterceptorService instance to use for announcing events.
	 */
	private static final InterceptorService		interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * A pattern to match named parameters in the SQL string.
	 */
	private static final Pattern				pattern				= Pattern.compile( ":\\w+" );

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
	 * Struct of query options from the original BoxLang code.
	 * <p>
	 * Used to set options on the Statement or PreparedStatement before executing:
	 * <ul>
	 * <li>maxRows
	 * <li>fetchSize
	 * <li>queryTimeout
	 * <li>etc, etc.
	 * </ul>
	 *
	 * @see QueryOptions#toStruct()
	 */
	private IStruct								queryOptions;

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
	public PendingQuery( @Nonnull String sql, Object bindings, IStruct queryOptions ) {
		this.sql			= sql;
		this.originalSql	= sql.trim();
		this.queryOptions	= queryOptions;
		this.parameters		= processBindings( bindings );

		interceptorService.announce(
		    BoxEvent.ON_QUERY_BUILD,
		    Struct.of(
		        "sql", this.originalSql,
		        "parameters", this.parameters,
		        "pendingQuery", this,
		        "options", queryOptions
		    )
		);
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string and a list of parameters.
	 * This constructor uses the provided SQL string as the original SQL.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters A list of {@link QueryParameter} to use as bindings.
	 */
	public PendingQuery( @Nonnull String sql, @Nonnull List<QueryParameter> parameters ) {
		this( sql, parameters, new Struct() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
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
	 * Creates a new PendingQuery instance from a SQL string, a list of parameters, and the original SQL string.
	 *
	 * @param parameters An {@link Array} of `queryparam` {@link IStruct} instances to convert to {@link QueryParameter} instances and use as bindings.
	 */
	private List<QueryParameter> buildParameterList( @Nonnull Array parameters ) {
		return parameters.stream().map( QueryParameter::fromAny ).collect( Collectors.toList() );
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string and an {@link IStruct} of named parameters.
	 * The `IStruct` should map the `String` `name` to either an `Object` `value` or a `queryparam` `IStruct`.
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
				throw new BoxRuntimeException( "Missing param in query: [" + paramName + "]. SQL: " + sql );
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
	 * Executes the PendingQuery on a given {@link Connection} and returns the results in an {@link ExecutedQuery} instance.
	 *
	 * @param conn The Connection to execute this PendingQuery on.
	 *
	 * @throws DatabaseException If a {@link SQLException} occurs, wraps it in a DatabaseException and throws.
	 *
	 * @return An ExecutedQuery instance with the results of this JDBC execution, as well as a link to this PendingQuery instance.
	 *
	 * @see ExecutedQuery
	 */
	public @Nonnull ExecutedQuery execute( @Nonnull Connection conn ) {
		try {
			if ( this.parameters.isEmpty() ) {
				return executeStatement( conn );
			} else {
				return executePreparedStatement( conn );
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
			    e
			);
		}
	}

	private ExecutedQuery executeStatement( Connection conn ) throws SQLException {
		// @TODO: Consider refactoring this to use a try-with-resources block, as the ExecutedQuery
		// should not need the Statement object once the constructor completes and returns.
		Statement statement = conn.createStatement();

		applyStatementOptions( statement );

		interceptorService.announce(
		    BoxEvent.PRE_QUERY_EXECUTE,
		    Struct.of(
		        "sql", getOriginalSql(),
		        "bindings", getParameterValues(),
		        "pendingQuery", this
		    )
		);

		long	startTick	= System.currentTimeMillis();
		boolean	hasResults	= statement.execute( this.sql, Statement.RETURN_GENERATED_KEYS );
		long	endTick		= System.currentTimeMillis();

		// @TODO: Close the statement to prevent resource leaks!
		return new ExecutedQuery(
		    this,
		    statement,
		    endTick - startTick,
		    hasResults
		);
	}

	private ExecutedQuery executePreparedStatement( Connection conn ) throws SQLException {
		PreparedStatement statement = conn.prepareStatement( this.sql, Statement.RETURN_GENERATED_KEYS );
		// The param index starts from 1
		for ( int i = 1; i <= this.parameters.size(); i++ ) {
			QueryParameter	param			= this.parameters.get( i - 1 );
			Integer			scaleOrLength	= param.getScaleOrLength();
			if ( scaleOrLength == null ) {
				statement.setObject( i, param.getValue(), param.getSqlTypeAsInt() );
			} else {
				statement.setObject( i, param.getValue(), param.getSqlTypeAsInt(), scaleOrLength );
			}
		}

		applyStatementOptions( statement );

		interceptorService.announce(
		    BoxEvent.PRE_QUERY_EXECUTE,
		    Struct.of(
		        "sql", getOriginalSql(),
		        "bindings", getParameterValues(),
		        "pendingQuery", this
		    )
		);

		long	startTick	= System.currentTimeMillis();
		boolean	hasResults	= statement.execute();
		long	endTick		= System.currentTimeMillis();

		return new ExecutedQuery(
		    this,
		    statement,
		    endTick - startTick,
		    hasResults
		);
	}

	private void applyStatementOptions( Statement statement ) throws SQLException {
		if ( this.queryOptions.containsKey( Key.queryTimeout ) ) {
			Integer queryTimeout = ( Integer ) this.queryOptions.getOrDefault( Key.queryTimeout, 0 );
			if ( queryTimeout > 0 ) {
				statement.setQueryTimeout( queryTimeout );
			}
		}

		if ( this.queryOptions.containsKey( Key.maxRows ) ) {
			Integer maxRows = ( Integer ) this.queryOptions.getOrDefault( Key.maxRows, 0 );
			if ( maxRows > 0 ) {
				statement.setLargeMaxRows( maxRows );
			}
		}
		if ( this.queryOptions.containsKey( Key.fetchSize ) ) {
			Integer fetchSize = ( Integer ) this.queryOptions.getOrDefault( Key.fetchSize, 0 );
			if ( fetchSize > 0 ) {
				statement.setFetchSize( fetchSize );
			}
		}
		/**
		 * TODO: Implement the following options:
		 *
		 * timezone
		 * dbtype
		 * username
		 * password
		 * blockfactor
		 * cachedAfter
		 * cachedWithin
		 * debug
		 * ormoptions
		 * cacheID
		 * cacheRegion
		 * clientInfo
		 * fetchClientInfo
		 * lazy
		 * psq
		 */
	}
}
