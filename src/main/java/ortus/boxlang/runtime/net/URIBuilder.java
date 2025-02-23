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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class URIBuilder {

	private @Nullable String				scheme;
	private @Nullable String				userInfo;
	private @Nullable String				host;
	private @Nullable Integer				port;
	private @Nullable String				path;
	private @Nonnull List<NameValuePair>	queryParams;
	private @Nonnull String					fragment;
	private @Nonnull Charset				charset;

	public URIBuilder() throws URISyntaxException {
		this( "" );
	}

	public URIBuilder( @Nonnull String string ) throws URISyntaxException {
		this( string, Charset.defaultCharset() );
	}

	public URIBuilder( @Nonnull String string, @Nonnull Charset charset ) throws URISyntaxException {
		this( URI.create( string ), charset );
	}

	public URIBuilder( @Nonnull URI uri ) throws URISyntaxException {
		this( uri, Charset.defaultCharset() );
	}

	public URIBuilder( @Nonnull URI uri, @Nonnull Charset charset ) throws URISyntaxException {
		this.scheme			= uri.getScheme();
		this.userInfo		= uri.getUserInfo();
		this.host			= uri.getHost();
		this.port			= uri.getPort();
		this.path			= uri.getPath();
		this.charset		= charset;
		this.queryParams	= parseQueryString( uri.getQuery() );
		this.fragment		= uri.getFragment();
	}

	public void addParameter( @Nonnull String name, @Nullable String value ) {
		this.queryParams.add( new NameValuePair( name, value ) );
	}

	public URI build() throws URISyntaxException {
		URI baseURI = new URI(
		    this.scheme,
		    this.userInfo,
		    this.host,
		    this.port != null ? this.port : -1,
		    "",
		    null,
		    this.fragment
		);
		// We use resolve to build the final URI to prevent double-encoding
		return this.queryParams.size() > 0
		    ? baseURI.resolve( this.path + "?" + this.queryParams.stream().map( ( NameValuePair::toString ) ).collect( Collectors.joining( "&" ) ) )
		    : baseURI.resolve( this.path );
	}

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
