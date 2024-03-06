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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.HTTP.HTTPStatusReasons;
import ortus.boxlang.runtime.util.HTTP.URIBuilder;

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
		    new Attribute( Key.result, "string" )
			/*
			 * TODO:
			 * port
			 * method
			 * proxyserver
			 * proxyport
			 * proxyuser
			 * proxypassword
			 * username
			 * password
			 * useragent
			 * charset
			 * resolveurl
			 * throwonerror
			 * redirect
			 * timeout
			 * getasbinary
			 * delimiter
			 * name
			 * columns
			 * firstrowasheaders
			 * textqualifier
			 * file
			 * multipart
			 * clientcertpassword
			 * clientcert
			 * path
			 * compression
			 * authType
			 * domain
			 * workstation
			 * cachedWithin
			 * encodeURL
			 */
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
		executionState.put( Key.HTTPParams, new Array() );

		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		String	variableName	= StringCaster.cast( attributes.getOrDefault( Key.result, "cfhttp" ) );
		String	theURL			= attributes.getAsString( Key.URL );
		String	method			= StringCaster.cast( attributes.getOrDefault( Key.method, "GET" ) ).toUpperCase();
		Array	params			= executionState.getAsArray( Key.HTTPParams );
		Struct	HTTPResult		= new Struct();

		try {
			HttpRequest.Builder			builder			= HttpRequest.newBuilder();
			URIBuilder					uriBuilder		= new URIBuilder( theURL );
			HttpRequest.BodyPublisher	bodyPublisher	= HttpRequest.BodyPublishers.noBody();
			for ( Object p : params ) {
				IStruct	param	= StructCaster.cast( p );
				String	type	= param.getAsString( Key.type );
				switch ( type.toLowerCase() ) {
					case "header" -> builder.header( param.getAsString( Key._NAME ), param.getAsString( Key.value ) );
					case "body" -> bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					case "xml" -> {
						builder.header( "Content-Type", "text/xml" );
						bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					}
					// @TODO move this to a non-deprecated method
					case "cgi" -> builder.header( param.getAsString( Key._NAME ), java.net.URLEncoder.encode( param.getAsString( Key.value ) ) );
					case "file" -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
					case "url" -> uriBuilder.addParameter( param.getAsString( Key._NAME ), StringCaster.cast( param.get( Key.value ) ) );
					default -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
				}
			}
			builder.method( method, bodyPublisher );
			builder.uri( uriBuilder.build() );
			HttpRequest				request				= builder.build();
			HttpClient				client				= HttpClient.newHttpClient();
			HttpResponse<String>	response			= client.send( request, HttpResponse.BodyHandlers.ofString() );

			HttpHeaders				httpHeaders			= response.headers();
			IStruct					headers				= transformToResponseHeaderStruct( httpHeaders.map(), response );
			String					httpVersionString	= response.version() == HttpClient.Version.HTTP_1_1 ? "HTTP/1.1" : "HTTP/2";
			String					statusCodeString	= String.valueOf( response.statusCode() );
			String					statusText			= HTTPStatusReasons.getReasonForStatus( response.statusCode() );

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
			HTTPResult.put( Key.fileContent, response.body() );
			HTTPResult.put( Key.errorDetail, "" );
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

			// Set the result back into the page
			ExpressionInterpreter.setVariable( context, variableName, HTTPResult );

			return DEFAULT_RETURN;
		} catch ( URISyntaxException | IOException | InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage() );
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

	private <T> IStruct transformToResponseHeaderStruct( Map<String, List<String>> headersMap, HttpResponse<T> response ) {
		IStruct responseHeaders = new Struct();

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
