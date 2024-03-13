package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.jdbc.DBManager;

public interface IDBManagingContext {

	public DBManager getDBManager();
}
