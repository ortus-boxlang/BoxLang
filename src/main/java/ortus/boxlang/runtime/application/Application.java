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
package ortus.boxlang.runtime.application;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.SessionPrefixFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * I represent an Application in BoxLang
 */
public class Application {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The name of this application. Unique per runtime
	 */
	private Key								name;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant							startTime;

	/**
	 * Bit that determines if the application is running or not. Accesible by multiple threads
	 */
	private volatile boolean				started							= false;

	/**
	 * The scope for this application
	 */
	private ApplicationScope				applicationScope;

	/**
	 * The cache service helper
	 */
	protected CacheService					cacheService					= BoxRuntime.getInstance().getCacheService();

	/**
	 * The sessions for this application
	 */
	private ICacheProvider					sessionsCache;

	/**
	 * The listener that started this application (used for stopping it)
	 */
	private BaseApplicationListener			startingListener				= null;

	/**
	 * The application logger
	 */
	private BoxLangLogger					logger;

	/**
	 * Application cache key filter for it's sessions
	 */
	private ICacheKeyFilter					sessionCacheFilter;

	/**
	 * Static strings for comparison
	 */
	private static final String				SESSION_STORAGE_MEMORY			= "memory";

	/**
	 * Default session cache properties
	 */
	private static final IStruct			defaultSessionCacheProperties	= Struct.of(
	    Key.evictCount, 1,
	    Key.evictionPolicy, "LRU",
	    Key.freeMemoryPercentageThreshold, 0,
	    // Key.TOD, 2147483647 is the largest integer allowed by Java but the ConcurrentStore will allocate 2147483647/4 as the initial size of the Concurent
	    // map and will result in OOM errors
	    Key.maxObjects, 100000,
	    Key.defaultLastAccessTimeout, 3600,
	    Key.defaultTimeout, 3600,
	    Key.objectStore, "ConcurrentStore",
	    Key.reapFrequency, 120,
	    Key.resetTimeoutOnAccess, true,
	    Key.useLastAccessTimeouts, false
	);
	private static final Key				DEFAULT_SESSION_CACHEKEY		= Key.bxSessions;

	/**
	 * An application can have a collection of class loaders that it can track and manage.
	 * Each class loader is created according to the different javaSettings that could
	 * be defined in the application listener.
	 */
	private Map<String, DynamicClassLoader>	classLoaders					= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The name of the application
	 */
	public Application( Key name ) {
		this.logger	= BoxRuntime.getInstance().getLoggingService().getLogger( "application" );
		this.name	= name;
		prepApplication();
	}

	/**
	 * Called to prep an application before starting
	 * Used to encapsulate and to use it from the constructor and restarts
	 */
	private void prepApplication() {
		this.sessionCacheFilter	= new SessionPrefixFilter( this.name.getName() );
		// Create the application scope
		this.applicationScope	= new ApplicationScope();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Class Loader Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the application's class loaders
	 *
	 * @return The class loader map
	 */
	public Map<String, DynamicClassLoader> getClassLoaders() {
		return this.classLoaders;
	}

	/**
	 * Verify if the class loader exists by cache key
	 *
	 * @param loaderKey The key of the class loader
	 */
	public boolean hasClassLoader( String loaderKey ) {
		return this.classLoaders.containsKey( loaderKey );
	}

	/**
	 * Get a class loader by cache key
	 *
	 * @param loaderKey The key of the class loader
	 *
	 * @return The class loader
	 */
	public DynamicClassLoader getClassLoader( String loaderKey ) {
		return this.classLoaders.get( loaderKey );
	}

	/**
	 * Count how many class loaders we have loaded
	 */
	public long getClassLoaderCount() {
		return this.classLoaders.size();
	}

	/**
	 * Startup the class loader paths from the this.javaSettings.loadPaths
	 *
	 * @param requestContext The request context
	 */
	public void startupClassLoaderPaths( RequestBoxContext requestContext ) {
		URL[] loadPathsUrls = this.startingListener.getJavaSettingsLoadPaths( requestContext );

		// if we don't have any return out
		if ( loadPathsUrls.length == 0 ) {
			logger.trace( "===> Setting the context classLoader to the [runtime] loader during startupClassLoaderPaths via [{}]",
			    Thread.currentThread().getName() );
			// If there are no javasettings, ensure we just use the runtime CL
			Thread.currentThread().setContextClassLoader( BoxRuntime.getInstance().getRuntimeLoader() );
			return;
		}

		// Get or compute a class loader according to the incoming URIs for classes to load
		// Remember that Application.bx is instantiated per request, so each request could be different
		String loaderCacheKey = EncryptionUtil.hash( Arrays.toString( loadPathsUrls ) );
		this.classLoaders.computeIfAbsent( loaderCacheKey,
		    key -> {
			    logger.debug( "Application ClassLoader [{}] registered with these paths: [{}]", this.name, Arrays.toString( loadPathsUrls ) );
			    return new DynamicClassLoader( this.name, loadPathsUrls, BoxRuntime.getInstance().getRuntimeLoader(), false );
		    } );
		// Make sure our thread is using the right class loader
		logger.trace( "===> Setting the context classLoader to the [javasettings] loader during startupClassLoaderPaths via [{}]",
		    Thread.currentThread().getName() );
		Thread.currentThread().setContextClassLoader( this.classLoaders.get( loaderCacheKey ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * App Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get starting listener
	 *
	 * @return The starting listener
	 */
	public BaseApplicationListener getStartingListener() {
		return this.startingListener;
	}

	/**
	 * Start the application if not already started.
	 *
	 * The sequence of events is:
	 * 1. The application start times are recorded and the application is marked as started
	 * 2. The application listener is set
	 * 3. The class loader paths are set
	 * 4. The session storage is set
	 * 5. The application is announced to the interceptor service (onApplicationStart)
	 * 6. The application listener is called (onApplicationStart)
	 *
	 * @param context The context starting up the application
	 *
	 * @return The started application
	 */
	public Application start( IBoxContext context ) {
		// Apps started, just return
		if ( this.started ) {
			return this;
		}

		// Start the app
		synchronized ( this ) {
			// Double lock
			if ( this.started ) {
				return this;
			}

			// Record startup
			this.startTime			= Instant.now();
			this.started			= true;

			// Get the app listener (Application.bx)
			this.startingListener	= context.getRequestContext().getApplicationListener();
			// Startup the class loader
			startupClassLoaderPaths( context.getRequestContext() );
			// Startup the caches
			startupAppCaches( context.getRequestContext() );
			// Startup the schedulers
			startupAppSchedulers( context.getRequestContext() );
			// Startup session storages
			startupSessionStorage( context.getApplicationContext() );

			// Announce it globally
			BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationStart, Struct.of(
			    "application", this,
			    "listener", this.startingListener
			) );

			// Announce it to the listener
			if ( startingListener != null ) {
				startingListener.onApplicationStart( context, new Object[] {} );
			} else {
				logger.debug( "No listener found for application [{}]", this.name );
			}
		}

		logger.debug( "Application.start() - {}", this.name );
		return this;
	}

	/**
	 * Startup the application caches if any are defined in the settings of the Application.bx
	 *
	 * <pre>
	 * this.caches = {
	 *    myCache = {
	 * 	  	provider = "memory",
	 * 	  	properties = {
	 * 			// Cache properties
	 * 			maxObjects = 1000,
	 * 			// Default timeout
	 * 			defaultTimeout = 3600,
	 * 			// Default last access timeout
	 * 			defaultLastAccessTimeout = 3600,
	 * 			// Eviction policy
	 * 			evictionPolicy = "LRU"
	 * 	   }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param appContext The application context
	 */
	public void startupAppCaches( RequestBoxContext requestContext ) {
		StructCaster.attempt( requestContext.getConfigItems( Key.applicationSettings, Key.caches ) )
		    .ifPresent( appCaches -> {
			    for ( Entry<Key, Object> entry : appCaches.entrySet() ) {
				    Key	cacheName		= buildAppCacheKey( entry.getKey() );
				    IStruct cacheDefinition = StructCaster.cast( entry.getValue() );

				    // If the cacheDefinition doesn't have a provider, default to the default provider
				    cacheDefinition.computeIfAbsent( Key.provider, k -> Key.boxCacheProvider );
				    cacheDefinition.computeIfAbsent( Key.properties, k -> new Struct() );

				    // Register the caches with the cache service
				    this.cacheService.createCacheIfAbsent(
				        cacheName,
				        Key.of( cacheDefinition.get( Key.provider ) ),
				        StructCaster.cast( cacheDefinition.get( Key.properties ) )
				    );
			    }
		    } );
	}

	/**
	 * Startup the application schedulers if any are defined in the settings of the Application.bx
	 *
	 * <pre>
	 * this.schedulers = [ "path.to.Scheduler", "another.Scheduler" ]
	 * </pre>
	 *
	 * @param requestContext The request context
	 */
	public void startupAppSchedulers( RequestBoxContext requestContext ) {
		BIFDescriptor		schedulerStart		= BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.schedulerStart );
		SchedulerService	schedulerService	= BoxRuntime.getInstance().getSchedulerService();

		// Get the schedulers from the application settings
		ArrayCaster
		    .attempt( requestContext.getConfigItems( Key.applicationSettings, Key.schedulers ) )
		    .ifPresent( appSchedulers -> {
			    for ( Object scheduler : appSchedulers ) {
				    // Get the scheduler class name
				    String schedulerClassName = StringCaster.cast( scheduler );
				    Key	schedulerClassKey	= Key.of( schedulerClassName );
				    // If we don't have it registered by name, then register it
				    if ( !schedulerService.hasScheduler( schedulerClassKey ) ) {
					    schedulerStart.invoke(
					        requestContext,
					        new Object[] { schedulerClassName },
					        false,
					        Key.schedulerStart
					    );
				    }
			    }
		    } );
	}

	/**
	 * Build a cache key for the application
	 *
	 * @param cacheName The name of the cache
	 *
	 * @return The cache key for the application
	 */
	public Key buildAppCacheKey( Key cacheName ) {
		return Key.of( this.name.getName() + ":" + cacheName.getName() );
	}

	/**
	 * Startup the session storage
	 *
	 * @param appContext The application context
	 *
	 * @throws BoxRuntimeException If the session storage cache is not a string
	 */
	private void startupSessionStorage( ApplicationBoxContext appContext ) {
		// @formatter:off
		IStruct	settings = this.startingListener.getSettings();
		String	sessionStorageName = StringCaster
		    .attempt( settings.get( Key.sessionStorage ) )
			// If not a string, advice the user
		    .ifEmpty( () -> {
				throw new BoxRuntimeException( "Session storage directive must be a string that matches a registered cache" );
			} )
			// If present, make sure it has a value or default it
			.map( ( String setting ) -> setting.trim().isEmpty() ? SESSION_STORAGE_MEMORY : setting.trim() )
			// Return the right value or the default name
		    .getOrDefault( SESSION_STORAGE_MEMORY );
		// @formatter:on

		// Now we can get the right cache name to use
		Key		sessionCacheName	= sessionStorageName.equals( SESSION_STORAGE_MEMORY )
		    ? DEFAULT_SESSION_CACHEKEY
		    : Key.of( sessionStorageName );

		// Create the memory cache if not already created
		// This is a stop-gap, it should really never run. But if it does log it so we can fix it
		if ( sessionCacheName.equals( DEFAULT_SESSION_CACHEKEY ) && !cacheService.hasCache( DEFAULT_SESSION_CACHEKEY ) ) {

			logger.warn( "Creating default session memory cache  as it doesn't exist" );

			cacheService.createCache(
			    DEFAULT_SESSION_CACHEKEY,
			    Key.boxCacheProvider,
			    defaultSessionCacheProperties
			);
		}

		// If the cache doesn't exist by now, then throw an exception
		if ( !cacheService.hasCache( sessionCacheName ) ) {
			throw new BoxRuntimeException(
			    "Session storage cache not defined in the cache services or config [" + sessionCacheName + "]" +
			        "Defined cache names are : " + this.cacheService.getRegisteredCaches()
			);
		}

		// Now store it
		this.sessionsCache = this.cacheService.getCache( sessionCacheName );
		// Register the session cleanup interceptor for: BEFORE_CACHE_ELEMENT_REMOVED
		this.sessionsCache
		    .getInterceptorPool()
		    .register( data -> {
			    ICacheProvider targetCache = ( ICacheProvider ) data.get( "cache" );
			    String		key			= ( String ) data.get( "key" );

			    targetCache
			        .get( key )
			        .ifPresent( maybeSession -> {
				        if ( maybeSession instanceof Session castedSession ) {
					        logger.debug( "Session storage cache [{}] shutdown session [{}]", targetCache.getName(), key );
					        castedSession.shutdown( this.startingListener );
					        logger.debug( "Session storage cache [{}] shutdown and removed session [{}]", targetCache.getName(), key );
				        }
			        } );

			    return false;
		    }, BoxEvent.BEFORE_CACHE_ELEMENT_REMOVED.key() );
		logger.debug( "Session storage cache [{}] created for the application [{}]", sessionCacheName, this.name );
	}

	/**
	 * Get a session by ID for this application, creating if neccessary if not found
	 *
	 * @param ID      The ID of the session
	 * @param context The context of the request that is creating/getting the session
	 *
	 * @return The session object
	 */
	public Session getOrCreateSession( Key ID, RequestBoxContext context ) {
		Duration	timeoutDuration	= null;
		Object		sessionTimeout	= context.getConfigItems( Key.applicationSettings, Key.sessionTimeout );
		String		cacheKey		= Session.buildCacheKey( ID, this.name );

		// Duration is the default, but if not, we will use the number as seconds
		// Which is what the cache providers expect
		if ( sessionTimeout instanceof Duration castedTimeout ) {
			timeoutDuration = castedTimeout;
		} else {
			timeoutDuration = Duration.ofSeconds( LongCaster.cast( sessionTimeout ) );
		}
		// Dumb Java! It needs a final variable to use in the lambda
		final Duration	finalTimeoutDuration	= timeoutDuration;

		// logger.debug( "**** getOrCreateSession {} Timeout {} ", ID, timeoutDuration );

		// Get or create the session
		Session			targetSession			= ( Session ) this.sessionsCache.getOrSet(
		    cacheKey,
		    () -> new Session( ID, this, finalTimeoutDuration ),
		    timeoutDuration,
		    timeoutDuration
		);

		// Is the session still valid?
		if ( targetSession.isShutdown() || targetSession.isExpired() ) {
			// If not, remove it
			this.sessionsCache.clear( cacheKey );
			// And create a new one
			targetSession = new Session( ID, this, finalTimeoutDuration );
			this.sessionsCache.set( cacheKey, targetSession, timeoutDuration, timeoutDuration );
		}

		return targetSession;
	}

	/**
	 * How many sessions are currently tracked
	 */
	public long getSessionCount() {
		return hasStarted() ? this.sessionsCache.getKeysStream( sessionCacheFilter ).count() : 0;
	}

	/**
	 * Return the sessions cache object
	 *
	 * @return
	 */
	public ICacheProvider getSessionsCache() {
		return this.sessionsCache;
	}

	/**
	 * Get the scope for this application
	 *
	 * @return The scope
	 */
	public ApplicationScope getApplicationScope() {
		return this.applicationScope;
	}

	/**
	 * Get the name of this application
	 *
	 * @return The name
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * Get the start time of the application
	 *
	 * @return the application start time, or null if not started
	 */
	public Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * Has this application expired.
	 * We look at the application start time and the application timeout to determine if it has expired
	 *
	 * @return True if the application has expired, false otherwise
	 */
	public boolean isExpired() {
		Object		appTimeout	= this.startingListener.getSettings().get( Key.applicationTimeout );
		Duration	appDuration	= null;
		// Duration is the default, but if not, we will use the number as seconds
		// Which is what the cache providers expect
		if ( appTimeout instanceof Duration castedTimeout ) {
			appDuration = castedTimeout;
		} else {
			appDuration = Duration.ofMinutes( LongCaster.cast( appTimeout ) );
		}

		// If the duration is zero, then it never expires
		if ( appDuration.isZero() ) {
			return false;
		}

		// If the start time + the duration is before now, then it's expired
		// Example: 10:00 + 1 hour = 11:00, now is 11:01, so it's expired : true
		// Example: 10:00 + 1 hour = 11:00, now is 10:59, so it's not expired : false
		return this.startTime.plus( appDuration ).isBefore( Instant.now() );
	}

	/**
	 * Check if the application is running
	 *
	 * @return True if the application is running
	 */
	public boolean hasStarted() {
		return this.started;
	}

	/**
	 * Restart this application
	 */
	public synchronized void restart( IBoxContext context ) {
		// Announce it
		BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationRestart, Struct.of(
		    "application", this
		) );
		shutdown( true );
		// call the constructor
		prepApplication();
		// Start the application again
		start( context );
	}

	/**
	 * Shutdown this application
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	public synchronized void shutdown( boolean force ) {
		// If the app has already been shutdown, don't do it again4
		if ( !hasStarted() ) {
			logger.debug( "Can't shutdown application [{}] as it's already shutdown", this.name );
			return;
		}

		// Announce it globally
		RequestBoxContext requestContext = this.getStartingListener().getRequestContext();
		try {
			BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationEnd, Struct.of(
			    "application", this,
			    "context", requestContext
			) );
		} catch ( Exception e ) {
			logger.error( "Error announcing onApplicationEnd", e );
		}

		// Shutdown all sessions if NOT in a cluster
		if ( !BooleanCaster.cast( this.startingListener.getSettings().get( Key.sessionCluster ) ) ) {
			this.sessionsCache.getKeysStream( sessionCacheFilter )
			    .parallel()
			    .map( Key::of )
			    .map( sessionKey -> ( Session ) this.sessionsCache.get( sessionKey.getName() ).get() )
			    .forEach( session -> session.shutdown( this.getStartingListener() ) );
		}

		// Shutdown all class loaders
		this.classLoaders.values().forEach( t -> {
			try {
				t.close();
			} catch ( IOException e ) {
				logger.error( "Error closing class loader", e );
			}
		} );

		// Announce it to the listener
		if ( this.startingListener != null ) {
			try {
				// Any buffer output in this context will be discarded
				this.startingListener.onApplicationEnd(
				    requestContext,
				    new Object[] { applicationScope }
				);
			} catch ( Exception e ) {
				logger.error( "Error calling onApplicationEnd", e );
			}
		}

		// Clear out the data
		this.started = false;
		this.sessionsCache.clearAll( sessionCacheFilter );
		this.classLoaders.clear();
		this.applicationScope	= null;
		this.startTime			= null;

		logger.debug( "Application.shutdown() - {}", this.name );
	}

}
