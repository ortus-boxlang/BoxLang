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
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
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
	 * Logger
	 */
	private static final Logger			logger				= LoggerFactory.getLogger( FileSystemStore.class );

	/**
	 * The extension for the cache files
	 */
	private static final String			FILE_EXTENSION		= ".cache";

	/**
	 * The matcher for the cache files
	 */
	private static final PathMatcher	cacheFileMatcher	= FileSystems.getDefault().getPathMatcher( "glob:*" + FILE_EXTENSION );

	/**
	 * The pool that holds the objects
	 */
	private Path						directory;

	/**
	 * Constructor
	 */
	public FileSystemStore() {
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
		this.directory	= Path.of( config.getAsString( Key.directory ) ).toAbsolutePath();

		// Make sure our cache directories exist
		try {
			Files.createDirectories( this.directory );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		logger.debug(
		    "FileSystemStore({}) initialized with a max size of {}",
		    provider.getName(),
		    IntegerCaster.cast( config.get( Key.maxObjects ) )
		);
		return this;
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
		return Key.of( FilenameUtils.removeExtension( path.getFileName().toString() ) );
	}

	/**
	 * Converts a key to a full path object in the pool
	 *
	 * @param key The key of the cache entry
	 *
	 * @return The Path object of the cache entry file
	 */
	public Path cacheKeyToPath( Key key ) {
		return Path.of( directory.toString(), key.getNameNoCase() + FILE_EXTENSION ).toAbsolutePath();
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
		logger.debug(
		    "FileSystemStore({}) was shutdown",
		    provider.getName()
		);
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
		logger.debug(
		    "FileSystemStore({}) was flushed",
		    provider.getName()
		);
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
		getEntryStream()
		    .parallel()
		    .map( path -> ( BoxCacheEntry ) this.getQuiet( pathToCacheKey( path ) ) )
		    .sorted( getPolicy().getComparator() )
		    // Exclude eternal objects from eviction
		    .filter( entry -> !entry.isEternal() )
		    // Check how many to evict according to the config count
		    .limit( evictCount )
		    // Evict it & Log Stats
		    .forEach( entry -> {
			    logger.debug(
			        "FileSystemStore({}) evicted [{}]",
			        provider.getName(),
			        entry.key().getName()
			    );
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
		return getEntryStream().anyMatch( path -> key.equals( pathToCacheKey( path ) ) );
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
		Path foundEntry = getEntryStream().filter( path -> key.equals( pathToCacheKey( path ) ) ).findFirst().orElse( null );
		return foundEntry == null
		    ? null
		    : deserializeEntry( foundEntry );
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
	 * Returns a stream of all cache entry paths
	 *
	 * @return A stream of all cache entry paths
	 */
	private Stream<Path> getEntryStream() {
		try {
			return Files.walk( directory, 1 )
			    .filter( path -> cacheFileMatcher.matches( path.getFileName() ) );
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
	 * Deserialize an entry from the file system
	 *
	 * @param entryPath The path to the entry
	 *
	 * @return The deserialized entry
	 */
	private ICacheEntry deserializeEntry( Path entryPath ) {
		Object result = FileSystemUtil.deserializeFromFile( entryPath );
		if ( result instanceof ICacheEntry ) {
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
