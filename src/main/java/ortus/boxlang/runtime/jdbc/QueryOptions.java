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

import javax.annotation.Nullable;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
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
	 * The DataSource object to use for executions
	 */
	private DataSource			datasource;

	/**
	 * The query options struct
	 */
	private IStruct				options;

	/**
	 * The result variable name
	 */
	private @Nullable String	resultVariableName;

	/**
	 * The return type of the query. Available options are "query", "array", or "struct".
	 */
	private String				returnType;

	/**
	 * The column key to use when returning a struct.
	 */
	private String				columnKey;

	/**
	 * The datasource username to use for the connection, if any
	 */
	private String				username;

	/**
	 * The datasource password to use for the connection, if any
	 */
	private String				password;

	/**
	 * The query timeout in seconds
	 */
	private Integer				queryTimeout;

	/**
	 * The maximum number of rows to return from the query, defaults to all
	 */
	private Long				maxRows;

	/**
	 * The JDBC connection manager, which is a contextual transaction and connection state object used to retrieve the correct connection for the query.
	 */
	private ConnectionManager	connectionManager;

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
	 * @param datasourceService The datasource service, which is a registry of configured datasources.
	 * @param connectionManager The JDBC connection manager, which is a contextual transaction and connection state object used to retrieve the correct
	 *                          connection for
	 *                          the query. This is important for executing a query within a transaction.
	 *
	 * @param options           Struct of query options. Backwards-compatible with the old-style <code>&lt;cfquery&gt;</code> from CFML.
	 */
	public QueryOptions( ConnectionManager connectionManager, IStruct options ) {
		this.connectionManager	= connectionManager;
		this.options			= options;
		this.resultVariableName	= options.getAsString( Key.result );
		this.username			= options.getAsString( Key.username );
		this.password			= options.getAsString( Key.password );
		this.queryTimeout		= options.getAsInteger( Key.timeout );
		Integer intMaxRows = options.getAsInteger( Key.maxRows );
		this.maxRows = Long.valueOf( intMaxRows != null ? intMaxRows : -1 );

		determineDataSource();
		determineReturnType();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the configured datasource.
	 *
	 * @return The configured datasource.
	 */
	public DataSource getDataSource() {
		return this.datasource;
	}

	/**
	 * Get a connection to the configured datasource, optionally passing the `username` and `password` options if defined.
	 *
	 * @return A connection to the configured datasource.
	 */
	public Connection getConnnection() {
		if ( wantsUsernameAndPassword() ) {
			return this.connectionManager.getConnection( getDataSource(), this.username, this.password );
		} else {
			return this.connectionManager.getConnection( getDataSource() );
		}
	}

	/**
	 * Do we want a result struct
	 *
	 * @return True if the query should return a struct, false otherwise.
	 */
	public boolean wantsResultStruct() {
		return this.resultVariableName != null;
	}

	/**
	 * Get the result variable name, if any.
	 *
	 * @return The result variable name, if any.
	 */
	public @Nullable String getResultVariableName() {
		return this.resultVariableName;
	}

	public Integer getQueryTimeout() {
		return this.queryTimeout;
	}

	public Long getMaxRows() {
		return this.maxRows;
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
	private boolean wantsUsernameAndPassword() {
		return this.username != null;
	}

	/**
	 * Determines the datasource to use according to the options and/or BoxLang Defaults
	 */
	private void determineDataSource() {
		if ( this.options.containsKey( "datasource" ) ) {
			var						datasourceObject	= this.options.get( Key.datasource );
			CastAttempt<IStruct>	datasourceAsStruct	= StructCaster.attempt( datasourceObject );

			// ON THE FLY DATASOURCE
			if ( datasourceAsStruct.wasSuccessful() ) {
				this.datasource = this.connectionManager.getOnTheFlyDataSource( datasourceAsStruct.get() );
			}
			// NAMED DATASOURCE
			else if ( datasourceObject instanceof String datasourceName ) {
				this.datasource = this.connectionManager.getDatasourceOrThrow( Key.of( datasourceName ) );
			}
			// INVALID DATASOURCE
			else {
				throw new BoxRuntimeException( "Invalid datasource type: " + datasourceObject.getClass().getName() );
			}
		} else {
			this.datasource = this.connectionManager.getDefaultDatasourceOrThrow();
		}
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
					throw new BoxRuntimeException( "You must defined a `columnKey` option when using `returnType: struct`." );
				}
				this.returnType = "struct";
			}
			default -> throw new BoxRuntimeException( "Unknown return type: " + returnTypeString );
		}
	}

}
