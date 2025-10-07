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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.net.HTTPStatusReasons;
import ortus.boxlang.runtime.net.HttpManager;
import ortus.boxlang.runtime.net.HttpRequestMultipartBody;
import ortus.boxlang.runtime.net.URIBuilder;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.util.ZipUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Make HTTP requests and handle responses", allowsBody = true )
public class HTTP extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constants
	 * --------------------------------------------------------------------------
	 */
	private final static String		BASIC_AUTH_DELIMITER	= ":";
	private static final String		AUTHMODE_BASIC			= "BASIC";
	private static final String		AUTHMODE_NTLM			= "NTLM";

	protected static ArrayList<Key>	BINARY_REQUEST_VALUES	= new ArrayList<Key>() {

																{
																	add( Key.of( "true" ) );
																	add( Key.of( "yes" ) );
																}
															};

	protected static Key			BINARY_NEVER			= Key.of( "never" );

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
		    new Attribute( Key.method, "string", "GET", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "GET", "POST", "PUT", "DELETE", "HEAD", "TRACE", "OPTIONS", "PATCH" )
		    ) ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
		    new Attribute( Key.userAgent, "string", "BoxLang" ),
		    new Attribute( Key.charset, "string", "UTF-8" ),
		    new Attribute( Key.resolveUrl, "boolean", false ),
		    new Attribute( Key.throwOnError, "boolean", true ),
		    new Attribute( Key.redirect, "boolean", true ),
		    new Attribute( Key.timeout, "numeric", Set.of( Validator.min( 1 ) ) ),
		    new Attribute( Key.getAsBinary, "string", "auto", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "true", "false", "auto", "no", "yes", "never" )
		    ) ),
		    new Attribute( Key.result, "string", "bxhttp", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY
		    ) ),
		    new Attribute( Key.file, "string" ),
		    new Attribute( Key.multipart, "boolean", false, Set.of( Validator.TYPE ) ),
		    new Attribute( Key.multipartType, "string", "form-data",
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( "form-data", "related" ) ) ),
		    new Attribute( Key.clientCertPassword, "string" ),
		    new Attribute( Key.path, "string" ),
		    new Attribute( Key.clientCert, "string" ),
		    new Attribute( Key.compression, "string" ),
		    new Attribute( Key.authType, "string", AUTHMODE_BASIC,
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( AUTHMODE_BASIC, AUTHMODE_NTLM ) ) ),
		    new Attribute( Key.cachedWithin, "string" ),
		    new Attribute( Key.encodeUrl, "boolean", true, Set.of( Validator.TYPE ) ),
		    // Proxy server
		    new Attribute( Key.proxyServer, "string", Set.of( Validator.requires( Key.proxyPort ) ) ),
		    new Attribute( Key.proxyPort, "integer", Set.of( Validator.requires( Key.proxyServer ) ) ),
		    new Attribute( Key.proxyUser, "string", Set.of( Validator.requires( Key.proxyPassword ) ) ),
		    new Attribute( Key.proxyPassword, "string", Set.of( Validator.requires( Key.proxyUser ) ) ),
		    // Currently unimplemented attributes
		    // Alt name for result
		    new Attribute( Key._NAME, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    // CSV parsing
		    new Attribute( Key.delimiter, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.columns, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.firstRowAsHeaders, "boolean", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.textQualifier, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    // NTLM
		    new Attribute( Key.domain, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.workstation, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.httpVersion, "string", "HTTP/2", Set.of( Validator.valueOneOf( "HTTP/1.1", "HTTP/2" ) ) )
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
		// Allow for restricted headers to be set so we can send the Host header and Content-Length
		// Java’s HttpClient (introduced in Java 11) blocks setting certain sensitive headers for security reasons
		if ( System.getProperty( "jdk.httpclient.allowRestrictedHeaders" ) == null ) {
			System.setProperty( "jdk.httpclient.allowRestrictedHeaders", "host,content-length" );
		}

		// Keeps track of the HTTPParams
		executionState.put( Key.HTTPParams, new Array() );

		// Process the component for HTTPParams
		BodyResult bodyResult = processBody( context, body );

		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		// Prepare invocation of the HTTP request
		Key		binaryOperator		= Key.of( attributes.getAsString( Key.getAsBinary ) );
		Boolean	isBinaryRequested	= BINARY_REQUEST_VALUES
		    .stream()
		    .anyMatch( value -> value.equals( binaryOperator ) );
		Boolean	isBinaryNever		= binaryOperator.equals( BINARY_NEVER );
		String	variableName		= attributes.getAsString( Key.result );
		String	theURL				= attributes.getAsString( Key.URL );
		String	method				= attributes.getAsString( Key.method ).toUpperCase();
		Array	params				= executionState.getAsArray( Key.HTTPParams );
		Struct	HTTPResult			= new Struct();
		URI		targetURI			= null;
		String	authMode			= attributes.getAsString( Key.authType ).toUpperCase();
		String	outputDirectory		= attributes.getAsString( Key.path );
		String	requestID			= UUID.randomUUID().toString();
		Instant	startTime			= Instant.now();
		if ( attributes.containsKey( Key.clientCert ) ) {
			attributes.put( Key.clientCert, FileSystemUtil.expandPath( context, attributes.getAsString( Key.clientCert ) ).absolutePath().toString() );
		}

		// We allow the `file` attribute to become the full file path if the `path` attribute is empty
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

		HttpClient.Version httpVersion = attributes.getAsString( Key.httpVersion ).equalsIgnoreCase( "HTTP/1.1" )
		    ? HttpClient.Version.HTTP_1_1
		    : HttpClient.Version.HTTP_2;

		try {
			HttpRequest.Builder	builder		= HttpRequest.newBuilder();
			URIBuilder			uriBuilder	= new URIBuilder( theURL );
			if ( attributes.get( Key.port ) != null ) {
				uriBuilder.setPort( attributes.getAsInteger( Key.port ) );
			}
			HttpRequest.BodyPublisher	bodyPublisher	= null;
			List<IStruct>				formFields		= new ArrayList<>();
			List<IStruct>				files			= new ArrayList<>();

			// Basic metadata for the request and results
			builder
			    .version( httpVersion )
			    .header( "User-Agent", attributes.getAsString( Key.userAgent ) )
			    .header( "Accept", "*/*" )
			    .header( "Accept-Encoding", "gzip, deflate" )
			    .header( "x-request-id", requestID );

			// Set the values into the HTTPResult Struct
			HTTPResult.put( Key.requestID, requestID );
			HTTPResult.put( Key.userAgent, attributes.getAsString( Key.userAgent ) );

			// Set username/password if they are set
			if ( attributes.get( Key.username ) != null && attributes.get( Key.password ) != null ) {
				if ( authMode.equals( AUTHMODE_BASIC ) ) {
					String	auth		= ( attributes.get( Key.username ) != null ? StringCaster.cast( attributes.get( Key.username ) ) : null )
					    + BASIC_AUTH_DELIMITER
					    + StringCaster.cast( attributes.getOrDefault( Key.password, "" ) );
					String	encodedAuth	= Base64.getEncoder().encodeToString( auth.getBytes() );
					builder.header( "Authorization", "Basic " + encodedAuth );
				} else if ( authMode.equals( AUTHMODE_NTLM ) ) {
					// TODO: This will need to be implemented as separate type of smb request
					throw new BoxRuntimeException( "NTLM authentication is not currently supported." );
				}
			}

			// Process the HTTPParams
			for ( Object p : params ) {
				IStruct	param	= StructCaster.cast( p );
				String	type	= StringCaster.cast( param.get( Key.type ) );
				switch ( type.toLowerCase() ) {
					// We need to use `setHeader` to overwrite any previously set headers
					case "header" -> {
						String headerName = StringCaster.cast( param.get( Key._NAME ) );
						// We need to downgrade our HTTP version if a TE header is present and is not `trailers`
						// because HTTP/2 does not support the TE header with any other values
						if ( headerName.equalsIgnoreCase( "TE" ) && !StringCaster.cast( param.get( Key.value ) ).equalsIgnoreCase( "trailers" ) ) {
							httpVersion = HttpClient.Version.HTTP_1_1;
							builder.version( httpVersion );
						}
						builder.setHeader( headerName, StringCaster.cast( param.get( Key.value ) ) );
					}
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
					case "xml" -> {
						if ( bodyPublisher != null ) {
							throw new BoxRuntimeException( "Cannot use a xml httpparam with an existing http body: " + bodyPublisher.toString() );
						}
						builder.header( "Content-Type", "text/xml" );
						bodyPublisher = HttpRequest.BodyPublishers.ofString( StringCaster.cast( param.get( Key.value ) ) );
					}
					// @TODO move URLEncoder.encode usage a non-deprecated method
					case "cgi" -> builder.header( StringCaster.cast( param.get( Key._NAME ) ),
					    BooleanCaster.cast( param.getOrDefault( Key.encoded, false ) )
					        ? EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ) )
					        : StringCaster.cast( param.get( Key.value ) )
					);
					case "file" -> files.add( param );
					case "url" -> uriBuilder.addParameter(
					    StringCaster.cast( param.get( Key._NAME ) ),
					    BooleanCaster.cast( param.getOrDefault( Key.encoded, true ) )
					        ? EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ), StandardCharsets.UTF_8 )
					        : StringCaster.cast( param.get( Key.value ) )
					);
					case "formfield" -> formFields.add( param );
					case "cookie" -> builder.header( "Cookie",
					    StringCaster.cast( param.get( Key._NAME ) ) + "="
					        + EncryptionUtil.urlEncode( StringCaster.cast( param.get( Key.value ) ), StandardCharsets.UTF_8 ) );
					default -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
				}
			}

			// Process Files
			if ( !files.isEmpty() ) {
				if ( bodyPublisher != null ) {
					throw new BoxRuntimeException( "Cannot use a multipart body with an existing http body: " + bodyPublisher.toString() );
				}
				HttpRequestMultipartBody.Builder multipartBodyBuilder = new HttpRequestMultipartBody.Builder();
				for ( IStruct param : files ) {
					ResolvedFilePath	path		= FileSystemUtil.expandPath( context, StringCaster.cast( param.get( Key.file ) ) );
					File				file		= path.absolutePath().toFile();
					String				mimeType	= Optional.ofNullable( param.getAsString( Key.mimetype ) )
					    .orElseGet( () -> URLConnection.getFileNameMap().getContentTypeFor( file.getName() ) );
					multipartBodyBuilder.addPart( StringCaster.cast( param.get( Key._name ) ), file, mimeType, file.getName() );
				}

				for ( IStruct formField : formFields ) {
					multipartBodyBuilder.addPart( StringCaster.cast( formField.get( Key._name ) ), StringCaster.cast( formField.get( Key.value ) ) );
				}
				HttpRequestMultipartBody multipartBody = multipartBodyBuilder.build();
				builder.header( "Content-Type", multipartBody.getContentType() );
				bodyPublisher = HttpRequest.BodyPublishers.ofByteArray( multipartBody.getBody() );
			}
			// Process Form Fields
			else if ( !formFields.isEmpty() ) {
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
				builder.header( "Content-Type", "application/x-www-form-urlencoded" );
			}

			if ( bodyPublisher == null ) {
				bodyPublisher = HttpRequest.BodyPublishers.noBody();
			}

			builder.method( method, bodyPublisher );
			targetURI = uriBuilder.build();
			builder.uri( targetURI );

			if ( attributes.containsKey( Key.timeout ) ) {
				builder.timeout( Duration.ofSeconds( attributes.getAsInteger( Key.timeout ) ) );
			}

			// Create a default HTTP Client or a Proxy based Client
			HttpClient client = attributes.containsKey( Key.clientCert ) || attributes.containsKey( Key.proxyServer )
			    || !attributes.getAsBoolean( Key.redirect )
			        ? HttpManager.getCustomClient( attributes )
			        : HttpManager.getClient();

			// Append our debug cert header if in debug mode and the cert has been assigned
			if ( BooleanCaster.cast( attributes.getOrDefault( Key.debug, false ) ) && attributes.containsKey( HttpManager.encodedCertKey ) ) {
				builder.header( "X-Client-Cert", attributes.getAsString( HttpManager.encodedCertKey ) );
			}

			HttpRequest	targetHTTPRequest	= builder.build();

			// Announce the HTTP request
			final var	finalTargetURI		= targetURI;
			interceptorService.announce( BoxEvent.ON_HTTP_REQUEST, () -> Struct.ofNonConcurrent(
			    Key.requestID, requestID,
			    Key.httpClient, client,
			    Key.httpRequest, targetHTTPRequest,
			    Key.targetURI, finalTargetURI,
			    Key.attributes, attributes
			) );

			// Adding the request
			HTTPResult.put(
			    Key.request,
			    Struct.of(
			        Key.URL, targetURI,
			        Key.method, method,
			        Key.timeout, targetHTTPRequest.timeout(),
			        Key.multipart, attributes.getAsBoolean( Key.multipart ),
			        Key.headers, Struct.fromMap( targetHTTPRequest.headers().map() )
			    ) );

			// TODO : should we move the catch block below and add an `exceptionally` handler for the future?
			CompletableFuture<HttpResponse<byte[]>>	future		= client.sendAsync( targetHTTPRequest, HttpResponse.BodyHandlers.ofByteArray() );

			// Announce the HTTP request
			HttpResponse<byte[]>					response	= future.get(); // block and wait for the response

			// Announce the HTTP RAW response
			// Useful for debugging and pre-processing and timing, since the other events are after the response is processed
			interceptorService.announce( BoxEvent.ON_HTTP_RAW_RESPONSE, () -> Struct.ofNonConcurrent(
			    Key.requestID, requestID,
			    Key.response, response,
			    Key.httpClient, client,
			    Key.httpRequest, targetHTTPRequest,
			    Key.targetURI, finalTargetURI,
			    Key.attributes, attributes
			) );

			// Start Processing Results
			HttpHeaders	httpHeaders		= Optional.ofNullable( response.headers() )
			    .orElse( HttpHeaders.of( Map.of(), ( a, b ) -> true ) );
			IStruct		headers			= transformToResponseHeaderStruct( httpHeaders.map() );
			byte[]		responseBytes	= response.body();
			Object		responseBody	= null;

			// Process body if not null
			if ( responseBytes != null ) {
				String	contentType		= headers.getAsString( Key.of( "content-type" ) );
				String	contentEncoding	= headers.getAsString( Key.of( "content-encoding" ) );

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
				if ( ( isBinaryRequested || isBinaryContentType ) && !isBinaryNever ) {
					responseBody = responseBytes;
				} else if ( isBinaryNever && isBinaryContentType ) {
					throw new BoxRuntimeException( "The response is a binary type, but the getAsBinary attribute was set to 'never'" );
				} else {
					charset			= contentType != null && contentType.contains( "charset=" )
					    ? extractCharset( contentType )
					    : "UTF-8";
					responseBody	= new String( responseBytes, Charset.forName( charset ) );
				}

				// Prepare all the result variables now that we have the response
				String	httpVersionString	= response.version() == HttpClient.Version.HTTP_1_1 ? "HTTP/1.1" : "HTTP/2";
				String	statusCodeString	= String.valueOf( response.statusCode() );
				String	statusText			= HTTPStatusReasons.getReasonForStatus( response.statusCode() );

				headers.put( Key.HTTP_Version, httpVersionString );
				headers.put( Key.status_code, statusCodeString );
				headers.put( Key.explanation, statusText );

				HTTPResult.put( Key.responseHeader, headers );
				HTTPResult.put( Key.header, generateHeaderString( generateStatusLine( httpVersionString, statusCodeString, statusText ), headers ) );
				HTTPResult.put( Key.HTTP_Version, httpVersionString );
				HTTPResult.put( Key.statusCode, response.statusCode() );
				HTTPResult.put( Key.status_code, response.statusCode() );
				HTTPResult.put( Key.statusText, statusText );
				HTTPResult.put( Key.status_text, statusText );
				HTTPResult.put( Key.fileContent, response.statusCode() == 408 ? "Request Timeout" : responseBody );
				HTTPResult.put( Key.errorDetail, response.statusCode() == 408 ? response.body() : "" );
				Optional<String> contentTypeHeader = httpHeaders.firstValue( "Content-Type" );
				contentTypeHeader.ifPresent( ( headerContentType ) -> {
					String[] contentTypeParts = headerContentType.split( ";\s*" );
					if ( contentTypeParts.length > 0 ) {
						HTTPResult.put( Key.mimetype, contentTypeParts[ 0 ] );
					}
					if ( contentTypeParts.length > 1 ) {
						HTTPResult.put( Key.charset, extractCharset( headerContentType ) );
					}
				} );
				HTTPResult.put( Key.cookies, generateCookiesQuery( headers ) );
				HTTPResult.put( Key.executionTime, Duration.between( startTime, Instant.now() ).toMillis() );

				// Set the result back into the caller using the variable name
				ExpressionInterpreter.setVariable( context, variableName, HTTPResult );

				// Announce the HTTP response
				interceptorService.announce( BoxEvent.ON_HTTP_RESPONSE, () -> Struct.ofNonConcurrent(
				    Key.requestID, requestID,
				    Key.response, response,
				    Key.result, HTTPResult
				) );

				if ( outputDirectory != null ) {
					String fileName = attributes.getAsString( Key.file );
					if ( fileName == null || fileName.trim().isEmpty() ) {
						String dispositionHeader = headers.getAsString( Key.of( "content-disposition" ) );
						if ( dispositionHeader != null ) {
							Pattern	pattern	= Pattern.compile( "filename=\"?([^\";]+)\"?" );
							Matcher	matcher	= pattern.matcher( dispositionHeader );
							if ( matcher.find() ) {
								fileName = matcher.group( 1 );
							}
						} else {
							fileName = Path.of( targetURI.getPath() ).getFileName().toString();
						}

						if ( fileName == null || fileName.trim().isEmpty() ) {
							throw new BoxRuntimeException( "Unable to determine filename from response" );
						}
					}

					String destinationPath = Path.of( outputDirectory, fileName ).toAbsolutePath().toString();

					if ( responseBody instanceof String responseString ) {
						FileSystemUtil.write( destinationPath, responseString, charset, true );
					} else if ( responseBody instanceof byte[] bodyBytes ) {
						FileSystemUtil.write( destinationPath, bodyBytes, true );
					}
				}
			}

			return DEFAULT_RETURN;
		} catch ( ExecutionException e ) {
			Throwable innerException = e.getCause();
			if ( innerException instanceof SocketException ) {
				HTTPResult.put( Key.responseHeader, Struct.EMPTY );
				HTTPResult.put( Key.header, "" );
				HTTPResult.put( Key.statusCode, 502 );
				HTTPResult.put( Key.status_code, 502 );
				HTTPResult.put( Key.statusText, "Bad Gateway" );
				HTTPResult.put( Key.status_text, "Bad Gateway" );
				HTTPResult.put( Key.fileContent, "Connection Failure" );
				if ( innerException instanceof ConnectException ) {
					if ( targetURI != null ) {
						HTTPResult.put( Key.errorDetail, String.format( "Unknown host: %s: Name or service not known.", targetURI.getHost() ) );
					} else {
						HTTPResult.put( Key.errorDetail, String.format( "Unknown host: %s: Name or service not known.", theURL ) );
					}
				} else {
					HTTPResult.put( Key.errorDetail, "Connection Failure: " + innerException.getMessage() );
				}
				ExpressionInterpreter.setVariable( context, variableName, HTTPResult );
			} else if ( innerException instanceof HttpTimeoutException ) {
				HTTPResult.put( Key.responseHeader, Struct.EMPTY );
				HTTPResult.put( Key.header, "" );
				HTTPResult.put( Key.statusCode, 408 );
				HTTPResult.put( Key.status_code, 408 );
				HTTPResult.put( Key.statusText, "Request Timeout" );
				HTTPResult.put( Key.status_text, "Request Timeout" );
				HTTPResult.put( Key.fileContent, "Request Timeout" );
				HTTPResult.put( Key.errorDetail, "The request timed out after " + attributes.getAsInteger( Key.timeout ) + " second(s)" );
				ExpressionInterpreter.setVariable( context, variableName, HTTPResult );
			} else {
				// retrhow any unknown exception types
				throw new BoxRuntimeException( "An error occurred while processing the HTTP request", "ExecutionException", innerException );
			}
			return DEFAULT_RETURN;
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new BoxRuntimeException( "The request was interrupted", "InterruptedException", e );
		} catch ( URISyntaxException | IOException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Generate a Query of cookies from the headers
	 *
	 * @param headers The headers to parse
	 *
	 * @return A Query of cookies
	 */
	private Query generateCookiesQuery( IStruct headers ) {
		Query cookies = new Query();
		cookies.addColumn( Key._NAME, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.value, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.path, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.domain, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.expires, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.secure, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.httpOnly, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.samesite, QueryColumnType.VARCHAR );

		Object				cookieValue		= headers.getOrDefault( Key.of( "Set-Cookie" ), new Array() );
		CastAttempt<Array>	isValuesArray	= ArrayCaster.attempt( cookieValue );
		if ( isValuesArray.wasSuccessful() ) {
			Array values = isValuesArray.getOrFail();
			for ( Object value : values ) {
				parseCookieStringIntoQuery( StringCaster.cast( value ), cookies );
			}
		} else {
			parseCookieStringIntoQuery( StringCaster.cast( cookieValue ), cookies );
		}

		return cookies;
	}

	/**
	 * Parse a cookie string into a Query
	 *
	 * @param cookieString The cookie string to parse
	 * @param cookies      The Query to add the cookies to
	 */
	private void parseCookieStringIntoQuery( String cookieString, Query cookies ) {
		IStruct		cookieStruct;
		String[]	parts	= cookieString.split( ";" );
		if ( parts.length == 0 ) {
			return;
		}

		String[] nameAndValue = parts[ 0 ].split( "=" );
		if ( nameAndValue.length != 2 ) {
			return;
		}

		cookieStruct = new Struct();
		cookieStruct.put( Key._NAME, nameAndValue[ 0 ] );
		cookieStruct.put( Key.value, nameAndValue[ 1 ] );

		if ( parts.length > 1 ) {
			Arrays.stream( parts, 1, parts.length )
			    .forEach( metadata -> {
				    String[] metadataParts = metadata.split( "=" );
				    if ( metadataParts.length == 0 ) {
					    return;
				    }
				    Key	metadataType	= Key.of( metadataParts[ 0 ] );
				    Object metadataValue = true;
				    if ( metadataParts.length == 2 ) {
					    metadataValue = metadataParts[ 1 ];
				    }

				    if ( metadataType.equals( Key.of( "max-age" ) ) ) {
					    metadataType = Key.expires;
					    metadataValue = StringCaster.cast( DoubleCaster.cast( metadataValue ) / 60 / 60 / 24 );
				    }

				    cookieStruct.put( metadataType, metadataValue );
			    } );
		}

		cookies.add( cookieStruct );
	}

	/**
	 * Generate a status line from the HTTP version, status code, and status text
	 *
	 * @param httpVersionString The HTTP version string
	 * @param statusCodeString  The status code string
	 * @param statusText        The status text
	 *
	 * @return The generated status line
	 */
	private String generateStatusLine( String httpVersionString, String statusCodeString, String statusText ) {
		return httpVersionString + " " + statusCodeString + " " + statusText;
	}

	/**
	 * Generate a header string from the status line and headers
	 *
	 * @param statusLine The status line
	 * @param headers    The headers to include in the string
	 *
	 * @return The generated header string
	 */
	private String generateHeaderString( String statusLine, IStruct headers ) {
		return statusLine + " " + headers.entrySet()
		    .stream()
		    .sorted( Map.Entry.comparingByKey() )
		    .map( entry -> {
			    StringBuilder	sb				= new StringBuilder();
			    Object			headerValues	= entry.getValue();
			    CastAttempt<Array> isValuesArray = ArrayCaster.attempt( headerValues );
			    if ( isValuesArray.wasSuccessful() ) {
				    Array values = isValuesArray.getOrFail();
				    for ( Object value : values ) {
					    String headerValue = StringCaster.cast( value );
					    sb.append( entry.getKey().getName() + ": " + headerValue + " " );
				    }
			    } else {
				    String headerValue = StringCaster.cast( headerValues );
				    sb.append( entry.getKey().getName() + ": " + headerValue + " " );
			    }
			    return sb.toString().trim();
		    } ).collect( Collectors.joining( " " ) );
	}

	/**
	 * Transform the headers map into a response header struct
	 *
	 * @param headersMap The headers map to transform
	 *
	 * @return The transformed response header struct
	 */
	private IStruct transformToResponseHeaderStruct( Map<String, List<String>> headersMap ) {
		IStruct responseHeaders = new Struct();

		if ( headersMap == null ) {
			return responseHeaders;
		}

		// Add all the headers to our struct
		for ( String headerName : headersMap.keySet() ) {
			if ( ":status".equals( headerName ) ) {
				continue;
			}
			Key		headerNameKey	= Key.of( headerName );
			Array	values			= ( Array ) responseHeaders.getOrDefault( headerNameKey, new Array() );
			values.addAll( headersMap.get( headerName ) );
			responseHeaders.put( headerNameKey, values );
		}

		for ( Key structHeaderKey : responseHeaders.keySet() ) {
			CastAttempt<Array> isValuesArray = ArrayCaster.attempt( responseHeaders.get( structHeaderKey ) );
			if ( isValuesArray.wasSuccessful() ) {
				Array values = isValuesArray.getOrFail();
				if ( values.size() == 1 ) {
					responseHeaders.put( structHeaderKey, values.get( 0 ) );
				}
			}
		}

		return responseHeaders;
	}

	/**
	 * Extract the charset from the content type string
	 *
	 * @param contentType The content type string to extract the charset from
	 *
	 * @return The extracted charset, or null if not found
	 */
	private static String extractCharset( String contentType ) {
		if ( contentType == null || contentType.isEmpty() ) {
			return null;
		}

		Pattern	pattern	= Pattern.compile( "charset=([a-zA-Z0-9-]+)" );
		Matcher	matcher	= pattern.matcher( contentType );

		if ( matcher.find() ) {
			return matcher.group( 1 );
		}
		return null;
	}
}
