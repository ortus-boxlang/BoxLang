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

	public Transaction( DataSource datasource, Connection connection ) {
		this.datasource	= datasource;
		this.connection	= connection;
	}

	public Connection getConnection() {
		return connection;
	}

	/**
	 * Begin the transaction by setting autoCommit to false on the underlying connection.
	 */
	public void begin() {
		try {
			connection.setAutoCommit( false );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to begin transaction:", e );
		}
	}

	/**
	 * Commit the transaction
	 */
	public void commit() {
		try {
			connection.commit();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to commit transaction:", e );
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
		try {
			if ( savepoint != null ) {
				connection.rollback( savepoints.get( Key.of( savepoint ) ) );
			} else {
				connection.rollback();
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to rollback transaction:", e );
		}
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 */
	public void setSavepoint( String savepoint ) {
		try {
			savepoints.put( Key.of( savepoint ), connection.setSavepoint( savepoint.toUpperCase() ) );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to set savepoint:", e );
		}
	}

	/**
	 * Shutdown the transaction by re-enabling auto commit mode and closing the connection to the database (i.e. releasing it back to the connection pool
	 * from whence it came.)
	 */
	public void end() {
		try {
			if ( connection.getAutoCommit() ) {
				connection.setAutoCommit( true );
			}
			connection.close();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error closing connection:", e );
		}
	}
}
