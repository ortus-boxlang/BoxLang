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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

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
	    "afterCacheElementInsert",
	    "afterCacheElementRemoved",
	    "afterCacheElementUpdated",
	    "afterCacheClearAll",
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
		logger.info( "CacheService.onShutdown()" );
	}

}
