package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.RandomStringGenerator;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;

/**
 * A child transaction object used for implementing nested JDBC transactions.
 * <p>
 * Utilizes savepoints to manage the nested transaction state:
 * <ul>
 * <li>On transaction begin, a &lt;code&gt;BEGIN&lt;code&gt; savepoint is created.</li>
 * <li>On transaction commit, a &lt;code&gt;COMMIT&lt;code&gt; savepoint is created.</li>
 * <li>On transaction rollback, the transaction is rolled back to the child transaction's &lt;code&gt;BEGIN&lt;code&gt; savepoint. (Unless a savepoint name is passed, in which case the transaction is rolled back to that savepoint only.)</li>
 * <li>On transaction end, an &lt;code&gt;END&lt;code&gt; savepoint is created.</li>
 * </ul>
 * <p>
 * The savepoint names are prefixed with a unique identifier to ensure that they don't collide with savepoints created in the parent transaction.
 */
public class ChildTransaction implements ITransaction {

	/**
	 * Logger
	 */
	private static final BoxLangLogger	logger		= BoxRuntime.getInstance().getLoggingService().DATASOURCE_LOGGER;
	/**
	 * The parent transaction.
	 */
	private ITransaction				parent;

	/**
	 * The underlying JDBC connection.
	 */
	private BoxConnection				connection;

	/**
	 * The prefix for savepoints created in this transaction.
	 * <p>
	 * This is used to ensure that savepoints created in this transaction are unique, i.e. they don't collide with the parent transaction's savepoints.
	 *
	 */
	private final String				savepointPrefix;

	/**
	 * --------------------------------------------------------------------------
	 * Key constants used for savepoints demarcating the start and end of a child (nested) transaction.
	 * --------------------------------------------------------------------------
	 */
	private static final Key			BEGIN		= Key.of( "BEGIN" );
	private static final Key			END			= Key.of( "END" );
	private static final Key			COMMIT		= Key.of( "COMMIT" );

	/**
	 * Stores the savepoints used in this transaction, referenced from <code>transactionSetSavepoint( "mySavepoint" )</code> and
	 * <code>transactionRollback( "mySavepoint" )</code>.
	 *
	 * Each savepoint name uses a Key to avoid case sensitivity issues with the lookup, and each JDBC savepoint is created with the name in UPPERCASE for
	 * the same reason.
	 */
	private Map<Key, Savepoint>			savepoints	= new HashMap<>();

	/**
	 * Construct a nested transaction, attaching the given @param parent transaction.
	 *
	 * @param parent The parent transaction to attach to.
	 */
	public ChildTransaction( ITransaction parent ) {
		this.parent = parent;
		RandomStringGenerator generator = new RandomStringGenerator.Builder()
		    .withinRange( '0', 'z' )
		    .filteredBy( Character::isLetterOrDigit )
		    .build();

		this.savepointPrefix = ( "CHILD_" + generator.generate( 12 ) + "_" ).toUpperCase();
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
	 * Will log a warning if called; otherwise no action taken.
	 */
	public ChildTransaction setIsolationLevel( int isolationLevel ) {
		logger.warn(
		    "Cannot set isolation level on a nested transaction. No action required; this nested transaction will use the isolation level defined by the parent" );
		return this;
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
	public BoxConnection getBoxConnection() {
		if ( this.connection == null ) {
			this.connection = this.parent.getBoxConnection();
			// now that we've obtained a connection, we can "begin" the child transaction.
			if ( !this.savepoints.containsKey( generateSavepointKey( ChildTransaction.BEGIN ) ) ) {
				begin();
			}
		}
		return this.connection;
	}

	/**
	 * Get (creating if none found) the connection associated with the parent transaction.
	 * 
	 * This method is deprecated. Use getBoxConnection() instead.
	 */
	@Deprecated
	public Connection getConnection() {
		return getBoxConnection();
	}

	/**
	 * Set the datasource on the parent transaction.
	 * <p>
	 * Calls the same method on the parent transaction, allowing the child transaction to inherit the datasource. Will no-op if the parent transaction already has a datasource set.
	 */
	public ChildTransaction setDataSource( DataSource datasource ) {
		if ( this.parent.getDataSource() != null ) {
			return this;
		}
		this.parent.setDataSource( datasource );
		return this;
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
		if ( !this.savepoints.containsKey( generateSavepointKey( ChildTransaction.COMMIT ) ) ) {
			setSavepoint( ChildTransaction.COMMIT );
		}
		return this;
	}

	/**
	 * Rollback the entire child transaction.
	 *
	 * The transaction will be rolled back to the last committed point, and will ignore any set savepoints.
	 */
	public ChildTransaction rollback() {
		return rollback( ChildTransaction.BEGIN );
	}

	/**
	 * Rollback the child transaction up to the last (named) savepoint.
	 *
	 * @param savepoint The name of the savepoint to rollback to or NULL for no savepoint.
	 */
	public ChildTransaction rollback( Key savepoint ) {
		Key savepointKey = generateSavepointKey( savepoint );
		logger.debug( "Rolling back child transaction to savepoint {}", savepointKey );
		this.parent.rollback( savepointKey );
		return this;
	}

	/**
	 * Set a savepoint in the transaction
	 *
	 * @param savepoint The name of the savepoint
	 */
	public Savepoint setSavepoint( Key savepoint ) {
		Key			savepointKey	= generateSavepointKey( savepoint );
		Savepoint	jdbcSavepoint	= this.parent.setSavepoint( savepointKey );
		if ( jdbcSavepoint != null ) {
			this.savepoints.put( savepointKey, jdbcSavepoint );
		}
		return jdbcSavepoint;
	}

	/**
	 * Get the savepoints used in this transaction.
	 * <p>
	 * This method returns a map of savepoint names to their associated savepoint objects, allowing you to manage and rollback to specific points in the transaction.
	 * 
	 * @see #setSavepoint(Key)
	 * @see #rollback(Key)
	 * 
	 * @return A map of savepoint Keys to JDBC savepoint objects.
	 */
	public Map<Key, Savepoint> getSavepoints() {
		return this.savepoints;
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

	private Key generateSavepointKey( Key savepoint ) {
		return savepoint.getNameNoCase().startsWith( "child_" ) ? savepoint : Key.of( this.savepointPrefix + savepoint.getName().toUpperCase() );
	}
}
