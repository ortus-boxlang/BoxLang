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

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.cache.store.IObjectStore;
import ortus.boxlang.runtime.cache.store.ObjectStoreType;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Cache Provider for BoxLang
 * This is an optional base class for all cache providers.
 */
public abstract class AbstractCacheProvider implements ICacheProvider {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public static final Class<ObjectStoreType>	TYPES				= ObjectStoreType.class;

	/**
	 * --------------------------------------------------------------------------
	 * Protected Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The name of the cache provider
	 */
	protected Key								name;

	/**
	 * Atomic Boolean if the cache provider is enabled
	 */
	protected AtomicBoolean						enabled				= new AtomicBoolean( false );

	/**
	 * Reporting enabled bit
	 */
	protected boolean							reportingEnabled	= false;

	/**
	 * The ICacheStats object for this cache provider
	 */
	protected ICacheStats						stats;

	/**
	 * The cache configuration
	 */
	protected CacheConfig						config;

	/**
	 * Cache Service
	 */
	protected CacheService						cacheService;

	/**
	 * --------------------------------------------------------------------------
	 * Base Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the cache service that is using this provider
	 */
	public CacheService getCacheService() {
		return this.cacheService;
	}

	/**
	 * Get the cache stats
	 */
	public ICacheStats getStats() {
		return this.stats;
	}

	/**
	 * Clear The stats
	 */
	public ICacheProvider clearStats() {
		this.stats.reset();
		return this;
	}

	/**
	 * Get the name of the cache provider
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * Set the name of the cache provider
	 */
	public ICacheProvider setName( Key name ) {
		this.name = name;
		return this;
	}

	/**
	 * Get the provider type
	 *
	 * @return The provider type
	 */
	public String getType() {
		return this.config.provider.getName();
	}

	/**
	 * Get the cache configuration
	 *
	 * @return The cache configuration
	 */
	public CacheConfig getConfig() {
		return this.config;
	}

	/**
	 * Is it enabled
	 */
	public boolean isEnabled() {
		return this.enabled.get();
	}

	/**
	 * Is reporting enabled
	 */
	public boolean isReportingEnabled() {
		return this.reportingEnabled;
	}

	/**
	 * Get a cache objects metadata about its performance. This value is a structure of name-value pairs of metadata.
	 *
	 * @param key A varargs of keys of the object
	 *
	 * @return The metadata structure of structures
	 */
	public IStruct getCachedObjectMetadata( String... key ) {
		IStruct results = new Struct();
		for ( String k : key ) {
			results.put( k, this.getCachedObjectMetadata( k ) );
		}
		return results;
	}

	/**
	 * Configure the cache provider for operation
	 *
	 * @param cacheService The cache service that is configuring the cache provider
	 * @param config       The configuration object
	 *
	 * @return The cache provider
	 */
	public ICacheProvider configure( CacheService cacheService, CacheConfig config ) {
		this.cacheService	= cacheService;
		this.config			= config;
		this.name			= config.name;
		return this;
	}

	/**
	 * Get an object from the store with metadata tracking using a CompletableFuture
	 *
	 * @param key The key to retrieve
	 *
	 * @return CompletableFuture of the value retrieved or null
	 */
	public CompletableFuture<Optional<Object>> getAsync( String key ) {
		return CompletableFuture.supplyAsync( () -> get( key ), getTaskScheduler().executor() );
	}

	/**
	 * Sets an object in the storage
	 *
	 * @param key               The key to store
	 * @param value             The value to store
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 */
	public void set( String key, Object value, Duration timeout, Duration lastAccessTimeout ) {
		set( key, value, timeout, lastAccessTimeout, new Struct() );
	}

	/**
	 * Sets an object in the storage with a default last access timeout
	 *
	 * @param key     The key to store
	 * @param value   The value to store
	 * @param timeout The timeout in seconds
	 */
	public void set( String key, Object value, Duration timeout ) {
		Duration lastAccessTimeout = Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultLastAccessTimeout ) );
		set( key, value, timeout, lastAccessTimeout, new Struct() );
	}

	/**
	 * Sets an object in the storage using the default timeout and last access timeout
	 *
	 * @param key   The key to store
	 * @param value The value to store
	 */
	public void set( String key, Object value ) {
		Duration	timeout				= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultTimeout ) );
		Duration	lastAccessTimeout	= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultLastAccessTimeout ) );
		set( key, value, timeout, lastAccessTimeout );
	}

	/**
	 * Set's multiple objects in the storage using all the same default timeout and last access timeouts
	 *
	 * @param entries The keys and cache entries to store
	 */
	public void set( IStruct entries ) {
		Duration	timeout				= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultTimeout ) );
		Duration	lastAccessTimeout	= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultLastAccessTimeout ) );

		entries.forEach( ( key, value ) -> this.set( key.getName(), value, timeout, lastAccessTimeout ) );
	}

	/**
	 * Set's multiple objects in the storage using all the same default timeout and last access timeouts
	 *
	 * @param entries           The keys and cache entries to store in the cache
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 */
	public void set( IStruct entries, Duration timeout, Duration lastAccessTimeout ) {
		entries.forEach( ( key, value ) -> this.set( key.getName(), value, timeout, lastAccessTimeout ) );
	}

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
	public Object getOrSet( String key, Supplier<Object> provider, Duration timeout, Duration lastAccessTimeout, IStruct metadata ) {
		// Get the object
		return this.get( key ).orElseGet( () -> {
			// Add a sync lock
			synchronized ( key.toLowerCase().intern() ) {
				// Check again
				return this.get( key ).orElseGet( () -> {
					// Get the value
					var value = provider.get();
					// Set the value
					this.set( key, value, timeout, lastAccessTimeout, metadata );
					// Return the value
					return value;
				} );
			}
		} );
	}

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
	public Object getOrSet( String key, Supplier<Object> provider, Duration timeout, Duration lastAccessTimeout ) {
		return this.getOrSet( key, provider, timeout, lastAccessTimeout, new Struct() );
	}

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
	public Object getOrSet( String key, Supplier<Object> provider, Duration timeout ) {
		Duration lastAccessTimeout = Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultLastAccessTimeout ) );
		return this.getOrSet( key, provider, timeout, lastAccessTimeout );
	}

	/**
	 * Tries to get an object from the cache, if not found, it will call the lambda to get the value and store it in the cache
	 * with the default timeout and last access timeout
	 *
	 * @param key      The key to retrieve
	 * @param provider The lambda to call if the key is not found
	 *
	 * @return The object
	 */
	public Object getOrSet( String key, Supplier<Object> provider ) {
		Duration	timeout				= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultTimeout ) );
		Duration	lastAccessTimeout	= Duration.ofSeconds( this.config.properties.getAsLong( Key.defaultLastAccessTimeout ) );
		return this.getOrSet( key, provider, timeout, lastAccessTimeout );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a referece to the CacheService Task Scheduler
	 *
	 * @return The task scheduler
	 */
	protected ExecutorRecord getTaskScheduler() {
		return this.cacheService.getTaskScheduler();
	}

	/**
	 * JVM Threshold checks
	 */
	protected boolean memoryThresholdCheck() {
		var threshold = this.config.properties.getAsInteger( Key.freeMemoryPercentageThreshold );

		// Is it enabled or not
		if ( threshold == 0 ) {
			return false;
		}

		var jvmThreshold = ( ( Runtime.getRuntime().freeMemory() / Runtime.getRuntime().maxMemory() ) * 100 );
		return ( threshold < jvmThreshold );
	}

	/**
	 * Announce an event in the Runtime
	 *
	 * @param event The event to announce
	 * @param data  The data to announce
	 */
	protected void announce( Key event, IStruct data ) {
		this.cacheService.announce( event, data );
	}

	/**
	 * Build out the object store according to the configuration
	 *
	 * @param config The configuration
	 *
	 * @return The object store created and configured
	 */
	protected static IObjectStore buildObjectStore( CacheConfig config ) {
		// Store Type
		Object thisStore = config.properties.getAsString( Key.objectStore );

		// Is this a store object already?
		if ( thisStore instanceof IObjectStore castedStore ) {
			return castedStore;
		}
		// else if it's a string it must be a core alias or a custom class path
		else if ( thisStore instanceof String castedStore ) {
			// Is it a core alias?
			if ( ObjectStoreType.isCore( Key.of( castedStore ) ) ) {
				// It's a core alias
				return ObjectStoreType
				    .getValueByKey( Key.of( castedStore ) )
				    .buildStore();
			} else {
				// It's a custom class path
				return buildObjectStoreByClass( castedStore );
			}
		} else {
			throw new BoxRuntimeException(
			    "The object store is not a valid type [" +
			        thisStore.getClass().getName() +
			        "] Valid types are: " + ObjectStoreType.values().toString()
			);
		}
	}

	/**
	 * Build the object store by class path
	 *
	 * @param storeClasspath The class path of the object store
	 *
	 * @return The object store
	 *
	 * @throws BoxRuntimeException If the object store cannot be loaded
	 * @throws BoxRuntimeException If the object store does not implement IObjectStore
	 * @throws BoxRuntimeException If the object store cannot be instantiated
	 */
	protected static IObjectStore buildObjectStoreByClass( String storeClasspath ) {
		try {
			// Load the class: TODO Change to JavaResolver later
			Class<?> clazz = Class.forName( storeClasspath );
			if ( IObjectStore.class.isAssignableFrom( clazz ) ) {
				// Create an instance of the class
				try {
					return ( IObjectStore ) clazz.getDeclaredConstructor().newInstance();
				} catch ( IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
					throw new BoxRuntimeException( "Cannot call the constructor on the object store: " + storeClasspath, e );
				}
			} else {
				throw new BoxRuntimeException( "The object does not implement IObjectStore: " + storeClasspath );
			}
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			// Log the error
			throw new BoxRuntimeException( "Unable to load the custom object store: " + storeClasspath, e );
		}
	}

}
