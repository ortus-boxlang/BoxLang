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
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.scheduler.SchedulerStart;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.SessionPrefixFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.DateTimeHelper;
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
	 * The timestamp when the application was started
	 */
	private Instant							startTime;

	/**
	 * The duration of the application before it times out
	 */
	Duration								appDuration						= Duration.ZERO;

	/**
	 * The timestamp when the application was last accessed
	 */
	private Instant							lastAccessTime;

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
	 * The async service
	 */
	protected AsyncService					asyncService					= BoxRuntime.getInstance().getAsyncService();

	/**
	 * The Application service
	 */
	protected ApplicationService			applicationService				= BoxRuntime.getInstance().getApplicationService();

	/**
	 * Scheduler Service
	 */
	protected SchedulerService				schedulerService				= BoxRuntime.getInstance().getSchedulerService();

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
	 * The task which checks for application timeout. We keep a reference here so we can cancel on forced shutdown.
	 */
	private Future<?>						expiringFuture;

	/**
	 * Started schedulers
	 */
	private List<Key>						startedSchedulers				= new ArrayList<>();

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
		this.logger	= BoxRuntime.getInstance().getLoggingService().APPLICATION_LOGGER;
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

			// Get the app listener (Application.bx)
			this.startingListener = context.getRequestContext().getApplicationListener();
			// This will mark the context as a dependent thread so web runtimes will detach from the HTTP Exchange, but keep its values around
			// It's not ideal to store the starting listener and context for a long time, but it's the easiest way to fire onApplicationEnd() and onSessionEnd()
			// later on and ensure they have access to the same form, url, and CGI scopes that were present when the application was started.
			// TODO: the context when the application started isn't even nececarily the same as the context whe the session started, so even that isn't quite right.
			// Both app end and session end generally run outside of an HTTP request, so it's ambiguous what should really be available to them.
			this.startingListener.getRequestContext().registerDependentThread();
			// Startup the class loader
			startupClassLoaderPaths( context.getRequestContext() );
			// Startup the caches
			startupAppCaches( context.getRequestContext() );
			// Startup session storages
			startupSessionStorage( context.getApplicationContext() );

			// We need a way to kow if we're inside an application start to prevent starting the session too soon.
			// This happens if we update the application from inside of the onApplicationStart() method.
			context.getRequestContext().putAttachment( Key.onApplicationStart, true );
			try {
				// Announce it globally
				BoxRuntime.getInstance().getInterceptorService().announce(
				    Key.onApplicationStart,
				    Struct.of(
				        Key.application, this,
				        Key.listener, this.startingListener,
				        Key.context, context
				    )
				);

				// Announce it to the listener
				if ( startingListener != null ) {
					startingListener.onApplicationStart( context, new Object[] {} );
				} else {
					logger.debug( "No listener found for application [{}]", this.name );
				}
			} finally {
				context.getRequestContext().removeAttachment( Key.onApplicationStart );
			}

			// Startup the schedulers so the application can use them
			startupAppSchedulers( context.getRequestContext() );

			calculateAppDuration();

			// Start up expiry task
			startupExpiryTask();

			// Record startup
			this.startTime		= Instant.now();
			this.lastAccessTime	= this.startTime;
			this.started		= true;
		}

		logger.debug( "Application.start() - {}", this.name );
		return this;
	}

	/**
	 * Startup the application expiry task. If there is already an expiry task running, it will not start another.
	 */
	private void startupExpiryTask() {
		// App never expires, don't bother
		if ( appDuration.isZero() ) {
			return;
		}

		// Already running. Prevent doubling up threads.
		if ( this.expiringFuture != null && !this.expiringFuture.isDone() ) {
			return;
		}

		// wait for timeout minus last access instant
		Long secondsBeforeTimeout;
		// If app has not been accessed (still starting up), then use full duration
		if ( lastAccessTime == null ) {
			secondsBeforeTimeout = appDuration.toSeconds();
		} else {
			// Otherwise, only sleep the remaining time
			Duration timeSinceLastAccess = Duration.between( lastAccessTime, Instant.now() );
			secondsBeforeTimeout = appDuration.toSeconds() - timeSinceLastAccess.toSeconds();
		}

		// Use a virtual thread pool, so no platform threads will be harmed while our task is sleeping
		this.expiringFuture = asyncService.newExecutor( "application-timeouts", ExecutorType.VIRTUAL )
		    .submit( () -> {
			    try {
				    // At the exact moment of expiry...
				    Thread.sleep( secondsBeforeTimeout * 1000 );
				    // ... wake up and check if we have actually expired
				    checkExpiry();
			    } catch ( InterruptedException e ) {
				    // Task was cancelled
			    } finally {
				    // When the task is done for any reason, we clear it
				    this.expiringFuture = null;
			    }
		    } );
	}

	/**
	 * Check if the application has expired, and if so, shutdown.
	 * If not (because the last access time was updated), restart the expiry task to check again later.
	 */
	private void checkExpiry() {
		if ( isExpired() ) {
			logger.info( "Application [{}] has expired after [{}]. Shutting down.", getName(), appDuration );
			// Remove it, will be recreated on next access
			applicationService.removeApplication( getName() );
			// Shut it down. force=false means it won't try to cancel the expiry thread (which we're currenty running inside of!)
			shutdown( false );
		} else {
			// Restart the expiry task
			startupExpiryTask();
		}
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
	 * @param requestContext The request context
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
		// Get the schedulers from the application settings
		ArrayCaster
		    .attempt( requestContext.getConfigItems( Key.applicationSettings, Key.schedulers ) )
		    .ifPresent( appSchedulers -> {
			    for ( Object item : appSchedulers ) {
				    String schedulerPath	= StringCaster.cast( item );
				    String schedulerName	= this.name.getName() + ":" + schedulerPath;
				    Key	schedulerNameKey	= Key.of( schedulerName );

				    // If we don't have it registered by name, then register it
				    if ( !schedulerService.hasScheduler( schedulerNameKey ) ) {
					    SchedulerStart.startScheduler( requestContext, schedulerPath, schedulerName, false );
					    startedSchedulers.add( schedulerNameKey );
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

		// If we have an application defined cache with the same name, that will be used.
		if ( cacheService.hasCache( buildAppCacheKey( sessionCacheName ) ) ) {
			sessionCacheName = buildAppCacheKey( sessionCacheName );
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
			        .getQuiet( key )
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
		Object			sessionTimeout	= context.getConfigItems( Key.applicationSettings, Key.sessionTimeout );
		final Duration	timeoutDuration	= DateTimeHelper.convertTimeoutToDuration( sessionTimeout );
		String			cacheKey		= Session.buildCacheKey( ID, this.name );
		// Make sure our created duration is represented in the application metadata
		context.getRequestContext()
		    .getApplicationListener()
		    .getSettings()
		    .put( Key.sessionTimeout, timeoutDuration );

		// logger.debug( "**** getOrCreateSession {} Timeout {} ", ID, timeoutDuration );

		// Get or create the session
		Session targetSession = ( Session ) this.sessionsCache.getOrSet(
		    cacheKey,
		    () -> new Session( ID, this, timeoutDuration ),
		    timeoutDuration,
		    timeoutDuration
		);

		// Is the session still valid?
		if ( targetSession.isShutdown() || targetSession.isExpired() ) {
			// If not, remove it
			this.sessionsCache.clear( cacheKey );
			// And create a new one
			targetSession = new Session( ID, this, timeoutDuration );
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
	 * Get the start time of the application. This will be null if the application hasnt started, or is still in the process of starting.
	 *
	 * @return the application start time, or null if not started
	 */
	public Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * Get the last access time of the application.
	 *
	 * @return the application last access time
	 */
	public Instant getLastAccessTime() {
		return this.lastAccessTime;
	}

	/**
	 * Update the last access time of the application to the current time.
	 */
	public Application updateLastAccessTime() {
		this.lastAccessTime = Instant.now();
		return this;
	}

	/**
	 * Calculate the application duration from the application timeout setting
	 */
	private void calculateAppDuration() {
		Object appTimeout = this.startingListener.getSettings().get( Key.applicationTimeout );
		// Duration is the default, but if not, we will use the number as minutes
		// Which is what the cache providers expect
		if ( appTimeout instanceof Duration castedTimeout ) {
			this.appDuration = castedTimeout;
		} else {
			if ( appTimeout instanceof BigDecimal castDecimal ) {
				BigDecimal timeoutSeconds = castDecimal.multiply( BigDecimalCaster.cast( 60 ) );
				this.appDuration = Duration.ofSeconds( timeoutSeconds.longValue() );
			} else if ( appTimeout instanceof String && StringCaster.cast( appTimeout ).contains( "." ) ) {
				BigDecimal	castDecimal		= BigDecimalCaster.cast( appTimeout );
				BigDecimal	timeoutSeconds	= castDecimal.multiply( BigDecimalCaster.cast( 60 ) );
				this.appDuration = Duration.ofSeconds( timeoutSeconds.longValue() );
			} else {
				this.appDuration = Duration.ofMinutes( LongCaster.cast( appTimeout ) );
			}
		}
	}

	/**
	 * Has this application expired.
	 * We look at the application last access time and the application timeout to determine if it has expired
	 * Application last access time is updated every time this application is used on a new request.
	 *
	 * @return True if the application has expired, false otherwise
	 */
	public boolean isExpired() {
		// If the duration is zero, then it never expires
		if ( this.appDuration.isZero() ) {
			return false;
		}

		// App is still starting up
		if ( this.lastAccessTime == null ) {
			return false;
		}

		// If the start time + the duration is before now, then it's expired
		// Example: 10:00 + 1 hour = 11:00, now is 11:01, so it's expired : true
		// Example: 10:00 + 1 hour = 11:00, now is 10:59, so it's not expired : false
		return this.lastAccessTime.plus( this.appDuration ).isBefore( Instant.now() );
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
		BoxRuntime.getInstance().getInterceptorService().announce(
		    Key.onApplicationRestart,
		    () -> Struct.ofNonConcurrent(
		        Key.application, this,
		        Key.context, context
		    )
		);
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
		// If the app has already been shutdown, don't do it again
		if ( !hasStarted() ) {
			logger.debug( "Can't shutdown application [{}] as it's already shutdown", this.name );
			return;
		}

		// Cancel the expiry task if forced
		if ( force ) {
			if ( this.expiringFuture != null ) {
				this.expiringFuture.cancel( true );
			}

			// Shutdown all started schedulers
			startedSchedulers.forEach( schedulerName -> {
				// TODO: Allow the user to configure a timeout for graceful shutdown
				schedulerService.removeScheduler( schedulerName );
			} );
		}

		// Announce it globally
		RequestBoxContext requestContext = this.getStartingListener().getRequestContext();
		try {
			BoxRuntime.getInstance().getInterceptorService().announce(
			    Key.onApplicationEnd,
			    () -> Struct.ofNonConcurrent(
			        Key.application, this,
			        Key.context, requestContext
			    ) );
		} catch ( AbortException ae ) {
			throw ae;
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

		// Announce it to the listener
		if ( this.startingListener != null ) {
			try {
				// Any buffer output in this context will be discarded
				this.startingListener.onApplicationEnd(
				    requestContext,
				    new Object[] { applicationScope }
				);
			} catch ( AbortException ae ) {
				throw ae;
			} catch ( Exception e ) {
				logger.error( "Error calling onApplicationEnd", e );
			}
		}

		// Shutdown all class loaders
		this.classLoaders.values().forEach( t -> {
			try {
				t.close();
			} catch ( IOException e ) {
				logger.error( "Error closing class loader", e );
			}
		} );

		// Shut down any datasources associated with this application
		DatasourceService dataSourceService = BoxRuntime
		    .getInstance()
		    .getDataSourceService();

		// Converting the keyset to an array to avoid any concurrent modification issues
		for ( Key dsn : dataSourceService
		    .getByApplicationName( name )
		    .keySet()
		    .toArray( new Key[ 0 ] ) ) {
			dataSourceService.remove( dsn );
		}

		// Shutdown our application caches
		StructCaster.attempt( requestContext.getConfigItems( Key.applicationSettings, Key.caches ) )
		    .ifPresent( appCaches -> {
			    for ( Entry<Key, Object> entry : appCaches.entrySet() ) {
				    Key cacheName = buildAppCacheKey( entry.getKey() );
				    this.cacheService.shutdownCache( cacheName );
			    }
		    } );

		// Clear out the data
		this.started = false;
		this.classLoaders.clear();
		this.applicationScope	= null;
		this.startTime			= null;

		logger.debug( "Application.shutdown() - {}", this.name );
	}

}
