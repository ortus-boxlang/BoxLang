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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The BoxLang Android MVC router: a fluent route table plus request resolution.
 * <p>
 * Resolution order for an incoming {@code (path, method)} (see {@link #resolve}):
 * <ol>
 * <li>The configured <b>default event</b> when the path is the root ({@code /}).</li>
 * <li>An <b>explicit route</b> match from the table (first registered wins).</li>
 * <li><b>Convention</b> resolution: {@code /users/save} → {@code Users.save},
 * {@code /users} → {@code Users.index}.</li>
 * </ol>
 * This mirrors ColdBox conventions: a handler with no explicit action defaults to
 * {@code index}, and the root maps to a single configurable default event.
 * <p>
 * The fluent API is registered from {@code Application.bx} {@code configureRouter()}:
 *
 * <pre>
 * router.setDefaultEvent( "Main.index" );
 * router.route( "/items/:id" ).to( "Items.show" );
 * router.get( "/items" ).withName( "items" ).to( "Items.list" );
 * router.post( "/items/add" ).to( "Items.add" );
 * </pre>
 */
public class Router {

	/**
	 * The default action used when a handler is referenced without one (ColdBox convention).
	 */
	public static final String	DEFAULT_ACTION	= "index";

	/**
	 * The ordered, explicit route table (first match wins).
	 */
	private final List<Route>	routes			= new ArrayList<>();

	/**
	 * The default event for the root path, in {@code Handler.action} form.
	 */
	private String				defaultEvent	= "Main.index";

	/**
	 * Begin defining a route for the given URI pattern (responds to any HTTP verb).
	 *
	 * @param pattern The URI pattern, e.g. {@code /items/:id}
	 *
	 * @return A {@link RouteBuilder} to finish defining the route
	 */
	public RouteBuilder route( String pattern ) {
		return new RouteBuilder( this, pattern );
	}

	/**
	 * Begin defining a GET-only route.
	 *
	 * @param pattern The URI pattern
	 *
	 * @return A {@link RouteBuilder} constrained to GET
	 */
	public RouteBuilder get( String pattern ) {
		return new RouteBuilder( this, pattern ).withMethods( "GET" );
	}

	/**
	 * Begin defining a POST-only route.
	 *
	 * @param pattern The URI pattern
	 *
	 * @return A {@link RouteBuilder} constrained to POST
	 */
	public RouteBuilder post( String pattern ) {
		return new RouteBuilder( this, pattern ).withMethods( "POST" );
	}

	/**
	 * Register a compiled route (used by {@link RouteBuilder#to(String)}).
	 *
	 * @param route The route to register
	 *
	 * @return This router for chaining
	 */
	public Router register( Route route ) {
		this.routes.add( route );
		return this;
	}

	/**
	 * Set the default event (for the root path) in {@code Handler.action} form.
	 *
	 * @param event The default event, e.g. {@code Main.index}
	 *
	 * @return This router for chaining
	 */
	public Router setDefaultEvent( String event ) {
		this.defaultEvent = event;
		return this;
	}

	/**
	 * @return The configured default event
	 */
	public String getDefaultEvent() {
		return this.defaultEvent;
	}

	/**
	 * @return An unmodifiable view of the explicit route table
	 */
	public List<Route> getRoutes() {
		return List.copyOf( this.routes );
	}

	/**
	 * Look up a route by its name.
	 *
	 * @param name The route name
	 *
	 * @return The matching {@link Route}, or {@code null} if none is named that
	 */
	public Route findByName( String name ) {
		for ( Route route : this.routes ) {
			if ( name.equals( route.getName() ) ) {
				return route;
			}
		}
		return null;
	}

	/**
	 * Match the path/method against the explicit route table only.
	 *
	 * @param path   The request path
	 * @param method The HTTP method (may be {@code null} for any)
	 *
	 * @return The first matching {@link RouteMatch}, or {@code null} if none match
	 */
	public RouteMatch match( String path, String method ) {
		for ( Route route : this.routes ) {
			RouteMatch result = route.match( path, method );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Fully resolve an incoming request to a handler + action, applying (in order) the
	 * default event, the explicit route table, then convention resolution.
	 *
	 * @param path   The request path
	 * @param method The HTTP method (may be {@code null} for any)
	 *
	 * @return A non-null {@link RouteMatch}
	 */
	public RouteMatch resolve( String path, String method ) {
		String normalized = Route.normalize( path );

		// 1. Root path -> default event
		if ( "/".equals( normalized ) ) {
			return event( this.defaultEvent, null );
		}

		// 2. Explicit route table
		RouteMatch explicit = match( normalized, method );
		if ( explicit != null ) {
			return explicit;
		}

		// 3. Convention: /handler[/action]
		String[]	segments	= normalized.substring( 1 ).split( "/" );
		String		handler		= capitalize( segments[ 0 ] );
		String		action		= segments.length > 1 ? segments[ 1 ] : DEFAULT_ACTION;
		return new RouteMatch( handler, action, null, null );
	}

	/**
	 * Build a convention {@link RouteMatch} from a {@code Handler.action} event string.
	 *
	 * @param eventString The event string, e.g. {@code Main.index}
	 * @param route       The originating route (may be {@code null})
	 *
	 * @return The corresponding {@link RouteMatch}
	 */
	static RouteMatch event( String eventString, Route route ) {
		int		dot		= eventString.lastIndexOf( '.' );
		String	handler	= dot > 0 ? eventString.substring( 0, dot ) : eventString;
		String	action	= dot > 0 ? eventString.substring( dot + 1 ) : DEFAULT_ACTION;
		return new RouteMatch( handler, action, null, route );
	}

	private static String capitalize( String value ) {
		if ( value == null || value.isEmpty() ) {
			return value;
		}
		return Character.toUpperCase( value.charAt( 0 ) ) + value.substring( 1 );
	}

	/**
	 * Fluent builder for a single {@link Route}. Terminate with {@link #to(String)}.
	 */
	public static class RouteBuilder {

		private final Router		router;
		private final String		pattern;
		private String				name;
		private final Set<String>	methods	= new LinkedHashSet<>();

		RouteBuilder( Router router, String pattern ) {
			this.router		= router;
			this.pattern	= pattern;
		}

		/**
		 * Give this route a unique name (for reverse lookups / relocate-by-name).
		 *
		 * @param name The route name
		 *
		 * @return This builder
		 */
		public RouteBuilder withName( String name ) {
			this.name = name;
			return this;
		}

		/**
		 * Constrain this route to the given HTTP verbs.
		 *
		 * @param methods One or more HTTP verbs (case-insensitive)
		 *
		 * @return This builder
		 */
		public RouteBuilder withMethods( String... methods ) {
			for ( String method : methods ) {
				this.methods.add( method.toUpperCase() );
			}
			return this;
		}

		/**
		 * Finish the route, mapping it to the given {@code Handler.action} target.
		 *
		 * @param target The target event, e.g. {@code Items.show}
		 *
		 * @return The owning {@link Router} for chaining
		 */
		public Router to( String target ) {
			int dot = target.lastIndexOf( '.' );
			if ( dot <= 0 ) {
				throw new IllegalArgumentException( "Route target must be in 'Handler.action' form: " + target );
			}
			String	handler	= target.substring( 0, dot );
			String	action	= target.substring( dot + 1 );
			this.router.register( new Route( this.name, this.pattern, handler, action, this.methods ) );
			return this.router;
		}
	}
}
