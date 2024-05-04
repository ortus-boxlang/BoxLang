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
import javax.annotation.Nullable;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
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

	private static final Pattern				pattern				= Pattern.compile( ":\\w+" );

	/**
	 * The SQL string to execute.
	 * If this SQL has parameters, they should be represented either as question marks (`?`)
	 * or as named bindings, prefixed with a colon (`:`)
	 */
	private @Nonnull final String				sql;

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
	 * The query timeout in seconds.
	 */
	private @Nullable Integer					queryTimeout;

	/**
	 * The maximum number of rows to return from the query.
	 */
	private long								maxRows;

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
	public PendingQuery(
	    @Nonnull String sql,
	    @Nonnull List<QueryParameter> parameters,
	    @Nonnull String originalSql ) {
		this.sql			= sql;
		this.originalSql	= originalSql.trim();
		this.parameters		= parameters;
		this.maxRows		= 0L;
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string and a list of parameters.
	 * This constructor uses the provided SQL string as the original SQL.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters A list of {@link QueryParameter} to use as bindings.
	 */
	public PendingQuery( @Nonnull String sql, @Nonnull List<QueryParameter> parameters ) {
		this( sql, parameters, sql );
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string, a list of parameters, and the original SQL string.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters An {@link Array} of `queryparam` {@link IStruct} instances to convert to {@link QueryParameter} instances and use as bindings.
	 */
	public PendingQuery( @Nonnull String sql, @Nonnull Array parameters ) {
		this( sql, parameters.stream().map( QueryParameter::fromAny ).collect( Collectors.toList() ) );
	}

	/**
	 * Creates a new PendingQuery instance from a SQL string with no parameters.
	 *
	 * @param sql The SQL string to execute
	 */
	public PendingQuery( @Nonnull String sql ) {
		this( sql, new ArrayList<>(), sql );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new PendingQuery instance from a SQL string and an {@link IStruct} of named parameters.
	 * The `IStruct` should map the `String` `name` to either an `Object` `value` or a `queryparam` `IStruct`.
	 *
	 * @param sql        The SQL string to execute
	 * @param parameters An `IStruct` of `String` `name` to either an `Object` `value` or a `queryparam` `IStruct`.
	 */
	public static @Nonnull PendingQuery fromStructParameters( @Nonnull String sql, @Nonnull IStruct parameters ) {
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
		return new PendingQuery( matcher.replaceAll( "?" ), params, sql );
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
		if ( this.queryTimeout != null ) {
			statement.setQueryTimeout( this.queryTimeout );
		}

		if ( this.maxRows > 0 ) {
			statement.setLargeMaxRows( this.maxRows );
		}
	}

	public PendingQuery setQueryTimeout( @Nullable Integer queryTimeout ) {
		this.queryTimeout = queryTimeout;
		return this;
	}

	public PendingQuery setMaxRows( long maxRows ) {
		this.maxRows = maxRows;
		return this;
	}
}
