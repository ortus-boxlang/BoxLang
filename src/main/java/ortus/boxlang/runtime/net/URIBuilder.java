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

import javax.annotation.Nonnull;

import org.checkerframework.checker.nullness.qual.Nullable;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class URIBuilder {

	private @Nullable String				basePath;
	private @Nonnull List<NameValuePair>	queryParams;

	public URIBuilder() throws URISyntaxException {
		this( "" );
	}

	public URIBuilder( @Nonnull String string ) throws URISyntaxException {
		String[] pathParts = string.split( "\\?" );
		this.basePath		= string = pathParts[ 0 ];
		this.queryParams	= pathParts.length > 1
		    ? parseQueryString( pathParts[ 1 ] )
		    : new ArrayList<NameValuePair>();
	}

	public URIBuilder( @Nonnull URI uri ) throws URISyntaxException {
		this( uri.toString() );
	}

	public void addParameter( @Nonnull String name, @Nullable String value ) {
		this.queryParams.add( new NameValuePair( name, value ) );
	}

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

	public URI build() throws URISyntaxException {
		String URIString = this.basePath;
		if ( this.queryParams.size() > 0 ) {
			URIString += "?" + this.queryParams.stream().map( ( NameValuePair::toString ) ).collect( Collectors.joining( "&" ) );
		}
		return new URI( URIString );
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
