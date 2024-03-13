package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class Transaction {

	/**
	 * The underlying JDBC connection.
	 */
	private Connection	connection;

	/**
	 * The datasource associated with this transaction.
	 */
	private DataSource	datasource;

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
	 * Rollback the transaction
	 *
	 * @param savepoint The name of the savepoint to rollback to
	 */
	public void rollback( String savepoint ) {
		try {
			// @TODO: Implement savepoints!
			connection.rollback();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to rollback transaction:", e );
		}
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepointName The name of the savepoint
	 */
	public void setSavepoint( String savepointName ) {
		try {
			connection.setSavepoint( savepointName );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to set savepoint:", e );
		}
	}

	/**
	 * Shutdown the transaction; mainly handles closing the connection to the database.
	 */
	public void shutdown() {
		try {
			connection.close();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error closing connection:", e );
		}
	}
}
