package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.jdbc.ConnectionManager;

public interface IJDBCCapableContext {

	/**
	 * Get the ConnectionManager (connection and transaction tracker) for this context.
	 */
	public ConnectionManager getConnectionManager();
}
