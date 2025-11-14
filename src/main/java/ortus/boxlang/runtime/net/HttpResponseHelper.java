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

import java.io.IOException;
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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

/**
 * Utility class for HTTP response parsing and formatting operations.
 * Provides static helper methods for:
 * - Header extraction and transformation
 * - Cookie parsing
 * - Response status line generation
 * - Content-Type charset extraction
 */
public class HttpResponseHelper {

	private static final Pattern CHARSET_PATTERN = Pattern.compile( "charset=([a-zA-Z0-9-]+)" );

	/**
	 * Extract the first header value by name from the headers Struct.
	 * Handles both single values and arrays of values.
	 *
	 * @param headers    The headers struct to search
	 * @param headerName The name of the header to extract
	 *
	 * @return The first header value, or null if not found
	 */
	public static String extractFirstHeaderByName( IStruct headers, Key headerName ) {
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
	 * Generate a Query of cookies from the response headers.
	 * Parses Set-Cookie headers and extracts cookie attributes.
	 *
	 * @param headers The response headers to parse
	 *
	 * @return A Query containing parsed cookie data with columns: name, value, path, domain, expires, secure, httpOnly, samesite
	 */
	public static Query generateCookiesQuery( IStruct headers ) {
		Query cookies = new Query();
		cookies.addColumn( Key._NAME, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.value, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.path, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.domain, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.expires, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.secure, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.httpOnly, QueryColumnType.VARCHAR );
		cookies.addColumn( Key.samesite, QueryColumnType.VARCHAR );

		Object				cookieValue		= headers.getOrDefault( Key.setCookie, new Array() );
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
	 * Parse a single cookie string into a Query row.
	 * Handles cookie attributes like path, domain, max-age, secure, httpOnly, and samesite.
	 *
	 * @param cookieString The cookie string to parse (e.g., "name=value; Path=/; Secure")
	 * @param cookies      The Query to add the parsed cookie to
	 */
	public static void parseCookieStringIntoQuery( String cookieString, Query cookies ) {
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
				    Key	metadataType	= Key.of( metadataParts[ 0 ].trim() );
				    Object metadataValue = true;
				    if ( metadataParts.length == 2 ) {
					    metadataValue = metadataParts[ 1 ];
				    }

				    // Convert max-age from seconds to days for expires
				    if ( metadataType.equals( Key.maxAge ) ) {
					    metadataType = Key.expires;
					    metadataValue = StringCaster.cast( DoubleCaster.cast( metadataValue ) / 60 / 60 / 24 );
				    }

				    cookieStruct.put( metadataType, metadataValue );
			    } );
		}

		cookies.add( cookieStruct );
	}

	/**
	 * Generate an HTTP status line.
	 *
	 * @param httpVersionString The HTTP version (e.g., "HTTP/1.1" or "HTTP/2")
	 * @param statusCodeString  The status code (e.g., "200", "404")
	 * @param statusText        The status text/reason phrase (e.g., "OK", "Not Found")
	 *
	 * @return The formatted status line (e.g., "HTTP/1.1 200 OK")
	 */
	public static String generateStatusLine( String httpVersionString, String statusCodeString, String statusText ) {
		return httpVersionString + " " + statusCodeString + " " + statusText;
	}

	/**
	 * Generate a formatted header string from the status line and headers.
	 * Headers are sorted alphabetically and formatted as "Name: Value" pairs.
	 *
	 * @param statusLine The HTTP status line
	 * @param headers    The headers to format
	 *
	 * @return A space-separated string of the status line and all headers
	 */
	public static String generateHeaderString( String statusLine, IStruct headers ) {
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
	 * Transform the Java HttpHeaders map into a BoxLang response header struct.
	 * Handles multi-value headers and filters out HTTP/2 pseudo-headers.
	 * Single-value headers are stored as strings, multi-value headers as arrays.
	 *
	 * @param headersMap The headers map from HttpResponse
	 *
	 * @return A BoxLang Struct containing the transformed headers
	 */
	public static IStruct transformToResponseHeaderStruct( Map<String, List<String>> headersMap ) {
		IStruct responseHeaders = new Struct( false );

		if ( headersMap == null ) {
			return responseHeaders;
		}

		// Add all the headers to our struct
		for ( String headerName : headersMap.keySet() ) {
			// Skip HTTP/2 pseudo-headers
			if ( ":status".equals( headerName ) ) {
				continue;
			}
			Key		headerNameKey	= Key.of( headerName );
			Array	values			= ( Array ) responseHeaders.getOrDefault( headerNameKey, new Array() );
			values.addAll( headersMap.get( headerName ) );
			responseHeaders.put( headerNameKey, values );
		}

		// Convert single-value arrays to simple strings
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
	 * Extract the charset from a Content-Type header value.
	 * Parses the charset parameter using a regex pattern.
	 *
	 * @param contentType The Content-Type header value (e.g., "text/html; charset=UTF-8")
	 *
	 * @return The extracted charset (e.g., "UTF-8"), or null if not found
	 */
	public static String extractCharset( String contentType ) {
		if ( contentType == null || contentType.isEmpty() ) {
			return null;
		}

		Matcher matcher = CHARSET_PATTERN.matcher( contentType );

		if ( matcher.find() ) {
			return matcher.group( 1 );
		}
		return null;
	}

	/**
	 * Decodes an InputStream based on the Content-Encoding header.
	 * Handles gzip and deflate compression transparently for streaming responses.
	 *
	 * @param inputStream     The raw input stream from the HTTP response
	 * @param contentEncoding The Content-Encoding header value (can be null or comma-separated)
	 *
	 * @return A decoded InputStream, or the original stream if no encoding is specified
	 *
	 * @throws IOException If decompression fails
	 */
	public static java.io.InputStream decodeInputStream( java.io.InputStream inputStream, String contentEncoding ) throws IOException {
		if ( contentEncoding == null || contentEncoding.isEmpty() ) {
			return inputStream;
		}

		// Process encodings in reverse order (they are applied in order, we need to decode in reverse)
		String[]			encodings		= contentEncoding.split( "," );
		java.io.InputStream	decodedStream	= inputStream;

		for ( int i = encodings.length - 1; i >= 0; i-- ) {
			String encoding = encodings[ i ].trim().toLowerCase();
			if ( encoding.equals( "gzip" ) ) {
				decodedStream = new java.util.zip.GZIPInputStream( decodedStream );
			} else if ( encoding.equals( "deflate" ) ) {
				decodedStream = new java.util.zip.InflaterInputStream( decodedStream );
			}
			// Ignore unknown encodings
		}

		return decodedStream;
	}

}
