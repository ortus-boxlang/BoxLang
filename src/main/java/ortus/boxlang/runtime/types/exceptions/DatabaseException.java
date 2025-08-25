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

import java.sql.SQLException;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This exception is the base exception for all database-related errors in the BoxLang runtime.
 */
public class DatabaseException extends BoxLangException {

	/**
	 * Native error code associated with exception. Database drivers typically provide error codes to diagnose failing
	 * database operations. Default value is -1.
	 */
	protected String	nativeErrorCode	= "";
	/**
	 * SQLState associated with exception. Database drivers typically provide error codes to help diagnose failing database
	 * operations. Default value is 1.
	 */
	protected String	SQLState		= "";
	/**
	 * The SQL statement sent to the data source.
	 */
	protected String	SQL				= "";
	/**
	 * The error message as reported by the database driver.
	 */
	protected String	queryError		= "";
	/**
	 * If the query uses the queryparam component, query parameter name-value pairs.
	 */
	protected String	where			= "";

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
	 * @param cause The cause
	 */
	public DatabaseException( Throwable cause ) {
		this( cause.getMessage(), cause );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public DatabaseException( String message, SQLException cause ) {
		this( message, cause.getSQLState(), String.valueOf( cause.getErrorCode() ), cause.getMessage(), null, null, null, cause );
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
		super( message, "database", nestSQLExceptions( cause ) );
		this.detail				= detail;
		this.nativeErrorCode	= nativeErrorCode;
		this.SQLState			= SQLState;
		this.SQL				= SQL;
		this.queryError			= queryError;
		this.where				= where;
	}

	static private Throwable nestSQLExceptions( Throwable cause ) {
		if ( cause == null ) {
			return null;
		}
		if ( cause instanceof SQLException se ) {
			while ( se.getNextException() != null ) {
				se = ( SQLException ) se.getNextException().initCause( se );
			}
			return se;
		}

		return cause;
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

	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		result.put( Key.nativeErrorCode, nativeErrorCode );
		result.put( Key.SQLState, SQLState );
		result.put( Key.sql, SQL );
		result.put( Key.queryError, queryError );
		result.put( Key.where, where );
		return result;
	}

}
