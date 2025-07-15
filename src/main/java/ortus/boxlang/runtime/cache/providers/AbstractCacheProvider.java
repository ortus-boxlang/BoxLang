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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * AbstractCacheProvider serves as the base implementation for all cache providers in the BoxLang runtime.
 * This abstract class provides common functionality and structure for cache providers including:
 *
 * <ul>
 * <li>Configuration management and initialization</li>
 * <li>Statistics tracking and reporting</li>
 * <li>Event announcement and interception capabilities</li>
 * <li>Object store creation and management</li>
 * <li>Asynchronous cache operations</li>
 * <li>Memory threshold monitoring</li>
 * <li>Timeout and duration handling utilities</li>
 * </ul>
 *
 * <p>
 * Cache providers extending this class must implement the specific cache operations
 * defined in the ICacheProvider interface while leveraging the common infrastructure
 * provided by this abstract implementation.
 * </p>
 *
 * <p>
 * The provider maintains an enabled state, configuration settings, statistics,
 * and an interceptor pool for handling cache-related events. It integrates with
 * the CacheService to provide a complete caching solution.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 * <li><strong>Thread-safe operations:</strong> Uses AtomicBoolean for state management</li>
 * <li><strong>Event-driven architecture:</strong> Supports cache event interception</li>
 * <li><strong>Flexible object stores:</strong> Supports both core and custom object store implementations</li>
 * <li><strong>Memory monitoring:</strong> Built-in JVM memory threshold checking</li>
 * <li><strong>Async support:</strong> CompletableFuture-based asynchronous operations</li>
 * </ul>
 *
 * @see ICacheProvider
 * @see CacheService
 * @see IObjectStore
 * @see ICacheStats
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.0.0
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
		var threshold = IntegerCaster.cast( this.config.properties.get( Key.freeMemoryPercentageThreshold ) );

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
	 * Converts an incoming timeout value to a duration object.
	 * If the timeout is already a duration, it will be returned as is.
	 *
	 * @param timeout The seconds to convert. This can be a duration, number or string representation of a number
	 *
	 * @return The duration of seconds according to the seconds passed, or 0 if the timeout is null or not a number
	 */
	public static Duration toDuration( Object timeout ) {
		return toDuration( timeout, Duration.ofSeconds( 0 ) );
	}

	/**
	 * Converts an incoming timeout value to a duration object.
	 * If the timeout is already a duration, it will be returned as is.
	 *
	 * @param timeout      The seconds to convert. This can be a duration, number or string representation of a number
	 * @param defaultValue The default value to use if the timeout is null or not a number
	 *
	 * @return The duration of seconds according to the seconds passed
	 */
	public static Duration toDuration( Object timeout, Duration defaultValue ) {
		if ( timeout instanceof Duration castedDuration ) {
			return castedDuration;
		}

		if ( timeout == null ) {
			return defaultValue;
		}

		if ( timeout instanceof String castedString && castedString.trim().isBlank() ) {
			return defaultValue;
		}

		// If it breaks here an exception will be thrown
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
		Object thisStore = StringCaster.cast( config.properties.get( Key.objectStore ) );

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
