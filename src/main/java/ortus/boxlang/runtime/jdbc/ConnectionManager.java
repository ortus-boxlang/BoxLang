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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the active JDBC Connection for the current request/thread/BoxLang context.
 *
 * Primrarily offers transactional context management by tracking whether the current context has an ongoing transaction and returning the appropriate
 * Connection object... However, this class also provides methods for retrieving a JDBC connection matching the datasource Key name or config Struct.
 */
public class ConnectionManager {

	/**
	 * Logger
	 */
	private static final Logger	logger	= LoggerFactory.getLogger( ConnectionManager.class );

	/**
	 * The active transaction (if any) for this request/thread/BoxLang context.
	 *
	 * @TODO: Consider converting this to a HashMap of transactions (using some unique key?) to allow us to track multiple (nested) transactions.
	 */
	private Transaction			transaction;

	/**
	 * Check if we are executing inside a transaction.
	 *
	 * @return true if this ConnectionManager object has a registered transaction, which only exists while a Transaction component is executing.
	 */
	public boolean isInTransaction() {
		return transaction != null;
	}

	/**
	 * Get the active transaction (if any) for this request/thread/BoxLang context.
	 *
	 * @return The BoxLang Transaction object, which manages an underlying JDBC Connection.
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * Set the active transaction for this request/thread/BoxLang context.
	 */
	public void setTransaction( Transaction transaction ) {
		this.transaction = transaction;
	}

	/**
	 * Set the active transaction for this request/thread/BoxLang context.
	 */
	public void endTransaction() {
		this.transaction = null;
	}

	/**
	 * Get the active transaction (if any) for this request/thread/BoxLang context. If none is found, the provided datasource is used to create a new
	 * transaction which is then returned.
	 *
	 * @param datasource DataSource to use if creating a new transaction. Not currently used if a transaction already exists.
	 *
	 * @return The current executing transaction.
	 */
	public Transaction getOrSetTransaction( DataSource datasource ) {
		if ( isInTransaction() ) {
			return getTransaction();
		}
		transaction = new Transaction( datasource );
		setTransaction( transaction );
		return transaction;
	}

	/**
	 * Get a JDBC Connection to the specified datasource.
	 * <p>
	 * This method uses the following logic to pull the correct connection for the given query/context:
	 * <ol>
	 * <li>check for a transactional context.</li>
	 * <li>If an active transaction is found, this method compares the provided datasource against the transaction's datasource.</li>
	 * <li>If the datasources match, this method then checks the username/password authentication (if not null)</li>
	 * <li>if all those checks succeed, the transactional connection is returned.
	 * <li>if any of those checks fail, a new connection is returned from the provided datasource.</li>
	 * </ol>
	 *
	 * @param datasource The datasource to get a connection for.
	 * @param username   The username to use for authentication - will not check authentication if null.
	 * @param password   The password to use for authentication - will not check authentication if null.
	 *
	 * @return A JDBC Connection object, possibly from a transactional context.
	 */
	public Connection getConnection( DataSource datasource, String username, String password ) {
		if ( isInTransaction() ) {
			logger.atTrace()
			    .log( "Am inside transaction context; will check datasource and authentication to determine if we should return the transactional connection" );
			if ( getTransaction().getDataSource().isSameAs( datasource )
			    && ( username == null || getTransaction().getDataSource().isAuthenticationMatch( username, password ) ) ) {
				logger.atTrace().log(
				    "Both the query datasource argument and authentication matches; proceeding with established transactional connection" );
				return getTransaction().getConnection();
			} else {
				// A different datasource was specified OR the authentication check failed; thus this is NOT a transactional query and we should use a new
				// connection.
				logger.atTrace()
				    .log( "Datasource OR authentication does not match transaction; Will ignore transaction context and return a new JDBC connection" );
				return datasource.getConnection( username, password );
			}
		}
		logger.atTrace().log( "Not within transaction; obtaining new connection from pool" );
		return datasource.getConnection( username, password );
	}

	/**
	 * Get a JDBC Connection to the specified datasource.
	 * <p>
	 * This method uses the following logic to pull the correct connection for the given query/context:
	 * <ol>
	 * <li>check for a transactional context.</li>
	 * <li>If an active transaction is found, this method compares the provided datasource against the transaction's datasource.</li>
	 * <li>If the datasources match, the transactional connection is returned.
	 * <li>if not, a new connection is returned from the provided datasource.</li>
	 * </ol>
	 *
	 * @param datasource The datasource to get a connection for.
	 *
	 * @return A JDBC Connection object, possibly from a transactional context.
	 */
	public Connection getConnection( DataSource datasource ) {
		if ( isInTransaction() ) {
			logger.atTrace()
			    .log( "Am inside transaction context; will check datasource to determine if we should return the transactional connection" );
			if ( getTransaction().getDataSource().isSameAs( datasource ) ) {
				logger.atTrace().log(
				    "The query datasource matches the transaction datasource; proceeding with established transactional connection" );
				return getTransaction().getConnection();
			} else {
				// A different datasource was specified OR the authentication check failed; thus this is NOT a transactional query and we should use a new
				// connection.
				logger.atTrace()
				    .log( "Datasource does not match transaction; Will ignore transaction context and return a new JDBC connection" );
				return datasource.getConnection();
			}
		}
		return datasource.getConnection();
	}
}
