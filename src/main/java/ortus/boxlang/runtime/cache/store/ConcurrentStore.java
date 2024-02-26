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

import java.util.concurrent.ConcurrentSkipListMap;
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
	 * The pool that holds the objects: Ordered by created timestamp
	 */
	private ConcurrentSkipListMap<Key, ICacheEntry>	pool;

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
	public void init( ICacheProvider provider, IStruct config ) {
		super.init( provider, config );
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
		// implement
	}

	/**
	 * Flush the store to a permanent storage.
	 * Only applicable to stores that support it.
	 *
	 * @return The number of objects flushed
	 */
	public int flush() {
		// implement
		return 0;
	}

	/**
	 * Reap the storage for expired objects
	 *
	 * @return The number of objects reaped
	 */
	public int reap() {
		// implement
		return 0;
	}

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize() {
		// implement
		return 0;
	}

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll() {
		// implement
	}

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public void clearAll( ICacheKeyFilter filter ) {
		// implement
	}

	/**
	 * Clears an object from the storage
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( Key key ) {
		// implement
		return false;
	}

	/**
	 * Clears multiple objects from the storage
	 *
	 * @param key The keys to clear
	 *
	 * @return A struct of keys and their clear status
	 */
	public IStruct clear( Key... keys ) {
		// implement
		return new Struct();
	}

	/**
	 * Get all the keys in the store
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys() {
		// implement
		return new Key[ 0 ];
	}

	/**
	 * Get all the keys in the store
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter ) {
		// implement
		return new Key[ 0 ];
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream() {
		// implement
		return null;
	}

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter ) {
		// implement
		return null;
	}

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key ) {
		// implement
		return false;
	}

	/**
	 * Check if multiple objects are in the store
	 *
	 * @param key A varargs of keys to lookup in the store
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( Key... keys ) {
		// implement
		return new Struct();
	}

	/**
	 * Check if multiple objects are in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( ICacheKeyFilter filter ) {
		// implement
		return new Struct();
	}

	/**
	 * Get an object from the store with metadata tracking
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or an empty cache entry if not found
	 */
	public ICacheEntry get( Key key ) {
		// implement
		return null;
	}

	/**
	 * Get multiple objects from the store with metadata tracking
	 *
	 * @param key The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( Key... keys ) {
		// implement
		return new Struct();
	}

	/**
	 * Get multiple objects from the store with metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( ICacheKeyFilter filter ) {
		// implement
		return new Struct();
	}

	/**
	 * Get an object from cache with no metadata tracking
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or an empty cache entry if not found
	 */
	public ICacheEntry getQuiet( Key key ) {
		// implement
		return null;
	}

	/**
	 * Get multiple objects from the store with no metadata tracking
	 *
	 * @param key The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( Key... keys ) {
		// implement
		return new Struct();
	}

	/**
	 * Get multiple objects from the store with no metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( ICacheKeyFilter filter ) {
		// implement
		return new Struct();
	}

	/**
	 * Expire an object from the store
	 *
	 * @param key The key to expire
	 *
	 * @return True if the object was expired, false otherwise (if the object was not found in the store)
	 */
	public boolean expire( Key key ) {
		// implement
		return false;
	}

	/**
	 * Expire check for an object in the store
	 *
	 * @param key The key to check
	 *
	 * @return True if the object is expired, false otherwise (could be not found in the store or not expired yet)
	 */
	public boolean isExpired( Key key ) {
		// implement
		return false;
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry ) {
		// implement
	}

	/**
	 * Set's multiple objects in the storage
	 *
	 * @param entries The keys and cache entries to store
	 */
	public void set( IStruct entries ) {
		// implement
	}

}
