package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSourceManager;

public interface IJDBCCapableContext {

	/**
	 * Get the ConnectionManager (connection and transaction tracker) for this context.
	 */
	public ConnectionManager getConnectionManager();

	/**
	 * Get the DataSourceManager from the parent application if found, else return the local datasource manager, creating one if needed.
	 */
	public DataSourceManager getDataSourceManager();
}
