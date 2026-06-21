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

import java.util.LinkedHashMap;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The BoxLang Android MVC front controller.
 * <p>
 * Implements the classic ColdBox request flow:
 * <ol>
 * <li>Resolve the route to a {@code handler.action} (default event / explicit / convention).</li>
 * <li>Build the request collection ({@code rc}) from incoming params + path placeholders.</li>
 * <li><b>Run the handler action FIRST</b> — it mutates {@code rc} and chooses the view/layout
 * (or relocates).</li>
 * <li>If relocating, return a relocate result; otherwise render the chosen view inside the
 * chosen layout (with {@code rc} in scope) and return the HTML.</li>
 * </ol>
 * The action receives the {@link MVCEvent} as {@code event}, the collection as {@code rc},
 * and every {@code rc} entry as a matching named argument (so
 * {@code function show( required numeric id )} just works for {@code /items/42}).
 * <p>
 * Android-free: depends only on the BoxLang core runtime, so it is unit-testable on a plain JVM.
 */
public class MVCDispatcher {

	/**
	 * The BoxLang runtime.
	 */
	private final BoxRuntime		runtime;

	/**
	 * The routing service (owns the router).
	 */
	private final RoutingService	routingService;

	/**
	 * The view/layout renderer.
	 */
	private final ViewRenderer		viewRenderer;

	/**
	 * The dot-delimited namespace handlers resolve under (e.g. {@code handlers}).
	 */
	private final String			handlersNamespace;

	/**
	 * Construct a dispatcher with the default {@code handlers} namespace.
	 *
	 * @param runtime        The BoxLang runtime
	 * @param routingService The routing service
	 * @param viewRenderer   The view renderer
	 */
	public MVCDispatcher( BoxRuntime runtime, RoutingService routingService, ViewRenderer viewRenderer ) {
		this( runtime, routingService, viewRenderer, "handlers" );
	}

	/**
	 * Construct a dispatcher.
	 *
	 * @param runtime           The BoxLang runtime
	 * @param routingService    The routing service
	 * @param viewRenderer      The view renderer
	 * @param handlersNamespace The dot-delimited handler namespace
	 */
	public MVCDispatcher( BoxRuntime runtime, RoutingService routingService, ViewRenderer viewRenderer, String handlersNamespace ) {
		this.runtime			= runtime;
		this.routingService		= routingService;
		this.viewRenderer		= viewRenderer;
		this.handlersNamespace	= handlersNamespace;
	}

	/**
	 * Dispatch a request through the full front-controller flow.
	 *
	 * @param context The request context
	 * @param path    The request path (e.g. {@code /items/42})
	 * @param method  The HTTP method (e.g. {@code GET}; may be {@code null})
	 * @param params  Incoming query/form/JSON params to seed {@code rc} (may be {@code null})
	 *
	 * @return The {@link DispatchResult} (rendered HTML or relocate target)
	 */
	public DispatchResult dispatch( IBoxContext context, String path, String method, IStruct params ) {
		// 1. Split any query string off the path and resolve the route.
		String		cleanPath	= pathOf( path );
		RouteMatch	match		= this.routingService.resolve( cleanPath, method );

		// 2. Build the request collection (rc): query string, incoming params, then path placeholders.
		IStruct		rc			= new Struct();
		parseQueryInto( path, rc );
		if ( params != null ) {
			rc.putAll( params );
		}
		match.getParams().forEach( ( key, value ) -> rc.put( Key.of( key ), value ) );

		MVCEvent event = new MVCEvent( rc, method );
		event.setCurrentEvent( match.getEvent() );

		// 3. Run the handler action FIRST.
		IClassRunnable handler = loadHandler( context, match.getHandler() );
		handler.dereferenceAndInvoke( context, Key.of( match.getAction() ), buildArgs( event ), false );

		// 4a. Relocate short-circuits rendering.
		if ( event.isRelocating() ) {
			return DispatchResult.relocate( event.getRelocateTarget() );
		}

		// 4b. Apply the implicit view convention if the action did not set one.
		if ( event.getView() == null ) {
			event.setView( implicitView( match ) );
		}

		String html = this.viewRenderer.render( context, event );
		return DispatchResult.rendered( html );
	}

	/**
	 * Load a handler BoxLang class instance by its short name.
	 *
	 * @param context The context
	 * @param handler The short handler name (e.g. {@code Items})
	 *
	 * @return The instantiated handler
	 */
	IClassRunnable loadHandler( IBoxContext context, String handler ) {
		String	target			= this.handlersNamespace + "." + handler;
		Object	classInstance	= context.invokeFunction( Key.createObject, new Object[] { target } );
		if ( classInstance instanceof IClassRunnable runnable ) {
			return runnable;
		}
		throw new BoxRuntimeException( "Handler [" + target + "] did not resolve to a BoxLang class." );
	}

	/**
	 * Build the named-argument map delivered to the action: {@code event}, {@code rc}, plus
	 * every {@code rc} entry as its own named argument.
	 *
	 * @param event The MVC event
	 *
	 * @return The named arguments map
	 */
	Map<Key, Object> buildArgs( MVCEvent event ) {
		Map<Key, Object> args = new LinkedHashMap<>();
		args.put( Key.of( "event" ), event );
		args.put( Key.of( "rc" ), event.getCollection() );
		// Spread rc entries so actions can declare them as explicit params.
		event.getCollection().forEach( args::put );
		return args;
	}

	/**
	 * @param uri A path possibly containing a query string
	 *
	 * @return The path portion (everything before {@code ?})
	 */
	static String pathOf( String uri ) {
		int q = uri.indexOf( '?' );
		return q < 0 ? uri : uri.substring( 0, q );
	}

	/**
	 * Parse a URL query string into the given collection (URL-decoded).
	 *
	 * @param uri    A path possibly containing a query string
	 * @param target The collection to populate
	 */
	static void parseQueryInto( String uri, IStruct target ) {
		int q = uri.indexOf( '?' );
		if ( q < 0 || q == uri.length() - 1 ) {
			return;
		}
		for ( String pair : uri.substring( q + 1 ).split( "&" ) ) {
			int eq = pair.indexOf( '=' );
			if ( eq > 0 ) {
				target.put(
				    Key.of( urlDecode( pair.substring( 0, eq ) ) ),
				    urlDecode( pair.substring( eq + 1 ) )
				);
			}
		}
	}

	private static String urlDecode( String value ) {
		return java.net.URLDecoder.decode( value, java.nio.charset.StandardCharsets.UTF_8 );
	}

	/**
	 * Derive the implicit view name for a match: {@code handler/action} (handler lower-cased).
	 *
	 * @param match The resolved route match
	 *
	 * @return The implicit view name, e.g. {@code items/list}
	 */
	String implicitView( RouteMatch match ) {
		return match.getHandler().toLowerCase() + "/" + match.getAction();
	}
}
