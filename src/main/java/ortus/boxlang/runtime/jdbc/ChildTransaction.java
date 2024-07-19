package ortus.boxlang.runtime.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.UUID;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A child transaction object used for implementing nested JDBC transactions.
 * <p>
 * Utilizes savepoints to manage the nested transaction state:
 * <ul>
 * <li>On transaction begin, a <codeBEGIN</code> savepoint is created.</li>
 * <li>On transaction commit, a <code>COMMIT</code> savepoint is created.</li>
 * <li>On transaction rollback, the transaction is rolled back to the child transaction's <code>BEGIN</code> savepoint. (Unless a savepoint name is passed, in which case the transaction is rolled back to that savepoint only.)</li>
 * <li>On transaction end, an <code>END</code> savepoint is created.</li>
 * </ul>
 * <p>
 * The savepoint names are prefixed with a unique identifier to ensure that they don't collide with savepoints created in the parent transaction.
 */
public class ChildTransaction implements ITransaction {

	/**
	 * Logger
	 */
	private static final Logger	logger	= LoggerFactory.getLogger( ChildTransaction.class );
	/**
	 * The parent transaction.
	 */
	private ITransaction		parent;

	/**
	 * The prefix for savepoints created in this transaction.
	 * <p>
	 * This is used to ensure that savepoints created in this transaction are unique, i.e. they don't collide with the parent transaction's savepoints.
	 * 
	 */
	private final String		savepointPrefix;

	/**
	 * --------------------------------------------------------------------------
	 * Key constants used for savepoints demarcating the start and end of a child (nested) transaction.
	 * --------------------------------------------------------------------------
	 */
	private static final Key	BEGIN	= Key.of( "BEGIN" );
	private static final Key	END		= Key.of( "END" );
	private static final Key	COMMIT	= Key.of( "COMMIT" );

	/**
	 * Construct a nested transaction, attaching the given @param parent transaction.
	 *
	 * @param transaction The parent transaction
	 */
	public ChildTransaction( ITransaction parent ) {
		this.parent				= parent;
		this.savepointPrefix	= "CHILD_" + UUID.randomUUID().toString() + "_";
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * No-op for setting the isolation level on a nested transaction.
	 * <p>
	 * The isolation level is set on the parent transaction, and all child transactions inherit the isolation level from their parent.
	 * <p>
	 * Will throw a BoxRuntimeException if called.
	 * 
	 * @throws BoxRuntimeException
	 */
	public ChildTransaction setIsolationLevel( int isolationLevel ) {
		throw new BoxRuntimeException( "Cannot set isolation level on a nested transaction." );
	}

	/**
	 * Get the configured transaction isolation level.
	 */
	public int getIsolationLevel() {
		return this.parent.getIsolationLevel();
	}

	/**
	 * Get (creating if none found) the connection associated with the parent transaction.
	 */
	public Connection getConnection() {
		return this.parent.getConnection();
	}

	/**
	 * Get the datasource associated with this transaction.
	 * <p>
	 * Useful for checking that a given query is using the same datasource as its wrapping transaction.
	 */
	public DataSource getDataSource() {
		return this.parent.getDataSource();
	}

	/**
	 * Begin the transaction - essentially a no-nop, as the transaction is only started when the connection is first acquired.
	 */
	public ChildTransaction begin() {
		setSavepoint( ChildTransaction.BEGIN );
		return this;
	}

	/**
	 * Commit the transaction
	 */
	public ChildTransaction commit() {
		setSavepoint( ChildTransaction.COMMIT );
		return this;
	}

	/**
	 * Rollback the entire child transaction.
	 *
	 * The transaction will be rolled back to the last committed point, and will ignore any set savepoints.
	 */
	public ChildTransaction rollback() {
		return rollback( Key.nulls );
	}

	/**
	 * Rollback the child transaction up to the last (named) savepoint.
	 *
	 * @param savepoint The name of the savepoint to rollback to or NULL for no savepoint.
	 */
	public ChildTransaction rollback( Key savepoint ) {
		if ( savepoint == Key.nulls ) {
			savepoint = ChildTransaction.BEGIN;
		}
		logger.debug( "Rolling back child transaction to savepoint {}", this.savepointPrefix + savepoint );
		this.parent.rollback( Key.of( this.savepointPrefix + savepoint.getNameNoCase() ) );
		return this;
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 */
	public ChildTransaction setSavepoint( Key savepoint ) {
		this.parent.setSavepoint( Key.of( this.savepointPrefix + savepoint.getNameNoCase() ) );
		return this;
	}

	/**
	 * Shutdown the transaction by re-enabling auto commit mode and closing the connection to the database (i.e. releasing it back to the connection pool
	 * from whence it came.)
	 */
	public ChildTransaction end() {
		setSavepoint( ChildTransaction.END );
		// @TODO: Release all child savepoints, except possibly on Oracle which doesn't support savepoint release?
		// https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/Connection.html#releaseSavepoint(java.sql.Savepoint)
		return this;
	}

	/**
	 * Getter for the parent transaction
	 */
	public ITransaction getParent() {
		return this.parent;
	}
}
