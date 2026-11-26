package ortus.boxlang.runtime.bifs.global.jdbc;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.ITransaction;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Base class for all JDBC transaction BIFs.
 *
 * While this class is handy for resolving the transaction context, I would much prefer a simpler approach whereby this class can disappear entirely.
 */
abstract class TransactionBIF extends BIF {

	/**
	 * Gets the transaction which is active in the given context.
	 *
	 * @param context The context in which the BIF is being invoked.
	 *
	 * @return The current transaction.
	 *
	 * @throws BoxRuntimeException If there is no active transaction, that is, the call is not inside a transaction block.
	 */
	public ITransaction getTransactionForContext( IBoxContext context ) {
		ConnectionManager connectionManager = context.getParentOfType( IJDBCCapableContext.class ).getConnectionManager();
		if ( !connectionManager.isInTransaction() ) {
			throw new BoxRuntimeException( "Transaction not started; Please place this method call inside a transaction{} block." );
		}
		return connectionManager.getTransaction();
	}
}
