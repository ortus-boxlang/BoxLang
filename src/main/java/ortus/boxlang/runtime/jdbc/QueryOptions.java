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

import javax.annotation.Nullable;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
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
	 * The datasource setting - purposely left as an Object to allow to support both datasource string names and on-the-fly datasource struct configurations.
	 */
	private Object				datasource;

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
	 * The fetch size for the query. Should be preferred over `maxRows` for large result sets, as `maxrows` will only truncate further rows from the
	 * result, whereas `fetchsize` will prevent the retrieval of those rows in the first place.
	 *
	 * @see Statement#setFetchSize(int)
	 */
	private Integer				fetchSize;

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
		this.options			= options;
		this.resultVariableName	= options.getAsString( Key.result );
		this.username			= options.getAsString( Key.username );
		this.password			= options.getAsString( Key.password );
		this.queryTimeout		= options.getAsInteger( Key.timeout );
		Integer intMaxRows = options.getAsInteger( Key.maxRows );
		this.maxRows	= Long.valueOf( intMaxRows != null ? intMaxRows : -1 );
		this.fetchSize	= ( Integer ) options.getOrDefault( Key.fetchSize, 0 );
		this.datasource	= options.get( Key.datasource );

		determineReturnType();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the configured 'datasource' query option. This could be a string or a datasource configuration struct.
	 */
	public Object getDataSource() {
		return this.datasource;
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

	public Integer getFetchSize() {
		return this.fetchSize;
	}

	public Long getMaxRows() {
		return this.maxRows;
	}

	public String getReturnType() {
		return this.returnType;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
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
		result.put( "fetchSize", this.getFetchSize() );
		result.put( "setQueryTimeout", this.getQueryTimeout() );
		result.put( "setMaxRows", this.getMaxRows() );
		return result;
	}

}
