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
import ortus.boxlang.runtime.net.BoxHttpClient.BoxHttpRequest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
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
	private static ArrayList<Key>		BINARY_REQUEST_VALUES	= new ArrayList<Key>() {

																	{
																		add( Key.of( "true" ) );
																		add( Key.of( "yes" ) );
																	}
																};
	private static Key					BINARY_NEVER			= Key.of( "never" );

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
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( "form-data", "related" ) ) ),
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
		    new Attribute( Key.password, "string" ),
		    new Attribute( Key.authType, "string", AUTHMODE_BASIC,
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( AUTHMODE_BASIC, AUTHMODE_NTLM ) ) ),
		    // Client certificates
		    new Attribute( Key.clientCert, "string" ),
		    new Attribute( Key.clientCertPassword, "string" ),
		    // Streaming/callbacks
		    new Attribute( Key.onChunk, "function" ),
		    new Attribute( Key.onError, "function" ),
		    new Attribute( Key.onComplete, "function" ),
		    // Proxy configuration
		    new Attribute( Key.proxyServer, "string", Set.of( Validator.requires( Key.proxyPort ) ) ),
		    new Attribute( Key.proxyPort, "integer", Set.of( Validator.requires( Key.proxyServer ) ) ),
		    new Attribute( Key.proxyUser, "string", Set.of( Validator.requires( Key.proxyPassword ) ) ),
		    new Attribute( Key.proxyPassword, "string", Set.of( Validator.requires( Key.proxyUser ) ) )
		};
	}

	/**
	 * I make an HTTP call using tons of attributes to control the request.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.URL The URL to which to make the HTTP request. Must start with http:// or https://
	 *
	 * @attribute.port The port to which to make the HTTP request. Defaults to the standard port for the protocol (80 for http, 443 for https)
	 *
	 * @attribute.method The HTTP method to use. One of GET, POST, PUT, DELETE, HEAD, TRACE, OPTIONS, PATCH. Default is GET.
	 *
	 * @attribute.username The username to use for authentication, if any.
	 *
	 * @attribute.password The password to use for authentication, if any.
	 *
	 * @attribute.userAgent The User-Agent string to send with the request. Default is "BoxLang".
	 *
	 * @attribute.charset The character set to use for the request. Default is UTF-8.
	 *
	 * @attribute.resolveUrl Whether to resolve the URL before making the request. Default is false.
	 *
	 * @attribute.throwOnError Whether to throw an error if the HTTP response status code is 400 or greater. Default is true.
	 *
	 * @attribute.redirect Whether to follow redirects. Default is true.
	 *
	 * @attribute.timeout The timeout for the request, in seconds. Default is no timeout.
	 *
	 * @attribute.getAsBinary Whether to return the response body as binary. One of true, false, auto, yes, no, never. Default is auto.
	 *
	 * @attribute.result The name of the variable in which to store the result Struct. Default is "bxhttp".
	 *
	 * @attribute.file The name of the file in which to store the response body. If not set, the response body is stored in the result Struct. If not provided with a `path`, the file attribute can be a full path to the file to write.
	 *
	 * @attribute.multipart Whether the request is a multipart request. Default is false.
	 *
	 * @attribute.multipartType The type of multipart request. One of form-data, related. Default is form-data.
	 *
	 * @attribute.clientCertPassword The password for the client certificate, if any.
	 *
	 * @attribute.path The directory in which to store the response file, if any. If a file attribute is not provided, the file name will be extracted from the Content-Disposition header if present. If no disposition header is present with the file name,
	 *                 an error will be thrown
	 *
	 * @attribute.clientCert The path to the client certificate, if any.
	 *
	 * @attribute.compression The compression type to use for the request, if any.
	 *
	 * @attribute.authType The authentication type to use. One of BASIC, NTLM. Default is BASIC.
	 *
	 * @attribute.cachedWithin If set, and a cached response is available within the specified duration (e.g. 10m for 10 minutes, 1h for 1 hour), the cached response will be returned instead of making a new request.
	 *
	 * @attribute.encodeUrl Whether to encode the URL. Default is true.
	 *
	 * @attribute.onChunk A callback function to process response data in chunks/streaming mode. When provided, the response will be processed incrementally. The callback receives a struct with: chunk (string or binary data), chunkNumber (integer,
	 *                    1-based), totalReceived (total bytes received), headers (on first chunk only), and result (HTTPResult struct). If not provided, the response is buffered and returned in the result variable as normal.
	 *
	 * @attribute.onError A callback function to handle errors during the HTTP request. The callback receives a struct with: error (exception object), message (error message), and result (HTTPResult struct with partial data if available). Called for both
	 *                    streaming and non-streaming requests.
	 *
	 * @attribute.onComplete A callback function called when the HTTP request completes successfully. The callback receives a struct with: result (HTTPResult struct), statusCode, and success (boolean). Called after all chunks are processed in streaming
	 *                       mode or after the full response is received in non-streaming mode.
	 *
	 * @attribute.proxyServer The proxy server to use, if any.
	 *
	 * @attribute.proxyPort The proxy server port to use, if any.
	 *
	 * @attribute.proxyUser The proxy server username to use, if any.
	 *
	 * @attribute.proxyPassword The proxy server password to use, if any.
	 *
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
		    attributes.getAsInteger( Key.connectionTimeout ),
		    attributes.getAsString( Key.proxyServer ),
		    attributes.getAsInteger( Key.proxyPort ),
		    attributes.getAsString( Key.proxyUser ),
		    attributes.getAsString( Key.proxyPassword ),
		    attributes.getAsString( Key.clientCert ),
		    attributes.getAsString( Key.clientCertPassword )
		);

		// Make the HTTP request
		BoxHttpRequest	httpRequest		= boxHttpClient
		    // Target URL and invocation context
		    .newRequest( attributes.getAsString( Key.URL ), context )
		    // HTTP Method (GET, POST, PUT, DELETE, etc.)
		    .method( attributes.getAsString( Key.method ) )
		    // Special URL Port if any
		    .port( attributes.getAsInteger( Key.port ) )
		    // Timeout in seconds
		    .timeout( attributes.getAsInteger( Key.timeout ) )
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
		    .onChunk( attributes.getAsFunction( Key.onChunk ) )
		    .onError( attributes.getAsFunction( Key.onError ) )
		    .onComplete( attributes.getAsFunction( Key.onComplete ) )
		    // Invoke the request
		    .invoke()
		    // Handle failures
		    .ifFailed( ( exception, result ) -> {
			    // Check the status code, if it's >= 400 and throwOnError is true, rethrow
			    int statusCode = IntegerCaster.cast( result.get( Key.statusCode ) );
			    if ( attributes.getAsBoolean( Key.throwOnError ) && statusCode >= 400 ) {
				    throw new BoxRuntimeException(
				        "HTTP request failed with status code " + statusCode,
				        exception
				    );
			    }
		    } );

		// Set the result variable before returning
		ExpressionInterpreter.setVariable(
		    context,
		    attributes.getAsString( Key.result ),
		    httpRequest.getHttpResult()
		);

		return bodyResult;
	}
}
