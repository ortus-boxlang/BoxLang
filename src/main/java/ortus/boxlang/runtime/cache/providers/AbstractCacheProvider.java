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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.cache.store.IObjectStore;
import ortus.boxlang.runtime.cache.store.ObjectStoreType;
import ortus.boxlang.runtime.cache.util.BoxCacheStats;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.InterceptorPool;
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
	 * Interceptor Local Pool
	 */
	protected InterceptorPool					interceptorPool;

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
	 * Get the interceptor pool for this cache
	 */
	public InterceptorPool getInterceptorPool() {
		return this.interceptorPool;
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
	 *
	 * @param name The name of the cache provider
	 *
	 * @return The cache provider
	 */
	public ICacheProvider setName( String name ) {
		return this.setName( Key.of( name ) );
	}

	/**
	 * Set the name of the cache provider
	 *
	 * @param name The name of the cache provider
	 *
	 * @return The cache provider
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
		this.cacheService		= cacheService;
		this.config				= config;
		this.name				= config.name;
		// Create the stats
		this.stats				= new BoxCacheStats();
		// Create local interceptor pool
		this.interceptorPool	= new InterceptorPool( this.name, cacheService.getRuntime() ).registerInterceptionPoint( BoxEvent.toArray() );
		return this;
	}

	/**
	 * Get an object from the store with metadata tracking using a CompletableFuture
	 *
	 * @param key The key to retrieve
	 *
	 * @return CompletableFuture of the value retrieved or null
	 */
	public CompletableFuture<Attempt<Object>> getAsync( String key ) {
		return CompletableFuture.supplyAsync( () -> get( key ), getTaskScheduler().executor() );
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
	 * Announce an event in the Runtime and the local pool with the provided {@link IStruct} of data.
	 *
	 * @param event The event to announce
	 * @param data  The data to announce
	 */
	protected void announce( Key event, IStruct data ) {
		this.interceptorPool.announce( event, data );
		this.cacheService.announce( event, data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( BoxEvent state, IStruct data ) {
		this.interceptorPool.announce( state.key(), data );
		this.cacheService.announce( state.key(), data );
	}

	/**
	 * Converts the seconds value to a duration.
	 *
	 * @param timeout The seconds to convert. This can be a duration, number or string representation of a number
	 *
	 * @return The duration of seconds according to the seconds passed
	 */
	public static Duration toDuration( Object timeout ) {
		if ( timeout instanceof Duration ) {
			return ( Duration ) timeout;
		}
		return Duration.ofSeconds( LongCaster.cast( timeout ) );
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
			        "] Valid types are: " + ObjectStoreType.values()
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
