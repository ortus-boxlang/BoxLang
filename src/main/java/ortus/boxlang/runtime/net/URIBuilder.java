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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.NameValuePair;

/**
 * A utility class for building and manipulating URIs (Uniform Resource Identifiers).
 * <p>
 * This class provides a fluent API for constructing URIs with proper handling of:
 * <ul>
 * <li>Base paths and URLs</li>
 * <li>Query string parameters (name-value pairs)</li>
 * <li>Port configuration</li>
 * <li>URL encoding preservation (does not double-encode)</li>
 * </ul>
 * <p>
 * The builder follows RFC 3986 URI specifications and ensures that pre-encoded
 * characters in paths and query strings are not double-encoded.
 * <p>
 * <b>Example Usage:</b>
 *
 * <pre>
 * URIBuilder builder = new URIBuilder( "http://example.com/api" );
 * builder.addParameter( "key", "value" );
 * builder.addParameter( "filter", "name=John" );
 * builder.setPort( 8080 );
 * URI uri = builder.build();
 * // Result: http://example.com:8080/api?key=value&filter=name=John
 * </pre>
 * <p>
 * <b>Thread Safety:</b> This class is not thread-safe. Each thread should use its own instance.
 *
 * @see URI
 * @see NameValuePair
 */
public class URIBuilder {

	/**
	 * The base path/URL without query parameters
	 */
	private @Nullable String				basePath;

	/**
	 * List of query parameters to be appended to the URI
	 */
	private @NonNull List<NameValuePair>	queryParams;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new URIBuilder with an empty base path.
	 * <p>
	 * This is useful when you want to build a URI from scratch by setting
	 * the base path later or only working with query parameters.
	 *
	 * @throws URISyntaxException if the initial empty string is not a valid URI
	 */
	public URIBuilder() throws URISyntaxException {
		this( "" );
	}

	/**
	 * Constructs a new URIBuilder from a string representation of a URI.
	 * <p>
	 * The string can contain:
	 * <ul>
	 * <li>A complete URL: "http://example.com/path?key=value"</li>
	 * <li>A relative path: "/api/endpoint"</li>
	 * <li>A URL with existing query parameters (which will be parsed and preserved)</li>
	 * </ul>
	 * <p>
	 * If the string contains query parameters (indicated by '?'), they will be
	 * automatically parsed and added to the query parameters list. Pre-encoded
	 * characters in the query string are preserved and not double-encoded.
	 *
	 * @param string The string representation of the URI to build from
	 *
	 * @throws URISyntaxException if the provided string is not a valid URI
	 */
	public URIBuilder( @NonNull String string ) throws URISyntaxException {
		String[] pathParts = string.split( "\\?" );
		this.basePath		= string = pathParts[ 0 ];
		this.queryParams	= pathParts.length > 1
		    ? parseQueryString( pathParts[ 1 ] )
		    : new ArrayList<NameValuePair>();
	}

	/**
	 * Constructs a new URIBuilder from an existing URI object.
	 * <p>
	 * This constructor converts the URI to a string and delegates to the
	 * string constructor, preserving all components including any existing
	 * query parameters.
	 *
	 * @param uri The URI object to build from
	 *
	 * @throws URISyntaxException if the URI cannot be converted to a valid string
	 */
	public URIBuilder( @NonNull URI uri ) throws URISyntaxException {
		this( uri.toString() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Adds a query parameter to the URI.
	 * <p>
	 * Query parameters are appended in the order they are added. Multiple
	 * parameters with the same name are allowed (e.g., for array-like parameters).
	 * <p>
	 * The parameter value should be provided in its unencoded form. If the value
	 * is already URL-encoded, it will be preserved as-is during the build process.
	 * <p>
	 * <b>Example:</b>
	 *
	 * <pre>
	 * builder.addParameter( "name", "John Doe" );
	 * builder.addParameter( "age", "30" );
	 * builder.addParameter( "tags", "java" );
	 * builder.addParameter( "tags", "programming" );
	 * // Results in: ?name=John Doe&age=30&tags=java&tags=programming
	 * </pre>
	 *
	 * @param name  The parameter name (required)
	 * @param value The parameter value (can be null)
	 */
	public void addParameter( @NonNull String name, @Nullable String value ) {
		this.queryParams.add( new NameValuePair( name, value ) );
	}

	/**
	 * Sets or updates the port number in the base URI.
	 * <p>
	 * This method reconstructs the base path with the new port number while
	 * preserving all other URI components (scheme, host, path, etc.).
	 * <p>
	 * If the base path is not a complete URI with a scheme and host, this
	 * method will throw a BoxRuntimeException.
	 * <p>
	 * <b>Example:</b>
	 *
	 * <pre>
	 * URIBuilder builder = new URIBuilder( "http://example.com/api" );
	 * builder.setPort( 8080 );
	 * // Base path becomes: http://example.com:8080/api
	 * </pre>
	 *
	 * @param port The port number to set (standard range: 1-65535)
	 *
	 * @throws BoxRuntimeException if the base path is not a valid URI or cannot be parsed
	 */
	public void setPort( Integer port ) {
		URI tempURI;
		try {
			tempURI	= new URI( this.basePath );
			tempURI	= new URI( tempURI.getScheme(), tempURI.getUserInfo(), tempURI.getHost(), port, tempURI.getPath(), null, null );
		} catch ( URISyntaxException e ) {
			throw new BoxRuntimeException( "Invalid URI: " + this.basePath, e );
		}

		this.basePath = tempURI.toString();
	}

	/**
	 * Builds and returns the final URI with all configured components.
	 * <p>
	 * This method combines the base path with all added query parameters to
	 * create a complete URI. Query parameters are joined with '&' and appended
	 * to the base path with '?' as the separator.
	 * <p>
	 * The method preserves URL encoding of parameters and does not perform
	 * double-encoding on already-encoded values.
	 * <p>
	 * <b>Example:</b>
	 *
	 * <pre>
	 * URIBuilder builder = new URIBuilder( "http://api.example.com/search" );
	 * builder.addParameter( "q", "hello world" );
	 * builder.addParameter( "limit", "10" );
	 * URI result = builder.build();
	 * // Result: http://api.example.com/search?q=hello world&limit=10
	 * </pre>
	 *
	 * @return A new URI instance representing the complete URI
	 *
	 * @throws URISyntaxException if the constructed URI string is not valid according to RFC 3986
	 */
	public URI build() throws URISyntaxException {
		String URIString = this.basePath;
		if ( this.queryParams.size() > 0 ) {
			URIString += "?" + this.queryParams.stream().map( ( NameValuePair::toString ) ).collect( Collectors.joining( "&" ) );
		}
		return new URI( URIString );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Parses a query string into a list of name-value pairs.
	 * <p>
	 * This method splits the query string by '&' to separate individual parameters,
	 * then splits each parameter by '=' to separate names from values. Parameters
	 * without values (e.g., "flag" in "?flag&other=value") are supported.
	 * <p>
	 * Pre-encoded values are preserved and not decoded, preventing double-encoding
	 * when the URI is built.
	 *
	 * @param queryString The raw query string to parse (without the leading '?')
	 *
	 * @return A list of NameValuePair objects representing the parsed parameters
	 */
	private List<NameValuePair> parseQueryString( String queryString ) {
		ArrayList<NameValuePair> newPairs = new ArrayList<>();
		if ( queryString != null ) {
			for ( String pair : queryString.split( "&" ) ) {
				String[] nameAndValue = pair.split( "=" );
				newPairs.add( NameValuePair.fromNativeArray( nameAndValue ) );
			}
		}
		return newPairs;
	}

}
