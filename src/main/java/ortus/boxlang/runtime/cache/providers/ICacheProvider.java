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
package ortus.boxlang.runtime.cache.providers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.store.IObjectStore;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

/**
 * A BoxLang cache provider that can talk to any cache implementation.
 */
public interface ICacheProvider {

	/**
	 * Get the cache service that is managing this cache provider
	 *
	 * @return The cache service
	 */
	public CacheService getCacheService();

	/**
	 * Get the stats object
	 */
	public ICacheStats getStats();

	/**
	 * Clear stats
	 *
	 * @return The cache provider
	 */
	public ICacheProvider clearStats();

	/**
	 * Get the name of the cache provider
	 */
	public Key getName();

	/**
	 * Set the name of the cache provider
	 *
	 * @param name The name of the cache provider
	 */
	public ICacheProvider setName( Key name );

	/**
	 * Get the cache provider type
	 */
	public String getType();

	/**
	 * Get the cache configuration for this provider
	 */
	public CacheConfig getConfig();

	/**
	 * Returns a flag if the cache provider is enabled
	 */
	public boolean isEnabled();

	/**
	 * Returns a flag indicating if the cache has reporting enabled
	 */
	public boolean isReportingEnabled();

	/**
	 * Configure the cache provider for operation
	 *
	 * @param cacheService The cache service that is configuring the cache provider
	 * @param config       The configuration object
	 *
	 * @return The cache provider
	 */
	public ICacheProvider configure( CacheService cacheService, CacheConfig config );

	/**
	 * Shutdown the cache provider
	 */
	public void shutdown();

	/**
	 * Get the object store if the cache provider supports it
	 */
	public IObjectStore getObjectStore();

	/**
	 * Get a structure of all the keys in the cache with their appropriate metadata structures. This is used to build the
	 * reporting.[keyX->[metadataStructure]]
	 *
	 * @param limit The limit of keys to return, default is all keys or 0
	 */
	public IStruct getStoreMetadataReport( int limit );

	/**
	 * Get a structure of all the keys in the cache with their appropriate metadata structures. This is used to build the
	 * reporting.[keyX->[metadataStructure]]
	 * This should be using a limit of 0, or all keys
	 */
	public IStruct getStoreMetadataReport();

	/**
	 * Get a key lookup structure where cachebox can build the report on.
	 * Ex: [timeout=timeout, lastAccessTimeout=idleTimeout]. It is a way for the
	 * visualizer to construct the columns correctly on the reports
	 */
	public IStruct getStoreMetadataKeyMap();

	/**
	 * Get a cache objects metadata about its performance. This value is a structure of name-value pairs of metadata.
	 *
	 * @param key The key of the object
	 *
	 * @return The metadata structure
	 */
	public IStruct getCachedObjectMetadata( String key );

	/**
	 * Get a cache objects metadata about its performance. This value is a structure of name-value pairs of metadata.
	 *
	 * @param key A varargs of keys of the object
	 *
	 * @return The metadata structure of structures
	 */
	public IStruct getCachedObjectMetadata( String... key );

	/**
	 * Reap the cache provider of any stale or expired objects
	 */
	public void reap();

	/**
	 * Get the size of the cache provider
	 */
	public int getSize();

	/**
	 * Clear all the elements in the cache provider
	 */
	public void clearAll();

	/**
	 * Clear all the elements in the cache provider with a ${@link ICacheKeyFilter} predicate.
	 * This can be a lambda or method reference since it's a functional interface.
	 *
	 * @param filter The filter that determines which keys to clear
	 */
	public boolean clearAll( ICacheKeyFilter filter );

	/**
	 * Clears an object from the cache provider with no stats updated or listeners
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clearQuiet( String key );

	/**
	 * Clears an object from the cache provider
	 *
	 * @param key The object key to clear
	 *
	 * @return True if the object was cleared, false otherwise (if the object was not found in the store)
	 */
	public boolean clear( String key );

	/**
	 * Clears multiple objects from the cache provider
	 *
	 * @param keys The keys to clear
	 *
	 * @return A struct of keys and their clear status
	 */
	public IStruct clear( String... keys );

	/**
	 * Get all the keys in the cache provider
	 *
	 * @return An array of keys in the cache
	 */
	public Array getKeys();

	/**
	 * Get all the keys in the cache provider using a filter
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return An array of keys in the cache
	 */
	public Array getKeys( ICacheKeyFilter filter );

	/**
	 * Get all the keys in the cache provider as a stream
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<String> getKeysStream();

	/**
	 * Get all the keys in the cache provider as a stream
	 *
	 * @param filter The filter that determines which keys to return
	 *
	 * @return A stream of keys in the cache
	 */
	public Stream<String> getKeysStream( ICacheKeyFilter filter );

	/**
	 * Check if an object is in the store
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookup( String key );

	/**
	 * Check if an object is in the store with no stats updated or listeners
	 *
	 * @param key The key to lookup in the store
	 *
	 * @return True if the object is in the store, false otherwise
	 */
	public boolean lookupQuiet( String key );

	/**
	 * Check if multiple objects are in the store
	 *
	 * @param keys A varargs of keys to lookup in the store
	 *
	 * @return A struct of keys and their lookup status
	 */
	public IStruct lookup( String... keys );

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
	 * @return The value retrieved or null
	 */
	public Optional<Object> get( String key );

	/**
	 * Get an object from the store with metadata tracking async.
	 *
	 * @param key The key to retrieve
	 *
	 * @return CompletableFuture of the value retrieved or null
	 */
	public CompletableFuture<Optional<Object>> getAsync( String key );

	/**
	 * Get multiple objects from the store with metadata tracking
	 *
	 * @param keys The keys to retrieve
	 *
	 * @return A struct of keys and their cache entries
	 */
	public IStruct get( String... keys );

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
	public Optional<Object> getQuiet( String key );

	/**
	 * Sets an object in the storage with no stats updated or listeners
	 *
	 * @param key   The key to store
	 * @param value The value to store
	 */
	public void setQuiet( Key key, ICacheEntry value );

	/**
	 * Sets an object in the storage
	 *
	 * @param key               The key to store
	 * @param value             The value to store
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 * @param metadata          The metadata to store
	 */
	public void set( String key, Object value, Duration timeout, Duration lastAccessTimeout, IStruct metadata );

	/**
	 * Sets an object in the storage
	 *
	 * @param key               The key to store
	 * @param value             The value to store
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 */
	public void set( String key, Object value, Duration timeout, Duration lastAccessTimeout );

	/**
	 * Sets an object in the storage with a default last access timeout
	 *
	 * @param key     The key to store
	 * @param value   The value to store
	 * @param timeout The timeout in seconds
	 */
	public void set( String key, Object value, Duration timeout );

	/**
	 * Sets an object in the storage using the default timeout and last access timeout
	 *
	 * @param key   The key to store
	 * @param value The value to store
	 */
	public void set( String key, Object value );

	/**
	 * Set's multiple objects in the storage using all the same default timeout and last access timeouts
	 *
	 * @param entries The keys and cache entries to store
	 */
	public void set( IStruct entries );

	/**
	 * Set's multiple objects in the storage using all the same default timeout and last access timeouts
	 *
	 * @param entries           The keys and cache entries to store in the cache
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 */
	public void set( IStruct entries, Duration timeout, Duration lastAccessTimeout );

	/**
	 * Tries to get an object from the cache, if not found, it will call the lambda to get the value and store it in the cache
	 * with the default timeout and last access timeout
	 * <p>
	 * This is a convenience method to avoid the double lookup pattern
	 * <p>
	 * <code>
	 * var value =
	 * cache.getOrSet( "myKey", () -> {
	 * return "myValue";
	 * });
	 * </code>
	 * <p>
	 * This is the same as:
	 * <code>
	 * var value = cache.get( "myKey" ).orElseGet( () -> {
	 * var value = "myValue";
	 * cache.set( "myKey", value );
	 * return value;
	 * });
	 * </code>
	 * <p>
	 * This method is thread safe and will only call the lambda once if the key is not found in the cache
	 * </p>
	 *
	 * @param key               The key to retrieve
	 * @param provider          The lambda to call if the key is not found
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 * @param metadata          The metadata to store
	 */
	public Optional<Object> getOrSet( String key, Supplier<Object> provider, Duration timeout, Duration lastAccessTimeout, IStruct metadata );

	/**
	 * Tries to get an object from the cache, if not found, it will call the lambda to get the value and store it in the cache
	 * with the default timeout and last access timeout
	 *
	 * @param key               The key to retrieve
	 * @param provider          The lambda to call if the key is not found
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 *
	 * @return The object
	 */
	public Optional<Object> getOrSet( String key, Supplier<Object> provider, Duration timeout, Duration lastAccessTimeout );

	/**
	 * Tries to get an object from the cache, if not found, it will call the lambda to get the value and store it in the cache
	 * with the default timeout and last access timeout
	 *
	 * @param key      The key to retrieve
	 * @param provider The lambda to call if the key is not found
	 * @param timeout  The timeout in seconds
	 *
	 * @return The object
	 */
	public Optional<Object> getOrSet( String key, Supplier<Object> provider, Duration timeout );

	/**
	 * Tries to get an object from the cache, if not found, it will call the lambda to get the value and store it in the cache
	 * with the default timeout and last access timeout
	 *
	 * @param key      The key to retrieve
	 * @param provider The lambda to call if the key is not found
	 *
	 * @return The object
	 */
	public Optional<Object> getOrSet( String key, Supplier<Object> provider );
}
