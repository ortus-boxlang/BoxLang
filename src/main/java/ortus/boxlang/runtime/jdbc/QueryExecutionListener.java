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
 * Lifecycle listener for JDBC query execution events.
 * <p>
 * Implementations can observe queries at three lifecycle points:
 * <ol>
 * <li>{@link #beforeExecution(PendingQuery)} — just before the SQL statement is sent to the driver</li>
 * <li>{@link #afterExecution(PendingQuery, ExecutedQuery)} — immediately after a successful execution</li>
 * <li>{@link #onError(PendingQuery, Exception)} — when an exception occurs during execution</li>
 * </ol>
 * <p>
 * Listener exceptions are caught and logged; they never interrupt query execution or propagate to callers.
 * <p>
 * Register listeners via {@link PendingQuery#addExecutionListener(QueryExecutionListener)} and remove them
 * via {@link PendingQuery#removeExecutionListener(QueryExecutionListener)}.
 */
public interface QueryExecutionListener {

	/**
	 * Called just before the SQL statement is sent to the JDBC driver.
	 * <p>
	 * The {@code pendingQuery} provides access to the SQL text, bound parameters,
	 * and query options. Implementations <em>must not</em> mutate query state here.
	 *
	 * @param pendingQuery The query about to be executed.
	 */
	void beforeExecution( PendingQuery pendingQuery );

	/**
	 * Called immediately after a query completes successfully.
	 *
	 * @param pendingQuery  The query that was executed.
	 * @param executedQuery The result produced by execution, including rows, generated keys, and timing.
	 */
	void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery );

	/**
	 * Called when an exception occurs during query execution.
	 * <p>
	 * This method is invoked for any {@link Exception} thrown by the JDBC driver or BoxLang's
	 * query execution infrastructure. The exception is still re-thrown to the caller after all
	 * listeners have been notified.
	 *
	 * @param pendingQuery The query that failed.
	 * @param exception    The exception that was raised.
	 */
	void onError( PendingQuery pendingQuery, Exception exception );
}