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
package ortus.boxlang.runtime.net;

import java.net.http.HttpClient;
import java.time.Instant;

import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.services.HttpService;

/**
 * This class represents an HTTP client for making network requests.
 * It encapsulates the implementation library we use for HTTP operations.
 * It encapsulates configuration and functionality for sending HTTP requests
 * and handling responses.
 */
public class BoxHttpClient {

	/**
	 * ------------------------------------------------------------------------------
	 * Consttants
	 * ------------------------------------------------------------------------------
	 */

	public static final String	HTTP_1					= "HTTP/1.1";
	public static final String	HTTP_2					= "HTTP/2";

	/**
	 * Connection Timeout in seconds
	 */
	public static final int		CONNECT_TIMEOUT_SECONDS	= 15;

	/**
	 * ------------------------------------------------------------------------------
	 * Properties
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * The underlying HttpClient used for making HTTP requests.
	 */
	private final HttpClient	httpClient;

	/**
	 * The HttpService that manages this client.
	 */
	private final HttpService	httpService;

	/**
	 * The Logger used for logging HTTP operations.
	 */
	private final BoxLangLogger	logger;

	/**
	 * Tracks the last date + time the client was used.
	 * Multiple threads may access this, so it should be handled carefully.
	 */
	private volatile Instant	lastUsedTimestamp;

	/**
	 * ------------------------------------------------------------------------------
	 * Constructor
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Constructor to create a BoxHttpClient with the specified HttpClient.
	 *
	 * @param httpClient  The underlying HttpClient to be used for HTTP operations.
	 * @param httpService The HttpService managing this client.
	 */
	public BoxHttpClient( HttpClient httpClient, HttpService httpService ) {
		this.httpClient		= httpClient;
		this.httpService	= httpService;
		this.logger			= this.httpService.getLogger();
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Fluent Request Builder
	 * ------------------------------------------------------------------------------
	 */

}
