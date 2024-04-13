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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.cache.providers.BoxCacheProvider;
import ortus.boxlang.runtime.cache.providers.CoreProviderType;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
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
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public static final Class<CoreProviderType>				CORE_TYPES	= CoreProviderType.class;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger								logger		= LoggerFactory.getLogger( CacheService.class );

	/**
	 * The async service
	 */
	private final AsyncService								asyncService;

	/**
	 * The interceptor service
	 */
	private final InterceptorService						interceptorService;

	/**
	 * The scheduled executor service record
	 */
	private final ExecutorRecord							executor;

	/**
	 * The caches registry
	 */
	private final Map<Key, ICacheProvider>					caches		= new ConcurrentHashMap<>();

	/**
	 * Registry of cache provider classes that you can register in BoxCache.
	 * These can be registered manually or via modules.
	 *
	 * The key is the unique provider name, and the value is the Class we will use to build out a new provider
	 */
	private final Map<Key, Class<? extends ICacheProvider>>	providers	= new ConcurrentHashMap<>();

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
		// Register the scheduled executor service
		this.executor			= this.asyncService.newScheduledExecutor( "cacheservice-tasks", 20 );
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
	 * This will create all the core caches and register them
	 */
	@Override
	public void onStartup() {
		BoxRuntime.timerUtil.start( "cacheservice-startup" );
		logger.atDebug().log( "+ Starting up Cache Service..." );

		// Create the default cache according to the configuration settings
		createDefaultCache( Key._DEFAULT, getRuntime().getConfiguration().runtime.defaultCache );

		// Create now all the registerd configured caches async
		this.runtime.getConfiguration().runtime.caches
		    .entrySet()
		    .parallelStream()
		    .forEach( entry -> {
			    CacheConfig config = ( CacheConfig ) entry.getValue();
			    createCache( entry.getKey(), config.provider, config.properties );
		    } );

		// Announce it
		announce(
		    BoxEvent.AFTER_CACHE_SERVICE_STARTUP,
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
		logger.atDebug().log( "+ Shutting down the Cache Service..." );

		// Announce it
		announce(
		    BoxEvent.BEFORE_CACHE_SERVICE_SHUTDOWN,
		    Struct.of( "cacheService", this )
		);

		// Shutdown all the caches asynchronously
		this.caches
		    .keySet()
		    .parallelStream()
		    .forEach( this::shutdownCache );

		// Announce it
		announce(
		    BoxEvent.AFTER_CACHE_SERVICE_SHUTDOWN,
		    Struct.of( "cacheService", this )
		);

		// Let it be known!
		logger.atDebug().log( "+ Cache Service shut down in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "cacheservice-shutdown" ) );
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
	public synchronized void shutdownCache( Key name ) {
		// Verify it exists or skip out
		if ( !this.caches.containsKey( name ) ) {
			return;
		}

		// Log it
		logger.atDebug().log( "Shutting down cache [{}]", name );

		// Get the cache
		var cache = this.caches.get( name );

		// Shutdown the cache
		try {

			// Announce it
			announce(
			    BoxEvent.BEFORE_CACHE_SHUTDOWN,
			    Struct.of( "cacheService", this, "cache", cache )
			);

			cache.shutdown();

			// Announce it
			announce(
			    BoxEvent.AFTER_CACHE_SHUTDOWN,
			    Struct.of( "cacheService", this, "cache", cache )
			);

			// Log it
			logger.atDebug().log( "Cache [{}] shut down succesfully", name );

		} catch ( Exception e ) {
			logger.atError().log( "Error shutting down cache [{}]: {}", name, e.getMessage() );
		}

		// Announce
		announce(
		    BoxEvent.BEFORE_CACHE_REMOVAL,
		    Struct.of( "cacheService", this, "cache", cache )
		);

		// Remove it from the registry
		this.caches.remove( name );

		// announce it
		announce(
		    BoxEvent.AFTER_CACHE_REMOVAL,
		    Struct.of( "cacheService", this, "cacheName", name )
		);
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
			    BoxEvent.BEFORE_CACHE_REPLACEMENT,
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
	public ICacheProvider registerCache( ICacheProvider provider ) {
		// Do we have one already with the name throw an exception
		if ( this.hasCache( provider.getName() ) ) {
			throw new BoxRuntimeException( "Cache [" + provider.getName() + "] already exists." );
		}

		// Register the new cache
		this.caches.put( provider.getName(), provider );

		// Announce
		announce(
		    BoxEvent.AFTER_CACHE_REGISTRATION,
		    Struct.of( "cacheService", this, "cache", provider )
		);

		return provider;
	}

	/**
	 * Create a new named default cache, register it and return it.
	 *
	 * @param name The name of the cache
	 *
	 * @return The created and registered cache
	 */
	public ICacheProvider createDefaultCache( Key name ) {
		return createDefaultCache( name, new CacheConfig() );
	}

	/**
	 * Create a new named default cache with a custom config, register it and return it.
	 *
	 * @param name   The name of the cache
	 * @param config The cache configuration
	 *
	 * @return The created and registered cache
	 */
	public ICacheProvider createDefaultCache( Key name, CacheConfig config ) {
		// If the name exists throw an exception
		if ( this.hasCache( name ) ) {
			throw new BoxRuntimeException( "Cache [" + name + "] already exists." );
		}

		// Create the cache
		var cache = new BoxCacheProvider()
		    .setName( name )
		    .configure( this, config );

		// Register it
		registerCache( cache );

		return cache;
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
	 * Create a new cache according to the name, provider and properties structure
	 *
	 * @param name       The name of the cache
	 * @param provider   A valid cache provider
	 * @param properties The properties to configure the cache
	 *
	 * @return The created and registered cache
	 */
	public ICacheProvider createCache( Key name, Key provider, IStruct properties ) {
		// Check if the name is available else throw an exception
		if ( hasCache( name ) ) {
			throw new BoxRuntimeException( "Cache [" + name + "] already exists." );
		}

		// Build the cache provider
		ICacheProvider cache = buildCacheProvider( provider )
		    .setName( name )
		    .configure( this, new CacheConfig( name, provider, properties ) );

		// Register it and return it
		return registerCache( cache );
	}

	/**
	 * Get an array of registered providers as strings
	 */
	public String[] getRegisteredProviders() {
		return this.providers.keySet()
		    .stream()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * Check if a provider exists
	 *
	 * @param provider The provider to check
	 */
	public boolean hasProvider( Key provider ) {
		return this.providers.containsKey( provider );
	}

	/**
	 * Get the requested provider by name or throw an exception if it does not exist
	 *
	 * @param provider The name of the provider
	 *
	 * @return The provider class
	 */
	public Class<? extends ICacheProvider> getProvider( Key provider ) {
		Class<? extends ICacheProvider> results = this.providers.get( provider );

		if ( results == null ) {
			throw new BoxRuntimeException(
			    "Cache Provider [" + provider + "] does not exist. Valid providers are: " + Arrays.toString( getRegisteredProviders() ) );
		}

		return results;
	}

	/**
	 * Register a new cache provider
	 * If the provider already exists, it will throw an exception
	 *
	 * @param name     The name of the provider
	 * @param provider The provider class
	 *
	 * @return The CacheService
	 */
	public CacheService registerProvider( Key name, Class<? extends ICacheProvider> provider ) {
		// If it exists throw an exception
		if ( this.providers.containsKey( name ) ) {
			throw new BoxRuntimeException( "Custom Provider [" + name + "] already exists." );
		}

		// Register it
		this.providers.put( name, provider );

		return this;
	}

	/**
	 * Remove a registered provider by name
	 * If the provider does not exist, it will skip out
	 *
	 * @param name The name of the provider
	 *
	 * @return True if the provider was removed, false otherwise
	 */
	public boolean removeProvider( Key name ) {
		return this.providers.remove( name ) != null;
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

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a core/custom provider
	 *
	 * @param provider The provider to build
	 *
	 * @return The built provider
	 */
	private ICacheProvider buildCacheProvider( Key provider ) {
		// Is this a core provider?
		if ( CoreProviderType.isCore( provider ) ) {
			return CoreProviderType.getValueByKey( provider ).buildProvider();
		}

		// Verify if we have this type of provider registered
		if ( hasProvider( provider ) ) {
			try {
				return getProvider( provider ).getDeclaredConstructor().newInstance();
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Error building cache provider [" + provider + "]: " + e.getMessage() );
			}
		}

		// Else throw an exception
		throw new BoxRuntimeException(
		    "No cache provider with the name [" + provider + "] is registered. Available providers are: " + Arrays.toString( getRegisteredProviders() )
		);
	}

}
