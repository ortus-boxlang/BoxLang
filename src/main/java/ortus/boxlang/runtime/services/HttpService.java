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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.Key;

/**
 * This service manages all HTTP clients in BoxLang
 */
public class HttpService extends BaseService {

	/**
	 * Concurrent map that stores all HTTP reusable clients
	 */
	private final ConcurrentMap<Key, BoxHttpClient>	clients						= new ConcurrentHashMap<>();

	/**
	 * Shutdown timeout in seconds
	 */
	private static final Long						SHUTDOWN_TIMEOUT_SECONDS	= 10L;

	/**
	 * The main HTTP logger
	 */
	private BoxLangLogger							logger;

	/**
	 * The HTTP Executor used by all clients
	 */
	private BoxExecutor								httpExecutor;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new HttpService with the given BoxRuntime.
	 *
	 * @param runtime The BoxRuntime instance.
	 */
	public HttpService( BoxRuntime runtime ) {
		super( runtime, Key.httpService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger			= runtime.getLoggingService().HTTP_LOGGER;
		this.httpExecutor	= runtime.getAsyncService().newVirtualExecutor( "httpClients" );
	}

	@Override
	public void onShutdown( Boolean force ) {
		if ( force ) {
			this.logger.info( "+ Http Service forced shutdown initiated" );
			this.httpExecutor.shutdownNow();
		} else {
			this.logger.info( "+ Http Service graceful shutdown initiated" );
			this.httpExecutor.shutdownAndAwaitTermination( SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS );
		}
		this.logger.info( "+ Http Service shutdown complete" );
	}

	@Override
	public void onStartup() {
		this.logger.info( "+ Http Service started" );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Helpers
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Get the HTTP Executor used by all clients
	 *
	 * @return The BoxExecutor instance
	 */
	public BoxExecutor getHttpExecutor() {
		return this.httpExecutor;
	}

	/**
	 * Get the HTTP Logger
	 *
	 * @return The BoxLangLogger instance
	 */
	public BoxLangLogger getLogger() {
		return this.logger;
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Client Management Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * How many HTTP clients are currently managed
	 */
	public int getClientCount() {
		return this.clients.size();
	}

	/**
	 * Verifies if a client with the given key exists
	 *
	 * @param key The client key
	 */
	public boolean hasClient( Key key ) {
		return this.clients.containsKey( key );
	}

	/**
	 * Retrieves the HTTP client associated with the given key
	 *
	 * @param key The client key
	 *
	 * @return The HttpClient instance, or null if not found
	 */
	public BoxHttpClient getClient( Key key ) {
		return this.clients.get( key );
	}

	/**
	 * Stores the given HTTP client with the associated key
	 *
	 * @param key    The client key
	 * @param client The HttpClient instance
	 */
	public BoxHttpClient putClient( Key key, BoxHttpClient client ) {
		return this.clients.put( key, client );
	}

	/**
	 * Remove the HTTP client associated with the given key
	 *
	 * @param key The client key
	 */
	public HttpService removeClient( Key key ) {
		this.clients.remove( key );
		return this;
	}

}
