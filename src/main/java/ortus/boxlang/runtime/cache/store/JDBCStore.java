/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.cache.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.jdbc.QueryExecute;
import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

/**
 * This object store keeps all objects in a JDBC database table.
 * Each cache entry is stored as a row with serialized data.
 */
public class JDBCStore extends AbstractStore {

	/**
	 * Database vendor enumeration for SQL dialect differences
	 */
	private enum DatabaseVendor {
		ORACLE,
		MYSQL,
		MARIADB,
		POSTGRESQL,
		SQLSERVER,
		DERBY,
		HSQLDB,
		SQLITE,
		UNKNOWN
	}

	/**
	 * The detected database vendor for this store
	 */
	private DatabaseVendor		vendor;

	/**
	 * The datasource to use for storage
	 */
	private DataSource			datasource;

	/**
	 * The table name to use for storage
	 */
	private String				tableName;

	/**
	 * Whether to automatically create the table if it doesn't exist
	 */
	private boolean				autoCreate;

	/**
	 * The context to use for executing queries
	 */
	private IJDBCCapableContext	context;

	/**
	 * The query options to use for executing queries
	 */
	private IStruct				queryOptions	= new Struct();

	/**
	 * Cache Logger
	 */
	private BoxLangLogger		logger;

	/**
	 * Pre-compiled SQL statements for common operations
	 */
	private String				sqlGetSize;
	private String				sqlClearAll;
	private String				sqlClearByKey;
	private String				sqlGetKeys;
	private String				sqlGetAllEntries;
	private String				sqlLookupByKey;
	private String				sqlGetByKey;
	private String				sqlUpdateEntry;
	private String				sqlInsertEntry;
	private String				sqlUpdateStats;
	private String				sqlUpdateStatsNoCreated;

	/**
	 * Constructor
	 */
	public JDBCStore() {
		// Mark this as a distributed store (persists to database)
		this.distributed = true;
	}

	/**
	 * Some storages require a method to initialize the storage or do
	 * object loading. This method is called when the cache provider is started.
	 *
	 * @param provider The cache provider associated with this store
	 * @param config   The configuration for the store
	 */
	@Override
	public IObjectStore init( ICacheProvider provider, IStruct config ) {
		// Initialize base store
		this.provider	= provider;
		this.config		= config;
		this.logger		= BoxRuntime.getInstance().getLoggingService().CACHE_LOGGER;

		// Get configuration with defaults
		String datasourceName = config.getAsString( Key.datasource );
		if ( datasourceName == null || datasourceName.isEmpty() ) {
			throw new BoxRuntimeException( "JDBCStore requires a 'datasource' configuration property" );
		}
		this.tableName	= StringCaster.cast( config.getOrDefault( Key.table, "boxlang_cache" ) );
		this.autoCreate	= BooleanCaster.attempt( config.get( Key.autoCreate ) ).orElse( true );

		// Populate the query options with the datasource and always return array of structs
		this.queryOptions.put( Key.datasource, datasourceName );
		this.queryOptions.put( Key.returnType, "array" );

		// Create a context for executing queries
		this.context	= new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );

		// Get the datasource, because it needs to exist in order for it to work
		this.datasource	= BoxRuntime.getInstance().getDataSourceService().get( Key.of( datasourceName ) );
		if ( this.datasource == null ) {
			throw new BoxRuntimeException( "JDBCStore datasource '" + datasourceName + "' not found." );
		}

		this.context.getConnectionManager().register( this.datasource );

		// Detect the database vendor once at initialization
		this.vendor = detectDatabaseVendor();

		// Create the table if needed
		if ( this.autoCreate ) {
			ensureTable();
		}

		// Pre-compile SQL statements for better performance
		compileSQLStatements();

		return this;
	}

	/**
	 * Get the datasource used by this store
	 *
	 * @return The datasource
	 */
	public DataSource getDatasource() {
		return this.datasource;
	}

	/**
	 * Get the table name used by this store
	 *
	 * @return The table name
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Get the auto create setting
	 *
	 * @return True if auto create is enabled, false otherwise
	 */
	public boolean isAutoCreate() {
		return this.autoCreate;
	}

	/**
	 * Get the context used for executing queries
	 *
	 * @return The box context
	 */
	public IBoxContext getContext() {
		return this.context;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Some storages require a shutdown method to close the storage or do
	 * object saving. This method is called when the cache provider is stopped.
	 */
	public void shutdown() {
		this.context.shutdownConnections();
	}

	/**
	 * Flush the store to a permanent storage.
	 * Only applicable to stores that support it.
	 *
	 * @return The number of objects flushed
	 */
	public int flush() {
		return 0;
	}

	/**
	 * Runs the eviction algorithm to remove objects from the store based on the eviction policy
	 * and eviction count.
	 */
	public void evict() {
		int evictCount = IntegerCaster.cast( this.config.get( Key.evictCount ) );
		if ( evictCount == 0 ) {
			return;
		}

		// Build SQL to delete entries based on policy
		// Filter out eternal entries (timeout = 0 AND lastAccessTimeout = 0)
		String	orderByClause	= getPolicy().getSQLOrderBy();
		String	sql				= buildEvictionSQL( evictCount, orderByClause );

		// Execute the delete
		QueryExecute.execute(
		    this.context,
		    sql,
		    Array.EMPTY,
		    this.queryOptions
		);

		// Record evictions in stats
		// For databases that return affected rows, we could use that count
		// For now, we'll record up to evictCount evictions
		for ( int i = 0; i < evictCount; i++ ) {
			getProvider().getStats().recordEviction();
		}
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		Array result = ( Array ) QueryExecute.execute(
		    this.context,
		    this.sqlGetSize,
		    Array.EMPTY,
		    this.queryOptions
		);

		if ( !result.isEmpty() ) {
			return ( ( IStruct ) result.get( 0 ) ).getAsInteger( Key.itemCount );
		}
		return 0;
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		QueryExecute.execute(
		    this.context,
		    this.sqlClearAll,
		    Array.EMPTY,
		    this.queryOptions
		);
	}

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public boolean clearAll( ICacheKeyFilter filter ) {
		getKeysStream( filter ).forEach( this::clear );
		return true;
	}

	/**
	 * Clears an object from the storage
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( Key key ) {
		// First check if the key exists
		boolean exists = lookup( key );
		if ( !exists ) {
			return false;
		}

		QueryExecute.execute(
		    this.context,
		    this.sqlClearByKey,
		    Array.of( key.getName() ),
		    this.queryOptions
		);

		return true;
	}

	/**
	 * Clears multiple objects from the storage
	 *
	 * @param keys The keys to clear
	 *
	 * @return A struct of keys and their clear status: true if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public IStruct clear( Key... keys ) {
		IStruct results = new Struct( false );
		for ( Key key : keys ) {
			results.put( key, clear( key ) );
		}
		return results;
	}

	/**
	 * Get all the keys in the store
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys() {
		Array result = ( Array ) QueryExecute.execute(
		    this.context,
		    this.sqlGetKeys,
		    Array.EMPTY,
		    this.queryOptions
		);

		return result.stream()
		    .map( row -> Key.of( ( ( IStruct ) row ).getAsString( Key.objectKey ) ) )
		    .toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return getKeysStream( filter ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		Array result = ( Array ) QueryExecute.execute(
		    this.context,
		    this.sqlGetKeys,
		    Array.EMPTY,
		    this.queryOptions
		);

		return result.stream()
		    .map( row -> Key.of( ( ( IStruct ) row ).getAsString( Key.objectKey ) ) );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return getKeysStream().filter( filter );
	}

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key ) {
		Array result = ( Array ) QueryExecute.execute(
		    this.context,
		    this.sqlLookupByKey,
		    Array.of( key.getName() ),
		    this.queryOptions
		);

		if ( !result.isEmpty() ) {
			return ( ( IStruct ) result.get( 0 ) ).getAsInteger( Key.itemCount ) > 0;
		}
		return false;
	}

	/**
	 * Check if multiple objects are in the store
	 *
	 * @param keys A varargs of keys to lookup in the store
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( Key... keys ) {
		IStruct results = new Struct( false );
		for ( Key key : keys ) {
			results.put( key, lookup( key ) );
		}
		return results;
	}

	/**
	 * Check if multiple objects are in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of the keys found. True if the object is in the store, false otherwise
	 */
	public IStruct lookup( ICacheKeyFilter filter ) {
		Key[] foundKeys = getKeys( filter );

		return Stream.of( foundKeys )
		    .map( key -> new AbstractMap.SimpleEntry<Key, Object>( key, lookup( key ) ) )
		    .collect( BLCollector.toStruct() );
	}

	/**
	 * Get an object from the store with metadata tracking: hits, lastAccess, etc
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or null if not found
	 */
	public ICacheEntry get( Key key ) {
		ICacheEntry results = getQuiet( key );

		if ( results != null ) {
			// Update Stats
			results.incrementHits().touchLastAccessed();

			// Is resetTimeoutOnAccess enabled? If so, jump up the creation time to increase the timeout
			if ( BooleanCaster.cast( this.config.get( Key.resetTimeoutOnAccess ) ) ) {
				results.resetCreated();
				updateEntryStats( key, results, true );
			} else {
				updateEntryStats( key, results, false );
			}

		}

		return results;
	}

	/**
	 * Get multiple objects from the store with metadata tracking
	 *
	 * @param keys The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( Key... keys ) {
		IStruct results = new Struct( false );
		for ( Key key : keys ) {
			results.put( key, get( key ) );
		}
		return results;
	}

	/**
	 * Get multiple objects from the store with metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( ICacheKeyFilter filter ) {
		IStruct results = new Struct( false );
		getKeysStream( filter ).forEach( key -> results.put( key, get( key ) ) );
		return results;
	}

	/**
	 * Get an object from cache with no metadata tracking
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or null if not found
	 */
	public ICacheEntry getQuiet( Key key ) {
		Array result = ( Array ) QueryExecute.execute(
		    this.context,
		    this.sqlGetByKey,
		    Array.of( key.getName() ),
		    this.queryOptions
		);

		if ( !result.isEmpty() ) {
			return deserializeEntry( ( IStruct ) result.get( 0 ) );
		}
		return null;
	}

	/**
	 * Get multiple objects from the store with no metadata tracking
	 *
	 * @param keys The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( Key... keys ) {
		IStruct results = new Struct( false );
		for ( Key key : keys ) {
			results.put( key, getQuiet( key ) );
		}
		return results;
	}

	/**
	 * Get multiple objects from the store with no metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( ICacheKeyFilter filter ) {
		IStruct results = new Struct( false );
		getKeysStream( filter ).forEach( key -> results.put( key, getQuiet( key ) ) );
		return results;
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry ) {
		// Serialize the entire entry object, just like FileSystemStore does
		String	serializedEntry	= serializeValue( entry );

		// Check if entry exists
		boolean	exists			= lookup( key );

		if ( exists ) {
			// Update existing entry - do NOT update created timestamp
			QueryExecute.execute(
			    this.context,
			    this.sqlUpdateEntry,
			    Array.of(
			        serializedEntry,
			        entry.hits(),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        entry.timeout(),
			        entry.lastAccessTimeout(),
			        key.getName()
			    ),
			    this.queryOptions
			);
		} else {
			// Insert new entry
			QueryExecute.execute(
			    this.context,
			    this.sqlInsertEntry,
			    Array.of(
			        key.getName(),
			        serializedEntry,
			        entry.hits(),
			        java.sql.Timestamp.from( entry.created() ),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        entry.timeout(),
			        entry.lastAccessTimeout()
			    ),
			    this.queryOptions
			);
		}
	}

	/**
	 * Set's multiple objects in the storage
	 *
	 * @param entries The keys and cache entries to store
	 */
	public void set( IStruct entries ) {
		entries.forEach( ( key, value ) -> set( key, ( ICacheEntry ) value ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Update the entry statistics in the database
	 *
	 * @param key          The key of the entry
	 * @param entry        The entry with updated stats
	 * @param resetCreated Whether to reset the created timestamp
	 */
	private void updateEntryStats( Key key, ICacheEntry entry, boolean resetCreated ) {
		if ( resetCreated ) {
			QueryExecute.execute(
			    this.context,
			    this.sqlUpdateStats,
			    Array.of(
			        entry.hits(),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        java.sql.Timestamp.from( entry.created() ),
			        key.getName()
			    ),
			    this.queryOptions
			);
		} else {
			QueryExecute.execute(
			    this.context,
			    this.sqlUpdateStatsNoCreated,
			    Array.of(
			        entry.hits(),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        key.getName()
			    ),
			    this.queryOptions
			);
		}
	}

	/**
	 * Serialize a value to a Base64-encoded string
	 *
	 * @param value The value to serialize
	 *
	 * @return The Base64-encoded serialized string
	 */
	private String serializeValue( Object value ) {
		return Base64.getEncoder().encodeToString(
		    ObjectMarshaller.serialize( this.context, value )
		);
	}

	/**
	 * Deserialize a value from a Base64-encoded string
	 *
	 * @param base64Data The Base64-encoded string to deserialize
	 *
	 * @return The deserialized value
	 */
	private Object deserializeValue( String base64Data ) {
		return ObjectMarshaller.deserialize(
		    this.context,
		    Base64.getDecoder().decode( base64Data )
		);
	}

	/**
	 * Deserialize a cache entry from a database row
	 *
	 * @param row The database row
	 *
	 * @return The deserialized cache entry
	 */
	private ICacheEntry deserializeEntry( IStruct row ) {
		try {
			// Get the cache value - it might be a CLOB or String depending on the database
			Object	cacheValueObj	= row.get( Key.objectValue );
			String	base64Value		= null;

			// Already a String
			if ( cacheValueObj instanceof String str ) {
				base64Value = str;
			} else {
				base64Value = StringCaster.cast( cacheValueObj );
			}

			// Deserialize the entire entry object, just like FileSystemStore does
			Object result = deserializeValue( base64Value );

			// If it's already an ICacheEntry, return it as-is (preserves all properties)
			if ( result instanceof ICacheEntry ) {
				return ( ICacheEntry ) result;
			} else {
				// Otherwise, wrap it in a new entry (backward compatibility)
				return new BoxCacheEntry(
				    this.provider.getName(),
				    row.getAsLong( Key.timeout ),
				    row.getAsLong( Key.lastAccessTimeout ),
				    Key.of( row.getAsString( Key.objectKey ) ),
				    result,
				    new Struct()
				);
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to deserialize cache entry", e );
		}
	}

	/**
	 * Get the database driver name for vendor detection
	 *
	 * @return The database driver name
	 */
	private String getDatabaseDriverName() {
		try ( Connection conn = this.datasource.getConnection() ) {
			return conn.getMetaData().getDriverName().toLowerCase();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to get database metadata", e );
		}
	}

	/**
	 * Detect the database vendor from the JDBC driver name.
	 * Called once during initialization to avoid repeated string comparisons.
	 *
	 * @return The detected DatabaseVendor enum value
	 */
	private DatabaseVendor detectDatabaseVendor() {
		String driverName = getDatabaseDriverName();

		if ( driverName.contains( "oracle" ) ) {
			return DatabaseVendor.ORACLE;
		} else if ( driverName.contains( "mariadb" ) ) {
			return DatabaseVendor.MARIADB;
		} else if ( driverName.contains( "mysql" ) ) {
			return DatabaseVendor.MYSQL;
		} else if ( driverName.contains( "postgres" ) ) {
			return DatabaseVendor.POSTGRESQL;
		} else if ( driverName.contains( "microsoft" ) || driverName.contains( "sqlserver" ) ) {
			return DatabaseVendor.SQLSERVER;
		} else if ( driverName.contains( "derby" ) ) {
			return DatabaseVendor.DERBY;
		} else if ( driverName.contains( "hsql" ) ) {
			return DatabaseVendor.HSQLDB;
		} else if ( driverName.contains( "sqlite" ) ) {
			return DatabaseVendor.SQLITE;
		} else {
			return DatabaseVendor.UNKNOWN;
		}
	}

	/**
	 * Create the cache table if it doesn't exist
	 */
	private void ensureTable() {
		// Verify if we can query the table
		try {
			QueryExecute.execute(
			    this.context,
			    "SELECT COUNT(*) as itemCount FROM " + this.tableName + " WHERE 1=0",
			    Array.EMPTY,
			    this.queryOptions
			);
			// Table exists, we're good
			return;
		} catch ( DatabaseException e ) {
			// Table doesn't exist, create it
			this.logger.info( "Cache table '" + this.tableName + "' does not exist. Creating it now." );
		}

		// Determine the database vendor
		String createTableSQL = getCreateTableSQL( this.tableName );

		try {
			QueryExecute.execute( this.context, createTableSQL, Array.EMPTY, this.queryOptions );
			// Create indexes separately (some databases don't support multiple statements)
			createIndexes();
		} catch ( DatabaseException e ) {
			throw new BoxRuntimeException( "Failed to create cache table: " + this.tableName, e );
		}
	}

	/**
	 * Pre-compile SQL statements for common operations to avoid string concatenation overhead.
	 * This method is called during initialization after the table is created/verified.
	 */
	private void compileSQLStatements() {
		// Read operations
		this.sqlGetSize					= "SELECT COUNT(*) as itemCount FROM " + this.tableName;
		this.sqlGetKeys					= "SELECT objectKey FROM " + this.tableName;
		this.sqlGetAllEntries			= "SELECT * FROM " + this.tableName;
		this.sqlLookupByKey				= "SELECT COUNT(*) as itemCount FROM " + this.tableName + " WHERE objectKey = ?";
		this.sqlGetByKey				= "SELECT * FROM " + this.tableName + " WHERE objectKey = ?";

		// Write operations
		this.sqlClearAll				= getClearAllSQL();
		this.sqlClearByKey				= "DELETE FROM " + this.tableName + " WHERE objectKey = ?";

		// Update operations
		// Note: created timestamp is NOT updated on regular updates - only on inserts
		// The sqlUpdateStats will update created only when resetTimeoutOnAccess=true
		this.sqlUpdateEntry				= "UPDATE " + this.tableName
		    + " SET objectValue = ?, hits = ?, lastAccessed = ?, timeout = ?, lastAccessTimeout = ? WHERE objectKey = ?";

		this.sqlInsertEntry				= "INSERT INTO " + this.tableName
		    + " (objectKey, objectValue, hits, created, lastAccessed, timeout, lastAccessTimeout) VALUES (?, ?, ?, ?, ?, ?, ?)";

		this.sqlUpdateStats				= "UPDATE " + this.tableName + " SET hits = ?, lastAccessed = ?, created = ? WHERE objectKey = ?";
		this.sqlUpdateStatsNoCreated	= "UPDATE " + this.tableName + " SET hits = ?, lastAccessed = ? WHERE objectKey = ?";
	}

	/**
	 * Get the CREATE TABLE SQL for the current database vendor
	 *
	 * @param tableName The table name to use in the CREATE TABLE statement
	 *
	 * @return The CREATE TABLE SQL statement for the current database vendor
	 */
	private String getCreateTableSQL( String tableName ) {
		switch ( this.vendor ) {
			case ORACLE :
				return String.format(
				    "CREATE TABLE %s ("
				        + "objectKey VARCHAR2(500) PRIMARY KEY, "
				        + "objectValue CLOB, "
				        + "hits NUMBER DEFAULT 0, "
				        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "lastAccessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "timeout NUMBER DEFAULT 0, "
				        + "lastAccessTimeout NUMBER DEFAULT 0"
				        + ")",
				    tableName
				);

			case MYSQL :
			case MARIADB :
				// MySQL/MariaDB can define indexes inline
				return String.format(
				    "CREATE TABLE %s ("
				        + "objectKey VARCHAR(500) PRIMARY KEY, "
				        + "objectValue LONGTEXT, "
				        + "hits BIGINT DEFAULT 0, "
				        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "lastAccessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "timeout BIGINT DEFAULT 0, "
				        + "lastAccessTimeout BIGINT DEFAULT 0, "
				        + "INDEX idx_lastAccessed (lastAccessed), "
				        + "INDEX idx_created (created), "
				        + "INDEX idx_hits (hits), "
				        + "INDEX idx_timeout (timeout, lastAccessTimeout)"
				        + ")",
				    tableName
				);

			case POSTGRESQL :
				return String.format(
				    "CREATE TABLE %s ("
				        + "objectKey VARCHAR(500) PRIMARY KEY, "
				        + "objectValue TEXT, "
				        + "hits BIGINT DEFAULT 0, "
				        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "lastAccessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "timeout BIGINT DEFAULT 0, "
				        + "lastAccessTimeout BIGINT DEFAULT 0"
				        + ")",
				    tableName
				);

			case SQLSERVER :
				return String.format(
				    "CREATE TABLE %s ("
				        + "objectKey VARCHAR(500) PRIMARY KEY, "
				        + "objectValue VARCHAR(MAX), "
				        + "hits BIGINT DEFAULT 0, "
				        + "created DATETIME DEFAULT GETDATE(), "
				        + "lastAccessed DATETIME DEFAULT GETDATE(), "
				        + "timeout BIGINT DEFAULT 0, "
				        + "lastAccessTimeout BIGINT DEFAULT 0"
				        + ")",
				    tableName
				);

			case DERBY :
			case HSQLDB :
			case SQLITE :
			case UNKNOWN :
			default :
				// Default to Derby/HSQLDB syntax - use VARCHAR for objectValue to avoid CLOB issues
				return String.format(
				    "CREATE TABLE %s ("
				        + "objectKey VARCHAR(500) PRIMARY KEY, "
				        + "objectValue VARCHAR(32672), "
				        + "hits BIGINT DEFAULT 0, "
				        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "lastAccessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				        + "timeout BIGINT DEFAULT 0, "
				        + "lastAccessTimeout BIGINT DEFAULT 0"
				        + ")",
				    tableName
				);
		}
	}

	/**
	 * Create indexes on the cache table for query optimization.
	 * Indexes are created separately to support databases that don't allow
	 * multiple statements in a single execute.
	 */
	private void createIndexes() {
		// MySQL/MariaDB already have indexes defined inline in CREATE TABLE
		if ( this.vendor == DatabaseVendor.MYSQL || this.vendor == DatabaseVendor.MARIADB ) {
			return;
		}

		// Create indexes for all other databases
		String[] indexes = {
		    "CREATE INDEX idx_" + this.tableName + "_lastAccessed ON " + this.tableName + "(lastAccessed)",
		    "CREATE INDEX idx_" + this.tableName + "_created ON " + this.tableName + "(created)",
		    "CREATE INDEX idx_" + this.tableName + "_hits ON " + this.tableName + "(hits)",
		    "CREATE INDEX idx_" + this.tableName + "_timeout ON " + this.tableName + "(timeout, lastAccessTimeout)"
		};

		for ( String indexSQL : indexes ) {
			try {
				QueryExecute.execute( this.context, indexSQL, Array.EMPTY, this.queryOptions );
			} catch ( DatabaseException e ) {
				// Log but don't fail - indexes are nice to have but not critical
				this.logger.warn( "Failed to create index: " + indexSQL + " - " + e.getMessage() );
			}
		}
	}

	/**
	 * Get the CLEAR ALL SQL statement for the current database vendor.
	 * Some databases don't support TRUNCATE TABLE or have different syntax.
	 *
	 * @return The appropriate clear all SQL statement
	 */
	private String getClearAllSQL() {
		// Most databases support TRUNCATE TABLE for fast clear
		switch ( this.vendor ) {
			case ORACLE :
			case MYSQL :
			case MARIADB :
			case POSTGRESQL :
			case SQLSERVER :
				return "TRUNCATE TABLE " + this.tableName;

			case DERBY :
			case HSQLDB :
			case SQLITE :
			case UNKNOWN :
			default :
				// Derby and some other databases don't support TRUNCATE
				// Use DELETE FROM instead (slower but universally supported)
				return "DELETE FROM " + this.tableName;
		}
	}

	/**
	 * Build database-specific DELETE SQL for eviction that handles different SQL dialects.
	 * Different databases have different ways to limit deletions:
	 * - MySQL/MariaDB/PostgreSQL: DELETE with subquery using LIMIT
	 * - SQL Server: DELETE TOP (n)
	 * - Oracle/Derby/HSQLDB: DELETE with subquery using FETCH FIRST n ROWS ONLY
	 *
	 * @param limit         The number of rows to evict
	 * @param orderByClause The ORDER BY clause from the eviction policy
	 *
	 * @return The database-specific DELETE SQL statement
	 */
	private String buildEvictionSQL( int limit, String orderByClause ) {
		switch ( this.vendor ) {
			case SQLSERVER :
				// SQL Server: DELETE TOP (n) FROM table WHERE ... ORDER BY ...
				return "DELETE TOP (" + limit + ") FROM " + this.tableName
				    + " WHERE NOT (timeout = 0 AND lastAccessTimeout = 0)"
				    + " ORDER BY " + orderByClause;

			case MYSQL :
			case MARIADB :
				// MySQL/MariaDB: DELETE FROM table WHERE key IN (SELECT key ... LIMIT n)
				return "DELETE FROM " + this.tableName
				    + " WHERE objectKey IN ("
				    + "SELECT objectKey FROM " + this.tableName
				    + " WHERE NOT (timeout = 0 AND lastAccessTimeout = 0)"
				    + " ORDER BY " + orderByClause
				    + " LIMIT " + limit
				    + ")";

			case POSTGRESQL :
				// PostgreSQL: DELETE FROM table WHERE key IN (SELECT key ... LIMIT n)
				return "DELETE FROM " + this.tableName
				    + " WHERE objectKey IN ("
				    + "SELECT objectKey FROM " + this.tableName
				    + " WHERE NOT (timeout = 0 AND lastAccessTimeout = 0)"
				    + " ORDER BY " + orderByClause
				    + " LIMIT " + limit
				    + ")";

			case ORACLE :
				// Oracle 12c+: DELETE WHERE key IN (SELECT ... FETCH FIRST n ROWS ONLY)
				return "DELETE FROM " + this.tableName
				    + " WHERE objectKey IN ("
				    + "SELECT objectKey FROM " + this.tableName
				    + " WHERE NOT (timeout = 0 AND lastAccessTimeout = 0)"
				    + " ORDER BY " + orderByClause
				    + " FETCH FIRST " + limit + " ROWS ONLY"
				    + ")";

			case DERBY :
			case HSQLDB :
			case SQLITE :
			case UNKNOWN :
			default :
				// Derby/HSQLDB: DELETE WHERE key IN (SELECT ... FETCH FIRST n ROWS ONLY)
				return "DELETE FROM " + this.tableName
				    + " WHERE objectKey IN ("
				    + "SELECT objectKey FROM " + this.tableName
				    + " WHERE NOT (timeout = 0 AND lastAccessTimeout = 0)"
				    + " ORDER BY " + orderByClause
				    + " FETCH FIRST " + limit + " ROWS ONLY"
				    + ")";
		}
	}
}