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
 * the {@link FlashScope} as {@code flash}, and every {@code rc} entry as a matching named
 * argument (so {@code function show( required numeric id )} just works for {@code /items/42}).
 * <p>
 * Android-free: depends only on the BoxLang core runtime, so it is unit-testable on a plain JVM.
 */
public class MVCDispatcher {

	/**
	 * The BoxLang runtime.
	 */
	private final BoxRuntime		runtime;

	/**
	 * The routing service (router + flash).
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
		// Rotate flash so this request can read what the previous request staged.
		FlashScope flash = this.routingService.getFlash();
		flash.persist();

		// 1. Resolve route
		RouteMatch	match	= this.routingService.resolve( path, method );

		// 2. Build the request collection (rc): incoming params first, then path placeholders.
		IStruct		rc		= new Struct();
		if ( params != null ) {
			rc.putAll( params );
		}
		match.getParams().forEach( ( key, value ) -> rc.put( Key.of( key ), value ) );

		MVCEvent event = new MVCEvent( rc, method, flash );
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
	 * Build the named-argument map delivered to the action: {@code event}, {@code rc},
	 * {@code flash}, plus every {@code rc} entry as its own named argument.
	 *
	 * @param event The MVC event
	 *
	 * @return The named arguments map
	 */
	Map<Key, Object> buildArgs( MVCEvent event ) {
		Map<Key, Object> args = new LinkedHashMap<>();
		args.put( Key.of( "event" ), event );
		args.put( Key.of( "rc" ), event.getCollection() );
		args.put( Key.of( "flash" ), event.getFlash() );
		// Spread rc entries so actions can declare them as explicit params.
		event.getCollection().forEach( args::put );
		return args;
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
