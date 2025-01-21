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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class HTTP extends Component {

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
		    new Attribute( Key.proxyServer, "string" ),
		    new Attribute( Key.proxyPort, "string" ),
		    new Attribute( Key.proxyUser, "string" ),
		    new Attribute( Key.proxyPassword, "string" ),
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
		        Validator.valueOneOf( "auto", "no", "yes", "never" )
		    ) ),
		    new Attribute( Key.result, "string", "bxhttp", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY
		    ) ),
		    new Attribute( Key.delimiter, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key._NAME, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.columns, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.firstRowAsHeaders, "boolean", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.textQualifier, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.file, "string", Set.of( Validator.requires( Key.path ) ) ),
		    new Attribute( Key.multipart, "boolean", false, Set.of( Validator.TYPE ) ),
		    new Attribute( Key.multipartType, "string", "form-data",
		        Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( "form-data", "related" ) ) ),
		    new Attribute( Key.clientCertPassword, "string" ),
		    new Attribute( Key.path, "string", Set.of( Validator.requires( Key.file ) ) ),
		    new Attribute( Key.clientCert, "string" ),
		    new Attribute( Key.compression, "string" ),
		    new Attribute( Key.authType, "string", "BASIC", Set.of( Validator.REQUIRED, Validator.NON_EMPTY, Validator.valueOneOf( "BASIC", "NTLM" ) ) ),
		    new Attribute( Key.domain, "string" ),
		    new Attribute( Key.workstation, "string" ),
		    new Attribute( Key.cachedWithin, "string" ),
		    new Attribute( Key.encodeUrl, "boolean", true, Set.of( Validator.TYPE ) ),
		};
	}

	/**
	 * I make an HTTP call
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// Keeps track of the HTTPParams
		executionState.put( Key.HTTPParams, new Array() );

		// Process the component for HTTPParams
		BodyResult bodyResult = processBody( context, body );

		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		// Prepare invocation of the HTTP request
		String	variableName	= attributes.getAsString( Key.result );
		String	theURL			= attributes.getAsString( Key.URL );
		String	method			= attributes.getAsString( Key.method ).toUpperCase();
		Array	params			= executionState.getAsArray( Key.HTTPParams );
		Struct	HTTPResult		= new Struct();
		URI		targetURI		= null;

		try {
			HttpRequest.Builder			builder			= HttpRequest.newBuilder();
			URIBuilder					uriBuilder		= new URIBuilder( theURL );
			HttpRequest.BodyPublisher	bodyPublisher	= null;
			List<IStruct>				formFields		= new ArrayList<>();
			List<IStruct>				files			= new ArrayList<>();
			builder.header( "User-Agent", "BoxLang" );

			for ( Object p : params ) {
				IStruct	param	= StructCaster.cast( p );
				String	type	= param.getAsString( Key.type );
				switch ( type.toLowerCase() ) {
					case "header" -> builder.header( param.getAsString( Key._NAME ), param.getAsString( Key.value ) );
					case "body" -> {
						if ( bodyPublisher != null ) {
							throw new BoxRuntimeException( "Cannot use a body httpparam with an existing http body: " + bodyPublisher.toString() );
						}
						bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					}
					case "xml" -> {
						if ( bodyPublisher != null ) {
							throw new BoxRuntimeException( "Cannot use a xml httpparam with an existing http body: " + bodyPublisher.toString() );
						}
						builder.header( "Content-Type", "text/xml" );
						bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					}
					// @TODO move URLEncoder.encode usage a non-deprecated method
					case "cgi" -> builder.header( param.getAsString( Key._NAME ),
					    java.net.URLEncoder.encode( param.getAsString( Key.value ), StandardCharsets.UTF_8 ) );
					case "file" -> files.add( param );
					case "url" -> uriBuilder.addParameter(
					    param.getAsString( Key._NAME ),
					    BooleanCaster.cast( param.getOrDefault( Key.encoded, true ) )
					        ? URLEncoder.encode( StringCaster.cast( param.get( Key.value ) ), StandardCharsets.UTF_8 )
					        : StringCaster.cast( param.get( Key.value ) )
					);
					case "formfield" -> formFields.add( param );
					case "cookie" -> builder.header( "Cookie",
					    param.getAsString( Key._NAME ) + "=" + URLEncoder.encode( param.getAsString( Key.value ), StandardCharsets.UTF_8 ) );
					default -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
				}
			}

			if ( !files.isEmpty() ) {
				if ( bodyPublisher != null ) {
					throw new BoxRuntimeException( "Cannot use a multipart body with an existing http body: " + bodyPublisher.toString() );
				}
				HttpRequestMultipartBody.Builder multipartBodyBuilder = new HttpRequestMultipartBody.Builder();
				for ( IStruct param : files ) {
					ResolvedFilePath	path		= FileSystemUtil.expandPath( context, param.getAsString( Key.file ) );
					File				file		= path.absolutePath().toFile();
					String				mimeType	= Optional.ofNullable( param.getAsString( Key.mimetype ) )
					    .orElseGet( () -> URLConnection.getFileNameMap().getContentTypeFor( file.getName() ) );
					multipartBodyBuilder.addPart( param.getAsString( Key._name ), file, mimeType, file.getName() );
				}

				for ( IStruct formField : formFields ) {
					multipartBodyBuilder.addPart( formField.getAsString( Key._name ), formField.getAsString( Key.value ) );
				}
				HttpRequestMultipartBody multipartBody = multipartBodyBuilder.build();
				builder.header( "Content-Type", multipartBody.getContentType() );
				bodyPublisher = HttpRequest.BodyPublishers.ofByteArray( multipartBody.getBody() );
			} else if ( !formFields.isEmpty() ) {
				if ( bodyPublisher != null ) {
					throw new BoxRuntimeException( "Cannot use a formfield httpparam with an existing http body: " + bodyPublisher.toString() );
				}
				bodyPublisher = HttpRequest.BodyPublishers.ofString(
				    formFields.stream()
				        .map( formField -> {
					        String value = formField.getAsString( Key.value );
					        if ( BooleanCaster.cast( formField.getOrDefault( Key.encoded, true ) ) ) {
						        value = URLEncoder.encode( value, StandardCharsets.UTF_8 );
					        }
					        return formField.getAsString( Key._name ) + "=" + value;
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
			HttpRequest	targetHTTPRequest	= builder.build();
			HttpClient	client				= HttpManager.getClient();

			// Announce the HTTP request
			interceptorService.announce( BoxEvent.ON_HTTP_REQUEST, Struct.of(
			    "httpClient", client,
			    "httpRequest", targetHTTPRequest,
			    "targetURI", targetURI,
			    "attributes", attributes
			) );

			// Announce the HTTP request
			CompletableFuture<HttpResponse<String>>	inflightRequest	= client.sendAsync( targetHTTPRequest, HttpResponse.BodyHandlers.ofString() );
			CompletableFuture<HttpResponse<String>>	winner			= inflightRequest;
			if ( attributes.containsKey( Key.timeout ) ) {
				winner = inflightRequest.applyToEither( HttpManager.getTimeoutRequestAsync( attributes.getAsInteger( Key.timeout ) ), result -> result );
			}
			HttpResponse<String> response = winner.get();

			// Announce the HTTP RAW response
			// Useful for debugging and pre-processing and timing, since the other events are after the response is processed
			interceptorService.announce( BoxEvent.ON_HTTP_RAW_RESPONSE, Struct.of(
			    "response", response
			) );

			// Start Processing Results
			HttpHeaders	httpHeaders			= Optional
			    .ofNullable( response.headers() )
			    .orElse( HttpHeaders.of( Map.of(), ( a, b ) -> true ) );
			IStruct		headers				= transformToResponseHeaderStruct(
			    httpHeaders.map()
			);
			String		httpVersionString	= response.version() == HttpClient.Version.HTTP_1_1 ? "HTTP/1.1" : "HTTP/2";
			String		statusCodeString	= String.valueOf( response.statusCode() );
			String		statusText			= HTTPStatusReasons.getReasonForStatus( response.statusCode() );

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
			HTTPResult.put( Key.fileContent, response.statusCode() == 408 ? "Request Timeout" : response.body() );
			HTTPResult.put( Key.errorDetail, response.statusCode() == 408 ? response.body() : "" );
			Optional<String> contentTypeHeader = httpHeaders.firstValue( "Content-Type" );
			contentTypeHeader.ifPresent( ( contentType ) -> {
				String[] contentTypeParts = contentType.split( ";\s*" );
				if ( contentTypeParts.length > 0 ) {
					HTTPResult.put( Key.mimetype, contentTypeParts[ 0 ] );
				}
				if ( contentTypeParts.length > 1 ) {
					String charset = contentTypeParts[ 1 ].replace( "charset=", "" );
					HTTPResult.put( Key.charset, charset );
				}
			} );
			HTTPResult.put( Key.cookies, generateCookiesQuery( headers ) );

			// Set the result back into the page using the variable name
			ExpressionInterpreter.setVariable( context, variableName, HTTPResult );

			// Announce the HTTP response
			interceptorService.announce( BoxEvent.ON_HTTP_RESPONSE, Struct.of(
			    "result", HTTPResult
			) );

			return DEFAULT_RETURN;
		} catch ( ExecutionException e ) {
			Throwable innerException = e.getCause();
			if ( innerException instanceof ConnectException ) {
				HTTPResult.put( Key.responseHeader, Struct.EMPTY );
				HTTPResult.put( Key.header, "" );
				HTTPResult.put( Key.statusCode, 502 );
				HTTPResult.put( Key.status_code, 502 );
				HTTPResult.put( Key.statusText, "Bad Gateway" );
				HTTPResult.put( Key.status_text, "Bad Gateway" );
				HTTPResult.put( Key.fileContent, "Connection Failure" );
				if ( targetURI != null ) {
					HTTPResult.put( Key.errorDetail, String.format( "Unknown host: %s: Name or service not known.", targetURI.getHost() ) );
				} else {
					HTTPResult.put( Key.errorDetail, String.format( "Unknown host: %s: Name or service not known.", theURL ) );
				}
				ExpressionInterpreter.setVariable( context, variableName, HTTPResult );
			} else {
				throw new BoxRuntimeException( innerException.getMessage() );
			}
			return DEFAULT_RETURN;
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new BoxRuntimeException( "The request was interrupted", "InterruptedException", e );
		} catch ( URISyntaxException | IOException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}
	}

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

	private String generateStatusLine( String httpVersionString, String statusCodeString, String statusText ) {
		return httpVersionString + " " + statusCodeString + " " + statusText;
	}

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
}
