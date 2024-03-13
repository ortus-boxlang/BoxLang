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

	private static final DataSourceManager	manager	= DataSourceManager.getInstance();

	private DataSource						datasource;
	private IStruct							options;
	private @Nullable String				resultVariableName;
	private String							returnType;
	private String							columnKey;
	private String							username;
	private String							password;
	private Integer							queryTimeout;
	private Long							maxRows;
	private DBManager						dbManager;

	/**
	 * Read in the provided query options and set private fields accordingly.
	 * <p>
	 * Will throw BoxRuntimeExceptions if certain options are not valid, such as an unknown <code>datasource</code> or <code>returnType</code>.
	 *
	 * @param dbManager The database manager, which is a contextual transaction and connection state object used to retrieve the correct connection for
	 *                  the query. This is important for executing a query within a transaction.
	 *
	 * @param options   Struct of query options. Backwards-compatible with the old-style <code>&lt;cfquery&gt;</code> from CFML.
	 */
	public QueryOptions( DBManager dbManager, IStruct options ) {
		this.dbManager	= dbManager;
		this.options	= options;
		determineDataSource();
		determineReturnType();
		this.resultVariableName	= options.getAsString( Key.result );
		this.username			= options.getAsString( Key.username );
		this.password			= options.getAsString( Key.password );
		this.queryTimeout		= options.getAsInteger( Key.timeout );
		Integer intMaxRows = options.getAsInteger( Key.maxRows );
		this.maxRows = Long.valueOf( intMaxRows != null ? intMaxRows : -1 );
	}

	public DBManager getDBManager() {
		return this.dbManager;
	}

	public DataSource getDataSource() {
		return this.datasource;
	}

	/**
	 * Get a connection to the configured datasource, optionally passing the `username` and `password` options if defined.
	 *
	 * @return A connection to the configured datasource.
	 */
	public Connection getConnnection() {
		// @TODO: If a datasource is configured on this query which does not match the Transaction's datasource, we should execute this query upon a new,
		// separate connection. Else we'll end up running the query against the wrong datasource. It would be good to test this in ACF and Lucee, but I'm 99%
		// sure this is the case there as well.
		if ( getDBManager().isInTransaction() ) {
			return getDBManager().getTransaction().getConnection();
		} else if ( wantsUsernameAndPassword() ) {
			return getDataSource().getConnection( getUsername(), getPassword() );
		} else {
			return getDataSource().getConnection();
		}
	}

	private boolean wantsUsernameAndPassword() {
		return this.username != null;
	}

	private String getUsername() {
		return this.username;
	}

	private String getPassword() {
		return this.password;
	}

	public boolean wantsResultStruct() {
		return this.resultVariableName != null;
	}

	public @Nullable String getResultVariableName() {
		return this.resultVariableName;
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
	 * Set the `datasource` field based on the `datasource` query option, if defined, or the default datasource if not.
	 *
	 * An exception will be thrown for any of these circumstances:
	 * <ul>
	 * <li>A datasource string name is passed which cannot be found
	 * <li>A datasource struct is passed which is not a valid datasource or a datasource connection could not be made.
	 * <li>No datasource is provided and no default datasource has been defined in this application.
	 * </ul>
	 */
	private void determineDataSource() {
		if ( this.options.containsKey( "datasource" ) ) {
			Object					datasourceObject	= this.options.get( Key.datasource );
			CastAttempt<IStruct>	datasourceAsStruct	= StructCaster.attempt( datasourceObject );
			if ( datasourceAsStruct.wasSuccessful() ) {
				this.datasource = DataSource.fromDataSourceStruct( datasourceAsStruct.getOrFail() );
			} else {
				CastAttempt<String>	datasourceAsString	= StringCaster.attempt( datasourceObject );
				String				datasourceName		= datasourceAsString.getOrFail();
				this.datasource = manager.getDataSource( Key.of( datasourceName ) );
				if ( this.datasource == null ) {
					throw new BoxRuntimeException( "No [" + datasourceName + "] datasource defined." );
				}
			}
		} else {
			this.datasource = manager.getDefaultDataSource();
			if ( this.datasource == null ) {
				throw new BoxRuntimeException(
				    "No default datasource has been defined. Either register a default datasource or provide a datasource name in the query options." );
			}
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

	public Integer getQueryTimeout() {
		return queryTimeout;
	}

	public Long getMaxRows() {
		return maxRows;
	}
}
