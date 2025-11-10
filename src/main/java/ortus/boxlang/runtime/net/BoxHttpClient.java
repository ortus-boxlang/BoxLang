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

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class represents an HTTP client for making network requests.
 * It encapsulates configuration and functionality for sending HTTP requests
 * and handling responses.
 */
public class BoxHttpClient {

	private static final Pattern	CHARSET_PATTERN	= Pattern.compile( "charset=([a-zA-Z0-9-]+)" );

	/**
	 * The underlying HttpClient used for making HTTP requests.
	 */
	private final HttpClient		httpClient;

	/**
	 * The HttpService that manages this client.
	 */
	private final HttpService		httpService;

	/**
	 * The Logger used for logging HTTP operations.
	 */
	private final BoxLangLogger		logger;

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
	 * Private Helpers
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Extract the first header value by name from the headers Struct
	 *
	 * @param headers
	 * @param headerName
	 *
	 * @return
	 */
	private static String extractFirstHeaderByName( IStruct headers, Key headerName ) {
		Object				headerValue		= headers.get( headerName );
		CastAttempt<Array>	isValuesArray	= ArrayCaster.attempt( headerValue );
		if ( isValuesArray.wasSuccessful() ) {
			Array values = isValuesArray.getOrFail();
			if ( values.size() > 0 ) {
				return StringCaster.cast( values.get( 0 ) );
			}
		} else if ( headerValue != null ) {
			return StringCaster.cast( headerValue );
		}
		return null;
	}

	/**
	 * Generate a Query of cookies from the headers
	 *
	 * @param headers The headers to parse
	 *
	 * @return A Query of cookies
	 */
	private static Query generateCookiesQuery( IStruct headers ) {
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
	private static void parseCookieStringIntoQuery( String cookieString, Query cookies ) {
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
	private static String generateStatusLine( String httpVersionString, String statusCodeString, String statusText ) {
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
	private static String generateHeaderString( String statusLine, IStruct headers ) {
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
	private static IStruct transformToResponseHeaderStruct( Map<String, List<String>> headersMap ) {
		IStruct responseHeaders = new Struct( false );

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

		Matcher matcher = CHARSET_PATTERN.matcher( contentType );

		if ( matcher.find() ) {
			return matcher.group( 1 );
		}
		return null;
	}

}
