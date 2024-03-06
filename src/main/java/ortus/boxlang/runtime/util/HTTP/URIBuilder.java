package ortus.boxlang.runtime.util.HTTP;

import org.checkerframework.checker.units.qual.A;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		return new URI(
		    this.scheme,
		    this.userInfo,
		    this.host,
		    this.port != null ? this.port : -1,
		    this.path,
		    this.queryParams.stream().map( ( NameValuePair::toString ) ).collect( Collectors.joining( "&" ) ),
		    this.fragment
		);
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
