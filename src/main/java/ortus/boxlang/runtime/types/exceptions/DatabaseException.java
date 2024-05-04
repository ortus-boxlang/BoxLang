/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Base exception for all database-related errors
 */
public class DatabaseException extends BoxLangException {

	public static final Key	NativeErrorCodeKey	= Key.of( "nativeErrorCode" );
	public static final Key	SQLStateKey			= Key.of( "SQLState" );
	public static final Key	SqlKey				= Key.of( "SQL" );
	public static final Key	queryErrorKey		= Key.of( "queryError" );
	public static final Key	whereKey			= Key.of( "where" );

	/**
	 * Native error code associated with exception. Database drivers typically provide error codes to diagnose failing
	 * database operations. Default value is -1.
	 */
	public String			nativeErrorCode		= "";
	/**
	 * SQLState associated with exception. Database drivers typically provide error codes to help diagnose failing database
	 * operations. Default value is 1.
	 */
	public String			SQLState			= "";
	/**
	 * The SQL statement sent to the data source.
	 */
	public String			SQL					= "";
	/**
	 * The error message as reported by the database driver.
	 */
	public String			queryError			= "";
	/**
	 * If the query uses the queryparam component, query parameter name-value pairs.
	 */
	public String			where				= "";

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public DatabaseException( String message ) {
		this( message, null, null, null, null, null, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public DatabaseException( String message, Throwable cause ) {
		this( message, "", null, null, null, null, null, cause );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param detail  The detail
	 */
	public DatabaseException( String message, String detail ) {
		this( message, detail, null, null, null, null, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param message         The message
	 * @param detail          The detail
	 * @param nativeErrorCode The native error code
	 * @param SQLState        The SQL state
	 * @param SQL             The SQL
	 * @param queryError      The query error
	 * @param where           The where
	 * @param cause           The cause
	 */
	public DatabaseException( String message, String detail, String nativeErrorCode, String SQLState, String SQL, String queryError, String where,
	    Throwable cause ) {
		super( message, "database", cause );
		this.detail				= detail;
		this.nativeErrorCode	= nativeErrorCode;
		this.SQLState			= SQLState;
		this.SQL				= SQL;
		this.queryError			= queryError;
		this.where				= where;
	}

	// getters

	/**
	 * Get the native error code
	 *
	 * @return The native error code
	 */
	public String getNativeErrorCode() {
		return nativeErrorCode;
	}

	/**
	 * Get the SQL state
	 *
	 * @return The SQL state
	 */
	public String getSQLState() {
		return SQLState;
	}

	/**
	 * Get the SQL
	 *
	 * @return The SQL
	 */
	public String getSQL() {
		return SQL;
	}

	/**
	 * Get the query error
	 *
	 * @return The query error
	 */
	public String getQueryError() {
		return queryError;
	}

	/**
	 * Get the where
	 *
	 * @return The where
	 */
	public String getWhere() {
		return where;
	}

}
