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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This object store keeps all objects in heap using Concurrent classes.
 * Naturally the store is ordered by {@code created} timestamp and can be used for concurrent access.
 */
public class ConcurrentSoftReferenceStore extends AbstractStore {

	/**
	 * Logger
	 */
	private static final Logger									logger	= LoggerFactory.getLogger( ConcurrentSoftReferenceStore.class );

	/**
	 * The concurrent pool of objects based on a soft reference
	 */
	private ConcurrentHashMap<Key, SoftReference<ICacheEntry>>	pool;

	/**
	 * Reverse lookup map for soft references
	 */
	private ConcurrentHashMap<Integer, Key>						softRefKeyMap;

	/**
	 * Reference queue for soft references
	 */
	private ReferenceQueue<ICacheEntry>							referenceQueue;

	/**
	 * Constructor
	 */
	public ConcurrentSoftReferenceStore() {
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
		int maxObjects = IntegerCaster.cast( config.get( Key.maxObjects ) );
		this.pool			= new ConcurrentHashMap<>( maxObjects / 4 );
		this.softRefKeyMap	= new ConcurrentHashMap<>( maxObjects / 4 );
		this.referenceQueue	= new ReferenceQueue<>();

		logger.debug(
		    "ConcurrentSoftReferenceStore({}) initialized with a max size of {}",
		    provider.getName(),
		    maxObjects
		);

		return this;
	}

	/**
	 * Get the pool of objects
	 * ConcurrentStore uses a ConcurrentHashMap to store the objects
	 *
	 * @return The pool of objects
	 */
	public ConcurrentMap<Key, SoftReference<ICacheEntry>> getPool() {
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
		getPool().clear();
		logger.debug(
		    "ConcurrentSoftReferenceStore({}) was shutdown",
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
		    "ConcurrentSoftReferenceStore({}) was flushed",
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
		getPool()
		    .entrySet()
		    // Stream it
		    .parallelStream()
		    // Map it to the cache entry from the soft reference
		    .map( reference -> reference.getValue().get() )
		    // Exclude eternal objects from eviction or nulls
		    .filter( entry -> entry != null && !entry.isEternal() )
		    // Sort using the policy comparator
		    .sorted( getPolicy().getComparator() )
		    // Check how many to evict according to the config count
		    .limit( evictCount )
		    // Evict it & Log Stats
		    .forEach( entry -> {
			    logger.debug(
			        "ConcurrentSoftReferenceStore({}) evicted [{}]",
			        provider.getName(),
			        entry.key()
			    );
			    getPool().remove( entry.key() );
			    getProvider().getStats().recordEviction();
		    } );

		// Evict all garbage collected soft references
		evictSoftReferences();
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		return getPool().size();
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		getPool().clear();
		this.softRefKeyMap.clear();
	}

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public boolean clearAll( ICacheKeyFilter filter ) {
		this.softRefKeyMap.values().removeIf( filter );
		return getPool().keySet().removeIf( filter );
	}

	/**
	 * Clears an object from the storage
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( Key key ) {
		// Remove the soft reference from the pool
		SoftReference<ICacheEntry> reference = this.pool.remove( key );
		if ( reference != null ) {
			// Remove the soft reference from the reverse lookup map
			this.softRefKeyMap.remove( reference.hashCode() );
			return true;
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
		return getPool().keySet().toArray( new Key[ 0 ] );
	}

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		return getPool().keySet().parallelStream().filter( filter ).toArray( Key[]::new );
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		return getPool().keySet().stream();
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		return getPool().keySet().stream().filter( filter );
	}

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key ) {
		var entry = this.pool.get( key );

		if ( entry != null ) {
			ICacheEntry cacheEntry = entry.get();
			// If the entry is null, it was collected by the GC
			if ( cacheEntry == null ) {
				clear( key );
				getProvider().getStats().recordGCHit();
				return false;
			}
			return true;
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
		IStruct results = new Struct();
		getPool()
		    .keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, true ) );
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
		getPool()
		    .keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, get( key ) ) );
		return results;
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
		getPool()
		    .keySet()
		    .parallelStream()
		    .filter( filter )
		    .forEach( key -> results.put( key, getQuiet( key ) ) );
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
		var reference = this.pool.get( key );

		if ( reference != null ) {
			ICacheEntry cacheEntry = reference.get();
			// If the entry is null, it was collected by the GC
			if ( cacheEntry == null ) {
				clear( key );
				getProvider().getStats().recordGCHit();
			} else {
				return cacheEntry;
			}
		}

		return null;
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry ) {
		// Create Soft Reference Wrapper and register with Queue
		SoftReference<ICacheEntry> softReference = createSoftReference( key, entry );
		// Store the soft reference in the pool
		this.pool.put( key, softReference );
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
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Evict soft references from the store that have been collected
	 */
	@SuppressWarnings( "unchecked" )
	public synchronized void evictSoftReferences() {
		SoftReference<ICacheEntry> collected;
		while ( ( collected = ( SoftReference<ICacheEntry> ) this.referenceQueue.poll() ) != null ) {
			if ( verifySoftReference( collected ) ) {
				clear( getSoftReferenceKey( collected ) );
				this.softRefKeyMap.remove( collected.hashCode() );
				getProvider().getStats().recordGCHit();
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a soft reference for an incoming entry
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 *
	 * @return The soft reference created
	 */
	private SoftReference<ICacheEntry> createSoftReference( Key key, ICacheEntry entry ) {
		// Create Soft Reference Wrapper and register with Queue
		SoftReference<ICacheEntry> softReference = new SoftReference<>( entry, this.referenceQueue );
		// Create a reverse lookup map for the soft reference key
		this.softRefKeyMap.put( softReference.hashCode(), key );
		return softReference;
	}

	/**
	 * Verify if the soft reference is in the key map
	 *
	 * @param softReference The soft reference to verify
	 *
	 * @return True if the soft reference is in the key map via the hash code
	 */
	private boolean verifySoftReference( SoftReference<ICacheEntry> softReference ) {
		return this.softRefKeyMap.containsKey( softReference.hashCode() );
	}

	/**
	 * Get the soft reference key from the key map
	 *
	 * @param softReference The soft reference to get the key for
	 *
	 * @return The key for the soft reference
	 */
	private Key getSoftReferenceKey( SoftReference<ICacheEntry> softReference ) {
		return this.softRefKeyMap.get( softReference.hashCode() );
	}

}
