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
package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.jdbc.ConnectionManager;

/**
 * This interface is used mostly on the RequestBoxContext and ThreadBoxContext classes to provide access to the ConnectionManager
 * and other JDBC-related functionality.
 */
public interface IJDBCCapableContext {

	/**
	 * Get the ConnectionManager (connection and transaction tracker) for this context.
	 */
	public ConnectionManager getConnectionManager();

	/**
	 * Shutdown the ConnectionManager and release any resources.
	 */
	public void shutdownConnections();
}
