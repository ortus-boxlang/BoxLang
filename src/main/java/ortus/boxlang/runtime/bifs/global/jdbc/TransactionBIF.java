package ortus.boxlang.runtime.bifs.global.jdbc;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IDBManagingContext;
import ortus.boxlang.runtime.jdbc.DBManager;
import ortus.boxlang.runtime.jdbc.Transaction;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Base class for all JDBC transaction BIFs.
 *
 * While this class is handy for resolving the transaction context, I would much prefer a simpler approach whereby this class can disappear entirely.
 */
abstract class TransactionBIF extends BIF {

	/**
	 * Appends an element to a list
	 *
	 * @param context The context in which the BIF is being invoked.
	 */
	public Transaction getTransactionForContext( IBoxContext context ) {
		DBManager dbManager = context.getParentOfType( IDBManagingContext.class ).getDBManager();
		if ( !dbManager.isInTransaction() ) {
			throw new BoxRuntimeException( "Transaction not started; Please place this method call inside a transaction{} block." );
		}
		return dbManager.getTransaction();
	}
}
