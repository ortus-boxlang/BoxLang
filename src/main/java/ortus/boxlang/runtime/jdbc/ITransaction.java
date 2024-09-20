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

import ortus.boxlang.runtime.scopes.Key;

/**
 * A transaction interface which defines transaction management methods for JDBC connections.
 * <p>
 * There are two implementations of this interface: {@link Transaction} and {@link ChildTransaction}, which represent top-level and nested transactions respectively.
 */
public interface ITransaction {

	/**
	 * Set isolation level.
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction setIsolationLevel( int isolationLevel );

	/**
	 * Get the configured transaction isolation level.
	 */
	public int getIsolationLevel();

	/**
	 * Get (creating if none found) the connection associated with this transaction.
	 * <p>
	 * This method should be called by queries executed inside a transaction body to ensure they run on the correct (transactional) connection.
	 * Upon first execution, this method will acquire a connection from the datasource and store it for further use within the transaction.
	 */
	public Connection getConnection();

	/**
	 * Set the datasource associated with this transaction.
	 * <p>
	 * For transactions not initialized with a datasource, allows you to set the datasource after construction.
	 * <p>
	 * Will throw an exception if the datasource is already set.
	 */
	public ITransaction setDataSource( DataSource datasource );

	/**
	 * Get the datasource associated with this transaction.
	 * <p>
	 * Useful for checking that a given query is using the same datasource as its wrapping transaction.
	 */
	public DataSource getDataSource();

	/**
	 * Begin the transaction - essentially a no-nop, as the transaction is only started when the connection is first acquired. (think: when the first query is executed.)
	 */
	public ITransaction begin();

	/**
	 * Commit the transaction
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction commit();

	/**
	 * Rollback the entire transaction.
	 *
	 * The transaction will be rolled back to the last committed point, and will ignore any set savepoints.
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction rollback();

	/**
	 * Rollback the transaction up to the last (named) savepoint.
	 *
	 * @param savepoint The name of the savepoint to rollback to or NULL for no savepoint.
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction rollback( Key savepoint );

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction setSavepoint( Key savepoint );

	/**
	 * Shutdown the transaction by re-enabling auto commit mode and closing the connection to the database (i.e. releasing it back to the connection pool
	 * from whence it came.)
	 * 
	 * @return The transaction object for chainability.
	 */
	public ITransaction end();
}