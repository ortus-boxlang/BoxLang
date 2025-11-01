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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.conversion.RuntimeObjectInputStream;

/**
 * This object store keeps all objects in a JDBC database table.
 * Each cache entry is stored as a row with serialized data.
 */
public class JDBCStore extends AbstractStore {

	/**
	 * The datasource to use for storage
	 */
	private DataSource					datasource;

	/**
	 * The table name to use for storage
	 */
	private String						tableName;

	/**
	 * The schema name (optional)
	 */
	private String						schema;

	/**
	 * The database name (optional)
	 */
	private String						database;

	/**
	 * Whether to automatically create the table if it doesn't exist
	 */
	private boolean						autoCreate;

	/**
	 * The context to use for executing queries
	 */
	private ScriptingRequestBoxContext	context;

	/**
	 * Constructor
	 */
	public JDBCStore() {
		// Empty constructor
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
		this.provider	= provider;
		this.config		= config;

		// Get configuration with defaults
		String datasourceName = config.getAsString( Key.datasource );
		if ( datasourceName == null || datasourceName.isEmpty() ) {
			throw new BoxRuntimeException( "JDBCStore requires a 'datasource' configuration property" );
		}

		this.tableName	= config.getOrDefault( Key.table, "boxlang_cache" ).toString();
		this.schema		= config.getAsString( Key.of( "schema" ) );
		this.database	= config.getAsString( Key.of( "database" ) );
		this.autoCreate	= BooleanCaster.attempt( config.get( Key.of( "autoCreate" ) ) ).orElse( true );

		// Create a context for executing queries
		this.context	= new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );

		// Find the datasource - it might have a prefix and suffix
		String[]	availableDatasources	= BoxRuntime.getInstance().getDataSourceService().getNames();
		Key			datasourceKey			= null;

		// Look for a datasource that contains our datasource name
		for ( String dsName : availableDatasources ) {
			// Check if the datasource name contains our requested name
			// Format is usually: bx_[app_]name[_suffix] so we check for the pattern
			if ( dsName.contains( "_" + datasourceName + "_" ) || dsName.endsWith( "_" + datasourceName ) ) {
				datasourceKey = Key.of( dsName );
				break;
			}
		}

		if ( datasourceKey == null ) {
			throw new BoxRuntimeException( "Datasource '" + datasourceName + "' not found. Available: " + String.join( ", ", availableDatasources ) );
		}

		// Get the datasource
		this.datasource = BoxRuntime.getInstance().getDataSourceService().get( datasourceKey );
		if ( this.datasource == null ) {
			throw new BoxRuntimeException( "Datasource '" + datasourceName + "' could not be retrieved with key: " + datasourceKey.getName() );
		}

		// Setup the connection manager
		this.context.getConnectionManager().setDefaultDatasource( this.datasource );

		// Create the table if needed
		if ( this.autoCreate ) {
			createTableIfNotExists();
		}

		return this;
	}

	/**
	 * Get the full table name with schema if applicable
	 *
	 * @return The full table name
	 */
	private String getFullTableName() {
		StringBuilder fullName = new StringBuilder();
		if ( this.schema != null && !this.schema.isEmpty() ) {
			fullName.append( this.schema ).append( "." );
		}
		fullName.append( this.tableName );
		return fullName.toString();
	}

	/**
	 * Create the cache table if it doesn't exist
	 */
	private void createTableIfNotExists() {
		String fullTableName = getFullTableName();

		// Check if table exists using DBInfo
		try {
			Object result = QueryExecute.execute(
			    this.context,
			    "SELECT COUNT(*) as cnt FROM " + fullTableName + " WHERE 1=0",
			    new Array(),
			    Struct.of()
			);
			// Table exists, we're good
			return;
		} catch ( DatabaseException e ) {
			// Table doesn't exist, create it
		}

		// Determine the database vendor
		String createTableSQL = getCreateTableSQL( fullTableName );

		try {
			QueryExecute.execute( this.context, createTableSQL, new Array(), Struct.of() );
		} catch ( DatabaseException e ) {
			throw new BoxRuntimeException( "Failed to create cache table: " + fullTableName, e );
		}
	}

	/**
	 * Get the CREATE TABLE SQL for the current database vendor
	 *
	 * @param fullTableName The full table name including schema
	 *
	 * @return The CREATE TABLE SQL
	 */
	private String getCreateTableSQL( String fullTableName ) {
		String driverName = getDatabaseDriverName();

		// Build SQL based on database vendor - using TEXT types for Base64 encoded values
		if ( driverName.contains( "oracle" ) ) {
			return String.format(
			    "CREATE TABLE %s ("
			        + "cache_key VARCHAR2(500) PRIMARY KEY, "
			        + "cache_value CLOB, "
			        + "hits NUMBER DEFAULT 0, "
			        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "timeout NUMBER DEFAULT 0, "
			        + "last_access_timeout NUMBER DEFAULT 0, "
			        + "metadata CLOB"
			        + ")",
			    fullTableName
			);
		} else if ( driverName.contains( "mysql" ) || driverName.contains( "mariadb" ) ) {
			return String.format(
			    "CREATE TABLE %s ("
			        + "cache_key VARCHAR(500) PRIMARY KEY, "
			        + "cache_value LONGTEXT, "
			        + "hits BIGINT DEFAULT 0, "
			        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "timeout BIGINT DEFAULT 0, "
			        + "last_access_timeout BIGINT DEFAULT 0, "
			        + "metadata LONGTEXT"
			        + ")",
			    fullTableName
			);
		} else if ( driverName.contains( "postgres" ) ) {
			return String.format(
			    "CREATE TABLE %s ("
			        + "cache_key VARCHAR(500) PRIMARY KEY, "
			        + "cache_value TEXT, "
			        + "hits BIGINT DEFAULT 0, "
			        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "timeout BIGINT DEFAULT 0, "
			        + "last_access_timeout BIGINT DEFAULT 0, "
			        + "metadata TEXT"
			        + ")",
			    fullTableName
			);
		} else if ( driverName.contains( "microsoft" ) || driverName.contains( "sqlserver" ) ) {
			return String.format(
			    "CREATE TABLE %s ("
			        + "cache_key VARCHAR(500) PRIMARY KEY, "
			        + "cache_value VARCHAR(MAX), "
			        + "hits BIGINT DEFAULT 0, "
			        + "created DATETIME DEFAULT GETDATE(), "
			        + "last_accessed DATETIME DEFAULT GETDATE(), "
			        + "timeout BIGINT DEFAULT 0, "
			        + "last_access_timeout BIGINT DEFAULT 0, "
			        + "metadata VARCHAR(MAX)"
			        + ")",
			    fullTableName
			);
		} else {
			// Default to Derby/HSQLDB syntax - use VARCHAR for cache_value to avoid CLOB issues
			return String.format(
			    "CREATE TABLE %s ("
			        + "cache_key VARCHAR(500) PRIMARY KEY, "
			        + "cache_value VARCHAR(32672), "
			        + "hits BIGINT DEFAULT 0, "
			        + "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			        + "timeout BIGINT DEFAULT 0, "
			        + "last_access_timeout BIGINT DEFAULT 0, "
			        + "metadata VARCHAR(1000)"
			        + ")",
			    fullTableName
			);
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
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Some storages require a shutdown method to close the storage or do
	 * object saving. This method is called when the cache provider is stopped.
	 */
	public void shutdown() {
		// Nothing to do
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
	public synchronized void evict() {
		int evictCount = IntegerCaster.cast( this.config.get( Key.evictCount ) );
		if ( evictCount == 0 ) {
			return;
		}

		// Get all entries and sort them by the eviction policy
		Stream<ICacheEntry> entries = getEntryStream()
		    .map( this::deserializeEntry )
		    .sorted( getPolicy().getComparator() )
		    .filter( entry -> !entry.isEternal() )
		    .limit( evictCount );

		// Delete the entries
		entries.forEach( entry -> {
			clear( entry.key() );
			getProvider().getStats().recordEviction();
		} );
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT COUNT(*) as cnt FROM " + fullTableName,
		    new Array(),
		    Struct.of()
		);

		if ( result instanceof Query query ) {
			return query.getRowAsStruct( 0 ).getAsInteger( Key.of( "cnt" ) );
		}
		return 0;
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		String fullTableName = getFullTableName();
		QueryExecute.execute(
		    this.context,
		    "DELETE FROM " + fullTableName,
		    new Array(),
		    Struct.of()
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
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "DELETE FROM " + fullTableName + " WHERE cache_key = ?",
		    Array.of( key.getName() ),
		    Struct.of()
		);

		// Check if any rows were affected
		if ( result instanceof Query query ) {
			IStruct metadata = query.getMetaData();
			return metadata.getAsInteger( Key.of( "recordCount" ) ) > 0;
		}
		return false;
	}

	/**
	 * Clears multiple objects from the storage
	 *
	 * @param keys The keys to clear
	 *
	 * @return A struct of keys and their clear status: true if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public IStruct clear( Key... keys ) {
		IStruct results = new Struct();
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
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT cache_key FROM " + fullTableName,
		    new Array(),
		    Struct.of()
		);

		if ( result instanceof Query query ) {
			return query.stream()
			    .map( row -> Key.of( ( ( IStruct ) row ).getAsString( Key.of( "cache_key" ) ) ) )
			    .toArray( Key[]::new );
		}
		return new Key[ 0 ];
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
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT cache_key FROM " + fullTableName,
		    new Array(),
		    Struct.of()
		);

		if ( result instanceof Query query ) {
			return query.stream()
			    .map( row -> Key.of( ( ( IStruct ) row ).getAsString( Key.of( "cache_key" ) ) ) );
		}
		return Stream.empty();
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
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT COUNT(*) as cnt FROM " + fullTableName + " WHERE cache_key = ?",
		    Array.of( key.getName() ),
		    Struct.of()
		);

		if ( result instanceof Query query ) {
			return query.getRowAsStruct( 0 ).getAsInteger( Key.of( "cnt" ) ) > 0;
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
		IStruct results = new Struct();
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
			}

			// Update the database with the new stats
			updateEntryStats( key, results );
		}

		return results;
	}

	/**
	 * Update the entry statistics in the database
	 *
	 * @param key   The key of the entry
	 * @param entry The entry with updated stats
	 */
	private void updateEntryStats( Key key, ICacheEntry entry ) {
		String fullTableName = getFullTableName();
		QueryExecute.execute(
		    this.context,
		    "UPDATE " + fullTableName + " SET hits = ?, last_accessed = ?, created = ? WHERE cache_key = ?",
		    Array.of(
		        entry.hits(),
		        java.sql.Timestamp.from( entry.lastAccessed() ),
		        java.sql.Timestamp.from( entry.created() ),
		        key.getName()
		    ),
		    Struct.of()
		);
	}

	/**
	 * Get multiple objects from the store with metadata tracking
	 *
	 * @param keys The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( Key... keys ) {
		IStruct results = new Struct();
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
		IStruct results = new Struct();
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
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT * FROM " + fullTableName + " WHERE cache_key = ?",
		    Array.of( key.getName() ),
		    Struct.of()
		);

		if ( result instanceof Query query && query.size() > 0 ) {
			// Materialize CLOB data immediately after query returns
			IStruct row = materializeClobData( query.getRowAsStruct( 0 ) );
			return deserializeEntry( row );
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
		IStruct results = new Struct();
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
		IStruct results = new Struct();
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
		String	fullTableName	= getFullTableName();
		String	serializedValue	= serializeValue( entry.rawValue() );
		String	metadata		= entry.metadata().asString();

		// Check if entry exists
		boolean	exists			= lookup( key );

		if ( exists ) {
			// Update existing entry
			QueryExecute.execute(
			    this.context,
			    "UPDATE " + fullTableName
			        + " SET cache_value = ?, hits = ?, created = ?, last_accessed = ?, timeout = ?, last_access_timeout = ?, metadata = ? WHERE cache_key = ?",
			    Array.of(
			        serializedValue,
			        entry.hits(),
			        java.sql.Timestamp.from( entry.created() ),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        entry.timeout(),
			        entry.lastAccessTimeout(),
			        metadata,
			        key.getName()
			    ),
			    Struct.of()
			);
		} else {
			// Insert new entry
			QueryExecute.execute(
			    this.context,
			    "INSERT INTO " + fullTableName
			        + " (cache_key, cache_value, hits, created, last_accessed, timeout, last_access_timeout, metadata) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
			    Array.of(
			        key.getName(),
			        serializedValue,
			        entry.hits(),
			        java.sql.Timestamp.from( entry.created() ),
			        java.sql.Timestamp.from( entry.lastAccessed() ),
			        entry.timeout(),
			        entry.lastAccessTimeout(),
			        metadata
			    ),
			    Struct.of()
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
	 * Get a stream of all entries in the store
	 *
	 * @return A stream of all entries
	 */
	private Stream<IStruct> getEntryStream() {
		String	fullTableName	= getFullTableName();
		Object	result			= QueryExecute.execute(
		    this.context,
		    "SELECT * FROM " + fullTableName,
		    new Array(),
		    Struct.of()
		);

		if ( result instanceof Query query ) {
			// Eagerly collect all rows and materialize CLOB values to avoid lazy evaluation issues
			java.util.List<IStruct> materializedRows = new java.util.ArrayList<>();
			for ( Object row : query ) {
				IStruct rowStruct = ( IStruct ) row;
				// Materialize CLOB data immediately
				materializedRows.add( materializeClobData( rowStruct ) );
			}
			return materializedRows.stream();
		}
		return Stream.empty();
	}

	/**
	 * Materialize CLOB data in a struct to avoid lazy evaluation issues
	 * 
	 * @param row The row struct that may contain CLOB values
	 * 
	 * @return A struct with CLOB values converted to strings
	 */
	private IStruct materializeClobData( IStruct row ) {
		Object cacheValueObj = row.get( Key.of( "cache_value" ) );
		if ( cacheValueObj instanceof java.sql.Clob clob ) {
			try {
				long	length		= clob.length();
				String	stringValue	= clob.getSubString( 1, ( int ) length );
				// Create a new struct with the string value
				IStruct	newRow		= new Struct( IStruct.TYPES.LINKED );
				row.entrySet().forEach( entry -> {
					if ( entry.getKey().equals( Key.of( "cache_value" ) ) ) {
						newRow.put( entry.getKey(), stringValue );
					} else {
						newRow.put( entry.getKey(), entry.getValue() );
					}
				} );
				return newRow;
			} catch ( java.sql.SQLException e ) {
				throw new BoxRuntimeException( "Failed to read CLOB data", e );
			}
		}
		return row;
	}

	/**
	 * Serialize a value to a Base64-encoded string
	 *
	 * @param value The value to serialize
	 *
	 * @return The Base64-encoded serialized string
	 */
	private String serializeValue( Object value ) {
		try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
			try ( ObjectOutputStream oos = new ObjectOutputStream( baos ) ) {
				oos.writeObject( value );
				return Base64.getEncoder().encodeToString( baos.toByteArray() );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to serialize cache value", e );
		}
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
			Object	cacheValueObj	= row.get( Key.of( "cache_value" ) );
			String	base64Value		= null;

			if ( cacheValueObj instanceof java.sql.Clob clob ) {
				// Convert CLOB to String - must be done immediately before connection closes
				try {
					long length = clob.length();
					base64Value = clob.getSubString( 1, ( int ) length );
				} catch ( java.sql.SQLException e ) {
					throw new BoxRuntimeException( "Failed to read CLOB data", e );
				}
			} else if ( cacheValueObj instanceof String str ) {
				// Already a String
				base64Value = str;
			} else {
				// Try to cast to string
				base64Value = cacheValueObj.toString();
			}

			Object				value			= deserializeValue( base64Value );

			// Parse timestamps
			java.sql.Timestamp	createdTs		= ( java.sql.Timestamp ) row.get( Key.of( "created" ) );
			java.sql.Timestamp	lastAccessedTs	= ( java.sql.Timestamp ) row.get( Key.of( "last_accessed" ) );

			// Create the cache entry
			BoxCacheEntry		entry			= new BoxCacheEntry(
			    this.provider.getName(),
			    row.getAsLong( Key.of( "timeout" ) ),
			    row.getAsLong( Key.of( "last_access_timeout" ) ),
			    Key.of( row.getAsString( Key.of( "cache_key" ) ) ),
			    value,
			    Struct.of()
			);

			// Set the stats
			entry.setHits( row.getAsLong( Key.of( "hits" ) ) );

			return entry;
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to deserialize cache entry", e );
		}
	}

	/**
	 * Deserialize a value from a Base64-encoded string
	 *
	 * @param base64Data The Base64-encoded string to deserialize
	 *
	 * @return The deserialized value
	 */
	private Object deserializeValue( String base64Data ) {
		try {
			byte[] data = Base64.getDecoder().decode( base64Data );
			try ( ByteArrayInputStream bais = new ByteArrayInputStream( data ) ) {
				try ( RuntimeObjectInputStream ois = new RuntimeObjectInputStream( bais ) ) {
					return ois.readObject();
				} catch ( ClassNotFoundException e ) {
					throw new BoxRuntimeException( "Cannot cast the deserialized object to a known class.", e );
				}
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to deserialize cache value", e );
		}
	}

}
