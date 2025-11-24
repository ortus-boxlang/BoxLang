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
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.net.soap.BoxSoapClient;
import ortus.boxlang.runtime.net.soap.WsdlDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * This service manages all HTTP clients in BoxLang.
 * It is responsible for creating, caching, and reusing HTTP clients based on their configuration.
 * It also handles the lifecycle events of the HTTP service within the BoxLang runtime.
 */
public class HttpService extends BaseService {

	/**
	 * Concurrent map that stores all HTTP reusable clients
	 */
	private final ConcurrentMap<Key, BoxHttpClient>										clients						= new ConcurrentHashMap<>();

	/**
	 * Concurrent map that stores all SOAP/WSDL clients
	 */
	private final ConcurrentMap<String, ortus.boxlang.runtime.net.soap.BoxSoapClient>	soapClients					= new ConcurrentHashMap<>();

	/**
	 * Concurrent map that stores parsed WSDL definitions
	 */
	private final ConcurrentMap<String, ortus.boxlang.runtime.net.soap.WsdlDefinition>	wsdlDefinitions				= new ConcurrentHashMap<>();

	/**
	 * Shutdown timeout in seconds
	 */
	private static final Long															SHUTDOWN_TIMEOUT_SECONDS	= 10L;

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
		this.clients.clear();
		this.soapClients.clear();
		this.wsdlDefinitions.clear();
		this.logger.info( "+ Http Service shutdown complete" );
	}

	@Override
	public void onStartup() {
		// Allow for restricted headers to be set so we can send the Host header and Content-Length
		// Java’s HttpClient (introduced in Java 11) blocks setting certain sensitive headers for security reasons
		if ( System.getProperty( "jdk.httpclient.allowRestrictedHeaders" ) == null ) {
			System.setProperty( "jdk.httpclient.allowRestrictedHeaders", "host,content-length" );
		}
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
		this.clients.put( key, client );
		return client;
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

	/**
	 * Get all cached client keys
	 * <p>
	 * This method returns an array of all keys for the cached HTTP clients.
	 *
	 * @return An Array of Keys representing all cached clients
	 */
	public Array getAllClientKeys() {
		Array keys = new Array();
		for ( Key key : this.clients.keySet() ) {
			keys.add( key.getName() );
		}
		return keys;
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
		for ( Key key : this.clients.keySet() ) {
			allStats.put( key.getName(), getClientStats( key ) );
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
		this.clients.clear();
		return this;
	}

	/**
	 * Get or Build the HTTP client associated with the incoming connection details.
	 * <p>
	 * This method will either return an existing cached client or build a new one
	 * based on the provided connection parameters. Clients are cached based on their
	 * configuration to enable connection pooling and reuse.
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

		// Build a unique key for this client configuration
		Key clientKey = buildClientKey(
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

		// Return cached client if it exists
		if ( hasClient( clientKey ) ) {
			synchronized ( this.clients ) {
				if ( hasClient( clientKey ) ) {
					this.logger.debug( "Reusing cached HTTP client with key: {}", clientKey );
					return getClient( clientKey );
				}
			}
		}

		// Build a new HttpClient with the specified configuration
		this.logger.debug( "Building new HTTP client with key: {}", clientKey );

		// Create HttpClient builder
		HttpClient.Builder builder = HttpClient.newBuilder()
		    // Configure Executor
		    .executor( this.httpExecutor.executor() )
		    // Configure redirect policy
		    .followRedirects( followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER )
		    // Configure HTTP version
		    .version( httpVersion.equalsIgnoreCase( BoxHttpClient.HTTP_1 )
		        ? HttpClient.Version.HTTP_1_1
		        : HttpClient.Version.HTTP_2
		    );

		// Configure connect timeout
		if ( connectTimeout != null ) {
			builder.connectTimeout( Duration.ofSeconds( connectTimeout ) );
		}

		// Configure proxy
		if ( proxyServer != null && proxyPort != null ) {
			builder.proxy( ProxySelector.of( new InetSocketAddress( proxyServer, proxyPort ) ) );

			// Configure proxy authentication if credentials provided
			if ( proxyUser != null && proxyPassword != null ) {
				builder.authenticator( new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication( proxyUser, proxyPassword.toCharArray() );
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
		this.logger.debug( "HTTP client created and cached with key: {}", clientKey );
		return putClient( clientKey, new BoxHttpClient( builder.build(), this ) );
	}

	/**
	 * Builds a unique cache key for an HTTP client based on its configuration.
	 * <p>
	 * The key is constructed from all configuration parameters that affect the
	 * underlying HttpClient behavior, ensuring that clients with identical
	 * configurations can be reused.
	 *
	 * @param httpVersion     The HTTP version to use
	 * @param followRedirects Whether to follow redirects
	 * @param connectTimeout  The connection timeout in seconds
	 * @param proxyServer     The proxy server address
	 * @param proxyPort       The proxy server port
	 * @param proxyUser       The proxy authentication username
	 * @param proxyPassword   The proxy authentication password
	 * @param clientCertPath  The path to the client certificate
	 * @param clientCertPass  The client certificate password
	 *
	 * @return A unique Key identifying this client configuration
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
		// Build a composite string from all configuration parameters
		StringBuilder keyBuilder = new StringBuilder();

		keyBuilder.append( "v=" ).append( httpVersion ).append( ";" );
		keyBuilder.append( "redir=" ).append( followRedirects ).append( ";" );
		keyBuilder.append( "timeout=" ).append( connectTimeout != null ? connectTimeout : "none" ).append( ";" );
		// Note: debug flag intentionally excluded - it doesn't affect HttpClient configuration

		// Proxy configuration
		if ( proxyServer != null && proxyPort != null ) {
			keyBuilder.append( "proxy=" ).append( proxyServer ).append( ":" ).append( proxyPort ).append( ";" );
			if ( proxyUser != null ) {
				keyBuilder.append( "proxyAuth=" ).append( proxyUser ).append( ";" );
				// Note: We don't include password in the key for security, but we include a flag
				keyBuilder.append( "proxyPass=" ).append( proxyPassword != null ? "yes" : "no" ).append( ";" );
			}
		}

		// Client certificate configuration
		if ( clientCertPath != null ) {
			keyBuilder.append( "cert=" ).append( clientCertPath ).append( ";" );
			// Note: We don't include password in the key for security, but we include a flag
			keyBuilder.append( "certPass=" ).append( clientCertPass != null ? "yes" : "no" ).append( ";" );
		}

		// Generate SHA-256 hash of the configuration string using EncryptionUtil
		String hash = EncryptionUtil.hash( keyBuilder.toString(), "SHA-256" );

		// Build the key
		return Key.of( "bx-http-" + hash );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * SOAP/WSDL Client Management Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Get or create a SOAP client from a WSDL URL.
	 * This method caches both the parsed WSDL definition and the SoapClient instance
	 * for efficient reuse across multiple requests.
	 *
	 * @param wsdlUrl The WSDL URL to parse and create a client for
	 *
	 * @return A configured SoapClient instance
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.BoxRuntimeException If WSDL parsing or client creation fails
	 */
	public BoxSoapClient getOrCreateSoapClient( String wsdlUrl ) {
		// Check if we already have a cached client for this WSDL
		BoxSoapClient cachedClient = this.soapClients.get( wsdlUrl );
		if ( cachedClient != null ) {
			this.logger.debug( "Reusing cached SOAP client for WSDL: {}", wsdlUrl );
			return cachedClient;
		}

		// Double-checked locking for thread safety
		synchronized ( this.soapClients ) {
			cachedClient = this.soapClients.get( wsdlUrl );
			if ( cachedClient != null ) {
				return cachedClient;
			}

			this.logger.debug( "Creating new SOAP client for WSDL: {}", wsdlUrl );

			// Check if we have a cached WSDL definition
			ortus.boxlang.runtime.net.soap.WsdlDefinition definition = this.wsdlDefinitions.get( wsdlUrl );
			if ( definition != null ) {
				this.logger.debug( "Reusing cached WSDL definition for: {}", wsdlUrl );
			} else {
				// Parse the WSDL and cache the definition
				this.logger.debug( "Parsing WSDL from URL: {}", wsdlUrl );
				definition = ortus.boxlang.runtime.net.soap.WsdlParser.parse( wsdlUrl );
				this.wsdlDefinitions.put( wsdlUrl, definition );
			}

			// Create the SOAP client from the definition
			ortus.boxlang.runtime.net.soap.BoxSoapClient newClient = ortus.boxlang.runtime.net.soap.BoxSoapClient.fromDefinition( definition, this,
			    this.logger );

			// Cache and return
			this.soapClients.put( wsdlUrl, newClient );
			this.logger.info( "Created and cached SOAP client for WSDL: {}", wsdlUrl );

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
	 * Get a cached WSDL definition
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return The WsdlDefinition instance, or null if not cached
	 */
	public WsdlDefinition getWsdlDefinition( String wsdlUrl ) {
		return this.wsdlDefinitions.get( wsdlUrl );
	}

	/**
	 * Check if a WSDL definition is cached
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return True if a definition is cached for this WSDL
	 */
	public boolean hasWsdlDefinition( String wsdlUrl ) {
		return this.wsdlDefinitions.containsKey( wsdlUrl );
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
	 * Remove a WSDL definition from the cache
	 *
	 * @param wsdlUrl The WSDL URL
	 *
	 * @return This HttpService instance for method chaining
	 */
	public HttpService removeWsdlDefinition( String wsdlUrl ) {
		this.wsdlDefinitions.remove( wsdlUrl );
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
	 * Clear all cached WSDL definitions
	 *
	 * @return This HttpService instance for method chaining
	 */
	public HttpService clearAllWsdlDefinitions() {
		this.wsdlDefinitions.clear();
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
	 * Get the count of cached WSDL definitions
	 *
	 * @return The number of cached WSDL definitions
	 */
	public int getWsdlDefinitionCount() {
		return this.wsdlDefinitions.size();
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
