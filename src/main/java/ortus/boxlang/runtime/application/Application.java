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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.PrefixFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	private Key						name;

	/**
	 * The timestamp when the runtime was started
	 */
	private Instant					startTime;

	/**
	 * Bit that determines if the application is running or not. Accesible by multiple threads
	 */
	private volatile boolean		started							= false;

	/**
	 * The scope for this application
	 */
	private ApplicationScope		applicationScope;

	/**
	 * The cache service helper
	 */
	protected CacheService			cacheService					= BoxRuntime.getInstance().getCacheService();

	/**
	 * The sessions for this application
	 */
	private ICacheProvider			sessionsCache;

	/**
	 * The listener that started this application (used for stopping it)
	 */
	private ApplicationListener		startingListener				= null;

	/**
	 * Logger
	 */
	private static final Logger		logger							= LoggerFactory.getLogger( Application.class );

	/**
	 * Application cache key filter
	 */
	private final ICacheKeyFilter	cacheFilter;

	/**
	 * Static strings for comparison
	 */
	private static final String		SESSION_STORAGE_MEMORY			= "memory";

	/**
	 * The applications' interceptor pool
	 */
	private InterceptorPool			interceptorPool;

	/**
	 * Default session cache properties
	 */
	private static final IStruct	defaultSessionCacheProperties	= Struct.of(
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
	private static final Key		DEFAULT_SESSION_CACHEKEY		= Key.boxlangSessions;

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
		this.name				= name;
		this.cacheFilter		= new PrefixFilter( this.name.getName() );
		this.applicationScope	= new ApplicationScope();
		// Startup the interceptor pool for this application
		this.interceptorPool	= new InterceptorPool( this.name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Announcement Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the interceptor pool for this application
	 *
	 * @return
	 */
	public InterceptorPool getInterceptorPool() {
		return this.interceptorPool;
	}

	/**
	 * Helper to Announce an event with the provided {@link IStruct} of data and the app context
	 *
	 * @param state   The state to announce
	 * @param data    The data to announce
	 * @param context The application context
	 */
	public void announce( BoxEvent state, IStruct data, IBoxContext appContext ) {
		announce( state.key(), data, appContext );
	}

	/**
	 * Helper to Announce an event with the provided {@link IStruct} of data and the app context
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data, IBoxContext appContext ) {
		getInterceptorPool().announce( state, data, appContext );
	}

	/**
	 * --------------------------------------------------------------------------
	 * App Methods
	 * --------------------------------------------------------------------------
	 */

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
			// Startup session storages
			startupSessionStorage();

			// Announce it globally
			BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationStart, Struct.of(
			    "application", this,
			    "listener", this.startingListener
			) );
			// Announce it locally
			getInterceptorPool().announce( Key.onApplicationStart, Struct.of(
			    "application", this,
			    "listener", this.startingListener
			), context );
			// Announce it to the listener
			if ( startingListener != null ) {
				startingListener.onApplicationStart( context, new Object[] {} );
			}
		}

		logger.debug( "Application.start() - {}", this.name );
		return this;
	}

	/**
	 * Startup the session storage
	 */
	private void startupSessionStorage() {
		// Startup session storages
		Object	storageDirective	= this.startingListener.getSettings().get( Key.sessionStorage );
		// If the session storage is a string, use it, otherwise default to memory
		String	sessionStorage		= storageDirective instanceof String castedString ? castedString : SESSION_STORAGE_MEMORY;

		// Get the cache name according to the storage directive or default to memory
		Key		sessionCacheName	= sessionStorage.equals( SESSION_STORAGE_MEMORY )
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
			        "Defined cache names are : " + Arrays.toString( this.cacheService.getRegisteredCaches() )
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
		String		entryKey		= this.name + Session.idConcatenator + ID;
		Duration	timeoutDuration	= null;
		Object		sessionTimeout	= startingListener.getSettings().get( Key.sessionTimeout );
		if ( sessionTimeout instanceof Duration ) {
			timeoutDuration = ( Duration ) sessionTimeout;
		} else {
			timeoutDuration = Duration
			    .ofMillis( LongCaster.cast( IntegerCaster.cast( startingListener.getSettings().get( Key.sessionTimeout ) ).longValue() * 8.64e+7 ) );
		}

		Optional<Object> session = sessionsCache.getOrSet(
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
		shutdown();
		start( context );
	}

	/**
	 * Shutdown this application
	 */
	public synchronized void shutdown() {
		// If the app has already been shutdown, don't do it again4
		if ( !hasStarted() ) {
			logger.debug( "Can't shutdown application [{}] as it's already shutdown", this.name );
			return;
		}

		// Announce it globally
		BoxRuntime.getInstance().getInterceptorService().announce( Key.onApplicationEnd, Struct.of(
		    "application", this
		) );
		// Announce it locally
		getInterceptorPool().announce( Key.onApplicationEnd, Struct.of(
		    "application", this
		), BoxRuntime.getInstance().getRuntimeContext() );

		// Shutdown all sessions
		if ( !BooleanCaster.cast( this.startingListener.getSettings().get( Key.sessionCluster ) ) ) {
			sessionsCache.getKeysStream( cacheFilter )
			    .parallel()
			    .map( Key::of )
			    .map( key -> getSession( key ) )
			    .forEach( Session::shutdown );
		}

		if ( this.startingListener != null ) {
			// Any buffer output in this context will be discarded
			this.startingListener.onApplicationEnd(
			    new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() ),
			    new Object[] { applicationScope }
			);
		}

		// Clear out the data
		this.sessionsCache.clearAll( cacheFilter );
		this.applicationScope	= null;
		this.startTime			= null;
		this.started			= false;
		this.interceptorPool	= null;

		logger.debug( "Application.shutdown() - {}", this.name );
	}

}
