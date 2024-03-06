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

import java.util.stream.Stream;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * The main interface for object storages for a BoxLangCache.
 * This store can be used to store objects in memory or in a persistent storage or whatever you want.
 * A cache provider coordinates user interactions an acts more like a cache service to the underlying
 * cache store.
 *
 * Each object store must adhere to this interface and will receive {@link Key} objects as keys so the
 * object store can decide how to store the objects if in case-sensitive or case-insensitive manner.
 *
 * Each object store will also use the {@link ICacheEntry} to store the data so it's consistent across
 * all cache providers and stores.
 *
 * You can implement your own store to store objects in a different way or create your own
 * cache provider to talk to different Caches as well.
 */
public interface IObjectStore {

	/**
	 * Get the configuration for the store
	 */
	public IStruct getConfig();

	/**
	 * Get the associated cache provider
	 */
	public ICacheProvider getProvider();

	/**
	 * Some storages require a method to initialize the storage or do
	 * object loading. This method is called when the cache provider is started.
	 *
	 * @param provider The cache provider associated with this store
	 * @param config   The configuration for the store
	 */
	public IObjectStore init( ICacheProvider provider, IStruct config );

	/**
	 * Some storages require a shutdown method to close the storage or do
	 * object saving. This method is called when the cache provider is stopped.
	 */
	public void shutdown();

	/**
	 * Flush the store to a permanent storage.
	 * Only applicable to stores that support it.
	 *
	 * @return The number of objects flushed
	 */
	public int flush();

	/**
	 * Runs the eviction algorithm to remove objects from the store based on the eviction policy
	 * and eviction count.
	 */
	public void evict();

	/**
	 * Get the size of the store, not the size in bytes but the number of objects in the store
	 */
	public int getSize();

	/**
	 * Clear all the elements in the store
	 */
	public void clearAll();

	/**
	 * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public void clearAll( ICacheKeyFilter filter );

	/**
	 * Clears an object from the storage
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( Key key );

	/**
	 * Clears multiple objects from the storage
	 *
	 * @param key The keys to clear
	 *
	 * @return A struct of keys and their clear status
	 */
	public IStruct clear( Key... keys );

	/**
	 * Get all the keys in the store
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys();

	/**
	 * Get all the keys in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Key[] getKeys( ICacheKeyFilter filter );

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream();

	/**
	 * Get all the keys in the store as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<Key> getKeysStream( ICacheKeyFilter filter );

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( Key key );

	/**
	 * Check if multiple objects are in the store
	 *
	 * @param key A varargs of keys to lookup in the store
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( Key... keys );

	/**
	 * Check if multiple objects are in the store using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( ICacheKeyFilter filter );

	/**
	 * Get an object from the store with metadata tracking
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or null
	 */
	public ICacheEntry get( Key key );

	/**
	 * Get multiple objects from the store with metadata tracking
	 *
	 * @param key The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( Key... keys );

	/**
	 * Get multiple objects from the store with metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( ICacheKeyFilter filter );

	/**
	 * Get an object from cache with no metadata tracking
	 *
	 * @param key The key to retrieve
	 *
	 * @return The cache entry retrieved or null
	 */
	public ICacheEntry getQuiet( Key key );

	/**
	 * Get multiple objects from the store with no metadata tracking
	 *
	 * @param key The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( Key... keys );

	/**
	 * Get multiple objects from the store with no metadata tracking using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct getQuiet( ICacheKeyFilter filter );

	/**
	 * Sets an object in the storage
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 */
	public void set( Key key, ICacheEntry entry );

	/**
	 * Set's multiple objects in the storage
	 *
	 * @param entries The keys and cache entries to store
	 */
	public void set( IStruct entries );

}
