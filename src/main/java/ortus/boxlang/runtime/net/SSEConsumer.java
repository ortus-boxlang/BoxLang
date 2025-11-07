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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class SSEConsumer {

	/**
	 * --------------------------------------------------------------------------
	 * Constants
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default connection timeout in seconds
	 */
	public static final int					DEFAULT_TIMEOUT			= 30;

	/**
	 * Default idle timeout in seconds (0 = no timeout)
	 */
	public static final int					DEFAULT_IDLE_TIMEOUT	= 0;

	/**
	 * Default maximum reconnection attempts
	 */
	public static final int					DEFAULT_MAX_RECONNECTS	= 5;

	/**
	 * Default initial reconnection delay in milliseconds
	 */
	public static final long				DEFAULT_RECONNECT_DELAY	= 1000;

	/**
	 * Default Proxy Port
	 */
	public static final int					DEFAULT_PROXY_PORT		= 8080;

	/**
	 * Default User-Agent header value
	 */
	public static final String				DEFAULT_USER_AGENT		= "BoxLang/" + BoxRuntime.getInstance().getVersionInfo().getAsString( Key.version );

	/**
	 * Logger instance for SSEConsumer
	 */
	private static final BoxLangLogger		logger					= BoxRuntime.getInstance().getLoggingService().RUNTIME_LOGGER;

	/**
	 * Sentinel value used to indicate no data was available during an idle polling cycle.
	 * Using a distinct non-empty string ensures that legitimate blank lines (""), which are
	 * SSE event delimiters, flow through to the parser and trigger event dispatch.
	 */
	private static final String				NO_DATA_SENTINEL		= "\u0000";

	/**
	 * Virtual Executor
	 */
	private final BoxExecutor				boxExecutor				= BoxRuntime.getInstance().getAsyncService().getExecutor( "io-tasks" );

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The URL of the SSE endpoint.
	 */
	private final String					url;

	/**
	 * The bound context for function invocation.
	 */
	private final IBoxContext				context;

	/**
	 * Custom headers to include in the HTTP request.
	 */
	private final IStruct					headers;

	/**
	 * Custom User-Agent header value.
	 */
	private final String					userAgent;

	/**
	 * Username for HTTP basic authentication.
	 */
	private final String					username;

	/**
	 * Password for HTTP basic authentication.
	 */
	private final String					password;

	/**
	 * Proxy server host for HTTP requests.
	 */
	private final String					proxyServer;

	/**
	 * Proxy server port for HTTP requests.
	 */
	private final int						proxyPort;

	/**
	 * Username for proxy authentication.
	 */
	private final String					proxyUser;

	/**
	 * Password for proxy authentication.
	 */
	private final String					proxyPassword;

	/**
	 * Connection timeout in seconds.
	 */
	private final int						timeoutSeconds;

	/**
	 * Idle timeout in seconds (0 = no timeout, listen forever).
	 */
	private final int						idleTimeoutSeconds;

	/**
	 * Maximum number of reconnection attempts.
	 */
	private final int						maxReconnects;

	/**
	 * Initial reconnection delay in milliseconds.
	 */
	private final long						initialReconnectDelay;

	/**
	 * Callback function invoked when a message is received.
	 */
	private final Function					onMessage;

	/**
	 * Callback function invoked when the SSE connection is opened.
	 */
	private final Function					onOpen;

	/**
	 * Callback function invoked when the SSE connection is closed.
	 */
	private final Function					onClose;

	/**
	 * Callback function invoked when an error occurs.
	 */
	private final Function					onError;

	/**
	 * Callback function invoked when a named event is received.
	 */
	private final Function					onEvent;

	/**
	 * True if the connection is closed.
	 */
	private final AtomicBoolean				closed					= new AtomicBoolean( false );

	/**
	 * Current number of reconnection attempts.
	 */
	private final AtomicInteger				reconnectAttempts		= new AtomicInteger( 0 );

	/**
	 * Last received event ID for reconnection. Stored as a String for full SSE spec compliance.
	 * Null indicates no ID has been received yet. Numeric convenience parsing offered via getLastEventId().
	 */
	private final AtomicReference<String>	lastEventId				= new AtomicReference<>( null );

	/**
	 * Current reconnect delay in milliseconds (can be updated by retry directives).
	 */
	private final AtomicLong				currentReconnectDelay;

	/**
	 * Future representing the current connection task.
	 */
	private volatile Future<?>				connectionFuture;

	/**
	 * Private constructor - use Builder to create instances.
	 *
	 * @param builder The builder instance with configured properties
	 */
	private SSEConsumer( Builder builder ) {
		// Connection settings
		this.context				= builder.context;
		this.url					= builder.url;
		this.headers				= builder.headers;
		this.userAgent				= builder.userAgent;
		this.username				= builder.username;
		this.password				= builder.password;
		this.proxyServer			= builder.proxyServer;
		this.proxyPort				= builder.proxyPort;
		this.proxyUser				= builder.proxyUser;
		this.proxyPassword			= builder.proxyPassword;
		this.timeoutSeconds			= builder.timeoutSeconds;
		this.idleTimeoutSeconds		= builder.idleTimeoutSeconds;
		this.maxReconnects			= builder.maxReconnects;
		this.initialReconnectDelay	= builder.initialReconnectDelay;

		// Initialize current reconnect delay with initial value
		this.currentReconnectDelay	= new AtomicLong( builder.initialReconnectDelay );

		// Callbacks
		this.onMessage				= builder.onMessage;
		this.onOpen					= builder.onOpen;
		this.onClose				= builder.onClose;
		this.onError				= builder.onError;
		this.onEvent				= builder.onEvent;
	}

	/**
	 * Connects to the SSE endpoint and starts consuming events.
	 * This method is non-blocking and returns immediately.
	 *
	 * @return A Future that completes when the connection ends
	 */
	public Future<?> connect() {
		// Check if a connection is already in progress
		if ( this.connectionFuture != null && !this.connectionFuture.isDone() ) {
			logger.warn( "Connection already in progress for URL: {}", this.url );
			return this.connectionFuture;
		}

		// Start the connection task in a virtual thread
		this.connectionFuture = this.boxExecutor.submit( ( this::performConnection ) );
		return this.connectionFuture;
	}

	/**
	 * Returns true if the connection is closed.
	 *
	 * @return true if closed, false otherwise
	 */
	public boolean isClosed() {
		return this.closed.get();
	}

	/**
	 * Performs the actual connection and event processing.
	 * This manages the interruptions and reconnections.
	 */
	private void performConnection() {
		// Main connection loop with reconnection logic
		while ( !this.closed.get() && this.reconnectAttempts.get() <= this.maxReconnects ) {
			try {
				connectInternal();
				// If we reach here, connection ended gracefully
				break;
			} catch ( Exception e ) {
				logger.error( "SSE connection failed for URL: {}", this.url, e );

				// Invoke onError callback
				if ( this.onError != null ) {
					try {
						this.context.invokeFunction( this.onError, new Object[] { e, this } );
					} catch ( Exception callbackError ) {
						logger.error( "Error in onError callback", callbackError );
					}
				}

				// Handle reconnection with exponential backoff
				if ( !this.closed.get() && this.reconnectAttempts.incrementAndGet() <= this.maxReconnects ) {
					long	baseDelay	= this.currentReconnectDelay.get();
					long	delay		= baseDelay * ( 1L << Math.min( this.reconnectAttempts.get() - 1, 10 ) );
					logger.info(
					    "Reconnecting in {} ms (attempt {} of {}, base delay {} ms)",
					    delay,
					    this.reconnectAttempts.get(),
					    this.maxReconnects,
					    baseDelay
					);

					try {
						Thread.sleep( delay );
					} catch ( InterruptedException ie ) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					logger.error( "Max reconnection attempts reached for URL: {}", this.url );
					break;
				}
			}
		}

		// Ensure close callback is called
		close();
	}

	/**
	 * This does the actual connection to the SSE endpoint and processes incoming events.
	 */
	private void connectInternal() throws IOException, InterruptedException {
		// Create HttpClient with proxy support if configured
		HttpClient			client			= createHttpClient();

		HttpRequest.Builder	requestBuilder	= HttpRequest.newBuilder()
		    .uri( URI.create( this.url ) )
		    .header( "Accept", "text/event-stream" )
		    .header( "Cache-Control", "no-cache" )
		    .header( "User-Agent", this.userAgent )
		    .timeout( Duration.ofSeconds( this.timeoutSeconds ) )
		    .GET();

		// Add custom headers
		this.headers.entrySet().forEach( entry -> {
			Key		headerName	= entry.getKey();
			Object	headerValue	= entry.getValue();
			requestBuilder.header( headerName.getName(), StringCaster.cast( headerValue ) );
		} );

		// Add HTTP Basic Authentication if username/password are provided
		if ( this.username != null && this.password != null ) {
			String	credentials			= this.username + ":" + this.password;
			String	encodedCredentials	= java.util.Base64.getEncoder().encodeToString( credentials.getBytes( StandardCharsets.UTF_8 ) );
			requestBuilder.header( "Authorization", "Basic " + encodedCredentials );
		}

		// Add Last-Event-ID if we have one
		String lastId = this.lastEventId.get();
		if ( lastId != null && !lastId.isEmpty() ) {
			requestBuilder.header( "Last-Event-ID", lastId );
		}

		// Build and send the request
		HttpRequest					request		= requestBuilder.build();
		HttpResponse<InputStream>	response	= client.send(
		    request,
		    HttpResponse.BodyHandlers.ofInputStream()
		);

		// Check response status
		if ( response.statusCode() != 200 ) {
			throw new IOException( "HTTP " + response.statusCode() + " response from SSE endpoint" );
		}

		// Connection successful - reset reconnect attempts and call onOpen
		this.reconnectAttempts.set( 0 );
		if ( this.onOpen != null ) {
			try {
				this.context.invokeFunction( this.onOpen, new Object[] { this } );
			} catch ( Exception e ) {
				logger.error( "Error in onOpen callback", e );
			}
		}

		SSEParser parser = new SSEParser();

		// Use try-with-resources to ensure the stream is properly closed
		try ( BufferedReader reader = new BufferedReader( new InputStreamReader( response.body(), StandardCharsets.UTF_8 ) ) ) {
			long	lastActivityTime	= System.currentTimeMillis();
			String	line;

			while ( !this.closed.get() && ( line = readLineWithTimeout( reader, lastActivityTime ) ) != null ) {
				// If this is the idle polling sentinel, skip without updating activity time
				if ( line == NO_DATA_SENTINEL ) {
					continue;
				}

				// Update last activity ONLY when real data (including blank delimiter lines) arrives
				lastActivityTime = System.currentTimeMillis();

				// Forward all lines (including blank lines) to parser so event delimiters are processed
				Attempt<SSEParserResult> parseResult = parser.parseLine( line );
				if ( parseResult.wasSuccessful() ) {
					SSEParserResult result = parseResult.get();

					// Handle different types of parser results
					switch ( result ) {
						case SSEEvent event -> {
							// Update last event ID if present
							if ( event.id() != null ) {
								this.lastEventId.set( event.id() );
								if ( logger.isDebugEnabled() ) {
									logger.debug( "Event ID received: {}", event.id() );
								}
							}

							// Dispatch event to appropriate callback
							try {
								// If event type is known, use onEvent callback
								if ( event.event() != null && this.onEvent != null ) {
									this.context.invokeFunction( this.onEvent, new Object[] {
									    event.event(),
									    event.data(),
									    event.id(),
									    this
									} );
								}
								// Otherwise, use onMessage callback
								else if ( this.onMessage != null ) {
									this.context.invokeFunction( this.onMessage, new Object[] {
									    event.data(),
									    event.id(),
									    this
									} );
								}
							} catch ( Exception e ) {
								logger.error( "Error in event callback", e );
								// Invoke onError callback
								if ( this.onError != null ) {
									try {
										this.context.invokeFunction( this.onError, new Object[] { e, this } );
									} catch ( Exception callbackError ) {
										logger.error( "Error in onError callback", callbackError );
									}
								}
							}
						}
						case SSERetryDirective retryDirective -> {
							// Update the reconnect delay based on server directive
							long newDelay = retryDirective.retryDelayMs();
							this.currentReconnectDelay.set( newDelay );
							logger.debug( "Server requested retry delay update to {} ms", newDelay );
						}
					}
				}
			}
		} catch ( SocketTimeoutException e ) {
			if ( this.idleTimeoutSeconds > 0 ) {
				logger.info( "SSE connection idle timeout after {} seconds", this.idleTimeoutSeconds );
				close();
				return;
			}
			throw e;
		}
	}

	/**
	 * Creates an HttpClient with proxy support if configured.
	 *
	 * @return HttpClient instance
	 */
	private HttpClient createHttpClient() {
		HttpClient.Builder clientBuilder = HttpClient.newBuilder();

		// Configure proxy if host is provided
		if ( this.proxyServer != null && !this.proxyServer.trim().isEmpty() ) {
			int				port			= this.proxyPort > 0 ? this.proxyPort : DEFAULT_PROXY_PORT; // Default proxy port

			ProxySelector	proxySelector	= ProxySelector.of( new InetSocketAddress( this.proxyServer, port ) );
			clientBuilder.proxy( proxySelector );

			// Add proxy authentication if provided
			if ( this.proxyUser != null && this.proxyPassword != null ) {
				java.net.Authenticator authenticator = new java.net.Authenticator() {

					@Override
					protected java.net.PasswordAuthentication getPasswordAuthentication() {
						if ( getRequestorType() == RequestorType.PROXY ) {
							return new java.net.PasswordAuthentication(
							    SSEConsumer.this.proxyUser,
							    SSEConsumer.this.proxyPassword.toCharArray()
							);
						}
						return null;
					}
				};
				clientBuilder.authenticator( authenticator );
			}
		}

		return clientBuilder.build();
	}

	/**
	 * Closes the SSE connection and invokes the onClose callback.
	 * This method is idempotent and thread-safe.
	 */
	public void close() {
		// Ensure we only close once
		if ( this.closed.compareAndSet( false, true ) ) {
			logger.debug( "Closing SSE connection for URL: {}", this.url );

			// Try to cancel the connection task
			if ( this.connectionFuture != null ) {
				this.connectionFuture.cancel( true );
			}

			// Invoke onClose callback
			if ( this.onClose != null ) {
				try {
					this.context.invokeFunction( this.onClose, new Object[] { this } );
				} catch ( Exception e ) {
					logger.error( "Error in onClose callback", e );
				}
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the Future representing the current connection task.
	 *
	 * @return connection Future
	 */
	public Future<?> getConnectionFuture() {
		return this.connectionFuture;
	}

	/**
	 * Returns the current number of reconnection attempts.
	 *
	 * @return reconnection attempt count
	 */
	public int getReconnectAttempts() {
		return this.reconnectAttempts.get();
	}

	/**
	 * Returns the last received event ID as a long if it is numeric.
	 * Non-numeric or absent IDs return -1. Use getLastEventIdString() for raw value.
	 *
	 * @return numeric event ID or -1
	 */
	public long getLastEventId() {
		String id = this.lastEventId.get();
		if ( id == null ) {
			return -1;
		}
		try {
			return Long.parseLong( id );
		} catch ( NumberFormatException e ) {
			return -1;
		}
	}

	/**
	 * Returns the raw last event ID string (may be non-numeric or null if not yet received).
	 *
	 * @return last event ID string or null
	 */
	public String getLastEventIdString() {
		return this.lastEventId.get();
	}

	/**
	 * Returns the custom headers for the HTTP request.
	 *
	 * @return IStruct containing the headers
	 */
	public IStruct getHeaders() {
		return this.headers;
	}

	/**
	 * Returns the User-Agent header value.
	 *
	 * @return User-Agent string
	 */
	public String getUserAgent() {
		return this.userAgent;
	}

	/**
	 * Returns the connection timeout in seconds.
	 *
	 * @return timeout in seconds
	 */
	public int getTimeoutSeconds() {
		return this.timeoutSeconds;
	}

	/**
	 * Returns the idle timeout in seconds.
	 *
	 * @return idle timeout in seconds (0 = no timeout)
	 */
	public int getIdleTimeoutSeconds() {
		return this.idleTimeoutSeconds;
	}

	/**
	 * Returns the maximum number of reconnection attempts.
	 *
	 * @return maximum reconnection attempts
	 */
	public int getMaxReconnects() {
		return this.maxReconnects;
	}

	/**
	 * Returns the initial reconnection delay in milliseconds.
	 *
	 * @return initial reconnection delay in milliseconds
	 */
	public long getInitialReconnectDelay() {
		return this.initialReconnectDelay;
	}

	/**
	 * Returns the current reconnection delay in milliseconds (may have been updated by retry directives).
	 *
	 * @return current reconnection delay in milliseconds
	 */
	public long getCurrentReconnectDelay() {
		return this.currentReconnectDelay.get();
	}

	/**
	 * Returns the username for HTTP basic authentication.
	 *
	 * @return username, or null if not set
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Returns true if HTTP basic authentication is configured.
	 *
	 * @return true if both username and password are set
	 */
	public boolean hasBasicAuth() {
		return this.username != null && this.password != null;
	}

	/**
	 * Returns the proxy server host.
	 *
	 * @return proxy host, or null if not set
	 */
	public String getProxyServer() {
		return this.proxyServer;
	}

	/**
	 * Returns the proxy server port.
	 *
	 * @return proxy port, or -1 if not set
	 */
	public int getProxyPort() {
		return this.proxyPort;
	}

	/**
	 * Returns the proxy username.
	 *
	 * @return proxy username, or null if not set
	 */
	public String getProxyUser() {
		return this.proxyUser;
	}

	/**
	 * Returns true if proxy is configured.
	 *
	 * @return true if proxy host is set
	 */
	public boolean hasProxy() {
		return this.proxyServer != null && !this.proxyServer.trim().isEmpty();
	}

	/**
	 * Returns true if proxy authentication is configured.
	 *
	 * @return true if both proxy username and password are set
	 */
	public boolean hasProxyAuth() {
		return this.proxyUser != null && this.proxyPassword != null;
	}

	/**
	 * Reads a line from the BufferedReader with idle timeout support.
	 *
	 * @param reader           The BufferedReader to read from
	 * @param lastActivityTime The timestamp of the last activity
	 *
	 * @return The line read, or null if end of stream or timeout
	 *
	 * @throws IOException if an I/O error occurs
	 */
	private String readLineWithTimeout( BufferedReader reader, long lastActivityTime ) throws IOException {
		// If no idle timeout is configured, block normally for the next line
		if ( this.idleTimeoutSeconds <= 0 ) {
			return reader.readLine();
		}

		long	currentTime		= System.currentTimeMillis();
		long	idleTime		= currentTime - lastActivityTime;
		long	idleTimeoutMs	= this.idleTimeoutSeconds * 1000L;

		// Enforce idle timeout
		if ( idleTime >= idleTimeoutMs ) {
			throw new SocketTimeoutException( "Idle timeout exceeded: " + this.idleTimeoutSeconds + " seconds" );
		}

		// Non-blocking check for available data
		if ( reader.ready() ) {
			return reader.readLine();
		}

		// Brief sleep to avoid busy spin when no data
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return null; // Propagate termination
		}

		// Indicate no data this cycle; caller will skip without updating activity
		return NO_DATA_SENTINEL;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Builder
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new Builder instance for SSEConsumer.
	 *
	 * @return Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for creating SSEConsumer instances with fluent API.
	 */
	public static class Builder {

		private String		url;
		private IBoxContext	context;
		private IStruct		headers					= Struct.ofNonConcurrent();
		private String		userAgent				= DEFAULT_USER_AGENT;
		private String		username;
		private String		password;
		private String		proxyServer;
		private int			proxyPort				= -1;
		private String		proxyUser;
		private String		proxyPassword;
		private int			timeoutSeconds			= DEFAULT_TIMEOUT;
		private int			idleTimeoutSeconds		= 0;
		private int			maxReconnects			= DEFAULT_MAX_RECONNECTS;
		private long		initialReconnectDelay	= DEFAULT_RECONNECT_DELAY;
		private Function	onMessage;
		private Function	onOpen;
		private Function	onClose;
		private Function	onError;
		private Function	onEvent;

		/**
		 * Sets the SSE endpoint URL.
		 *
		 * @param url The URL to connect to
		 *
		 * @return this builder
		 */
		public Builder url( String url ) {
			this.url = url;
			return this;
		}

		/**
		 * Sets the BoxLang context for callback invocation.
		 *
		 * @param context The BoxLang context
		 *
		 * @return this builder
		 */
		public Builder context( IBoxContext context ) {
			this.context = context;
			return this;
		}

		/**
		 * Adds a custom header to the request.
		 *
		 * @param name  Header name
		 * @param value Header value
		 *
		 * @return this builder
		 */
		public Builder header( String name, String value ) {
			this.headers.put( Key.of( name ), value );
			return this;
		}

		/**
		 * Sets multiple headers at once.
		 *
		 * @param headers IStruct of header name-value pairs
		 *
		 * @return this builder
		 */
		public Builder headers( IStruct headers ) {
			headers.entrySet()
			    .forEach( entry -> this.headers.put( entry.getKey(), entry.getValue() ) );
			return this;
		}

		/**
		 * Sets a custom User-Agent header value. If not set, defaults to BoxLang version.
		 *
		 * @param userAgent The custom User-Agent string
		 *
		 * @return this builder
		 */
		public Builder userAgent( String userAgent ) {
			this.userAgent = userAgent;
			return this;
		}

		/**
		 * Sets the username for HTTP basic authentication.
		 *
		 * @param username The username for authentication
		 *
		 * @return this builder
		 */
		public Builder username( String username ) {
			this.username = username;
			return this;
		}

		/**
		 * Sets the password for HTTP basic authentication.
		 *
		 * @param password The password for authentication
		 *
		 * @return this builder
		 */
		public Builder password( String password ) {
			this.password = password;
			return this;
		}

		/**
		 * Sets both username and password for HTTP basic authentication.
		 *
		 * @param username The username for authentication
		 * @param password The password for authentication
		 *
		 * @return this builder
		 */
		public Builder basicAuth( String username, String password ) {
			this.username	= username;
			this.password	= password;
			return this;
		}

		/**
		 * Sets the proxy server host.
		 *
		 * @param proxyServer The proxy server hostname or IP address
		 *
		 * @return this builder
		 */
		public Builder proxyServer( String proxyServer ) {
			this.proxyServer = proxyServer;
			return this;
		}

		/**
		 * Sets the proxy server port.
		 *
		 * @param proxyPort The proxy server port number
		 *
		 * @return this builder
		 */
		public Builder proxyPort( int proxyPort ) {
			this.proxyPort = proxyPort;
			return this;
		}

		/**
		 * Sets the proxy server host and port.
		 *
		 * @param proxyServer The proxy server hostname or IP address
		 * @param proxyPort   The proxy server port number
		 *
		 * @return this builder
		 */
		public Builder proxy( String proxyServer, int proxyPort ) {
			this.proxyServer	= proxyServer;
			this.proxyPort		= proxyPort;
			return this;
		}

		/**
		 * Sets the username for proxy authentication.
		 *
		 * @param proxyUser The username for proxy authentication
		 *
		 * @return this builder
		 */
		public Builder proxyUser( String proxyUser ) {
			this.proxyUser = proxyUser;
			return this;
		}

		/**
		 * Sets the password for proxy authentication.
		 *
		 * @param proxyPassword The password for proxy authentication
		 *
		 * @return this builder
		 */
		public Builder proxyPassword( String proxyPassword ) {
			this.proxyPassword = proxyPassword;
			return this;
		}

		/**
		 * Sets proxy authentication credentials.
		 *
		 * @param proxyUser     The username for proxy authentication
		 * @param proxyPassword The password for proxy authentication
		 *
		 * @return this builder
		 */
		public Builder proxyAuth( String proxyUser, String proxyPassword ) {
			this.proxyUser		= proxyUser;
			this.proxyPassword	= proxyPassword;
			return this;
		}

		/**
		 * Sets complete proxy configuration.
		 *
		 * @param proxyServer   The proxy server hostname or IP address
		 * @param proxyPort     The proxy server port number
		 * @param proxyUser     The username for proxy authentication (optional)
		 * @param proxyPassword The password for proxy authentication (optional)
		 *
		 * @return this builder
		 */
		public Builder proxy( String proxyServer, int proxyPort, String proxyUser, String proxyPassword ) {
			this.proxyServer	= proxyServer;
			this.proxyPort		= proxyPort;
			this.proxyUser		= proxyUser;
			this.proxyPassword	= proxyPassword;
			return this;
		}

		/**
		 * Sets the connection timeout.
		 *
		 * @param timeoutSeconds Timeout in seconds
		 *
		 * @return this builder
		 */
		public Builder timeout( int timeoutSeconds ) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		/**
		 * Sets the idle timeout in seconds. If no data is received within this time,
		 * the connection will be closed. A value of 0 means no timeout (listen forever).
		 *
		 * @param idleTimeoutSeconds Idle timeout in seconds (0 = no timeout)
		 *
		 * @return this builder
		 */
		public Builder idleTimeout( int idleTimeoutSeconds ) {
			this.idleTimeoutSeconds = Math.max( 0, idleTimeoutSeconds );
			return this;
		}

		/**
		 * Sets the maximum number of reconnection attempts.
		 *
		 * @param maxReconnects Maximum reconnect attempts
		 *
		 * @return this builder
		 */
		public Builder maxReconnects( int maxReconnects ) {
			this.maxReconnects = maxReconnects;
			return this;
		}

		/**
		 * Sets the initial reconnection delay.
		 *
		 * @param initialReconnectDelay Delay in milliseconds
		 *
		 * @return this builder
		 */
		public Builder reconnectDelay( long initialReconnectDelay ) {
			this.initialReconnectDelay = initialReconnectDelay;
			return this;
		}

		/**
		 * Sets the callback for message events.
		 *
		 * @param onMessage Function to call on message (data, id)
		 *
		 * @return this builder
		 */
		public Builder onMessage( Function onMessage ) {
			this.onMessage = onMessage;
			return this;
		}

		/**
		 * Sets the callback for connection open events.
		 *
		 * @param onOpen Function to call on connection open
		 *
		 * @return this builder
		 */
		public Builder onOpen( Function onOpen ) {
			this.onOpen = onOpen;
			return this;
		}

		/**
		 * Sets the callback for connection close events.
		 *
		 * @param onClose Function to call on connection close
		 *
		 * @return this builder
		 */
		public Builder onClose( Function onClose ) {
			this.onClose = onClose;
			return this;
		}

		/**
		 * Sets the callback for error events.
		 *
		 * @param onError Function to call on errors
		 *
		 * @return this builder
		 */
		public Builder onError( Function onError ) {
			this.onError = onError;
			return this;
		}

		/**
		 * Sets the callback for named events.
		 *
		 * @param onEvent Function to call on named events (eventType, data, id)
		 *
		 * @return this builder
		 */
		public Builder onEvent( Function onEvent ) {
			this.onEvent = onEvent;
			return this;
		}

		/**
		 * Builds the SSEConsumer instance.
		 *
		 * @return A new SSEConsumer instance
		 *
		 * @throws IllegalArgumentException if required fields are missing
		 */
		public SSEConsumer build() {
			if ( this.url == null || this.url.trim().isEmpty() ) {
				throw new IllegalArgumentException( "URL is required" );
			}
			if ( this.context == null ) {
				throw new IllegalArgumentException( "Context is required" );
			}

			return new SSEConsumer( this );
		}
	}
}
