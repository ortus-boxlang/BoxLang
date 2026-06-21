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

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * End-to-end tests for the front-controller flow against a REAL {@link BoxRuntime}: the
 * dispatcher loads fixture handler classes, runs the action first, and the
 * {@link ViewRenderer} renders the chosen view inside the layout — capturing actual HTML.
 * <p>
 * Fixtures live under {@code src/test/resources/app}. No Android required.
 */
class MVCDispatcherTest {

	static BoxRuntime	runtime;
	static String		viewsRoot;
	static String		layoutsRoot;

	RoutingService		routingService;
	ViewRenderer		viewRenderer;
	MVCDispatcher		dispatcher;
	IBoxContext			context;

	@BeforeAll
	static void setUpClass() {
		runtime = BoxRuntime.getInstance( true );

		Path appDir = Paths.get( "src/test/resources/app" ).toAbsolutePath();
		viewsRoot	= appDir.resolve( "views" ).toString();
		layoutsRoot	= appDir.resolve( "layouts" ).toString();

		// Map "/app" to the fixture so handlers resolve as app.handlers.Items.
		runtime.getConfiguration().registerMapping( "/app", Struct.of(
		    Key.path, appDir.toString(),
		    Key.external, true
		) );
	}

	@BeforeEach
	void setUpEach() {
		this.context		= new ScriptingRequestBoxContext();
		this.routingService	= new RoutingService();
		this.viewRenderer	= new ViewRenderer( runtime, viewsRoot, layoutsRoot );
		this.dispatcher		= new MVCDispatcher( runtime, this.routingService, this.viewRenderer, "app.handlers" );

		Router router = this.routingService.getRouter();
		router.setDefaultEvent( "Main.index" );
		router.get( "/items" ).withName( "items" ).to( "Items.list" );
		router.get( "/items/:id" ).to( "Items.show" );
		router.post( "/items/add" ).to( "Items.add" );
	}

	@DisplayName( "ViewRenderer renders a view wrapped in its layout with rc in scope" )
	@Test
	void testRenderViewInLayout() {
		IStruct rc = new Struct();
		rc.put( Key.of( "items" ), new Object[] { "Apple", "Banana" } );

		MVCEvent event = new MVCEvent( rc, "GET" );
		event.setView( "items/list" );

		String html = this.viewRenderer.render( this.context, event );

		// Layout chrome is present...
		assertThat( html ).contains( "<h1>My App</h1>" );
		assertThat( html ).contains( "<main>" );
		// ...wrapping the view content, which read rc.
		assertThat( html ).contains( "[Apple]" );
		assertThat( html ).contains( "[Banana]" );
	}

	@DisplayName( "noLayout() renders the bare view without layout chrome" )
	@Test
	void testRenderNoLayout() {
		IStruct rc = new Struct();
		rc.put( Key.of( "items" ), new Object[] { "Solo" } );

		MVCEvent event = new MVCEvent( rc, "GET" );
		event.setView( "items/list" ).noLayout();

		String html = this.viewRenderer.render( this.context, event );
		assertThat( html ).contains( "[Solo]" );
		assertThat( html ).doesNotContain( "<h1>My App</h1>" );
	}

	@DisplayName( "Dispatch runs the handler action first, then renders the chosen view" )
	@Test
	void testDispatchList() {
		DispatchResult result = this.dispatcher.dispatch( this.context, "/items", "GET", null );

		assertThat( result.isRelocate() ).isFalse();
		assertThat( result.getHtml() ).contains( "<h1>My App</h1>" );
		assertThat( result.getHtml() ).contains( "[Apple]" );
		assertThat( result.getHtml() ).contains( "[Cherry]" );
	}

	@DisplayName( "Dispatch binds a path parameter to the action and into rc" )
	@Test
	void testDispatchShowWithPathParam() {
		DispatchResult result = this.dispatcher.dispatch( this.context, "/items/42", "GET", null );

		assertThat( result.isRelocate() ).isFalse();
		assertThat( result.getHtml() ).contains( "Item: 42" );
	}

	@DisplayName( "A relocating action short-circuits rendering" )
	@Test
	void testDispatchRelocate() {
		IStruct params = new Struct();
		params.put( Key.of( "title" ), "Widget" );

		DispatchResult result = this.dispatcher.dispatch( this.context, "/items/add", "POST", params );

		assertThat( result.isRelocate() ).isTrue();
		assertThat( result.getRelocateTarget() ).startsWith( "/items?notice=" );
		assertThat( result.getHtml() ).isNull();
	}

	@DisplayName( "A relocate can carry data via the query string (no flash/session needed)" )
	@Test
	void testNoticeAcrossRelocate() {
		IStruct params = new Struct();
		params.put( Key.of( "title" ), "Widget" );

		// Request 1: POST that relocates with a notice in the query string.
		DispatchResult post = this.dispatcher.dispatch( this.context, "/items/add", "POST", params );
		assertThat( post.isRelocate() ).isTrue();

		// Request 2: the relocated GET parses the query string into rc and renders it.
		DispatchResult get = this.dispatcher.dispatch( this.context, post.getRelocateTarget(), "GET", null );
		assertThat( get.getHtml() ).contains( "Added: Widget" );
	}

	@DisplayName( "Query-string parameters are parsed into rc" )
	@Test
	void testQueryStringIntoRC() {
		DispatchResult result = this.dispatcher.dispatch( this.context, "/items?notice=Hello%20World", "GET", null );
		assertThat( result.getHtml() ).contains( "Hello World" );
	}
}
