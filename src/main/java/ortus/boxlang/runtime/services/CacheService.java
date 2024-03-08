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
package ortus.boxlang.runtime.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This is a service that provides caching functionality to BoxLang.
 * It is a core service and is started up when the runtime starts.
 * It consists on the ability to register/build cache providers
 * that can be used anywhere in BoxLang.
 */
public class CacheService extends BaseService {

	/**
	 * Service Events
	 */
	public static final Map<String, Key>	CACHE_EVENTS	= Stream.of(
	    // Cache Events
	    "afterCacheElementInsert",
	    "afterCacheElementRemoved",
	    "afterCacheElementUpdated",
	    "afterCacheClearAll",
	    // Factory Events
	    "afterCacheRegistration",
	    "afterCacheRemoval",
	    "beforeCacheRemoval",
	    "beforeCacheReplacement",
	    "afterCacheServiceConfiguration",
	    "beforeCacheServiceShutdown",
	    "afterCacheServiceShutdown",
	    "beforeCacheShutdown",
	    "afterCacheShutdown"
	).collect( Collectors.toMap(
	    eventName -> eventName,
	    Key::of
	) );

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger				logger			= LoggerFactory.getLogger( CacheService.class );

	/**
	 * The async service
	 */
	private final AsyncService				asyncService;

	/**
	 * The interceptor service
	 */
	private final InterceptorService		interceptorService;

	/**
	 * The scheduled executor service record
	 */
	private final ExecutorRecord			executor;

	/**
	 * The caches registry
	 */
	private final Map<Key, ICacheProvider>	caches			= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The runtime instance
	 */
	public CacheService( BoxRuntime runtime ) {
		super( runtime );
		this.asyncService		= runtime.getAsyncService();
		this.interceptorService	= runtime.getInterceptorService();
		// Add the service events
		this.interceptorService.registerInterceptionPoint( CACHE_EVENTS.values().toArray( Key[]::new ) );
		// Register the scheduled executor service
		this.executor = this.asyncService.newScheduledExecutor( "cacheservice-tasks", 20 );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the scheduled executor service assigned to all caching services
	 *
	 * @return The scheduled executor record
	 */
	public ExecutorRecord getTaskScheduler() {
		return this.executor;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		BoxRuntime.timerUtil.start( "cacheservice-startup" );
		logger.atInfo().log( "+ Starting up Cache Service..." );

		// Read the configuration from disk
		// this.config = this.runtime.getConfiguration().runtime.caches;

		// Register the core providers

		// Announce it
		announce(
		    CACHE_EVENTS.get( "afterCacheServiceConfiguration" ),
		    Struct.of( "cacheService", this )
		);

		// Let it be known!
		logger.atInfo().log( "+ Cache Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "cacheservice-startup" ) );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force True if the shutdown is forced
	 */
	@Override
	public void onShutdown( Boolean force ) {
		BoxRuntime.timerUtil.start( "cacheservice-shutdown" );
		logger.atInfo().log( "+ Shutting down the Cache Service..." );

		// Announce it
		announce(
		    CACHE_EVENTS.get( "beforeCacheServiceShutdown" ),
		    Struct.of( "cacheService", this )
		);

		// Shutdown all the caches asynchronously
		this.caches
		    .keySet()
		    .parallelStream()
		    .forEach( this::shutdownCache );

		// Announce it
		announce(
		    CACHE_EVENTS.get( "afterCacheServiceShutdown" ),
		    Struct.of( "cacheService", this )
		);

		// Let it be known!
		logger.atInfo().log( "+ Cache Service shut down in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "cacheservice-shutdown" ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Cache Service Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Shutdown a cache by name and remove it from the registry. If the cache does not exist, it will skip out.
	 *
	 * @param name The name of the cache
	 */
	public void shutdownCache( Key name ) {
		// Verify it exists or skip out
		if ( !this.caches.containsKey( name ) ) {
			return;
		}

		// Log it
		logger.atInfo().log( "Shutting down cache [{}]", name );

		// Get the cache
		var cache = this.caches.get( name );

		// Announce it
		announce(
		    CACHE_EVENTS.get( "beforeCacheShutdown" ),
		    Struct.of( "cacheService", this, "cache", cache )
		);

		// Shutdown the cache
		try {
			cache.shutdown();

			// Announce it
			announce(
			    CACHE_EVENTS.get( "afterCacheShutdown" ),
			    Struct.of( "cacheService", this, "cache", cache )
			);

			// Log it
			logger.atInfo().log( "Cache [{}] shut down succesfully", name );

		} catch ( Exception e ) {
			logger.atError().log( "Error shutting down cache [{}]: {}", name, e.getMessage() );
		}

		// Remove it from the registry
		this.caches.remove( name );
	}

	/**
	 * Get a reference to a registered cache provider.
	 * If the cache provider does not exist, it will throw an exception.
	 *
	 * @param name The name of the cache
	 *
	 * @throws BoxRuntimeException If the cache does not exist
	 *
	 * @return The cache provider
	 */
	public ICacheProvider getCache( Key name ) {
		var results = this.caches.get( name );

		if ( results == null ) {
			throw new BoxRuntimeException( "Cache [" + name + "] does not exist. Valid caches are: " + getRegisteredCaches() );
		}

		return results;
	}

	/**
	 * Get back the default cache
	 *
	 * @return The default cache
	 */
	public ICacheProvider getDefaultCache() {
		return this.getCache( Key._DEFAULT );
	}

	/**
	 * Verify if we have a cache registered with the given name
	 *
	 * @param name The name of the cache
	 *
	 * @return True if the cache is registered, false otherwise
	 */
	public boolean hasCache( Key name ) {
		return this.caches.containsKey( name );
	}

	/**
	 * Replace a registered cache with a new one of the same name that's already configured and ready to go.
	 *
	 * @param name        The name of the cache to replace
	 * @param newProvider The new cache provider
	 *
	 * @return The CacheService
	 */
	public CacheService replaceCache( Key name, ICacheProvider newProvider ) {
		// Shutdown the old cache if it exists
		if ( this.hasCache( name ) ) {
			// Announce
			announce(
			    CACHE_EVENTS.get( "beforeCacheReplacement" ),
			    Struct.of(
			        "cacheService", this,
			        "oldCache", this.getCache( name ),
			        "newCache", newProvider
			    )
			);
			// Shutdown the old cache
			this.shutdownCache( name );
		}

		// Register the new cache
		registerCache( newProvider );

		return this;
	}

	/**
	 * This registers an ICacheProvider with the CacheService and links it to the service.
	 * It will also announce the registration of the cache. It's your job to make sure the cache is ready to go.
	 *
	 * @param provider The cache provider to register
	 *
	 * @throws BoxRuntimeException If a cache with the same name already exists
	 */
	public void registerCache( ICacheProvider provider ) {
		// Do we have one already with the name throw an exception
		if ( this.hasCache( provider.getName() ) ) {
			throw new BoxRuntimeException( "Cache [" + provider.getName() + "] already exists." );
		}

		// Register the new cache
		this.caches.put( provider.getName(), provider );

		// Announce
		announce(
		    CACHE_EVENTS.get( "afterCacheRegistration" ),
		    Struct.of( "cacheService", this, "cache", provider )
		);
	}

	/**
	 * Get all the registered caches back as an array of strings
	 *
	 * @return The registered caches as an array of strings
	 */
	public String[] getRegisteredCaches() {
		return this.caches.keySet()
		    .stream()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * Create a new cache according to the provider and properties, register it and returns it.
	 *
	 * @param name       The name of the cache
	 * @param provider   A valid cache provider
	 * @param properties The properties to configure the cache
	 *
	 * @return
	 */
	public static ICacheProvider createCache( Key name, Key provider, Struct properties ) {
		return null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Global Cache operations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This removes all caches from the cache service and triggers a shutdown on each of them
	 * The CacheService will be left in a state where it has no caches registered.
	 */
	public void removeAllCaches() {
		// Do it async using a stream
		this.caches
		    .keySet()
		    .parallelStream()
		    .forEach( this::shutdownCache );
	}

	/**
	 * Clear all elements in all caches
	 */
	public void clearAllCaches() {
		// Do it async using a stream
		this.caches.values()
		    .parallelStream()
		    .forEach( ICacheProvider::clear );
	}

	/**
	 * Reap all elements in all caches
	 */
	public void reapAllCaches() {
		// Do it async using a stream
		this.caches.values()
		    .parallelStream()
		    .forEach( ICacheProvider::reap );
	}

}
