package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.jdbc.ConnectionManager;

public interface IJDBCCapableContext {

	public ConnectionManager getConnectionManager();
}
