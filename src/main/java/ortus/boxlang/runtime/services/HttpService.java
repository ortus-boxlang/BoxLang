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

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.net.soap.BoxSoapClient;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * This service manages all HTTP clients in BoxLang.
 * It is responsible for creating, caching, and reusing HTTP clients based on their configuration.
 * Clients are stored in BoxLang's native cache with TTL and last-access eviction so stale
 * connections are reclaimed automatically without unbounded memory growth.
 * It also handles the lifecycle events of the HTTP service within the BoxLang runtime.
 */
public class HttpService extends BaseService {

	/**
	 * The name of the BoxLang cache used to store reusable HTTP clients.
	 */
	public static final String															HTTP_CLIENT_CACHE_NAME			= "bxHttpClients";

	/**
	 * Default absolute TTL for cached HTTP clients (seconds).
	 * A client that has been alive longer than this is evicted regardless of use.
	 */
	public static final int																HTTP_CLIENT_DEFAULT_TIMEOUT		= 3600;	// 1 hour

	/**
	 * Default last-access (idle) TTL for cached HTTP clients (seconds).
	 * A client that has not been used for this long is eligible for eviction.
	 */
	public static final int																HTTP_CLIENT_LAST_ACCESS_TIMEOUT	= 1800;	// 30 minutes

	/**
	 * BoxLang cache that stores all reusable HTTP clients.
	 * Replaces the raw ConcurrentHashMap to gain automatic TTL/idle eviction
	 * and thread-safe atomic get-or-create via {@code getOrSet}.
	 */
	private ICacheProvider																clientCache;

	/**
	 * Concurrent map that stores all SOAP/WSDL clients
	 */
	private final ConcurrentMap<String, ortus.boxlang.runtime.net.soap.BoxSoapClient>	soapClients						= new ConcurrentHashMap<>();

	/**
	 * Shutdown timeout in seconds
	 */
	private static final Long															SHUTDOWN_TIMEOUT_SECONDS		= 10L;

	/**
	 * The main HTTP logger
	 */
	private BoxLangLogger																logger;

	/**
	 * The HTTP Executor used by all clients
	 */
	private BoxExecutor																	httpExecutor;

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
		this.soapClients.clear();
		this.logger.info( "+ Http Service shutdown complete" );
	}

	@Override
	public void onStartup() {
		// Allow for restricted headers to be set so we can send the Host header and Content-Length
		// Java's HttpClient (introduced in Java 11) blocks setting certain sensitive headers for security reasons
		if ( System.getProperty( "jdk.httpclient.allowRestrictedHeaders" ) == null ) {
			System.setProperty( "jdk.httpclient.allowRestrictedHeaders", "host,content-length" );
		}

		// Register a dedicated BoxLang cache for HTTP client pooling.
		// Using the cache gives us automatic TTL + idle-eviction so stale clients
		// (and their underlying connection pools) are reclaimed without unbounded growth.
		// createCacheIfAbsent is safe to call on every startup (idempotent).
		this.clientCache = runtime.getCacheService().createCacheIfAbsent(
		    Key.of( HTTP_CLIENT_CACHE_NAME ),
		    Key.boxCacheProvider,
		    Struct.of(
		        "defaultTimeout", HTTP_CLIENT_DEFAULT_TIMEOUT,
		        "defaultLastAccessTimeout", HTTP_CLIENT_LAST_ACCESS_TIMEOUT
		    )
		);

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
		return this.clientCache.getSize();
	}

	/**
	 * Verifies if a client with the given key exists
	 *
	 * @param key The client key
	 */
	public boolean hasClient( Key key ) {
		return this.clientCache.lookup( key.getName() );
	}

	/**
	 * Retrieves the HTTP client associated with the given key
	 *
	 * @param key The client key
	 *
	 * @return The BoxHttpClient instance, or null if not found
	 */
	public BoxHttpClient getClient( Key key ) {
		return ( BoxHttpClient ) this.clientCache.get( key.getName() ).orElse( null );
	}

	/**
	 * Stores the given HTTP client with the associated key
	 *
	 * @param key    The client key
	 * @param client The BoxHttpClient instance
	 */
	public BoxHttpClient putClient( Key key, BoxHttpClient client ) {
		this.clientCache.set( key.getName(), client );
		return client;
	}

	/**
	 * Remove the HTTP client associated with the given key
	 *
	 * @param key The client key
	 */
	public HttpService removeClient( Key key ) {
		this.clientCache.clear( key.getName() );
		return this;
	}

	/**
	 * Get all cached client keys
	 * <p>
	 * This method returns an array of all keys for the cached HTTP clients.
	 *
	 * @return An Array of key name strings representing all cached clients
	 */
	public Array getAllClientKeys() {
		return this.clientCache.getKeys();
	}

	/**
	 * Get Client Stats
	 *
	 * @return A struct of client statistics
	 */
	public IStruct getClientStats( Key key ) {
		BoxHttpClient client = getClient( key );
		if ( client == null ) {
			throw new BoxRuntimeException( "No HTTP client found for key: " + key.getName() );
		}
		return client.getStatistics();
	}

	/**
	 * Get all the client stats as a struct of structs
	 *
	 * @return A struct containing stats for all clients
	 */
	public IStruct getAllClientStats() {
		IStruct allStats = new Struct( false );
		for ( Object keyObj : this.clientCache.getKeys() ) {
			String			keyStr	= keyObj.toString();
			BoxHttpClient	client	= ( BoxHttpClient ) this.clientCache.get( keyStr ).orElse( null );
			if ( client != null ) {
				allStats.put( keyStr, client.getStatistics() );
			}
		}
		return allStats;
	}

	/**
	 * Clear all cached HTTP clients
	 * <p>
	 * This method removes all cached clients from the service.
	 * Useful for testing or when you need to force recreation of all clients.
	 *
	 * @return This HttpService instance for method chaining
	 */
	public HttpService clearAllClients() {
		this.clientCache.clearAll();
		return this;
	}

	/**
	 * Get or Build the HTTP client associated with the incoming connection details.
	 * <p>
	 * This method will either return an existing cached client or build a new one
	 * based on the provided connection parameters. Clients are cached based on their
	 * configuration to enable connection pooling and reuse.
	 * <p>
	 * Thread safety is guaranteed by the underlying cache's {@code getOrSet} operation
	 * which uses per-key locking to ensure that only one client is ever built per
	 * unique configuration, even under heavy concurrent load.
	 *
	 * @param httpVersion     The HTTP version to use ("HTTP/1.1" or "HTTP/2")
	 * @param followRedirects Whether to follow redirects automatically
	 * @param connectTimeout  The connection timeout in seconds (null for no timeout)
	 * @param proxyServer     The proxy server address (null if no proxy)
	 * @param proxyPort       The proxy server port (null if no proxy)
	 * @param proxyUser       The proxy authentication username (null if no auth)
	 * @param proxyPassword   The proxy authentication password (null if no auth)
	 * @param clientCertPath  The path to the client certificate (null if none)
	 * @param clientCertPass  The client certificate password (null if none)
	 *
	 * @return The BoxHttpClient instance (cached or newly created)
	 */
	public BoxHttpClient getOrBuildClient(
	    String httpVersion,
	    boolean followRedirects,
	    Integer connectTimeout,
	    String proxyServer,
	    Integer proxyPort,
	    String proxyUser,
	    String proxyPassword,
	    String clientCertPath,
	    String clientCertPass ) {

		// Default httpVersion if null
		if ( httpVersion == null ) {
			httpVersion = BoxHttpClient.HTTP_2;
		}

		// Build a deterministic, human-readable cache key for this client configuration
		Key				clientKey			= buildClientKey(
		    httpVersion,
		    followRedirects,
		    connectTimeout,
		    proxyServer,
		    proxyPort,
		    proxyUser,
		    proxyPassword,
		    clientCertPath,
		    clientCertPass
		);
		String			cacheKey			= clientKey.getName();

		// Capture final versions for the lambda
		final String	finalHttpVersion	= httpVersion;
		final String	finalProxyUser		= proxyUser;
		final String	finalProxyPassword	= proxyPassword;

		// Use the cache's thread-safe getOrSet — this replaces the broken
		// double-checked locking that existed previously. The cache applies
		// per-key synchronisation so at most one client is ever built per key,
		// even under heavy concurrent load.
		return ( BoxHttpClient ) this.clientCache.getOrSet(
		    cacheKey,
		    () -> {
			    this.logger.trace( "Building new HTTP client with key: {}", cacheKey );

			    // Create HttpClient builder
			    HttpClient.Builder builder = HttpClient.newBuilder()
			        // Configure Executor
			        .executor( this.httpExecutor.executor() )
			        // Configure redirect policy
			        .followRedirects( followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER )
			        // Configure HTTP version
			        .version( finalHttpVersion.equalsIgnoreCase( BoxHttpClient.HTTP_1 )
			            ? HttpClient.Version.HTTP_1_1
			            : HttpClient.Version.HTTP_2
			        );

			    // Configure connect timeout
			    if ( connectTimeout != null ) {
				    builder.connectTimeout( Duration.ofSeconds( connectTimeout ) );
			    }

			    // Configure proxy
			    if ( proxyServer != null && !proxyServer.isEmpty() && proxyPort != null ) {
				    builder.proxy( ProxySelector.of( new InetSocketAddress( proxyServer, proxyPort ) ) );

				    // Configure proxy authentication if credentials provided
				    if ( finalProxyUser != null && !finalProxyUser.isEmpty() && finalProxyPassword != null && !finalProxyPassword.isEmpty() ) {
					    builder.authenticator( new Authenticator() {

						    @Override
						    protected PasswordAuthentication getPasswordAuthentication() {
							    return new PasswordAuthentication( finalProxyUser, finalProxyPassword.toCharArray() );
						    }
					    } );
				    }
			    }

			    // Configure client certificate (SSL/TLS)
			    if ( clientCertPath != null ) {
				    try {
					    // Verify the certificate file exists before attempting to load
					    java.io.File certFile = new java.io.File( clientCertPath );
					    if ( !certFile.exists() ) {
						    throw new BoxRuntimeException( "Client certificate file not found: " + clientCertPath );
					    }
					    if ( !certFile.canRead() ) {
						    throw new BoxRuntimeException( "Client certificate file is not readable: " + clientCertPath );
					    }

					    // Load the client certificate keystore using EncryptionUtil
					    KeyStore keyStore = EncryptionUtil.loadPKCS12KeyStore( clientCertPath, clientCertPass );
					    if ( keyStore == null ) {
						    throw new BoxRuntimeException(
						        "Failed to load client certificate keystore (check password or file format): " + clientCertPath
						    );
					    }

					    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
					    keyManagerFactory.init( keyStore, clientCertPass != null ? clientCertPass.toCharArray() : null );

					    SSLContext sslContext = SSLContext.getInstance( "TLS" );
					    sslContext.init( keyManagerFactory.getKeyManagers(), null, new SecureRandom() );

					    builder.sslContext( sslContext );
				    } catch ( BoxRuntimeException e ) {
					    // Re-throw BoxRuntimeException as-is (these are our validation errors)
					    this.logger.error( "Client certificate configuration error: {}", e.getMessage() );
					    throw e;
				    } catch ( Exception e ) {
					    this.logger.error( "Failed to configure client certificate: {}", clientCertPath, e );
					    throw new BoxRuntimeException(
					        "Failed to configure client certificate: " + clientCertPath,
					        e
					    );
				    }
			    }

			    // Create our BoxHttpClient wrapper
			    this.logger.trace( "HTTP client created and cached with key: {}", cacheKey );
			    return new BoxHttpClient( builder.build(), this );
		    }
		);
	}

	/**
	 * Build a deterministic, human-readable cache key that uniquely identifies an
	 * HTTP client configuration.
	 * <p>
	 * Key format:
	 *
	 * <pre>
	 * bx-http|{version}|{redirect}|{timeout}|{proxyPart}|{certPart}
	 * </pre>
	 *
	 * <ul>
	 * <li>{@code version} — {@code h2} or {@code h1}</li>
	 * <li>{@code redirect} — {@code redir} or {@code noredir}</li>
	 * <li>{@code timeout} — {@code t<N>} or {@code tnull}</li>
	 * <li>{@code proxyPart} — {@code noproxy} | {@code <host>:<port>} |
	 * {@code <host>:<port>:<user>:<passHash8>} | {@code <host>:<port>:<user>:nopass}</li>
	 * <li>{@code certPart} — {@code nocert} | {@code cert:<pathHash16>:nopass} |
	 * {@code cert:<pathHash16>:hascertpass}</li>
	 * </ul>
	 * <p>
	 * The proxy password is represented as the first 8 characters of its SHA-256 hash so that:
	 * <ol>
	 * <li>Two different passwords yield different cache keys (fixes the old {@code proxyPass=yes/no} bug)</li>
	 * <li>The raw password is never stored in the key</li>
	 * </ol>
	 *
	 * @param httpVersion     HTTP version string (e.g. "HTTP/2" or "HTTP/1.1")
	 * @param followRedirects Whether to follow HTTP redirects
	 * @param connectTimeout  Connection timeout in seconds, or null
	 * @param proxyServer     Proxy hostname, or null
	 * @param proxyPort       Proxy port, or null
	 * @param proxyUser       Proxy username, or null
	 * @param proxyPassword   Proxy password, or null
	 * @param clientCertPath  Path to client certificate, or null
	 * @param clientCertPass  Client certificate password, or null
	 *
	 * @return A deterministic {@link Key} suitable for cache lookup and storage
	 */
	public Key buildClientKey(
	    String httpVersion,
	    boolean followRedirects,
	    Integer connectTimeout,
	    String proxyServer,
	    Integer proxyPort,
	    String proxyUser,
	    String proxyPassword,
	    String clientCertPath,
	    String clientCertPass ) {

		// Version segment: h2 or h1
		String	version		= ( httpVersion != null && httpVersion.equalsIgnoreCase( BoxHttpClient.HTTP_1 ) ) ? "h1" : "h2";

		// Redirect segment
		String	redirect	= followRedirects ? "redir" : "noredir";

		// Timeout segment
		String	timeout		= ( connectTimeout != null ) ? "t" + connectTimeout : "tnull";

		// Proxy segment — include a short hash of the password so different
		// passwords produce different keys without exposing the raw credential
		String	proxyPart;
		if ( proxyServer == null || proxyServer.isEmpty() || proxyPort == null ) {
			proxyPart = "noproxy";
		} else if ( proxyUser == null || proxyUser.isEmpty() ) {
			proxyPart = proxyServer + ":" + proxyPort;
		} else {
			String passSegment;
			if ( proxyPassword == null || proxyPassword.isEmpty() ) {
				passSegment = "nopass";
			} else {
				// First 8 chars of SHA-256 hash — differentiates passwords without exposing them
				passSegment = EncryptionUtil.hash( proxyPassword, "SHA-256" ).substring( 0, 8 );
			}
			proxyPart = proxyServer + ":" + proxyPort + ":" + proxyUser + ":" + passSegment;
		}

		// Certificate segment
		String certPart;
		if ( clientCertPath == null || clientCertPath.isEmpty() ) {
			certPart = "nocert";
		} else {
			// Hash the path to avoid special characters in the key
			String	pathHash	= EncryptionUtil.hash( clientCertPath, "SHA-256" ).substring( 0, 16 );
			String	passLabel	= ( clientCertPass != null && !clientCertPass.isEmpty() ) ? "hascertpass" : "nopass";
			certPart = "cert:" + pathHash + ":" + passLabel;
		}

		return Key.of( "bx-http|" + version + "|" + redirect + "|" + timeout + "|" + proxyPart + "|" + certPart );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * SOAP/WSDL Client Management Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Get or create a SOAP client from a WSDL URL.
	 * This method caches the SoapClient instance for efficient reuse across multiple requests.
	 *
	 * @param wsdlUrl The WSDL URL to parse and create a client for
	 * @param context The BoxLang execution context
	 *
	 * @return A configured SoapClient instance
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.BoxRuntimeException If WSDL parsing or client creation fails
	 */
	public BoxSoapClient getOrCreateSoapClient( String wsdlUrl, IBoxContext context ) {
		// Check if we already have a cached client for this WSDL
		BoxSoapClient cachedClient = this.soapClients.get( wsdlUrl );
		if ( cachedClient != null ) {
			this.logger.trace( "Reusing cached SOAP client for WSDL: {}", wsdlUrl );
			return cachedClient;
		}

		// Double-checked locking for thread safety
		synchronized ( this.soapClients ) {
			cachedClient = this.soapClients.get( wsdlUrl );
			if ( cachedClient != null ) {
				return cachedClient;
			}

			this.logger.trace( "Creating new SOAP client for WSDL: {}", wsdlUrl );

			// Parse the WSDL and create the client
			BoxSoapClient newClient = BoxSoapClient.fromWsdl( wsdlUrl, this, context );

			// Cache and return
			this.soapClients.put( wsdlUrl, newClient );
			this.logger.trace( "Created and cached SOAP client for WSDL: {}", wsdlUrl );

			return newClient;
		}
	}

	/**
	 * Get a cached SOAP client
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return The SoapClient instance, or null if not cached
	 */
	public BoxSoapClient getSoapClient( String wsdlUrl ) {
		return this.soapClients.get( wsdlUrl );
	}

	/**
	 * Check if a SOAP client is cached
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return True if a client is cached for this WSDL
	 */
	public boolean hasSoapClient( String wsdlUrl ) {
		return this.soapClients.containsKey( wsdlUrl );
	}

	/**
	 * Remove a SOAP client from the cache
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return This HttpService instance for method chaining
	 */
	public HttpService removeSoapClient( String wsdlUrl ) {
		this.soapClients.remove( wsdlUrl );
		return this;
	}

	/**
	 * Clear all cached SOAP clients
	 *
	 * @return This HttpService instance for method chaining
	 */
	public HttpService clearAllSoapClients() {
		this.soapClients.clear();
		return this;
	}

	/**
	 * Get the count of cached SOAP clients
	 *
	 * @return The number of cached SOAP clients
	 */
	public int getSoapClientCount() {
		return this.soapClients.size();
	}

	/**
	 * Get statistics for a SOAP client
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return A struct containing client statistics
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.BoxRuntimeException If no client is cached for this WSDL
	 */
	public IStruct getSoapClientStats( String wsdlUrl ) {
		BoxSoapClient client = this.soapClients.get( wsdlUrl );
		if ( client == null ) {
			throw new ortus.boxlang.runtime.types.exceptions.BoxRuntimeException( "No SOAP client found for WSDL: " + wsdlUrl );
		}
		return client.getStatistics();
	}

	/**
	 * Get statistics for all SOAP clients
	 *
	 * @return A struct containing statistics for all clients
	 */
	public IStruct getAllSoapClientStats() {
		IStruct allStats = new Struct( false );
		for ( Map.Entry<String, ortus.boxlang.runtime.net.soap.BoxSoapClient> entry : this.soapClients.entrySet() ) {
			allStats.put( entry.getKey(), entry.getValue().getStatistics() );
		}
		return allStats;
	}

}
