package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class Transaction {

	/**
	 * The underlying JDBC connection.
	 */
	private Connection			connection;

	/**
	 * The datasource associated with this transaction.
	 */
	private DataSource			datasource;

	/**
	 * Stores the savepoints used in this transaction, referenced from <code>transactionSetSavepoint( "mySavepoint" )</code> and
	 * <code>transactionRollback( "mySavepoint" )</code>.
	 *
	 * Each savepoint name uses a Key to avoid case sensitivity issues with the lookup, and each JDBC savepoint is created with the name in UPPERCASE for
	 * the same reason.
	 */
	private Map<Key, Savepoint>	savepoints	= new HashMap<>();

	/**
	 * Constructor.
	 * <p>
	 * Note this constructor does NOT accept or open a connecton upon construction. This is so we avoid acquiring connections we don't use; i.e. for a
	 * transaction that may never execute a query. Instead, we only open the connection when <code>getConnection()</code> is first called.
	 *
	 * @param datasource The datasource associated with this transaction
	 */
	public Transaction( DataSource datasource ) {
		this.datasource = datasource;
	}

	/**
	 * Get (creating if none found) the connection associated with this transaction.
	 * <p>
	 * This method should be called by queries executed inside a transaction body to ensure they run on the correct (transactional) connection.
	 * Upon first execution, this method will acquire a connection from the datasource and store it for further use within the transaction.
	 */
	public Connection getConnection() {
		if ( connection == null ) {
			this.connection = datasource.getConnection();
			try {
				this.connection.setAutoCommit( false );
			} catch ( SQLException e ) {
				throw new DatabaseException( "Failed to begin transaction:", e );
			}
		}
		return connection;
	}

	/**
	 * Get the datasource associated with this transaction.
	 * <p>
	 * Useful for checking that a given query is using the same datasource as its wrapping transaction.
	 */
	public DataSource getDataSource() {
		return datasource;
	}

	/**
	 * Begin the transaction - essentially a no-nop, as the transaction is only started when the connection is first acquired.
	 */
	public void begin() {
	}

	/**
	 * Commit the transaction
	 */
	public void commit() {
		if ( connection == null ) {
			return;
		}
		try {
			connection.commit();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to commit transaction: " + e.getMessage(), e );
		}
	}

	/**
	 * Rollback the entire transaction.
	 *
	 * The transaction will be rolled back to the last committed point, and will ignore any set savepoints.
	 */
	public void rollback() {
		rollback( null );
	}

	/**
	 * Rollback the transaction up to the last (named) savepoint.
	 *
	 * @param savepoint The name of the savepoint to rollback to or NULL for no savepoint.
	 */
	public void rollback( String savepoint ) {
		if ( connection == null ) {
			return;
		}
		try {
			if ( savepoint != null ) {
				connection.rollback( savepoints.get( Key.of( savepoint ) ) );
			} else {
				connection.rollback();
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to rollback transaction:" + e.getMessage(), e );
		}
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 */
	public void setSavepoint( String savepoint ) {
		if ( connection == null ) {
			return;
		}
		try {
			savepoints.put( Key.of( savepoint ), connection.setSavepoint( savepoint.toUpperCase() ) );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to set savepoint: " + e.getMessage(), e );
		}
	}

	/**
	 * Shutdown the transaction by re-enabling auto commit mode and closing the connection to the database (i.e. releasing it back to the connection pool
	 * from whence it came.)
	 */
	public void end() {
		if ( connection == null ) {
			return;
		}
		try {
			if ( connection.getAutoCommit() ) {
				connection.setAutoCommit( true );
			}
			connection.close();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error closing connection: " + e.getMessage(), e );
		}
	}
}
