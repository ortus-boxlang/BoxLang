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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * This object store keeps all objects in heap using Concurrent classes.
 * Naturally the store is ordered by {@code created} timestamp and can be used for concurrent access.
 */
public class FileSystemStore extends AbstractStore implements IObjectStore {

	/**
	 * Logger
	 */
	private static final Logger	logger				= LoggerFactory.getLogger( FileSystemStore.class );

	private final String		extension			= ".cache";

	private final PathMatcher	cacheFileMatcher	= FileSystems.getDefault().getPathMatcher( "glob:*" + extension );

	/**
	 * The pool that holds the objects
	 */
	private Path				directory;

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
		this.directory	= Path.of( config.getAsString( Key.directory ) );

		logger.atDebug().log(
		    "FileSystemStore({}) initialized with a max size of {}",
		    provider.getName(),
		    config.getAsInteger( Key.maxObjects )
		);
		return this;
	}

	private Stream<Path> getEntryStream() {
		try {
			return Files.walk( directory, 1 )
			    .filter( path -> Files.isRegularFile( path ) && cacheFileMatcher.matches( path.getFileName() ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	private Stream<Path> getEntryStream( ICacheKeyFilter filter ) {
		List<Key> filteredKeys = getEntryStream()
		    .map( path -> pathToCacheKey( path ) )
		    .filter( filter )
		    .collect( Collectors.toList() );
		return getEntryStream()
		    .filter( path -> filteredKeys.contains( pathToCacheKey( path ) ) );
	}

	public Key pathToCacheKey( Path path ) {
		String fileName = StringCaster.cast( path.getFileName().toString() );
		return Key.of( fileName.substring( 0, fileName.length() - extension.length() ) );
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
		getEntryStream().forEach( path -> {
			try {
				Files.delete( path );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} );
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
		logger.atDebug().log(
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
		getEntryStream().forEach( path -> {
			try {
				Files.delete( path );
				getProvider().getStats().recordEviction();
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
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
		getEntryStream().forEach( path -> {
			try {
				Files.delete( path );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} );
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
		Path entry = getEntryStream().filter( path -> key.equals( pathToCacheKey( path ) ) ).findFirst().orElse( null );
		if ( entry != null ) {
			try {
				Files.delete( entry );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
			return true;
		} else {
			return false;
		}
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
		return getEntryStream().map( path -> pathToCacheKey( path ) ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return getEntryStream( filter ).map( path -> pathToCacheKey( path ) ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		return getEntryStream().map( path -> pathToCacheKey( path ) );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return getEntryStream( filter ).map( path -> pathToCacheKey( path ) );
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
		    .map( path -> pathToCacheKey( path ) )
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
			if ( this.config.getAsBoolean( Key.resetTimeoutOnAccess ) ) {
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

	private ICacheEntry deserializeEntry( Path entryPath ) {
		try ( InputStream fileStream = Files.newInputStream( entryPath ) ) {
			try ( ObjectInputStream objStream = new ObjectInputStream( fileStream ) ) {
				Object result = objStream.readObject();
				if ( result instanceof ICacheEntry ) {
					return ( ICacheEntry ) result;
				} else {
					return new BoxCacheEntry( Key.of( directory.toAbsolutePath().toString() ), 0l, 0l, pathToCacheKey( entryPath ), result, new Struct() );
				}
			} catch ( Throwable e ) {
				throw new BoxRuntimeException( String.format(
				    "The cache entry [%s] could not be read from the directory [%s]. The message received was: %s",
				    pathToCacheKey( entryPath ),
				    directory.toAbsolutePath().toString(),
				    e.getMessage()
				),
				    e
				);
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
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
		Path filePath = Path.of( directory.toAbsolutePath().toString(), key.getNameNoCase() + extension );
		try ( OutputStream fileStream = Files.newOutputStream( filePath ) ) {
			Files.deleteIfExists( filePath );
			try ( ObjectOutputStream objStream = new ObjectOutputStream( fileStream ) ) {
				objStream.writeObject( entry );
			} catch ( Throwable e ) {
				throw new BoxRuntimeException( String.format(
				    "The cache entry [%s] could not be written to the directory [%s]. The message received was: %s",
				    key.getName(),
				    directory.toAbsolutePath().toString(),
				    e.getMessage()
				),
				    e
				);
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
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

}
