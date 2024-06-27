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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.PrefixFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
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
	 * Logger
	 */
	private static final Logger				logger							= LoggerFactory.getLogger( Application.class );

	/**
	 * Application cache key filter
	 */
	private ICacheKeyFilter					cacheFilter;

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
	    Key.useLastAccessTimeouts, true
	);
	private static final Key				DEFAULT_SESSION_CACHEKEY		= Key.boxlangSessions;

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
		this.name = name;
		prepApplication();
	}

	/**
	 * Called to prep an application before starting
	 * Used to encapsulate and to use it from the constructor and restarts
	 */
	private void prepApplication() {
		this.cacheFilter		= new PrefixFilter( this.name.getName() );
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
	 * @param appContext The application context
	 */
	public void startupClassLoaderPaths( ApplicationBoxContext appContext ) {
		URL[] loadPathsUrls = this.startingListener.getJavaSettingsLoadPaths( appContext );

		// if we don't have any return out
		if ( loadPathsUrls.length == 0 ) {
			return;
		}

		// Get or compute a class loader according to the incoming URIs for classes to load
		String loaderCacheKey = EncryptionUtil.hash( Arrays.toString( loadPathsUrls ) );
		this.classLoaders.computeIfAbsent( loaderCacheKey,
		    key -> {
			    logger.debug( "Application ClassLoader [{}] registered with these paths: [{}]", this.name, Arrays.toString( loadPathsUrls ) );
			    return new DynamicClassLoader( this.name, loadPathsUrls, BoxRuntime.getInstance().getRuntimeLoader() );
		    } );
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
	 * Start the application if not already started
	 *
	 * @param context The context
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
			this.startingListener	= context.getParentOfType( RequestBoxContext.class ).getApplicationListener();
			ApplicationBoxContext appContext = context.getParentOfType( ApplicationBoxContext.class );
			// Startup the class loader
			startupClassLoaderPaths( appContext );
			// Startup session storages
			startupSessionStorage( appContext );

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
	 * Startup the session storage
	 *
	 * @param appContext The application context
	 *
	 * @throws BoxRuntimeException If the session storage cache is not a string
	 */
	private void startupSessionStorage( ApplicationBoxContext appContext ) {
		IStruct				settings			= this.startingListener.getSettings();
		CastAttempt<String>	directiveAttempt	= StringCaster.attempt( settings.get( Key.sessionStorage ) );
		String				sessionStorage		= SESSION_STORAGE_MEMORY;

		// Let's be nice and tell them what they put is not good if not a string.
		if ( directiveAttempt.ifFailed() ) {
			throw new BoxRuntimeException( "Session storage directive must be a string that matches a registered cache" );
		} else {
			sessionStorage = directiveAttempt.get().trim();
		}

		// If empty, default it
		if ( sessionStorage.isEmpty() ) {
			sessionStorage = SESSION_STORAGE_MEMORY;
		}

		// Get the cache name according to the storage directive or default to memory
		Key sessionCacheName = sessionStorage.equals( SESSION_STORAGE_MEMORY )
		    ? DEFAULT_SESSION_CACHEKEY
		    : Key.of( sessionStorage );

		// Create the memory cache if not already created
		if ( sessionCacheName.equals( DEFAULT_SESSION_CACHEKEY ) && !cacheService.hasCache( DEFAULT_SESSION_CACHEKEY ) ) {

			logger.debug( "Creating default session memory cache as it doesn't exist" );

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
		logger.debug( "Session storage cache [{}] created for the application [{}]", sessionCacheName, this.name );
	}

	/**
	 * Get a session by ID for this application, creating if neccessary if not found
	 *
	 * @param ID The ID of the session
	 *
	 * @return The session
	 */
	public Session getSession( Key ID ) {
		String		entryKey		= this.name + Session.ID_CONCATENATOR + ID;
		Duration	timeoutDuration	= null;
		Object		sessionTimeout	= this.startingListener.getSettings().get( Key.sessionTimeout );

		if ( sessionTimeout instanceof Duration ) {
			timeoutDuration = ( Duration ) sessionTimeout;
		} else {
			timeoutDuration = Duration
			    .ofMillis( LongCaster.cast( IntegerCaster.cast( startingListener.getSettings().get( Key.sessionTimeout ) ).longValue() * 8.64e+7 ) );
		}

		Optional<Object> session = this.sessionsCache.getOrSet(
		    entryKey,
		    () -> new Session( ID, this ),
		    timeoutDuration,
		    timeoutDuration
		);

		return ( Session ) session.get();
	}

	/**
	 * How many sessions are currently tracked
	 */
	public long getSessionCount() {
		return hasStarted() ? sessionsCache.getKeysStream( cacheFilter ).count() : 0;
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
		BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationEnd, Struct.of(
		    "application", this
		) );

		// Shutdown all sessions
		if ( !BooleanCaster.cast( this.startingListener.getSettings().get( Key.sessionCluster ) ) ) {
			sessionsCache.getKeysStream( cacheFilter )
			    .parallel()
			    .map( Key::of )
			    .map( key -> getSession( key ) )
			    .forEach( Session::shutdown );
		}

		// shutdown all class loaders
		this.classLoaders.values().forEach( t -> {
			try {
				t.close();
			} catch ( IOException e ) {
				logger.error( "Error closing class loader", e );
			}
		} );

		if ( this.startingListener != null ) {
			// Any buffer output in this context will be discarded
			this.startingListener.onApplicationEnd(
			    new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() ),
			    new Object[] { applicationScope }
			);
		}

		// Clear out the data
		this.started = false;
		this.sessionsCache.clearAll( cacheFilter );
		this.classLoaders.clear();
		this.applicationScope	= null;
		this.startTime			= null;

		logger.debug( "Application.shutdown() - {}", this.name );
	}

}
