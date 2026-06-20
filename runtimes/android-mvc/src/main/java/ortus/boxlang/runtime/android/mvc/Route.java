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
package ortus.boxlang.runtime.android.mvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable, compiled route definition for the BoxLang Android MVC front controller.
 * <p>
 * A route maps a URI pattern (e.g. {@code /items/:id}) and a set of HTTP verbs to a
 * {@code handler.action} target (e.g. {@code Items.show}). Path placeholders prefixed
 * with {@code :} are extracted as named parameters at match time.
 * <p>
 * Routes are immutable value objects: the URI pattern is compiled to a {@link Pattern}
 * once at construction and the {@code hashCode} is pre-computed per the project's
 * immutable-object performance convention.
 */
public class Route {

	/**
	 * Matches a {@code :name} placeholder segment in a URI pattern.
	 */
	private static final Pattern	PLACEHOLDER	= Pattern.compile( ":([a-zA-Z_][a-zA-Z0-9_]*)" );

	/**
	 * The optional, unique name of the route (used for reverse lookups / relocate-by-name).
	 */
	private final String			name;

	/**
	 * The original URI pattern, e.g. {@code /items/:id}.
	 */
	private final String			uriPattern;

	/**
	 * The handler name (BoxLang class under {@code handlers/}), e.g. {@code Items}.
	 */
	private final String			handler;

	/**
	 * The action method name on the handler, e.g. {@code show}.
	 */
	private final String			action;

	/**
	 * The set of upper-cased HTTP verbs this route responds to. Empty means "any verb".
	 */
	private final Set<String>		methods;

	/**
	 * The compiled regular expression for the URI pattern.
	 */
	private final Pattern			compiled;

	/**
	 * The ordered list of placeholder names extracted from the URI pattern.
	 */
	private final List<String>		paramNames;

	/**
	 * Pre-computed hashCode (immutable object).
	 */
	private final int				hashCode;

	/**
	 * Construct a compiled route.
	 *
	 * @param name       Optional route name (may be {@code null})
	 * @param uriPattern The URI pattern, e.g. {@code /items/:id}
	 * @param handler    The handler name, e.g. {@code Items}
	 * @param action     The action method name, e.g. {@code show}
	 * @param methods    The set of upper-cased HTTP verbs ({@code null} or empty == any)
	 */
	public Route( String name, String uriPattern, String handler, String action, Set<String> methods ) {
		this.name		= name;
		this.uriPattern	= normalize( uriPattern );
		this.handler	= handler;
		this.action		= action;
		this.methods	= methods == null ? Collections.emptySet() : Set.copyOf( methods );

		// Compile the URI pattern into a regex, capturing the placeholder names in order.
		List<String>	names	= new ArrayList<>();
		StringBuilder	regex	= new StringBuilder( "^" );
		Matcher			m		= PLACEHOLDER.matcher( this.uriPattern );
		int				last	= 0;
		while ( m.find() ) {
			regex.append( Pattern.quote( this.uriPattern.substring( last, m.start() ) ) );
			regex.append( "([^/]+)" );
			names.add( m.group( 1 ) );
			last = m.end();
		}
		regex.append( Pattern.quote( this.uriPattern.substring( last ) ) );
		// Allow an optional trailing slash.
		regex.append( "/?$" );

		this.compiled	= Pattern.compile( regex.toString() );
		this.paramNames	= Collections.unmodifiableList( names );
		this.hashCode	= computeHashCode();
	}

	/**
	 * Normalize a URI path: ensure a single leading slash and strip a trailing slash
	 * (except for the root path).
	 *
	 * @param uri The raw URI
	 *
	 * @return The normalized URI
	 */
	static String normalize( String uri ) {
		if ( uri == null || uri.isEmpty() ) {
			return "/";
		}
		String result = uri.trim();
		if ( !result.startsWith( "/" ) ) {
			result = "/" + result;
		}
		if ( result.length() > 1 && result.endsWith( "/" ) ) {
			result = result.substring( 0, result.length() - 1 );
		}
		return result;
	}

	/**
	 * Attempt to match the given path and HTTP method against this route.
	 *
	 * @param path   The incoming request path (will be normalized)
	 * @param method The incoming HTTP method (case-insensitive; {@code null} == any)
	 *
	 * @return A {@link RouteMatch} if this route matches, otherwise {@code null}
	 */
	public RouteMatch match( String path, String method ) {
		if ( !this.methods.isEmpty() && method != null && !this.methods.contains( method.toUpperCase() ) ) {
			return null;
		}

		Matcher m = this.compiled.matcher( normalize( path ) );
		if ( !m.matches() ) {
			return null;
		}

		Map<String, String> params = new LinkedHashMap<>();
		for ( int i = 0; i < this.paramNames.size(); i++ ) {
			params.put( this.paramNames.get( i ), m.group( i + 1 ) );
		}
		return new RouteMatch( this.handler, this.action, params, this );
	}

	/**
	 * @return The optional route name (may be {@code null})
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return The normalized URI pattern
	 */
	public String getUriPattern() {
		return this.uriPattern;
	}

	/**
	 * @return The handler name
	 */
	public String getHandler() {
		return this.handler;
	}

	/**
	 * @return The action method name
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @return The set of HTTP verbs (empty == any)
	 */
	public Set<String> getMethods() {
		return this.methods;
	}

	/**
	 * @return The ordered list of placeholder parameter names
	 */
	public List<String> getParamNames() {
		return this.paramNames;
	}

	private int computeHashCode() {
		int result = this.uriPattern.hashCode();
		result	= 31 * result + this.handler.hashCode();
		result	= 31 * result + this.action.hashCode();
		result	= 31 * result + this.methods.hashCode();
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( ! ( obj instanceof Route other ) ) {
			return false;
		}
		return this.uriPattern.equals( other.uriPattern )
		    && this.handler.equals( other.handler )
		    && this.action.equals( other.action )
		    && this.methods.equals( other.methods );
	}

	@Override
	public String toString() {
		return "Route[" + ( this.methods.isEmpty() ? "ANY" : String.join( "|", this.methods ) )
		    + " " + this.uriPattern + " -> " + this.handler + "." + this.action
		    + ( this.name != null ? " (" + this.name + ")" : "" ) + "]";
	}
}
