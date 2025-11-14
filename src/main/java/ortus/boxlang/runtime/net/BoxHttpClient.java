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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.util.ZipUtil;

/**
 * This class represents an HTTP client for making network requests.
 * It encapsulates the implementation library we use for HTTP operations.
 * It encapsulates configuration and functionality for sending HTTP requests
 * and handling responses.
 */
public class BoxHttpClient {

	/**
	 * ------------------------------------------------------------------------------
	 * Default Constants
	 * ------------------------------------------------------------------------------
	 */

	public static final String											HTTP_1						= "HTTP/1.1";
	public static final String											HTTP_2						= "HTTP/2";
	public static final String											DEFAULT_USER_AGENT			= "BoxLang-HttpClient/1.0";
	public static final String											DEFAULT_CHARSET				= StandardCharsets.UTF_8.name();
	public static final String											DEFAULT_METHOD				= "GET";
	public static final int												DEFAULT_CONNECTION_TIMEOUT	= 15;
	public static final int												DEFAULT_REQUEST_TIMEOUT		= 0;
	public static final boolean											DEFAULT_THROW_ON_ERROR		= true;

	// HTTP Status Codes
	public static final int												STATUS_REQUEST_TIMEOUT		= 408;
	public static final int												STATUS_INTERNAL_ERROR		= 500;
	public static final int												STATUS_BAD_GATEWAY			= 502;

	/**
	 * ------------------------------------------------------------------------------
	 * Static Services
	 * ------------------------------------------------------------------------------
	 */

	private static final BoxRuntime										runtime						= BoxRuntime.getInstance();
	private static final InterceptorService								interceptorService			= runtime.getInterceptorService();

	/**
	 * ------------------------------------------------------------------------------
	 * Client Properties
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * The underlying HttpClient used for making HTTP requests.
	 */
	private final HttpClient											httpClient;

	/**
	 * The HttpService that manages this client.
	 */
	private final HttpService											httpService;

	/**
	 * The Logger used for logging HTTP operations.
	 */
	private final BoxLangLogger											logger;

	/**
	 * Tracks the last date + time the client was used.
	 * Uses AtomicReference for thread-safe updates without synchronization.
	 */
	private final java.util.concurrent.atomic.AtomicReference<Instant>	lastUsedTimestamp			= new java.util.concurrent.atomic.AtomicReference<>( null );

	/**
	 * Statistics tracking for this client.
	 * Uses AtomicLong for thread-safe updates without synchronization.
	 */
	private final java.util.concurrent.atomic.AtomicLong				totalRequests				= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				successfulRequests			= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				failedRequests				= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				timeoutFailures				= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				connectionFailures			= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				tlsFailures					= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				httpProtocolFailures		= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				bytesReceived				= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				bytesSent					= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				totalExecutionTimeMs		= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final java.util.concurrent.atomic.AtomicLong				minExecutionTimeMs			= new java.util.concurrent.atomic.AtomicLong(
	    Long.MAX_VALUE );
	private final java.util.concurrent.atomic.AtomicLong				maxExecutionTimeMs			= new java.util.concurrent.atomic.AtomicLong( 0 );
	private final Instant												createdAt					= Instant.now();

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
	 * New Request Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Create a new fluent HTTP request builder.
	 *
	 * @param url     The URL to send the request to
	 * @param context The BoxLang context for the request
	 * @param debug   Whether debug mode is enabled
	 *
	 * @return A new HttpRequestBuilder instance
	 */
	public BoxHttpRequest newRequest( String url, ortus.boxlang.runtime.context.IBoxContext context, boolean debug ) {
		return new BoxHttpRequest( url, context, debug );
	}

	/**
	 * Create a new fluent HTTP request builder with debug mode disabled.
	 *
	 * @param url     The URL to send the request to
	 * @param context The BoxLang context for the request
	 *
	 * @return A new HttpRequestBuilder instance
	 */
	public BoxHttpRequest newRequest( String url, ortus.boxlang.runtime.context.IBoxContext context ) {
		return new BoxHttpRequest( url, context, false );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Client Getters
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Get the underlying HttpClient.
	 *
	 * @return The HttpClient
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Get the HttpService managing this client.
	 *
	 * @return The HttpService
	 */
	public HttpService getHttpService() {
		return this.httpService;
	}

	/**
	 * Get the logger for this client.
	 *
	 * @return The BoxLangLogger
	 */
	public BoxLangLogger getLogger() {
		return this.logger;
	}

	/**
	 * Get the last used timestamp.
	 *
	 * @return The Instant when this client was last used, or null if never used
	 */
	public Instant getLastUsedTimestamp() {
		return this.lastUsedTimestamp.get();
	}

	/**
	 * Update the last used timestamp to now.
	 */
	public void updateLastUsedTimestamp() {
		this.lastUsedTimestamp.set( Instant.now() );
	}

	/**
	 * Get statistics for this HTTP client.
	 *
	 * @return A Struct containing usage statistics
	 */
	public IStruct getStatistics() {
		long minTime = this.minExecutionTimeMs.get();
		return Struct.ofNonConcurrent(
		    Key.of( "totalRequests" ), this.totalRequests.get(),
		    Key.of( "successfulRequests" ), this.successfulRequests.get(),
		    Key.of( "failedRequests" ), this.failedRequests.get(),
		    Key.of( "timeoutFailures" ), this.timeoutFailures.get(),
		    Key.of( "connectionFailures" ), this.connectionFailures.get(),
		    Key.of( "tlsFailures" ), this.tlsFailures.get(),
		    Key.of( "httpProtocolFailures" ), this.httpProtocolFailures.get(),
		    Key.of( "bytesReceived" ), this.bytesReceived.get(),
		    Key.of( "bytesSent" ), this.bytesSent.get(),
		    Key.of( "totalExecutionTimeMs" ), this.totalExecutionTimeMs.get(),
		    Key.of( "minExecutionTimeMs" ), minTime == Long.MAX_VALUE ? 0 : minTime,
		    Key.of( "maxExecutionTimeMs" ), this.maxExecutionTimeMs.get(),
		    Key.of( "averageExecutionTimeMs" ),
		    this.totalRequests.get() > 0 ? this.totalExecutionTimeMs.get() / this.totalRequests.get() : 0,
		    Key.of( "createdAt" ), this.createdAt,
		    Key.of( "lastUsedTimestamp" ), this.lastUsedTimestamp.get()
		);
	}

	/**
	 * Get the total number of requests made by this client.
	 *
	 * @return The total request count
	 */
	public long getTotalRequests() {
		return this.totalRequests.get();
	}

	/**
	 * Get the number of successful requests.
	 *
	 * @return The successful request count
	 */
	public long getSuccessfulRequests() {
		return this.successfulRequests.get();
	}

	/**
	 * Get the number of failed requests.
	 *
	 * @return The failed request count
	 */
	public long getFailedRequests() {
		return this.failedRequests.get();
	}

	/**
	 * Get the number of timeout failures.
	 *
	 * @return The timeout failure count
	 */
	public long getTimeoutFailures() {
		return this.timeoutFailures.get();
	}

	/**
	 * Get the number of connection failures.
	 *
	 * @return The connection failure count
	 */
	public long getConnectionFailures() {
		return this.connectionFailures.get();
	}

	/**
	 * Get the number of TLS/SSL failures.
	 *
	 * @return The TLS failure count
	 */
	public long getTlsFailures() {
		return this.tlsFailures.get();
	}

	/**
	 * Get the number of HTTP protocol failures.
	 *
	 * @return The HTTP protocol failure count
	 */
	public long getHttpProtocolFailures() {
		return this.httpProtocolFailures.get();
	}

	/**
	 * Get the total bytes received by this client.
	 *
	 * @return The total bytes received
	 */
	public long getBytesReceived() {
		return this.bytesReceived.get();
	}

	/**
	 * Get the total bytes sent by this client.
	 *
	 * @return The total bytes sent
	 */
	public long getBytesSent() {
		return this.bytesSent.get();
	}

	/**
	 * Get the total execution time for all requests.
	 *
	 * @return The total execution time in milliseconds
	 */
	public long getTotalExecutionTimeMs() {
		return this.totalExecutionTimeMs.get();
	}

	/**
	 * Get the average execution time per request.
	 *
	 * @return The average execution time in milliseconds, or 0 if no requests have been made
	 */
	public long getAverageExecutionTimeMs() {
		long total = this.totalRequests.get();
		return total > 0 ? this.totalExecutionTimeMs.get() / total : 0;
	}

	/**
	 * Get the minimum execution time for a single request.
	 *
	 * @return The minimum execution time in milliseconds, or 0 if no requests have been made
	 */
	public long getMinExecutionTimeMs() {
		long min = this.minExecutionTimeMs.get();
		return min == Long.MAX_VALUE ? 0 : min;
	}

	/**
	 * Get the maximum execution time for a single request.
	 *
	 * @return The maximum execution time in milliseconds
	 */
	public long getMaxExecutionTimeMs() {
		return this.maxExecutionTimeMs.get();
	}

	/**
	 * Get the timestamp when this HTTP client was created.
	 *
	 * @return The creation timestamp
	 */
	public Instant getCreatedAt() {
		return this.createdAt;
	}

	/**
	 * Reset all statistics for this client.
	 * Useful for testing or periodic resets.
	 * Note: createdAt timestamp is NOT reset.
	 */
	public void resetStatistics() {
		this.totalRequests.set( 0 );
		this.successfulRequests.set( 0 );
		this.failedRequests.set( 0 );
		this.timeoutFailures.set( 0 );
		this.connectionFailures.set( 0 );
		this.tlsFailures.set( 0 );
		this.httpProtocolFailures.set( 0 );
		this.bytesReceived.set( 0 );
		this.bytesSent.set( 0 );
		this.totalExecutionTimeMs.set( 0 );
		this.minExecutionTimeMs.set( Long.MAX_VALUE );
		this.maxExecutionTimeMs.set( 0 );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Inner Classes: Fluent Request Builder
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Fluent HTTP request builder for creating and executing HTTP requests.
	 * Provides a rich DSL for configuring all aspects of an HTTP request.
	 */
	public class BoxHttpRequest {

		// The target URL
		private String										url;
		// The port number (if specified)
		private Integer										port;
		// The BoxLang context
		private ortus.boxlang.runtime.context.IBoxContext	context;
		// Unique request ID
		private final String								requestID			= java.util.UUID.randomUUID().toString();
		// Request start time, set by the invokers
		private DateTime									startTime;

		// If debug mode is enabled: not used right now, maybe later we do something with it.
		private boolean										debug				= false;
		// Http version
		private String										httpVersion			= HTTP_2;
		// The HTTP method
		private String										method				= DEFAULT_METHOD;
		// Request timeout in seconds
		private Integer										timeout				= DEFAULT_REQUEST_TIMEOUT;
		// The User-Agent header
		private String										userAgent			= DEFAULT_USER_AGENT;
		// The Charset to use
		private String										charset				= DEFAULT_CHARSET;
		// Whether to throw on error status codes
		private boolean										throwOnError		= DEFAULT_THROW_ON_ERROR;
		// Binary Markers
		private boolean										isBinaryRequest		= false;
		private boolean										isBinaryNever		= false;
		// Parameters and body
		private ortus.boxlang.runtime.types.Array			params				= new ortus.boxlang.runtime.types.Array();
		// Multi part mode
		private boolean										multipart			= false;

		// Basic Authentication
		private String										username;
		private String										password;
		// Only basic for now, maybe in the future NTLM, but that's legacy, kept here just in case, but never used
		private String										authType			= "BASIC";

		// Callbacks
		private ortus.boxlang.runtime.types.Function		onRequestStartCallback;
		private ortus.boxlang.runtime.types.Function		onChunkCallback;
		private ortus.boxlang.runtime.types.Function		onErrorCallback;
		private ortus.boxlang.runtime.types.Function		onCompleteCallback;

		// File handling
		private String										outputDirectory;
		private String										outputFile;

		// Results
		private IStruct										httpResult			= new Struct( false );
		// Internal HttpRequest being built
		private HttpRequest									targetHttpRequest;
		// Error Marker
		private boolean										error				= false;
		private String										errorMessage		= null;
		private Throwable									requestException	= null;

		/**
		 * ------------------------------------------------------------------------------
		 * Constructors
		 * ------------------------------------------------------------------------------
		 */

		/**
		 * Constructor
		 *
		 * @param url     The URL to send the request to
		 * @param context The BoxLang context
		 * @param debug   Whether debug mode is enabled
		 */
		public BoxHttpRequest(
		    String url, ortus.boxlang.runtime.context.IBoxContext context ) {
			this( url, context, false );
		}

		/**
		 * Constructor
		 *
		 * @param url     The URL to send the request to
		 * @param context The BoxLang context
		 * @param debug   Whether debug mode is enabled
		 */
		public BoxHttpRequest( String url, ortus.boxlang.runtime.context.IBoxContext context, boolean debug ) {
			this.url		= url;
			this.context	= context;
			this.debug		= debug;
		}

		/**
		 * ------------------------------------------------------------------------------
		 * Getters
		 * ------------------------------------------------------------------------------
		 */

		/**
		 * Get the HTTP Result Struct
		 */
		public IStruct getHttpResult() {
			return this.httpResult;
		}

		/**
		 * Has error
		 *
		 * @return true if there was an error, false otherwise
		 */
		public boolean hasError() {
			return this.error;
		}

		/**
		 * Was successful
		 *
		 * @return true if the request was successful, false otherwise
		 */
		public boolean wasSuccessful() {
			return !this.error;
		}

		/**
		 * Get the exception that occurred during the request, if any
		 *
		 * @return The exception that occurred, or null if none
		 */
		public Throwable getException() {
			return this.requestException;
		}

		/**
		 * Get the error message, if any
		 */
		public String getErrorMessage() {
			return this.errorMessage;
		}

		/**
		 * --------------------------------------------------------------------------
		 * Fluent DSL: Configuration Methods
		 * --------------------------------------------------------------------------
		 */

		/**
		 * Set the URL
		 *
		 * @param url The URL to send the request to
		 */
		public BoxHttpRequest url( String url ) {
			this.url = url;
			return this;
		}

		/**
		 * Set the context
		 *
		 * @param context The BoxLang context
		 */
		public BoxHttpRequest context( ortus.boxlang.runtime.context.IBoxContext context ) {
			this.context = context;
			return this;
		}

		/**
		 * Set the output directory for file downloads
		 *
		 * @param outputDirectory The output directory path
		 */
		public BoxHttpRequest outputDirectory( String outputDirectory ) {
			this.outputDirectory = outputDirectory;
			return this;
		}

		/**
		 * Set debug mode
		 *
		 * @param debug Whether debug mode is enabled
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest debug( boolean debug ) {
			this.debug = debug;
			return this;
		}

		/**
		 * Set whether the request is binary
		 */
		public BoxHttpRequest asBinary() {
			this.isBinaryRequest = true;
			return this;
		}

		/**
		 * Set binary never
		 */
		public BoxHttpRequest asBinaryNever() {
			this.isBinaryNever = true;
			return this;
		}

		/**
		 * Set HTTP/1.1 as the HTTP version
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest http1() {
			this.httpVersion = HTTP_1;
			return this;
		}

		/**
		 * Set HTTP/2 as the HTTP version
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest http2() {
			this.httpVersion = HTTP_2;
			return this;
		}

		/**
		 * Set the HTTP version (HTTP/1.1 or HTTP/2)
		 *
		 * @param version The HTTP version to use, null defaults to HTTP/2
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest httpVersion( String version ) {
			if ( version == null ) {
				version = HTTP_2;
			}
			this.httpVersion = version;
			return this;
		}

		/**
		 * Multipart mode
		 *
		 * @param multipart Whether the request is multipart
		 *
		 * @return
		 */
		public BoxHttpRequest multipart( boolean multipart ) {
			this.multipart = multipart;
			return this;
		}

		/**
		 * Set the HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE)
		 *
		 * @param method The HTTP method to use
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest method( String method ) {
			this.method = method.toUpperCase();
			return this;
		}

		/**
		 * Set the request timeout in seconds
		 *
		 * @param timeout The timeout in seconds
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest timeout( Integer timeout ) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Set the port number for the request
		 *
		 * @param port The port number
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest port( Integer port ) {
			this.port = port;
			return this;
		}

		/**
		 * Set the User-Agent header
		 *
		 * @param userAgent The user agent string
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest userAgent( String userAgent ) {
			this.userAgent = userAgent;
			return this;
		}

		/**
		 * Set the charset for the request
		 *
		 * @param charset The charset to use
		 *
		 * @return This builder for chaining
		 *
		 * @throws BoxRuntimeException if the charset is not supported
		 */
		public BoxHttpRequest charset( String charset ) {
			// Validate charset is supported
			if ( charset != null && !charset.isEmpty() ) {
				try {
					Charset.forName( charset );
				} catch ( Exception e ) {
					throw new BoxRuntimeException( "Invalid or unsupported charset: " + charset, e );
				}
			}
			this.charset = charset;
			return this;
		}

		/**
		 * Set whether to throw an error on HTTP error status codes (4xx, 5xx)
		 *
		 * @param throwOnError Whether to throw on error
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest throwOnError( boolean throwOnError ) {
			this.throwOnError = throwOnError;
			return this;
		}

		/**
		 * Set the params for the request
		 *
		 * @param params The params to set
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest params( ortus.boxlang.runtime.types.Array params ) {
			this.params = params;
			return this;
		}

		/**
		 * Add a header to the request
		 *
		 * @param name  The header name
		 * @param value The header value
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest header( String name, String value ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "header" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a URL parameter (query string parameter)
		 *
		 * @param name  The parameter name
		 * @param value The parameter value
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest urlParam( String name, String value ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "url" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			param.put( ortus.boxlang.runtime.scopes.Key.encoded, true );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a URL parameter with explicit encoding control
		 *
		 * @param name    The parameter name
		 * @param value   The parameter value
		 * @param encoded Whether the value should be URL encoded
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest urlParam( String name, String value, boolean encoded ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "url" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			param.put( ortus.boxlang.runtime.scopes.Key.encoded, encoded );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a form field parameter
		 *
		 * @param name  The field name
		 * @param value The field value
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest formField( String name, String value ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "formfield" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a form field parameter with explicit encoding control
		 *
		 * @param name    The field name
		 * @param value   The field value
		 * @param encoded Whether the value should be URL encoded
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest formField( String name, String value, boolean encoded ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "formfield" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			param.put( ortus.boxlang.runtime.scopes.Key.encoded, encoded );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a cookie to the request
		 *
		 * @param name  The cookie name
		 * @param value The cookie value
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest cookie( String name, String value ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "cookie" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.value, value );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a file to upload
		 *
		 * @param name     The field name
		 * @param filePath The path to the file to upload
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest file( String name, String filePath ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "file" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.file, filePath );
			this.params.add( param );
			return this;
		}

		/**
		 * Add a file to upload with explicit MIME type
		 *
		 * @param name     The field name
		 * @param filePath The path to the file to upload
		 * @param mimeType The MIME type of the file
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest file( String name, String filePath, String mimeType ) {
			ortus.boxlang.runtime.types.IStruct param = new ortus.boxlang.runtime.types.Struct();
			param.put( ortus.boxlang.runtime.scopes.Key.type, "file" );
			param.put( ortus.boxlang.runtime.scopes.Key._NAME, name );
			param.put( ortus.boxlang.runtime.scopes.Key.file, filePath );
			param.put( ortus.boxlang.runtime.scopes.Key.mimetype, mimeType );
			this.params.add( param );
			return this;
		}

		/**
		 * Configure Basic Authentication
		 *
		 * @param username The username
		 * @param password The password
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest withBasicAuth( String username, String password ) {
			this.username	= username;
			this.password	= password;
			this.authType	= "BASIC";
			return this;
		}

		/**
		 * Set the authentication type
		 *
		 * @param authType The authentication type (BASIC, NTLM)
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest authType( String authType ) {
			this.authType = authType.toUpperCase();
			return this;
		}

		/**
		 * Set the callback function for request start (called before request is sent)
		 *
		 * @param callback The request start callback function
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest onRequestStart( ortus.boxlang.runtime.types.Function callback ) {
			this.onRequestStartCallback = callback;
			return this;
		}

		/**
		 * Set the callback function for streaming chunk processing
		 *
		 * @param callback The chunk callback function
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest onChunk( ortus.boxlang.runtime.types.Function callback ) {
			this.onChunkCallback = callback;
			return this;
		}

		/**
		 * Set the callback function for error handling
		 *
		 * @param callback The error callback function
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest onError( ortus.boxlang.runtime.types.Function callback ) {
			this.onErrorCallback = callback;
			return this;
		}

		/**
		 * Set the callback function for request completion
		 *
		 * @param callback The completion callback function
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest onComplete( ortus.boxlang.runtime.types.Function callback ) {
			this.onCompleteCallback = callback;
			return this;
		}

		/**
		 * Set the output file for saving the response
		 *
		 * @param file The file name or path
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest outputFile( String file ) {
			this.outputFile = file;
			return this;
		}

		/**
		 * Conditional execution - execute the lambda only if the condition is true
		 *
		 * @param condition The condition to check
		 * @param action    The action to execute if condition is true
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest when( boolean condition, java.util.function.Consumer<BoxHttpRequest> action ) {
			if ( condition ) {
				action.accept( this );
			}
			return this;
		}

		/**
		 * Conditional execution - execute the lambda only if the value is null
		 *
		 * @param value  The value to check
		 * @param action The action to execute if value is null
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest ifNull( Object value, java.util.function.Consumer<BoxHttpRequest> action ) {
			if ( value == null ) {
				action.accept( this );
			}
			return this;
		}

		/**
		 * Conditional execution - execute the lambda only if the value is not null
		 *
		 * @param value  The value to check
		 * @param action The action to execute if value is not null
		 *
		 * @return This builder for chaining
		 */
		public BoxHttpRequest ifNotNull( Object value, java.util.function.Consumer<BoxHttpRequest> action ) {
			if ( value != null ) {
				action.accept( this );
			}
			return this;
		}

		/**
		 * ------------------------------------------------------------------------------
		 * Processors
		 * ------------------------------------------------------------------------------
		 */

		/**
		 * Process form fields for the request
		 *
		 * @param requestBuilder The HttpRequest.Builder to modify
		 * @param bodyPublisher  The body publisher (if any)
		 * @param formFields     The list of form fields
		 *
		 * @return The body publisher with form data
		 *
		 * @throws BoxRuntimeException if an error occurs during processing
		 */
		private HttpRequest.BodyPublisher processFormFields(
		    HttpRequest.Builder requestBuilder,
		    HttpRequest.BodyPublisher bodyPublisher,
		    List<IStruct> formFields ) {
			// Cannot have an existing body when using form fields
			if ( bodyPublisher != null ) {
				throw new BoxRuntimeException( "Cannot use a formfield httpparam with an existing http body: " + bodyPublisher.toString() );
			}

			bodyPublisher = HttpRequest.BodyPublishers.ofString(
			    formFields.stream()
			        .map( formField -> {
				        String value = StringCaster.cast( formField.get( Key.value ) );
				        if ( BooleanCaster.cast( formField.getOrDefault( Key.encoded, false ) ) ) {
					        value = EncryptionUtil.urlEncode( value, StandardCharsets.UTF_8 );
				        }
				        return StringCaster.cast( formField.get( Key._name ) ) + "=" + value;
			        } )
			        .collect( Collectors.joining( "&" ) )
			);

			requestBuilder.header( "Content-Type", "application/x-www-form-urlencoded" );
			return bodyPublisher;
		}

		/**
		 * Process file uploads for the request
		 *
		 * @param requestBuilder The HttpRequest.Builder to modify
		 * @param bodyPublisher  The body publisher (if any)
		 * @param formFields     The list of form fields
		 * @param files          The list of files
		 *
		 * @return The body publisher with multipart data
		 *
		 * @throws IOException
		 *
		 * @throws BoxRuntimeException if an error occurs during processing
		 */
		private HttpRequest.BodyPublisher processFiles(
		    HttpRequest.Builder requestBuilder,
		    HttpRequest.BodyPublisher bodyPublisher,
		    List<IStruct> formFields,
		    List<IStruct> files ) throws IOException {
			// Cannot have an existing body when using multipart
			if ( bodyPublisher != null ) {
				throw new BoxRuntimeException( "Cannot use a multipart body with an existing http body: " + bodyPublisher.toString() );
			}

			HttpRequestMultipartBody.Builder multipartBodyBuilder = new HttpRequestMultipartBody.Builder();
			for ( IStruct param : files ) {
				ResolvedFilePath	path		= FileSystemUtil.expandPath( context, StringCaster.cast( param.get( Key.file ) ) );
				File				file		= path.absolutePath().toFile();
				String				mimeType	= Optional
				    .ofNullable( param.getAsString( Key.mimetype ) )
				    .orElseGet( () -> URLConnection.getFileNameMap().getContentTypeFor( file.getName() ) );
				multipartBodyBuilder.addPart( StringCaster.cast( param.get( Key._name ) ), file, mimeType, file.getName() );
			}

			for ( IStruct formField : formFields ) {
				multipartBodyBuilder.addPart( StringCaster.cast( formField.get( Key._name ) ), StringCaster.cast( formField.get( Key.value ) ) );
			}

			HttpRequestMultipartBody multipartBody = multipartBodyBuilder.build();
			requestBuilder.header( "Content-Type", multipartBody.getContentType() );
			bodyPublisher = HttpRequest.BodyPublishers.ofByteArray( multipartBody.getBody() );
			return bodyPublisher;
		}

		/**
		 * Process the parameters for the request
		 *
		 * @param uriBuilder     The URIBuilder to modify
		 * @param requestBuilder The HttpRequest.Builder to modify
		 * @param formFields     The list of form fields
		 * @param files          The list of files
		 * @param bodyPublisher  The body publisher (if any)
		 *
		 * @return The body publisher (possibly modified)
		 *
		 * @throws BoxRuntimeException if an error occurs during processing
		 */
		private HttpRequest.BodyPublisher processParams(
		    URIBuilder uriBuilder,
		    HttpRequest.Builder requestBuilder,
		    List<IStruct> formFields,
		    List<IStruct> files,
		    HttpRequest.BodyPublisher bodyPublisher ) {

			// Iterate over each param and process based on type
			for ( Object p : this.params ) {
				IStruct	param	= StructCaster.cast( p );
				String	type	= StringCaster.cast( param.get( Key.type ) );

				// Process based on type
				switch ( type.toLowerCase() ) {
					// We need to use `setHeader` to overwrite any previously set headers
					case "header" -> {
						String headerName = StringCaster.cast( param.get( Key._NAME ) );
						// We need to downgrade our HTTP version if a TE header is present and is not `trailers`
						// because HTTP/2 does not support the TE header with any other values
						if ( headerName.equalsIgnoreCase( "TE" ) && !StringCaster.cast( param.get( Key.value ) ).equalsIgnoreCase( "trailers" ) ) {
							requestBuilder.version( HttpClient.Version.HTTP_1_1 );
						}
						requestBuilder.setHeader( headerName, StringCaster.cast( param.get( Key.value ) ) );
					}

					// Set body content
					case "body" -> {
						if ( bodyPublisher != null ) {
							throw new BoxRuntimeException( "Cannot use a body httpparam with an existing http body: " + bodyPublisher.toString() );
						}
						if ( param.get( Key.value ) instanceof byte[] bodyBytes ) {
							bodyPublisher = HttpRequest.BodyPublishers.ofByteArray( bodyBytes );
						} else if ( StringCaster.attempt( param.get( Key.value ) ).wasSuccessful() ) {
							bodyPublisher = HttpRequest.BodyPublishers.ofString( StringCaster.cast( param.get( Key.value ) ) );
						} else {
							throw new BoxRuntimeException( "The body attribute provided is not a valid string nor a binary object." );
						}
					}

					// XML body
					case "xml" -> {
						if ( bodyPublisher != null ) {
							throw new BoxRuntimeException( "Cannot use a xml httpparam with an existing http body: " + bodyPublisher.toString() );
						}
						requestBuilder.header( "Content-Type", "text/xml" );
						bodyPublisher = HttpRequest.BodyPublishers.ofString( StringCaster.cast( param.get( Key.value ) ) );
					}

					// CGI parameter - uses URL encoding
					case "cgi" -> requestBuilder.header( StringCaster.cast( param.get( Key._NAME ) ),
					    BooleanCaster.cast( param.getOrDefault( Key.encoded, false ) )
					        ? EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ) )
					        : StringCaster.cast( param.get( Key.value ) )
					);

					// File to upload
					case "file" -> files.add( param );

					// URL parameter
					case "url" -> uriBuilder.addParameter(
					    StringCaster.cast( param.get( Key._NAME ) ),
					    BooleanCaster.cast( param.getOrDefault( Key.encoded, true ) )
					        ? EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ), StandardCharsets.UTF_8 )
					        : StringCaster.cast( param.get( Key.value ) )
					);

					// Form field
					case "formfield" -> formFields.add( param );

					// Cookie
					case "cookie" -> requestBuilder.header( "Cookie",
					    StringCaster.cast( param.get( Key._NAME ) )
					        + "="
					        + EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ), StandardCharsets.UTF_8 )
					);

					// Unhandled type
					default -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
				}
			}
			return bodyPublisher;
		}

		/**
		 * Setup Basic Authentication header
		 *
		 * @param requestBuilder The HttpRequest.Builder to add the header to
		 */
		private void setupBasicAuth( HttpRequest.Builder requestBuilder ) {
			String	auth		= ( this.username != null ? this.username : "" ) + ":" + ( this.password != null ? this.password : "" );
			String	encodedAuth	= Base64.getEncoder().encodeToString( auth.getBytes() );
			requestBuilder.header( "Authorization", "Basic " + encodedAuth );
		}

		/**
		 * --------------------------------------------------------------------------
		 * Process the request (Preparation)
		 * --------------------------------------------------------------------------
		 *
		 * @throws URISyntaxException
		 * @throws IOException
		 */
		private BoxHttpRequest prepareRequest() throws URISyntaxException, IOException {
			// Setup Request Builders
			HttpRequest.Builder			requestBuilder	= HttpRequest.newBuilder();
			URIBuilder					uriBuilder		= new URIBuilder( this.url );
			HttpRequest.BodyPublisher	bodyPublisher	= null;
			List<IStruct>				formFields		= new ArrayList<>();
			List<IStruct>				files			= new ArrayList<>();

			// Process Port if provided
			if ( this.port != null ) {
				uriBuilder.setPort( this.port );
			}

			// Basic metadata for the request and results
			requestBuilder
			    .version( this.httpVersion.equalsIgnoreCase( HTTP_1 ) ? HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2 )
			    .header( "User-Agent", this.userAgent )
			    .header( "Accept", "*/*" )
			    .header( "Accept-Encoding", "gzip, deflate" )
			    .header( "x-request-id", this.requestID );

			// Setup Basic Auth if provided
			if ( this.username != null && this.password != null ) {
				setupBasicAuth( requestBuilder );
			}

			// Process HTTP Params - returns the bodyPublisher if any body-type params were found
			bodyPublisher = processParams( uriBuilder, requestBuilder, formFields, files, bodyPublisher );

			// Process Files
			if ( !files.isEmpty() ) {
				bodyPublisher = processFiles( requestBuilder, bodyPublisher, formFields, files );
			} else if ( !formFields.isEmpty() ) {
				// Process Form Fields
				bodyPublisher = processFormFields( requestBuilder, bodyPublisher, formFields );
			}

			// Set body publisher to noBody if still null
			if ( bodyPublisher == null ) {
				bodyPublisher = HttpRequest.BodyPublishers.noBody();
			}

			// Finalize Request Building with different settings
			requestBuilder
			    .method( this.method, bodyPublisher )
			    .uri( uriBuilder.build() );

			// Set a request Timeout if specified
			if ( this.timeout > 0 ) {
				requestBuilder.timeout( java.time.Duration.ofSeconds( this.timeout ) );
			}

			// Build the final HttpRequest
			this.targetHttpRequest = requestBuilder.build();

			return this;
		}

		/**
		 * --------------------------------------------------------------------------
		 * Terminal Operations (Execution)
		 * --------------------------------------------------------------------------
		 */

		/**
		 * Execute the HTTP request and return the buffered response.
		 * This is a terminal operation.
		 *
		 * @return The HTTPResult struct with the response data
		 */
		public BoxHttpRequest invoke() {
			try {
				// Update client usage tracking
				BoxHttpClient.this.updateLastUsedTimestamp();
				BoxHttpClient.this.totalRequests.incrementAndGet();

				// Mark the start time of the request
				this.startTime = new DateTime();
				// This prepares the request, forms, files, etc.
				// The request builder is available as this.targetHttpRequest
				prepareRequest();

				// Track bytes sent (estimate from body publisher if available)
				this.targetHttpRequest.bodyPublisher().ifPresent( publisher -> {
					publisher.contentLength();
					long contentLength = publisher.contentLength();
					if ( contentLength > 0 ) {
						BoxHttpClient.this.bytesSent.addAndGet( contentLength );
					}
				} );

				// Prepare the Http Reesults with the request info so we can start
				this.httpResult.put( Key.requestID, this.requestID );
				this.httpResult.put( Key.userAgent, this.userAgent );
				this.httpResult.put( Key.stream, this.onChunkCallback != null ? true : false );
				this.httpResult.put( Key.startTime, this.startTime );
				this.httpResult.put(
				    Key.request,
				    Struct.ofNonConcurrent(
				        Key.charset, this.charset,
				        Key.headers, Struct.fromMap( this.targetHttpRequest.headers().map() ),
				        Key.httpVersion, this.httpVersion,
				        Key.method, method,
				        Key.multipart, this.multipart,
				        Key.timeout, this.timeout,
				        Key.URL, this.targetHttpRequest.uri().toString()
				    ) );

				// Announce we are ready to process
				interceptorService.announce(
				    BoxEvent.ON_HTTP_REQUEST,
				    ( java.util.function.Supplier<IStruct> ) () -> Struct.ofNonConcurrent(
				        Key.requestID, this.requestID,
				        Key.httpClient, BoxHttpClient.this,
				        Key.httpRequest, this.targetHttpRequest,
				        Key.targetURI, this.targetHttpRequest.uri(),
				        Key.httpResult, this.httpResult
				    )
				);

				// Call onRequestStart callback if provided (before sending the request)
				if ( this.onRequestStartCallback != null ) {
					context.invokeFunction(
					    this.onRequestStartCallback,
					    new Object[] {
					        Struct.ofNonConcurrent(
					            Key.request, this.targetHttpRequest,
					            Key.URL, this.targetHttpRequest.uri().toString(),
					            Key.method, this.method,
					            Key.headers, Struct.fromMap( this.targetHttpRequest.headers().map() ),
					            Key.httpResult, this.httpResult
					        )
					    }
					);
				}

				// Choose between streaming and buffered response based on onChunk callback
				if ( this.onChunkCallback != null ) {
					// Streaming mode: process response in chunks
					processStreamingResponse();
				} else {
					// Buffered mode: receive entire response then process
					HttpResponse<byte[]> response = httpClient
					    .sendAsync( this.targetHttpRequest, HttpResponse.BodyHandlers.ofByteArray() )
					    .get();

					// Announce the HTTP RAW response
					// Useful for debugging and pre-processing and timing, since the other events are after the response is processed
					interceptorService.announce(
					    BoxEvent.ON_HTTP_RAW_RESPONSE,
					    ( java.util.function.Supplier<IStruct> ) () -> Struct.ofNonConcurrent(
					        Key.requestID, this.requestID,
					        Key.response, response,
					        Key.httpClient, BoxHttpClient.this,
					        Key.httpRequest, this.targetHttpRequest,
					        Key.targetURI, this.targetHttpRequest.uri(),
					        Key.httpResult, this.httpResult,
					        Key.stream, false
					    ) );

					// Process buffered response
					processBufferedResponse( response );

					// Announce the HTTP response now that it's processed
					interceptorService.announce(
					    BoxEvent.ON_HTTP_RESPONSE,
					    ( java.util.function.Supplier<IStruct> ) () -> Struct.ofNonConcurrent(
					        Key.requestID, this.requestID,
					        Key.response, response,
					        Key.result, this.httpResult
					    ) );

					// Call onComplete callback if provided
					if ( this.onCompleteCallback != null ) {
						context.invokeFunction(
						    onCompleteCallback,
						    new Object[] { this.httpResult, response }
						);
					}
				}
			} catch ( ExecutionException e ) {
				Throwable innerException = e.getCause();
				this.error				= true;
				this.requestException	= e;
				this.errorMessage		= e.getMessage();

				logger.error( "ExecutionException during HTTP request", e );

				// Handle timeout exceptions FIRST (most specific)
				if ( innerException instanceof HttpTimeoutException ) {
					logger.debug( "HttpTimeoutException detected - request timed out after {} seconds", this.timeout );
					BoxHttpClient.this.timeoutFailures.incrementAndGet();
					HttpResponseHelper.populateErrorResponse(
					    this.httpResult,
					    STATUS_REQUEST_TIMEOUT,
					    "Request Timeout",
					    "Request Timeout",
					    "The request timed out after " + this.timeout + " second(s)",
					    this.charset,
					    Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis()
					);
					// Call onError callback if provided
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { innerException, this.httpResult } );
					}
				}
				// Handle connection exceptions
				else if ( innerException instanceof SocketException ) {
					logger.debug( "SocketException detected: {}", innerException.getMessage() );
					// Check if it's a TLS/SSL failure
					if ( innerException instanceof javax.net.ssl.SSLException ||
					    ( innerException.getCause() instanceof javax.net.ssl.SSLException ) ) {
						BoxHttpClient.this.tlsFailures.incrementAndGet();
					} else {
						BoxHttpClient.this.connectionFailures.incrementAndGet();
					}
					String errorDetail;
					if ( innerException instanceof ConnectException ) {
						if ( this.targetHttpRequest.uri() != null ) {
							errorDetail = String.format( "Unknown host: %s: Name or service not known.", this.targetHttpRequest.uri().getHost() );
						} else {
							errorDetail = String.format( "Unknown host: %s: Name or service not known.", this.targetHttpRequest.uri().toString() );
						}
					} else {
						errorDetail = "Connection Failure: " + innerException.getMessage();
					}
					HttpResponseHelper.populateErrorResponse(
					    this.httpResult,
					    STATUS_BAD_GATEWAY,
					    "Bad Gateway",
					    "Connection Failure",
					    errorDetail,
					    this.charset,
					    Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis()
					);
					// Call onError callback if provided
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { innerException, this.httpResult } );
					}
				}
				// Handle any other ExecutionException
				else {
					logger.error(
					    "Unhandled ExecutionException with inner exception: {}",
					    innerException != null ? innerException.getClass().getName() : "null", e
					);
					// Track as HTTP protocol failure if it's an HTTP-related exception
					if ( innerException != null &&
					    ( innerException.getClass().getName().contains( "Http" ) ||
					        innerException.getClass().getName().contains( "Protocol" ) ) ) {
						BoxHttpClient.this.httpProtocolFailures.incrementAndGet();
					}
					String errorDetail = innerException != null
					    ? innerException.getClass().getName() + ": " + innerException.getMessage()
					    : e.getMessage();
					HttpResponseHelper.populateErrorResponse(
					    this.httpResult,
					    STATUS_INTERNAL_ERROR,
					    "Internal Server Error",
					    "",
					    errorDetail,
					    this.charset,
					    Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis()
					);
					// Call onError callback if provided
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { innerException != null ? innerException : e, this.httpResult } );
					}
				}
			} catch ( InterruptedException e ) {
				// interrupt the thread
				Thread.currentThread().interrupt();
				logger.warn( "Thread interrupted during HTTP request", e );
				this.error				= true;
				this.errorMessage		= e.getMessage();
				this.requestException	= e;
				this.httpResult.put( Key.errorDetail, "Thread interrupted: " + e.getMessage() );
			}
			// BoxRuntimeException should always be re-thrown (e.g., binary validation errors, hard-stop errors, etc.)
			catch ( BoxRuntimeException e ) {
				logger.debug( "BoxRuntimeException during HTTP request - re-throwing", e );
				throw e;
			}
			// Catch-all for any other unexpected exceptions
			catch ( Exception e ) {
				logger.error( "Unexpected exception during HTTP request", e );
				if ( this.onErrorCallback != null ) {
					context.invokeFunction(
					    this.onErrorCallback,
					    new Object[] { e, this.httpResult }
					);
				}
				this.error				= true;
				this.errorMessage		= e.getMessage();
				this.requestException	= e;
				this.httpResult.put( Key.statusCode, STATUS_INTERNAL_ERROR );
				this.httpResult.put( Key.status_code, STATUS_INTERNAL_ERROR );
				this.httpResult.put( Key.statusText, "Internal Server Error" );
				this.httpResult.put( Key.status_text, "Internal Server Error" );
				this.httpResult.put( Key.errorDetail, e.getClass().getName() + ": " + e.getMessage() );
				this.httpResult.put( Key.charset, this.charset );
				this.httpResult.put( Key.executionTime, Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis() );
			} finally {
				// Always calculate execution time if not already set (success path)
				if ( this.startTime != null && !this.httpResult.containsKey( Key.executionTime ) ) {
					this.httpResult.put( Key.executionTime, Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis() );
				}

				// Update statistics based on request outcome
				if ( this.error ) {
					BoxHttpClient.this.failedRequests.incrementAndGet();
				} else {
					BoxHttpClient.this.successfulRequests.incrementAndGet();
				}

				// Track execution time
				if ( this.httpResult.containsKey( Key.executionTime ) ) {
					long executionTime = ( ( Number ) this.httpResult.get( Key.executionTime ) ).longValue();
					BoxHttpClient.this.totalExecutionTimeMs.addAndGet( executionTime );

					// Update min execution time
					long currentMin;
					do {
						currentMin = BoxHttpClient.this.minExecutionTimeMs.get();
						if ( executionTime >= currentMin )
							break;
					} while ( !BoxHttpClient.this.minExecutionTimeMs.compareAndSet( currentMin, executionTime ) );

					// Update max execution time
					long currentMax;
					do {
						currentMax = BoxHttpClient.this.maxExecutionTimeMs.get();
						if ( executionTime <= currentMax )
							break;
					} while ( !BoxHttpClient.this.maxExecutionTimeMs.compareAndSet( currentMax, executionTime ) );
				}

				// Track bytes received (from fileContent if available)
				if ( this.httpResult.containsKey( Key.fileContent ) ) {
					Object fileContent = this.httpResult.get( Key.fileContent );
					if ( fileContent instanceof String ) {
						BoxHttpClient.this.bytesReceived.addAndGet( ( ( String ) fileContent ).getBytes().length );
					} else if ( fileContent instanceof byte[] ) {
						BoxHttpClient.this.bytesReceived.addAndGet( ( ( byte[] ) fileContent ).length );
					}
				}
			}

			return this;
		}

		/**
		 * Invoke and if there is an exception, then throw it
		 *
		 * @throws Throwable
		 */
		public void invokeOrThrow() throws Throwable {
			try {
				invoke();
			} catch ( Throwable t ) {
				// Just in case
				throw t;
			}

			// If there was an exception during the request, throw it now
			if ( this.requestException != null ) {
				throw ( this.requestException );
			}
		}

		/**
		 * This can be used as a terminal operation to process the failure case
		 * of the HTTP request.
		 *
		 * The callback function will be called if the request failed.
		 * Example:
		 *
		 * <pre>
		 * httpRequestBuilder
		 *     .ifFailed( ( exception, httpResult ) -> {
		 * 		// Handle failure
		 * 	} );
		 * </pre>
		 *
		 * @param callback The function to call on failure
		 *
		 * @return The HttpRequestBuilder for chaining
		 */
		public BoxHttpRequest ifFailed( BiConsumer<Throwable, IStruct> callback ) {
			if ( this.error ) {
				callback.accept( this.requestException, this.httpResult );
			}
			return this;
		}

		/**
		 * This can be used as a terminal operation to process the success case
		 * of the HTTP request.
		 * The callback function will be called if the request was successful.
		 * Example:
		 *
		 * <pre>
		 * httpRequestBuilder
		 *     .ifSuccessful( ( httpResult ) -> {
		 * 		// Handle success
		 * 	} );
		 * </pre>
		 *
		 * @param callback The function to call on success
		 *
		 * @return The HttpRequestBuilder for chaining
		 */
		public BoxHttpRequest ifSuccessful( Consumer<IStruct> callback ) {
			if ( !this.error ) {
				callback.accept( this.httpResult );
			}
			return this;
		}

		/**
		 * Process a buffered HTTP response (non-streaming).
		 * This method handles decoding, charset conversion, binary validation,
		 * and output file saving.
		 *
		 * @param response The HTTP response with byte[] body
		 *
		 * @return The processed response body
		 *
		 * @throws IOException         If an I/O error occurs
		 * @throws BoxRuntimeException If the response is binary but getAsBinary is set to 'never'.
		 *                             Unable to determine filename from response for output.
		 */
		private Object processBufferedResponse( HttpResponse<byte[]> response ) throws IOException {
			// Get the response Headers and convert them to a native BoxLang struct
			HttpHeaders	httpHeaders			= Optional.ofNullable( response.headers() ).orElse( HttpHeaders.of( Map.of(), ( a, b ) -> true ) );
			IStruct		headers				= HttpResponseHelper.transformToResponseHeaderStruct( httpHeaders.map() );

			// Prepare HTTP response metadata as a string
			String		httpVersionString	= HttpResponseHelper.getHttpVersionString( response.version() );

			// Populate standard response metadata
			HttpResponseHelper.populateResponseMetadata( this.httpResult, headers, httpVersionString, response.statusCode() );

			// Determine binary response handling
			byte[]	responseBytes	= response.body();
			Object	responseBody	= null;

			// Process body if not null
			if ( responseBytes != null && responseBytes.length > 0 ) {
				String	contentType		= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );
				String	contentEncoding	= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentEncoding );

				// Decode content encoding if present (gzip, deflate)
				if ( contentEncoding != null ) {
					// Split the Content-Encoding header into individual encodings
					String[] encodings = contentEncoding.split( "," );
					for ( String encoding : encodings ) {
						encoding = encoding.trim().toLowerCase();
						if ( encoding.equals( "gzip" ) ) {
							responseBytes = ZipUtil.extractGZipContent( responseBytes );
						} else if ( encoding.equals( "deflate" ) ) {
							responseBytes = ZipUtil.inflateDeflatedContent( responseBytes );
						}
					}
				}

				// Determine if response should be treated as binary
				Boolean	isBinaryContentType	= FileSystemUtil.isBinaryMimeType( contentType );
				String	charset				= null;

				if ( ( this.isBinaryRequest || isBinaryContentType ) && !this.isBinaryNever ) {
					responseBody = responseBytes;
				} else if ( this.isBinaryNever && isBinaryContentType ) {
					throw new BoxRuntimeException( "The response is a binary type, but the getAsBinary attribute was set to 'never'" );
				} else {
					// Extract charset from Content-Type or use default
					charset = HttpResponseHelper.extractCharset( contentType );
					if ( charset == null ) {
						charset = this.charset;
					}
					// Validate charset before using
					try {
						responseBody = new String( responseBytes, Charset.forName( charset ) );
					} catch ( Exception e ) {
						logger.warn( "Invalid charset '{}', falling back to default: {}", charset, DEFAULT_CHARSET );
						responseBody	= new String( responseBytes, Charset.forName( DEFAULT_CHARSET ) );
						charset			= DEFAULT_CHARSET;
					}
				}
			}

			// Handle output file saving if specified (always execute, even if body is null/empty)
			if ( this.outputDirectory != null ) {
				String	filename		= HttpResponseHelper.resolveOutputFilename( headers, this.outputFile, this.targetHttpRequest.uri() );
				String	destinationPath	= Path.of( this.outputDirectory, filename ).toAbsolutePath().toString();

				// Write the file based on response body type (or empty file if no body)
				if ( responseBody instanceof String responseString ) {
					FileSystemUtil.write( destinationPath, responseString, this.charset, true );
				} else if ( responseBody instanceof byte[] bodyBytes ) {
					FileSystemUtil.write( destinationPath, bodyBytes, true );
				} else {
					// No body or null body - create empty file
					FileSystemUtil.write( destinationPath, "", this.charset, true );
				}
			}

			// Fill out the httpResult struct (always populated regardless of body presence)
			this.httpResult.put(
			    Key.fileContent,
			    response.statusCode() == STATUS_REQUEST_TIMEOUT ? "Request Timeout" : ( responseBody != null ? responseBody : "" )
			);
			this.httpResult.put( Key.errorDetail, response.statusCode() == STATUS_REQUEST_TIMEOUT ? "" : "" );
			this.httpResult.put( Key.executionTime, Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis() );

			// Process Content-Type header (mimetype, charset, text determination)
			String contentType = HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );
			HttpResponseHelper.processContentType( this.httpResult, headers, contentType, this.charset );

			return responseBody;
		}

		/**
		 * Process a streaming HTTP response with chunked callbacks
		 * <p>
		 * This method handles the response in a streaming fashion, invoking the onChunk callback
		 * for each chunk of data as it arrives. It reuses the same header processing and response
		 * metadata logic as the buffered response handler.
		 *
		 * @throws IOException          If an I/O error occurs
		 * @throws ExecutionException   If the HTTP request fails
		 * @throws InterruptedException If the request is interrupted
		 * @throws BoxRuntimeException  If response handling fails
		 */
		private void processStreamingResponse() throws IOException, ExecutionException, InterruptedException {
			// Prep for streaming
			StringBuilder						accumulatedContent	= new StringBuilder();
			AtomicLong							chunkCount			= new AtomicLong( 0 );
			AtomicBoolean						streamingError		= new AtomicBoolean( false );

			// Send request with streaming body handler (using InputStream for better control)
			HttpResponse<java.io.InputStream>	response			= httpClient
			    .sendAsync(
			        this.targetHttpRequest,
			        HttpResponse.BodyHandlers.ofInputStream()
			    )
			    .get();

			// Announce the HTTP RAW response (streaming variant)
			interceptorService.announce(
			    BoxEvent.ON_HTTP_RAW_RESPONSE,
			    ( java.util.function.Supplier<IStruct> ) () -> Struct.ofNonConcurrent(
			        Key.requestID, this.requestID,
			        Key.response, response,
			        Key.httpClient, BoxHttpClient.this,
			        Key.httpRequest, this.targetHttpRequest,
			        Key.targetURI, this.targetHttpRequest.uri(),
			        Key.httpResult, this.httpResult,
			        Key.stream, true
			    ) );

			// Process response headers and metadata
			HttpHeaders	httpHeaders			= Optional.ofNullable( response.headers() ).orElse( HttpHeaders.of( Map.of(), ( a, b ) -> true ) );
			IStruct		headers				= HttpResponseHelper.transformToResponseHeaderStruct( httpHeaders.map() );

			// Prepare HTTP response metadata
			String		httpVersionString	= HttpResponseHelper.getHttpVersionString( response.version() );

			// Populate standard response metadata
			HttpResponseHelper.populateResponseMetadata( this.httpResult, headers, httpVersionString, response.statusCode() );

			// Process Content-Type header and extract charset for streaming
			String			contentType		= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );
			String			charset			= HttpResponseHelper.processContentType( this.httpResult, headers, contentType, this.charset );
			String			contentEncoding	= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentEncoding );

			// Process the stream with chunk callbacks
			BoxLangLogger	streamLogger	= getLogger();
			streamLogger.debug( "Starting streaming response processing. Charset: {}, Content-Encoding: {}", charset, contentEncoding );

			// Read the response body line by line with proper charset
			try (
			    java.io.InputStream rawInputStream = response.body();
			    java.io.InputStream decodedInputStream = HttpResponseHelper.decodeInputStream( rawInputStream, contentEncoding );
			    java.io.InputStreamReader reader = new java.io.InputStreamReader( decodedInputStream, Charset.forName( charset ) );
			    java.io.BufferedReader bufferedReader = new java.io.BufferedReader( reader ) ) {
				String line;

				// Read each line (chunk) from the stream
				while ( ( line = bufferedReader.readLine() ) != null ) {
					if ( streamingError.get() ) {
						break; // Stop processing if error occurred
					}

					try {
						long currentChunk = chunkCount.incrementAndGet();
						streamLogger.debug( "Processing chunk #{}: {}", currentChunk, line.substring( 0, Math.min( 50, line.length() ) ) );

						// Accumulate content for final result
						accumulatedContent.append( line ).append( System.lineSeparator() );

						// Invoke onChunk callback with chunk data
						IStruct chunkData = Struct.ofNonConcurrent(
						    Key.of( "chunkNumber" ), currentChunk,
						    Key.of( "content" ), line,
						    Key.of( "totalBytes" ), accumulatedContent.length(),
						    Key.statusCode, response.statusCode(),
						    Key.headers, headers,
						    Key.httpResult, this.httpResult
						);

						streamLogger.debug( "Invoking onChunk callback for chunk #{}", currentChunk );
						context.invokeFunction(
						    onChunkCallback,
						    new Object[] { chunkData }
						);

					} catch ( Exception e ) {
						streamingError.set( true );
						streamLogger.error( "Error in onChunk callback", e );

						// Store error in httpResult
						this.error				= true;
						this.requestException	= e;
						this.errorMessage		= e.getMessage();
						this.httpResult.put( Key.errorDetail, "Streaming callback error: " + e.getMessage() );

						// Call onError callback if provided
						if ( onErrorCallback != null ) {
							try {
								context.invokeFunction( onErrorCallback, new Object[] { e, this.httpResult } );
							} catch ( Exception callbackError ) {
								streamLogger.error( "Error in onError callback during streaming", callbackError );
							}
						}
						break; // Stop processing on error
					}
				}
			}

			// Finalize httpResult after streaming completes
			String finalContent = accumulatedContent.toString();
			this.httpResult.put( Key.fileContent, finalContent );
			this.httpResult.put( Key.errorDetail, "" );
			this.httpResult.put( Key.executionTime, Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis() );

			// Handle output file saving if specified (streaming variant)
			if ( this.outputDirectory != null ) {
				String	filename		= HttpResponseHelper.resolveOutputFilename( headers, this.outputFile, this.targetHttpRequest.uri() );
				String	destinationPath	= Path.of( this.outputDirectory, filename ).toAbsolutePath().toString();
				FileSystemUtil.write( destinationPath, finalContent, charset, true );
			}

			// Announce the HTTP response now that streaming is complete
			interceptorService.announce(
			    BoxEvent.ON_HTTP_RESPONSE,
			    ( java.util.function.Supplier<IStruct> ) () -> Struct.ofNonConcurrent(
			        Key.requestID, this.requestID,
			        Key.response, response,
			        Key.result, this.httpResult,
			        Key.of( "streaming" ), true,
			        Key.of( "chunkCount" ), chunkCount.get()
			    ) );

			// Call onComplete callback if provided and no errors occurred
			if ( !streamingError.get() && this.onCompleteCallback != null ) {
				context.invokeFunction(
				    onCompleteCallback,
				    new Object[] { this.httpResult, response }
				);
			}
		}

	}
}
