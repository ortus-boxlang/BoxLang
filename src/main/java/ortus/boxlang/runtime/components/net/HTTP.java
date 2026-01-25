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
package ortus.boxlang.runtime.components.net;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Make HTTP requests and handle responses", allowsBody = true )
public class HTTP extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constants
	 * --------------------------------------------------------------------------
	 */
	private static final String			AUTHMODE_BASIC			= "BASIC";
	private static final String			AUTHMODE_NTLM			= "NTLM";
	private static final HttpService	httpService				= BoxRuntime.getInstance().getHttpService();

	/**
	 * Binary request values
	 */
	// @formatter:off
	private static ArrayList<Key>		BINARY_REQUEST_VALUES	= new ArrayList<Key>() {
		{
			add( Key.of( "true" ) );
			add( Key.of( "yes" ) );
		}
	};
	private static Key					BINARY_NEVER			= Key.of( "never" );
	// @formatter:on

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public HTTP() {
		super();
		declaredAttributes = new Attribute[] {
		    // Connection settings
		    new Attribute( Key.URL, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        ( cxt, comp, attr, attrs ) -> {
			        if ( !attrs.getAsString( attr.name() ).startsWith( "http" ) ) {
				        throw new BoxValidationException( comp, attr, "must start with 'http://' or 'https://'" );
			        }
		        }
		    ) ),
		    new Attribute( Key.port, "numeric" ),
		    new Attribute( Key.httpVersion, "string", "HTTP/2", Set.of( Validator.valueOneOf( "HTTP/1.1", "HTTP/2" ) ) ),
		    new Attribute( Key.timeout, "numeric", BoxHttpClient.DEFAULT_REQUEST_TIMEOUT ),
		    new Attribute( Key.connectionTimeout, "numeric", BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT ),
		    new Attribute( Key.redirect, "boolean", true ),
		    new Attribute( Key.resolveUrl, "boolean", false ),
		    new Attribute( Key.encodeUrl, "boolean", true, Set.of( Validator.TYPE ) ),
		    // Request settings
		    new Attribute( Key.method, "string", "GET", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "GET", "POST", "PUT", "DELETE", "HEAD", "TRACE", "OPTIONS", "PATCH" )
		    ) ),
		    new Attribute( Key.userAgent, "string", BoxHttpClient.DEFAULT_USER_AGENT ),
		    new Attribute( Key.charset, "string", StandardCharsets.UTF_8.name() ),
		    new Attribute( Key.compression, "string" ),
		    new Attribute( Key.multipart, "boolean", false, Set.of( Validator.TYPE ) ),
		    new Attribute( Key.multipartType, "string", "form-data",
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( "form-data", "related" ) )
		    ),
		    // Response handling
		    new Attribute( Key.result, "string", "bxhttp", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY
		    ) ),
		    new Attribute( Key.getAsBinary, "string", "auto", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "true", "false", "auto", "no", "yes", "never" )
		    ) ),
		    new Attribute( Key.throwOnError, "boolean", false ),
		    new Attribute( Key.file, "string" ),
		    new Attribute( Key.path, "string" ),
		    // Caching
		    new Attribute( Key.cachedWithin, "string" ),
		    // Authentication
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string", Set.of( Validator.requires( Key.username ) ) ),
		    // Client certificates
		    new Attribute( Key.clientCert, "string" ),
		    new Attribute( Key.clientCertPassword, "string" ),
		    // Streaming/callbacks
		    new Attribute( Key.sse, "boolean", false ),
		    new Attribute( Key.onRequestStart, "function" ),
		    new Attribute( Key.onChunk, "function" ),
		    new Attribute( Key.onMessage, "function" ),
		    new Attribute( Key.onError, "function" ),
		    new Attribute( Key.onComplete, "function" ),
		    // Proxy configuration
		    new Attribute( Key.proxyServer, "string", Set.of( Validator.requires( Key.proxyPort ) ) ),
		    new Attribute( Key.proxyPort, "integer", Set.of( Validator.requires( Key.proxyServer ) ) ),
		    new Attribute( Key.proxyUser, "string", Set.of( Validator.requires( Key.proxyPassword ) ) ),
		    new Attribute( Key.proxyPassword, "string", Set.of( Validator.requires( Key.proxyUser ) ) ),
		    // ----------------------------------------------------------------------------
		    // NON IMPLEMENTED ATTRIBUTES BELOW
		    // Left here so we can track them for future implementation if needed
		    // ----------------------------------------------------------------------------
		    new Attribute( Key._NAME, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    // CSV parsing
		    new Attribute( Key.delimiter, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.columns, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.firstRowAsHeaders, "boolean", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.textQualifier, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    // NTLM
		    new Attribute( Key.authType, "string", AUTHMODE_BASIC,
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( AUTHMODE_BASIC, AUTHMODE_NTLM ) )
		    ),
		    new Attribute( Key.domain, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.workstation, "string", Set.of( Validator.NOT_IMPLEMENTED ) )
		};
	}

	/**
	 * Makes an HTTP request with comprehensive control over request configuration and response handling.
	 * <p>
	 * This component provides a complete HTTP client implementation supporting various HTTP methods,
	 * authentication mechanisms, SSL/TLS client certificates, proxy configuration, streaming responses,
	 * and file download/upload operations. It wraps Java's HttpClient with a BoxLang-friendly interface.
	 * <p>
	 * <b>Basic Usage:</b>
	 *
	 * <pre>
	 * // Simple GET request
	 * bx:http url="https://api.example.com/users" {}
	 * // Result is stored in 'bxhttp' variable by default
	 * println( bxhttp.statusCode ); // 200
	 * println( bxhttp.fileContent ); // Response body
	 * </pre>
	 * <p>
	 * <b>POST with JSON:</b>
	 *
	 * <pre>
	 * bx:http method="POST" url="https://api.example.com/users" result="response" {
	 *     bx:httpparam type="header" name="Content-Type" value="application/json";
	 *     bx:httpparam type="body" value='{"name":"John","email":"john@example.com"}';
	 * }
	 * println( response.statusCode );
	 * </pre>
	 * <p>
	 * <b>File Upload (Multipart):</b>
	 *
	 * <pre>
	 * bx:http method="POST" url="https://api.example.com/upload" multipart=true {
	 *     bx:httpparam type="file" name="document" file="/path/to/document.pdf";
	 *     bx:httpparam type="formfield" name="description" value="Important document";
	 * }
	 * </pre>
	 * <p>
	 * <b>Download File:</b>
	 *
	 * <pre>
	 * bx:http url="https://example.com/file.pdf"
	 *         path="/downloads"
	 *         file="document.pdf"
	 *         getAsBinary="yes" {}
	 * // File saved to /downloads/document.pdf
	 * </pre>
	 * <p>
	 * <b>Authentication:</b>
	 *
	 * <pre>
	 * // Basic Authentication
	 * bx:http url="https://api.example.com/protected"
	 *         username="user"
	 *         password="pass"
	 *         authType="BASIC" {}
	 *
	 * // Client Certificate (Mutual TLS)
	 * bx:http url="https://api.example.com/secure"
	 *         clientCert="/path/to/cert.p12"
	 *         clientCertPassword="certpass" {}
	 * </pre>
	 * <p>
	 * <b>Streaming/Chunked Responses:</b>
	 *
	 * <pre>
	 * bx:http url="https://api.example.com/stream" onChunk=function(chunk) {
	 *     println( "Received chunk ##chunk.chunkNumber: ##chunk.totalReceived bytes" );
	 *     println( chunk.chunk ); // The actual data
	 * } {}
	 * </pre>
	 * <p>
	 * <b>Proxy Configuration:</b>
	 *
	 * <pre>
	 * bx:http url="https://api.example.com/data"
	 *         proxyServer="proxy.company.com"
	 *         proxyPort=8080
	 *         proxyUser="proxyuser"
	 *         proxyPassword="proxypass" {}
	 * </pre>
	 * <p>
	 * <b>Error Handling:</b>
	 *
	 * <pre>
	 * bx:http url="https://api.example.com/data"
	 *         throwOnError=true
	 *         onError=function(error) {
	 *     println( "Request failed: ##error.message" );
	 * } {}
	 * </pre>
	 * <p>
	 * <b>Response Structure (bxhttp variable):</b>
	 * <ul>
	 * <li><code>statusCode</code> - HTTP status code (e.g., 200, 404, 500)</li>
	 * <li><code>statusText</code> - HTTP status text (e.g., "OK", "Not Found")</li>
	 * <li><code>fileContent</code> - Response body as string or binary</li>
	 * <li><code>header</code> - All response headers as a single string</li>
	 * <li><code>responseHeader</code> - Struct containing individual headers</li>
	 * <li><code>charset</code> - Character encoding of the response</li>
	 * <li><code>errorDetail</code> - Error details if request failed</li>
	 * <li><code>mimeType</code> - Content-Type of the response</li>
	 * <li><code>text</code> - Boolean indicating if response is text</li>
	 * <li><code>cookies</code> - Array of cookies from Set-Cookie headers</li>
	 * <li><code>executionTime</code> - Request duration in milliseconds</li>
	 * <li><code>request</code> - Struct containing request details (URL, method, headers, etc.)</li>
	 * </ul>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component (can contain bx:httpparam tags)
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.URL The URL to which to make the HTTP request. Must start with http:// or https://. Required.
	 *
	 * @attribute.port The port number to use for the connection. If not specified, uses the standard port for the protocol (80 for HTTP, 443 for HTTPS).
	 *
	 * @attribute.httpVersion The HTTP protocol version to use. One of "HTTP/1.1" or "HTTP/2". Default is "HTTP/2".
	 *
	 * @attribute.timeout The request timeout in seconds. How long to wait for the server to respond. Default is 60 seconds.
	 *
	 * @attribute.connectionTimeout The connection timeout in seconds. How long to wait when establishing the connection. Default is 30 seconds.
	 *
	 * @attribute.redirect Whether to automatically follow HTTP redirects (3xx status codes). Default is true.
	 *
	 * @attribute.resolveUrl Whether to resolve relative URLs before making the request. Default is false.
	 *
	 * @attribute.encodeUrl Whether to URL-encode the query string parameters. Default is true.
	 *
	 * @attribute.method The HTTP method to use. One of GET, POST, PUT, DELETE, HEAD, TRACE, OPTIONS, PATCH. Default is GET. Required.
	 *
	 * @attribute.userAgent The User-Agent header value to send with the request. Default is "BoxLang-HttpClient/1.0".
	 *
	 * @attribute.charset The character encoding to use for the request and response. Default is "UTF-8".
	 *
	 * @attribute.compression The compression type to accept for the response (e.g., "gzip", "deflate"). Optional.
	 *
	 * @attribute.multipart Whether the request should be sent as multipart/form-data. Used for file uploads. Default is false.
	 *
	 * @attribute.multipartType The type of multipart request. One of "form-data" or "related". Default is "form-data". Required when multipart is true.
	 *
	 * @attribute.result The name of the variable in which to store the HTTP response struct. Default is "bxhttp". Required.
	 *
	 * @attribute.getAsBinary Controls how binary responses are handled. One of "true", "false", "auto", "yes", "no", or "never". Default is "auto". "auto" detects based on content-type, "never" throws an error if binary is received. Required.
	 *
	 * @attribute.throwOnError Whether to throw an exception if the HTTP response status code is 400 or greater. Default is false.
	 *
	 * @attribute.file The filename to use when saving the response to disk. Can be a full path if path attribute is not provided. Optional.
	 *
	 * @attribute.path The directory path where the response file should be saved. If file attribute is not provided, attempts to extract filename from Content-Disposition header. Optional.
	 *
	 * @attribute.cachedWithin If set, uses cached response if available within the specified duration (e.g., "10m" for 10 minutes, "1h" for 1 hour). Optional. (Note: Caching not yet implemented)
	 *
	 * @attribute.username The username for HTTP Basic or NTLM authentication. Optional.
	 *
	 * @attribute.password The password for HTTP Basic or NTLM authentication. Optional.
	 *
	 * @attribute.authType The authentication type to use. One of "BASIC" or "NTLM". Default is "BASIC". Required when username/password provided.
	 *
	 * @attribute.clientCert The file path to the PKCS12 (.p12/.pfx) client certificate for SSL/TLS mutual authentication. Optional.
	 *
	 * @attribute.clientCertPassword The password for the client certificate keystore. Optional.
	 *
	 * @attribute.onRequestStart A callback function called before the HTTP request is sent. Receives a struct with: request (HTTPRequest builder object), url (target URL), method (HTTP method), headers (struct of headers). Useful for logging, modifying
	 *                           request, or performing pre-flight checks. Optional.
	 *
	 * @attribute.onChunk A callback function for streaming/chunked response processing. Receives a struct with: chunk (data), chunkNumber (1-based), totalReceived (bytes), headers (first chunk only), result (HTTPResult struct). Optional.
	 *
	 * @attribute.onError A callback function to handle errors during the HTTP request. Receives a struct with: error (exception), message (error message), result (HTTPResult struct with partial data). Called for both streaming and non-streaming
	 *                    requests. Optional.
	 *
	 * @attribute.onComplete A callback function called when the HTTP request completes successfully. Receives a struct with: result (HTTPResult struct), statusCode, success (boolean). Called after all chunks in streaming mode. Optional.
	 *
	 * @attribute.proxyServer The hostname or IP address of the proxy server. Required if using a proxy.
	 *
	 * @attribute.proxyPort The port number of the proxy server. Required if using a proxy.
	 *
	 * @attribute.proxyUser The username for proxy authentication. Required if proxyPassword is provided.
	 *
	 * @attribute.proxyPassword The password for proxy authentication. Required if proxyUser is provided.
	 *
	 * @return BodyResult containing the execution result
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// Create HTTPParams array in execution state
		executionState.put( Key.HTTPParams, new Array() );
		// Process the component for HTTPParams
		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		// Prep all attributes
		Key				binaryOperator		= Key.of( attributes.getAsString( Key.getAsBinary ) );
		boolean			debug				= BooleanCaster.cast( attributes.getOrDefault( Key.debug, false ) );
		final String	encodedCertKey;
		boolean			isBinaryRequested	= BINARY_REQUEST_VALUES
		    .stream()
		    .anyMatch( value -> value.equals( binaryOperator ) );

		// onMessage is sugar for onChunk + sse=true
		if ( attributes.containsKey( Key.onMessage ) && attributes.get( Key.onMessage ) != null ) {
			attributes.put( Key.onChunk, attributes.get( Key.onMessage ) );
			attributes.put( Key.sse, true );
		}

		// Expand client certificate path if provided
		if ( attributes.containsKey( Key.clientCert ) ) {
			attributes.put( Key.clientCert, FileSystemUtil.expandPath( context, attributes.getAsString( Key.clientCert ) ).absolutePath().toString() );

			// In debug mode, extract certificate info for X-Client-Cert header
			if ( debug ) {
				encodedCertKey = EncryptionUtil.extractCertificateSubject(
				    attributes.getAsString( Key.clientCert ),
				    attributes.getAsString( Key.clientCertPassword )
				);
			} else {
				encodedCertKey = null;
			}
		} else {
			encodedCertKey = null;
		}

		// We allow the `file` attribute to become the full file path if the `path` attribute is empty
		String outputDirectory = attributes.getAsString( Key.path );
		if ( outputDirectory == null && attributes.getAsString( Key.file ) != null ) {
			Path filePath = FileSystemUtil.expandPath( context, attributes.getAsString( Key.file ) ).absolutePath();
			outputDirectory = filePath.getParent().toString();
			attributes.put( Key.file, filePath.getFileName().toString() );
		}

		// Prepare the output directory if it is set
		if ( outputDirectory != null ) {
			ResolvedFilePath outputPath = FileSystemUtil.expandPath( context, outputDirectory );
			if ( outputPath != null ) {
				outputDirectory = outputPath.absolutePath().toString();
			}
		}

		// Get a new or existing BoxHttpClient
		BoxHttpClient	boxHttpClient	= httpService.getOrBuildClient(
		    attributes.getAsString( Key.httpVersion ),
		    attributes.getAsBoolean( Key.redirect ),
		    IntegerCaster.cast( attributes.get( Key.connectionTimeout ) ),
		    attributes.getAsString( Key.proxyServer ),
		    attributes.getAsInteger( Key.proxyPort ),
		    attributes.getAsString( Key.proxyUser ),
		    attributes.getAsString( Key.proxyPassword ),
		    attributes.getAsString( Key.clientCert ),
		    attributes.getAsString( Key.clientCertPassword )
		);

		// Make the HTTP request
		IStruct			result			= ( IStruct ) boxHttpClient
		    // Target URL and invocation context
		    .newRequest( attributes.getAsString( Key.URL ), context )
		    // HTTP Method (GET, POST, PUT, DELETE, etc.)
		    .method( attributes.getAsString( Key.method ) )
		    // Special URL Port if any
		    .port( attributes.getAsInteger( Key.port ) )
		    // Timeout in seconds
		    .timeout( IntegerCaster.cast( attributes.get( Key.timeout ) ) )
		    // Charset for the request
		    .charset( attributes.getAsString( Key.charset ) )
		    // Whether to throw an error if the HTTP response status code is 400 or greater. Default is true.
		    .throwOnError( attributes.getAsBoolean( Key.throwOnError ) )
		    // Debug Mode
		    .debug( BooleanCaster.cast( attributes.getOrDefault( Key.debug, false ) ) )
		    // HTTP Version
		    .httpVersion( attributes.getAsString( Key.httpVersion ) )
		    // User Agent
		    .userAgent( attributes.getAsString( Key.userAgent ) )
		    // Are we multipart?
		    .multipart( attributes.getAsBoolean( Key.multipart ) )
		    // HTTP Params
		    .params( executionState.getAsArray( Key.HTTPParams ) )
		    // Outputs if any
		    .outputDirectory( outputDirectory )
		    .outputFile( attributes.getAsString( Key.file ) )
		    .when( attributes.get( Key.username ) != null && attributes.get( Key.password ) != null, ( request ) -> {
			    request.withBasicAuth(
			        attributes.getAsString( Key.username ),
			        attributes.getAsString( Key.password )
			    );
		    } )
		    // Encoded Client Certificate Header for Debugging
		    .when( debug && encodedCertKey != null, ( request ) -> {
			    request.header( "X-Client-Cert", encodedCertKey );
		    } )
		    // Binary Response Handling
		    .when( isBinaryRequested, ( request ) -> {
			    request.asBinary();
		    } )
		    .when( binaryOperator.equals( BINARY_NEVER ), ( request ) -> {
			    request.asBinaryNever();
		    } )
		    // CallBacks
		    .onRequestStart( attributes.getAsFunction( Key.onRequestStart ) )
		    .onChunk( attributes.getAsFunction( Key.onChunk ) )
		    .onError( attributes.getAsFunction( Key.onError ) )
		    .onComplete( attributes.getAsFunction( Key.onComplete ) )
		    // SSE Mode
		    .sse( attributes.getAsBoolean( Key.sse ) )
		    // Invoke the request
		    .send();

		// Set the result variable before returning
		ExpressionInterpreter.setVariable(
		    context,
		    attributes.getAsString( Key.result ),
		    result
		);

		return bodyResult;
	}
}
