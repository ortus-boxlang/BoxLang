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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.FileSystemUtil;

/**
 * This object store keeps all objects in the file system.
 * Each object is stored in a separate file.
 */
public class FileSystemStore extends AbstractStore {

	/**
	 * The extension for the cache files
	 */
	private static final String			FILE_EXTENSION					= ".cache";

	/**
	 * The suffix of the temporary files written before an entry is swapped into place
	 */
	private static final String			TEMP_FILE_SUFFIX				= ".tmp";

	/**
	 * How old an orphaned temporary file must be before it is removed on startup. Recently written ones may
	 * belong to another process that is mid-write.
	 */
	private static final long			ORPHANED_TEMP_FILE_AGE_MILLIS	= 5 * 60 * 1000L;

	/**
	 * Characters which cannot appear in a file name on Windows, plus the escape character itself so
	 * that the encoding stays reversible.
	 */
	private static final String			ILLEGAL_FILE_NAME_CHARS			= "<>:\"/\\|?*%";

	/**
	 * The digits used when escaping a character as two hex digits
	 */
	private static final String			HEX_DIGITS						= "0123456789ABCDEF";

	/**
	 * The matcher for the cache files
	 */
	private static final PathMatcher	cacheFileMatcher				= FileSystems.getDefault().getPathMatcher( "glob:*" + FILE_EXTENSION );

	/**
	 * The pool that holds the objects
	 */
	private Path						directory;

	/**
	 * Logger
	 */
	private static final BoxLangLogger	logger							= BoxRuntime.getInstance().getLoggingService().CACHE_LOGGER;

	/**
	 * Entry paths already reported as unreadable, so a corrupt entry that is polled every second does not
	 * flood the log.
	 */
	private final java.util.Set<String>	reportedCorrupt					= java.util.concurrent.ConcurrentHashMap.newKeySet();

	/**
	 * Constructor
	 */
	public FileSystemStore() {
		// Mark this as a distributed store (persists to file system)
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
		this.provider	= provider;
		this.config		= config;
		this.directory	= Path.of( config.getAsString( Key.directory ) ).toAbsolutePath();

		// Make sure our cache directories exist
		try {
			Files.createDirectories( this.directory );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		purgeOrphanedTempFiles();

		return this;
	}

	/**
	 * Delete temporary files left behind by a process that died mid-write.
	 * <p>
	 * Writes go to a temporary file and are then swapped in, so an interrupted write leaves a complete
	 * previous entry plus one orphan. Only files older than a few minutes are removed: the directory is
	 * shared between processes, so a recent temporary file may belong to another node writing right now.
	 */
	private void purgeOrphanedTempFiles() {
		long cutoff = System.currentTimeMillis() - ORPHANED_TEMP_FILE_AGE_MILLIS;

		try ( Stream<Path> paths = Files.list( this.directory ) ) {
			paths.filter( path -> path.getFileName().toString().endsWith( TEMP_FILE_SUFFIX ) )
			    .filter( path -> {
				    try {
					    return Files.getLastModifiedTime( path ).toMillis() < cutoff;
				    } catch ( IOException e ) {
					    return false;
				    }
			    } )
			    .forEach( path -> {
				    try {
					    Files.deleteIfExists( path );
					    logger.debug( "+ Removed orphaned cache temp file [{}]", path );
				    } catch ( IOException ignored ) {
					    // Another process may be using it; it will be collected on a later start
				    }
			    } );
		} catch ( IOException e ) {
			// A cache directory that cannot be listed is not a startup failure
			logger.debug( "+ Could not scan cache directory [{}] for temp files: {}", this.directory, e.getMessage() );
		}
	}

	/**
	 * Get the directory where the cache entries are stored
	 *
	 * @return The directory where the cache entries are stored
	 */
	public Path getDirectory() {
		return this.directory;
	}

	/**
	 * Converts a path object to an entry key by removing the extension
	 *
	 * @param path the Path object of the cache entry file
	 *
	 * @return The key of the cache entry
	 */
	public Key pathToCacheKey( Path path ) {
		return Key.of( decodeKeyFromFile( FilenameUtils.removeExtension( path.getFileName().toString() ) ) );
	}

	/**
	 * Converts a key to a full path object in the pool
	 *
	 * @param key The key of the cache entry
	 *
	 * @return The Path object of the cache entry file
	 */
	public Path cacheKeyToPath( Key key ) {
		return Path.of( directory.toString(), encodeKeyForFile( key.getNameNoCase() ) + FILE_EXTENSION ).toAbsolutePath();
	}

	/**
	 * Encode a cache key into a file name which is legal on every supported platform.
	 * <p>
	 * Cache keys are arbitrary strings and may contain characters such as {@code :} -- which is
	 * perfectly legal in a key but not in a Windows file name, so using the key verbatim fails to
	 * create the entry file. Any such character is escaped as {@code %XX} with its two digit uppercase
	 * hex code, which keeps {@link #pathToCacheKey(Path)} able to recover the original key exactly.
	 *
	 * @param key the cache key to encode
	 *
	 * @return the file name safe form of the key
	 */
	private static String encodeKeyForFile( String key ) {
		// Lazily allocate, since the common case needs no escaping at all
		StringBuilder encoded = null;
		for ( int i = 0; i < key.length(); i++ ) {
			char c = key.charAt( i );
			if ( c < 0x20 || ILLEGAL_FILE_NAME_CHARS.indexOf( c ) >= 0 ) {
				if ( encoded == null ) {
					encoded = new StringBuilder( key.length() + 8 ).append( key, 0, i );
				}
				encoded.append( '%' ).append( HEX_DIGITS.charAt( ( c >> 4 ) & 0xF ) ).append( HEX_DIGITS.charAt( c & 0xF ) );
			} else if ( encoded != null ) {
				encoded.append( c );
			}
		}
		return encoded == null ? key : encoded.toString();
	}

	/**
	 * Decode a file name produced by {@link #encodeKeyForFile(String)} back into the original key.
	 *
	 * @param fileName the encoded file name, without its extension
	 *
	 * @return the original cache key
	 */
	private static String decodeKeyFromFile( String fileName ) {
		if ( fileName.indexOf( '%' ) < 0 ) {
			return fileName;
		}
		StringBuilder decoded = new StringBuilder( fileName.length() );
		for ( int i = 0; i < fileName.length(); i++ ) {
			char c = fileName.charAt( i );
			if ( c == '%' && i + 2 < fileName.length() ) {
				int	hi	= Character.digit( fileName.charAt( i + 1 ), 16 );
				int	lo	= Character.digit( fileName.charAt( i + 2 ), 16 );
				if ( hi >= 0 && lo >= 0 ) {
					decoded.append( ( char ) ( ( hi << 4 ) | lo ) );
					i += 2;
					continue;
				}
			}
			decoded.append( c );
		}
		return decoded.toString();
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
	}

	/**
	 * Flush the store to a permanent storage.
	 * Only applicable to stores that support it.
	 *
	 * Not supported by this pool.
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
		getEntryStream()
		    .parallel()
		    .map( path -> ( BoxCacheEntry ) this.getQuiet( pathToCacheKey( path ) ) )
		    // A file may vanish between enumeration and read; skip nulls instead of NPE-ing
		    .filter( Objects::nonNull )
		    .sorted( getPolicy().getComparator() )
		    // Exclude eternal objects from eviction
		    .filter( entry -> !entry.isEternal() )
		    // Check how many to evict according to the config count
		    .limit( evictCount )
		    // Evict it & Log Stats
		    .forEach( entry -> {
			    try {
				    Files.delete( Path.of( entry.metadata().getAsString( Key.path ) ) );
			    } catch ( IOException e ) {
				    throw new BoxIOException( e );
			    }
			    getProvider().getStats().recordEviction();
		    } );
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		return ( int ) getEntryStream().count();
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		try {
			FileUtils.cleanDirectory( this.directory.toFile() );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public boolean clearAll( ICacheKeyFilter filter ) {
		getEntryStream( filter ).parallel().forEach( path -> {
			try {
				Files.delete( path );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} );
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
		Path target = cacheKeyToPath( key );
		if ( !Files.exists( target ) ) {
			return false;
		}

		try {
			Files.delete( target );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

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
		return getEntryStream().map( this::pathToCacheKey ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return getEntryStream( filter ).map( this::pathToCacheKey ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		return getEntryStream().map( this::pathToCacheKey );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return getEntryStream( filter ).map( this::pathToCacheKey );
	}

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key ) {
		return Files.exists( cacheKeyToPath( key ) );
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
		Key[] foundKeys = getEntryStream( filter )
		    .map( this::pathToCacheKey )
		    .toArray( Key[]::new );

		return Stream.of( foundKeys )
		    .map(
		        key -> new AbstractMap.SimpleEntry<Key, Object>( key, lookup( key ) )
		    ).collect( BLCollector.toStruct() );
	}

	/**
	 * Get an object from the store with metadata tracking: hits, lastAccess, etc
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or null if not found
	 */
	public ICacheEntry get( Key key ) {
		var results = getQuiet( key );

		if ( results != null ) {
			// Update Stats
			results
			    .incrementHits()
			    .touchLastAccessed();
			// Is resetTimeoutOnAccess enabled? If so, jump up the creation time to increase the timeout
			if ( BooleanCaster.cast( this.config.get( Key.resetTimeoutOnAccess ) ) ) {
				results.resetCreated();
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
		getEntryStream( filter )
		    .forEach( key -> results.put( pathToCacheKey( key ), get( pathToCacheKey( key ) ) ) );
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
		Path target = cacheKeyToPath( key );
		if ( !Files.exists( target ) ) {
			return null;
		}
		return deserializeEntry( target );
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
		getEntryStream( filter ).forEach( key -> results.put( pathToCacheKey( key ), getQuiet( pathToCacheKey( key ) ) ) );
		return results;
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry ) {
		Path filePath = cacheKeyToPath( key );
		entry.metadata().put( Key.path, filePath.toString() );
		FileSystemUtil.serializeToFile( entry, filePath );
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
	 * Returns a stream of all cache entry paths.
	 * The directory walk is materialized into a list and closed here, so consumers
	 * never leak a directory handle and always observe a stable snapshot of the store.
	 *
	 * @return A stream of all cache entry paths
	 */
	private Stream<Path> getEntryStream() {
		try ( Stream<Path> paths = Files.walk( directory, 1 )
		    .filter( path -> cacheFileMatcher.matches( path.getFileName() ) ) ) {
			return paths.toList().stream();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Returns a stream of cache entry paths, after applying a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of cache entry paths filtered by the filter
	 */
	private Stream<Path> getEntryStream( ICacheKeyFilter filter ) {
		return getEntryStream()
		    .map( this::pathToCacheKey )
		    .filter( filter )
		    .map( this::cacheKeyToPath );
	}

	/**
	 * Deserialize an entry from the file system.
	 * <p>
	 * A file that cannot be deserialized is treated as a <b>miss</b>, not as an error. The cache directory
	 * is shared between processes, so an entry file can be truncated by a crash, a full disk, or a
	 * filesystem that cannot replace atomically - none of which the caller did anything wrong to cause, and
	 * none of which should crash the process that happened to read it. Writes are atomic
	 * ({@link FileSystemUtil#serializeToFile}), so this is the belt to that braces: a corrupt entry is
	 * ignored and overwritten by the next write of that key.
	 *
	 * @param entryPath The path to the entry
	 *
	 * @return The deserialized entry, or null when the file is unreadable
	 */
	private ICacheEntry deserializeEntry( Path entryPath ) {
		Object result;
		try {
			result = FileSystemUtil.deserializeFromFile( entryPath );
		} catch ( Throwable e ) {
			// Logged once per path: this sits on the read path, so repeating it for every poll would bury
			// whatever else the log had to say.
			if ( this.reportedCorrupt.add( entryPath.toString() ) ) {
				logger.warn(
				    "+ Cache entry [{}] is unreadable and will be treated as a miss: {}",
				    entryPath,
				    e.getMessage()
				);
			}
			return null;
		}

		if ( result instanceof ICacheEntry ) {
			// Readable again (rewritten by someone), so a future corruption should be reported afresh
			this.reportedCorrupt.remove( entryPath.toString() );
			return ( ICacheEntry ) result;
		} else {
			return new BoxCacheEntry(
			    Key.of( this.directory.toString() ),
			    0l,
			    0l,
			    pathToCacheKey( entryPath ),
			    result,
			    new Struct()
			);
		}
	}

}
