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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This object store keeps all objects in heap using Concurrent classes.
 * Naturally the store is ordered by {@code created} timestamp and can be used for concurrent access.
 */
public class ConcurrentStore extends AbstractStore implements IObjectStore {

	/**
	 * Logger
	 */
	private static final Logger						logger	= LoggerFactory.getLogger( ConcurrentStore.class );

	/**
	 * The pool that holds the objects
	 */
	protected ConcurrentHashMap<Key, ICacheEntry>	pool;

	/**
	 * Constructor
	 */
	public ConcurrentStore() {
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
		this.pool		= new ConcurrentHashMap<>( config.getAsInteger( Key.maxObjects ) / 4 );

		logger.atDebug().log(
		    "ConcurrentStore({}) initialized with a max size of {}",
		    provider.getName(),
		    config.getAsInteger( Key.maxObjects )
		);
		return this;
	}

	/**
	 * Get the pool of objects
	 * ConcurrentStore uses a ConcurrentHashMap to store the objects
	 *
	 * @return The pool of objects
	 */
	public ConcurrentMap<Key, ICacheEntry> getPool() {
		return this.pool;
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
		this.pool.clear();
		logger.atDebug().log(
		    "ConcurrentStore({}) was shutdown",
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
		logger.atDebug().log(
		    "ConcurrentStore({}) was flushed",
		    provider.getName()
		);
		return 0;
	}

	/**
	 * Runs the eviction algorithm to remove objects from the store based on the eviction policy
	 * and eviction count.
	 */
	public void evict() {
		this.pool.entrySet()
		    // Stream it
		    .parallelStream()
		    // Sort using the policy comparator
		    .sorted( Map.Entry.comparingByValue( getPolicy().getComparator() ) )
		    // Exclude eternal objects and already expired objects they will be removed by the reaper
		    .filter( entry -> !entry.getValue().isEternal() && !entry.getValue().isExpired() )
		    // Check how many to expire according to the config count
		    .limit( this.config.getAsInteger( Key.evictCount ) )
		    // Evict it & Log Stats
		    .forEach( entry -> {
			    logger.atDebug().log(
			        "ConcurrentStore({}) evicted [{}]",
			        provider.getName(),
			        entry.getKey()
			    );
			    this.pool.remove( entry.getKey() );
			    getProvider().getStats().recordEviction();
		    } );
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		return this.pool.size();
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		this.pool.clear();
	}

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public void clearAll( ICacheKeyFilter filter ) {
		this.pool.keySet().removeIf( filter );
	}

	/**
	 * Clears an object from the storage
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( Key key ) {
		return this.pool.remove( key ) != null;
	}

	/**
	 * Clears multiple objects from the storage
	 *
	 * @param key The keys to clear
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
		return this.pool.keySet().toArray( new Key[ 0 ] );
	}

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return this.pool.keySet().parallelStream().filter( filter ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		return this.pool.keySet().stream();
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return this.pool.keySet().stream().filter( filter );
	}

	/**
	 * Check if an object is in the store and not expired
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key ) {
		// Key is in the store and not expired
		return this.pool.computeIfPresent(
		    key,
		    ( k, cacheEntry ) -> cacheEntry.isExpired() ? null : cacheEntry
		) != null;
	}

	/**
	 * Check if multiple objects are in the store
	 *
	 * @param key A varargs of keys to lookup in the store
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
	 * @return A struct of the keys found. True if the object is in the store, false otherwise or expired
	 */
	public IStruct lookup( ICacheKeyFilter filter ) {
		IStruct results = new Struct();
		this.pool
		    .entrySet()
		    .stream()
		    .filter( entry -> filter.test( entry.getKey() ) )
		    .forEach( entry -> results.put(
		        entry.getKey(),
		        !entry.getValue().isExpired()
		    ) );
		return results;
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
	 * @param key The keys to retrieve
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
		this.pool.keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, get( key ) ) );
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
		return this.pool.getOrDefault( key, null );
	}

	/**
	 * Get multiple objects from the store with no metadata tracking
	 *
	 * @param key The keys to retrieve
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
		this.pool.keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, getQuiet( key ) ) );
		return results;
	}

	/**
	 * Expire an object from the store
	 *
	 * @param key The key to expire
	 *
	 * @return True if the object was expired, false otherwise (if the object was not found in the store)
	 */
	public boolean expire( Key key ) {
		var results = getQuiet( key );
		if ( results != null ) {
			results.expire();
			return true;
		}
		return false;
	}

	/**
	 * Expire multiple objects from the store
	 *
	 * @param keys The keys to expire
	 *
	 * @return A struct of keys and their expire status
	 */
	public IStruct expire( Key... keys ) {
		IStruct results = new Struct();
		for ( Key key : keys ) {
			results.put( key, expire( key ) );
		}
		return results;
	}

	/**
	 * Expire multiple objects from the store using a filter
	 *
	 * @param filter The filter that determines which keys to expire
	 *
	 * @return A struct of keys and their expire status
	 */
	public IStruct expire( ICacheKeyFilter filter ) {
		IStruct results = new Struct();
		this.pool.keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, expire( key ) ) );
		return results;
	}

	/**
	 * Expire check for an object in the store
	 *
	 * @param key The key to check
	 *
	 * @return True if the object is expired, false otherwise (could be not found in the store or not expired yet)
	 */
	public boolean isExpired( Key key ) {
		var results = getQuiet( key );
		return results != null && results.isExpired();
	}

	/**
	 * Expire check for multiple objects in the store
	 *
	 * @param keys The keys to check
	 *
	 * @return A struct of keys and their expire status
	 */
	public IStruct isExpired( Key... keys ) {
		IStruct results = new Struct();
		for ( Key key : keys ) {
			results.put( key, isExpired( key ) );
		}
		return results;
	}

	/**
	 * Expire check for multiple objects in the store using a filter
	 *
	 * @param filter The filter that determines which keys to check
	 *
	 * @return A struct of keys and their expire status
	 */
	public IStruct isExpired( ICacheKeyFilter filter ) {
		IStruct results = new Struct();
		this.pool.keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, isExpired( key ) ) );
		return results;
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry ) {
		this.pool.put( key, entry );
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
