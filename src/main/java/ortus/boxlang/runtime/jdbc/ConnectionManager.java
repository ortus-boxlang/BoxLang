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

/**
 * Manages the active JDBC Connection for the current request/thread/BoxLang context.
 *
 * Primrarily offers transactional context management by tracking whether the current context has an ongoing transaction and returning the appropriate
 * Connection object... However, this class also provides methods for retrieving a JDBC connection matching the datasource Key name or config Struct.
 */
public class ConnectionManager {

	private Transaction transaction;

	/**
	 * Check if we are executing inside a transaction.
	 *
	 * @return
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
	 *
	 * @return The BoxLang Transaction object, which manages an underlying JDBC Connection.
	 */
	public void setTransaction( Transaction transaction ) {
		this.transaction = transaction;
	}

	/**
	 * Set the active transaction for this request/thread/BoxLang context.
	 *
	 * @return The BoxLang Transaction object, which manages an underlying JDBC Connection.
	 */
	public void endTransaction() {
		this.transaction = null;
	}
}