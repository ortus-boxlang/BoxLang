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
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * A transaction object that wraps a JDBC connection and provides transactional operations.
 */
public class Transaction {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The underlying JDBC connection.
	 */
	private Connection			connection;

	/**
	 * The datasource associated with this transaction.
	 */
	private DataSource			datasource;

	/**
	 * The transaction isolation level
	 */
	private Integer				isolationLevel			= null;

	/** The original transaction isolation level */
	private Integer				originalIsolationLevel	= null;

	/**
	 * Stores the savepoints used in this transaction, referenced from <code>transactionSetSavepoint( "mySavepoint" )</code> and
	 * <code>transactionRollback( "mySavepoint" )</code>.
	 *
	 * Each savepoint name uses a Key to avoid case sensitivity issues with the lookup, and each JDBC savepoint is created with the name in UPPERCASE for
	 * the same reason.
	 */
	private Map<Key, Savepoint>	savepoints				= new HashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor.
	 * <p>
	 * Note this constructor does NOT accept or open a connecton upon construction. This is so we avoid acquiring connections we don't use; i.e. for a
	 * transaction that may never execute a query. Instead, we only open the connection when <code>getConnection()</code> is first called.
	 *
	 * @param datasource The datasource associated with this transaction
	 */
	public Transaction( DataSource datasource ) {
		this.datasource = datasource;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set isolation level.
	 */
	public void setIsolationLevel( int isolationLevel ) {
		this.isolationLevel = isolationLevel;
	}

	/**
	 * Get the configured transaction isolation level.
	 */
	public int getIsolationLevel() {
		return this.isolationLevel;
	}

	/**
	 * Get (creating if none found) the connection associated with this transaction.
	 * <p>
	 * This method should be called by queries executed inside a transaction body to ensure they run on the correct (transactional) connection.
	 * Upon first execution, this method will acquire a connection from the datasource and store it for further use within the transaction.
	 */
	public Connection getConnection() {
		if ( this.connection == null ) {
			this.connection = this.datasource.getConnection();
			try {
				this.connection.setAutoCommit( false );

				if ( this.isolationLevel != null ) {
					this.originalIsolationLevel = this.connection.getTransactionIsolation();
					this.connection.setTransactionIsolation( this.isolationLevel );
				}
			} catch ( SQLException e ) {
				throw new DatabaseException( "Failed to begin transaction:", e );
			}
		}
		return this.connection;
	}

	/**
	 * Get the datasource associated with this transaction.
	 * <p>
	 * Useful for checking that a given query is using the same datasource as its wrapping transaction.
	 */
	public DataSource getDataSource() {
		return this.datasource;
	}

	/**
	 * Begin the transaction - essentially a no-nop, as the transaction is only started when the connection is first acquired.
	 */
	public void begin() {
	}

	/**
	 * Commit the transaction
	 */
	public void commit() {
		if ( this.connection == null ) {
			return;
		}
		try {
			this.connection.commit();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to commit transaction: " + e.getMessage(), e );
		}
	}

	/**
	 * Rollback the entire transaction.
	 *
	 * The transaction will be rolled back to the last committed point, and will ignore any set savepoints.
	 */
	public void rollback() {
		rollback( null );
	}

	/**
	 * Rollback the transaction up to the last (named) savepoint.
	 *
	 * @param savepoint The name of the savepoint to rollback to or NULL for no savepoint.
	 */
	public void rollback( String savepoint ) {
		if ( this.connection == null ) {
			return;
		}
		try {
			if ( savepoint != null ) {
				this.connection.rollback( savepoints.get( Key.of( savepoint ) ) );
			} else {
				this.connection.rollback();
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to rollback transaction:" + e.getMessage(), e );
		}
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 */
	public void setSavepoint( String savepoint ) {
		if ( this.connection == null ) {
			return;
		}
		try {
			savepoints.put( Key.of( savepoint ), this.connection.setSavepoint( savepoint.toUpperCase() ) );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to set savepoint: " + e.getMessage(), e );
		}
	}

	/**
	 * Shutdown the transaction by re-enabling auto commit mode and closing the connection to the database (i.e. releasing it back to the connection pool
	 * from whence it came.)
	 */
	public void end() {
		if ( this.connection == null ) {
			return;
		}
		try {
			if ( this.connection.getAutoCommit() ) {
				this.connection.setAutoCommit( true );
			}

			if ( this.isolationLevel != null ) {
				this.connection.setTransactionIsolation( this.originalIsolationLevel );
			}
			this.connection.close();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error closing connection: " + e.getMessage(), e );
		}
	}
}
