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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	public static final String				HTTP_1						= "HTTP/1.1";
	public static final String				HTTP_2						= "HTTP/2";
	public static final String				DEFAULT_USER_AGENT			= "BoxLang-HttpClient/1.0";
	public static final String				DEFAULT_CHARSET				= StandardCharsets.UTF_8.name();
	public static final String				DEFAULT_METHOD				= "GET";
	public static final int					DEFAULT_CONNECTION_TIMEOUT	= 15;
	public static final int					DEFAULT_REQUEST_TIMEOUT		= 0;
	public static final boolean				DEFAULT_THROW_ON_ERROR		= true;

	/**
	 * ------------------------------------------------------------------------------
	 * Static Services
	 * ------------------------------------------------------------------------------
	 */

	private static final BoxRuntime			runtime						= BoxRuntime.getInstance();
	private static final InterceptorService	interceptorService			= runtime.getInterceptorService();

	/**
	 * ------------------------------------------------------------------------------
	 * Client Properties
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * The underlying HttpClient used for making HTTP requests.
	 */
	private final HttpClient				httpClient;

	/**
	 * The HttpService that manages this client.
	 */
	private final HttpService				httpService;

	/**
	 * The Logger used for logging HTTP operations.
	 */
	private final BoxLangLogger				logger;

	/**
	 * Tracks the last date + time the client was used.
	 * Multiple threads may access this, so it should be handled carefully.
	 */
	private volatile Instant				lastUsedTimestamp;

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
	 * @return The Instant when this client was last used
	 */
	public Instant getLastUsedTimestamp() {
		return this.lastUsedTimestamp;
	}

	/**
	 * Update the last used timestamp to now.
	 */
	public void updateLastUsedTimestamp() {
		this.lastUsedTimestamp = Instant.now();
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
		 */
		public BoxHttpRequest charset( String charset ) {
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
			param.put( ortus.boxlang.runtime.scopes.Key._name, name );
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
			param.put( ortus.boxlang.runtime.scopes.Key._name, name );
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

					// @TODO move URLEncoder.encode usage a non-deprecated method
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
				// Mark the start time of the request
				this.startTime = new DateTime();
				// This prepares the request, forms, files, etc.
				// The request builder is available as this.targetHttpRequest
				prepareRequest();

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

				// Send the HTTP Request asynchronously
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
				        Key.httpResult, this.httpResult
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
			} catch ( ExecutionException e ) {
				Throwable innerException = e.getCause();
				this.error				= true;
				this.requestException	= e;
				this.errorMessage		= e.getMessage();

				// Handle connection exceptions
				if ( innerException instanceof SocketException ) {
					this.httpResult.put( Key.responseHeader, new Struct( false ) );
					this.httpResult.put( Key.header, "" );
					this.httpResult.put( Key.statusCode, 502 );
					this.httpResult.put( Key.status_code, 502 );
					this.httpResult.put( Key.statusText, "Bad Gateway" );
					this.httpResult.put( Key.status_text, "Bad Gateway" );
					this.httpResult.put( Key.fileContent, "Connection Failure" );
					if ( innerException instanceof ConnectException ) {
						if ( this.targetHttpRequest.uri() != null ) {
							this.httpResult.put( Key.errorDetail,
							    String.format( "Unknown host: %s: Name or service not known.", this.targetHttpRequest.uri().getHost() ) );
						} else {
							this.httpResult.put( Key.errorDetail,
							    String.format( "Unknown host: %s: Name or service not known.", this.targetHttpRequest.uri().toString() ) );
						}
					} else {
						this.httpResult.put( Key.errorDetail, "Connection Failure: " + innerException.getMessage() );
					}
					// Call onError callback if provided before re-throwing
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { innerException, this.httpResult } );
					}
				}
				// Handle timeout exceptions
				else if ( innerException instanceof HttpTimeoutException ) {
					this.httpResult.put( Key.responseHeader, new Struct() );
					this.httpResult.put( Key.header, "" );
					this.httpResult.put( Key.statusCode, 408 );
					this.httpResult.put( Key.status_code, 408 );
					this.httpResult.put( Key.statusText, "Request Timeout" );
					this.httpResult.put( Key.status_text, "Request Timeout" );
					this.httpResult.put( Key.fileContent, "Request Timeout" );
					this.httpResult.put( Key.errorDetail, "The request timed out after " + this.timeout + " second(s)" );
					// Call onError callback if provided before re-throwing
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { innerException, this.httpResult } );
					}
				} else {
					// Call onError callback if provided before re-throwing
					if ( onErrorCallback != null ) {
						context.invokeFunction( onErrorCallback, new Object[] { e, this.httpResult } );
					}
				}
			} catch ( InterruptedException e ) {
				// interrupt the thread
				Thread.currentThread().interrupt();
				this.error				= true;
				this.errorMessage		= e.getMessage();
				this.requestException	= e;
			}
			// Rarely occurs since we handle most IOExceptions in the ExecutionException block
			catch ( URISyntaxException | IOException e ) {
				if ( this.onErrorCallback != null ) {
					context.invokeFunction(
					    this.onErrorCallback,
					    new Object[] { e, this.httpResult }
					);
				}
				this.error				= true;
				this.errorMessage		= e.getMessage();
				this.requestException	= e;
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
		 * Process a buffered HTTP response (non-streaming)
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
			// Get the response Headers and convert them to a struct
			HttpHeaders	httpHeaders		= Optional.ofNullable( response.headers() )
			    .orElse( HttpHeaders.of( Map.of(), ( a, b ) -> true ) );
			IStruct		headers			= HttpResponseHelper.transformToResponseHeaderStruct( httpHeaders.map() );

			// Determine binary response handling
			byte[]		responseBytes	= response.body();
			Object		responseBody	= null;

			// Process body if not null
			if ( responseBytes != null ) {
				String	contentType		= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );
				String	contentEncoding	= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentEncoding );

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

				Boolean	isBinaryContentType	= FileSystemUtil.isBinaryMimeType( contentType );
				String	charset				= null;
				if ( ( this.isBinaryRequest || isBinaryContentType ) && !this.isBinaryNever ) {
					responseBody = responseBytes;
				} else if ( this.isBinaryNever && isBinaryContentType ) {
					throw new BoxRuntimeException( "The response is a binary type, but the getAsBinary attribute was set to 'never'" );
				} else {
					charset			= contentType != null && contentType.contains( "charset=" )
					    ? HttpResponseHelper.extractCharset( contentType )
					    : this.charset;
					responseBody	= new String( responseBytes, Charset.forName( charset ) );
				}

				// Prepare all the result variables now that we have the response
				String	httpVersionString	= response.version() == HttpClient.Version.HTTP_1_1 ? BoxHttpClient.HTTP_1 : BoxHttpClient.HTTP_2;
				String	statusCodeString	= String.valueOf( response.statusCode() );
				String	statusText			= HttpStatusReasons.getReason( response.statusCode() );

				// Process The response headers
				headers.put( Key.HTTP_Version, httpVersionString );
				headers.put( Key.status_code, statusCodeString );
				headers.put( Key.explanation, statusText );
				this.httpResult.put( Key.responseHeader, headers );
				this.httpResult.put( Key.header,
				    HttpResponseHelper.generateHeaderString( HttpResponseHelper.generateStatusLine( httpVersionString, statusCodeString, statusText ),
				        headers ) );

				// Fill out the rest of the httpResult struct
				this.httpResult.put( Key.HTTP_Version, httpVersionString );
				this.httpResult.put( Key.statusCode, response.statusCode() );
				this.httpResult.put( Key.status_code, response.statusCode() );
				this.httpResult.put( Key.statusText, statusText );
				this.httpResult.put( Key.status_text, statusText );
				this.httpResult.put( Key.fileContent, response.statusCode() == 408 ? "Request Timeout" : responseBody );
				this.httpResult.put( Key.errorDetail, response.statusCode() == 408 ? response.body() : "" );
				this.httpResult.put( Key.cookies, HttpResponseHelper.generateCookiesQuery( headers ) );
				this.httpResult.put( Key.executionTime, Duration.between( this.startTime.toInstant(), Instant.now() ).toMillis() );
				this.httpResult.put( Key.mimetype, "" );

				// Determine Content-Type and Charset
				Optional<String> contentTypeHeader = httpHeaders.firstValue( "Content-Type" );
				contentTypeHeader.ifPresent( ( headerContentType ) -> {
					String[] contentTypeParts = headerContentType.split( ";\s*" );
					if ( contentTypeParts.length > 0 ) {
						this.httpResult.put( Key.mimetype, contentTypeParts[ 0 ] );
					}
					if ( contentTypeParts.length > 1 ) {
						this.httpResult.put( Key.charset, HttpResponseHelper.extractCharset( headerContentType ) );
					}
				} );

				// Determine if the response is text based on Content-Type
				boolean isText = false;
				if ( contentType == null || contentType.isEmpty() ) {
					// No content type specified = text
					isText = true;
				} else {
					String lowerContentType = contentType.toLowerCase();
					isText = lowerContentType.startsWith( "text" )
					    || lowerContentType.startsWith( "message" )
					    || lowerContentType.equals( "application/octet-stream" );
				}
				this.httpResult.put( Key.text, isText );

				// Handle output file saving if specified
				if ( this.outputDirectory != null ) {
					if ( this.outputFile == null || this.outputFile.trim().isEmpty() ) {
						// If we do not have a filename, try to extract it from the Content-Disposition header
						String dispositionHeader = HttpResponseHelper.extractFirstHeaderByName( headers, Key.of( "content-disposition" ) );
						if ( dispositionHeader != null ) {
							Pattern	pattern	= Pattern.compile( "filename=\"?([^\";]+)\"?" );
							Matcher	matcher	= pattern.matcher( dispositionHeader );
							if ( matcher.find() ) {
								this.outputFile = matcher.group( 1 );
							}
						}
						// Fallback to extracting from the URL path
						else {
							this.outputFile = Path.of( this.targetHttpRequest.uri().getPath() ).getFileName().toString();
						}

						// Final Check
						if ( this.outputFile == null || this.outputFile.trim().isEmpty() ) {
							throw new BoxRuntimeException( "Unable to determine filename from response" );
						}
					}

					String destinationPath = Path.of( this.outputDirectory, this.outputFile ).toAbsolutePath().toString();

					if ( responseBody instanceof String responseString ) {
						FileSystemUtil.write( destinationPath, responseString, charset, true );
					} else if ( responseBody instanceof byte[] bodyBytes ) {
						FileSystemUtil.write( destinationPath, bodyBytes, true );
					}
				}
			}

			return responseBody;
		}

	}

}
