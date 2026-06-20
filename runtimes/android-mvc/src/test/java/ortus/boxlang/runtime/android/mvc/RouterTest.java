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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Router} and {@link Route} resolution logic.
 */
class RouterTest {

	@DisplayName( "It maps the root path to the configured default event" )
	@Test
	void testDefaultEvent() {
		Router		router	= new Router().setDefaultEvent( "Main.index" );
		RouteMatch	match	= router.resolve( "/", "GET" );

		assertThat( match.getHandler() ).isEqualTo( "Main" );
		assertThat( match.getAction() ).isEqualTo( "index" );
		assertThat( match.getEvent() ).isEqualTo( "Main.index" );
		assertThat( match.isExplicit() ).isFalse();
	}

	@DisplayName( "It matches an explicit route with a path parameter" )
	@Test
	void testPathParam() {
		Router router = new Router();
		router.route( "/items/:id" ).to( "Items.show" );

		RouteMatch match = router.resolve( "/items/42", "GET" );
		assertThat( match.getHandler() ).isEqualTo( "Items" );
		assertThat( match.getAction() ).isEqualTo( "show" );
		assertThat( match.getParams() ).containsEntry( "id", "42" );
		assertThat( match.isExplicit() ).isTrue();
	}

	@DisplayName( "It extracts multiple path parameters" )
	@Test
	void testMultiplePathParams() {
		Router router = new Router();
		router.route( "/blog/:year/:slug" ).to( "Blog.entry" );

		RouteMatch match = router.resolve( "/blog/2026/hello-world", "GET" );
		assertThat( match.getParams() ).containsEntry( "year", "2026" );
		assertThat( match.getParams() ).containsEntry( "slug", "hello-world" );
	}

	@DisplayName( "It honors HTTP method constraints" )
	@Test
	void testMethodConstraint() {
		Router router = new Router();
		router.post( "/items/add" ).to( "Items.add" );

		// GET should NOT match the POST-only route, so it falls back to convention.
		RouteMatch get = router.resolve( "/items/add", "GET" );
		assertThat( get.isExplicit() ).isFalse();
		assertThat( get.getHandler() ).isEqualTo( "Items" );
		assertThat( get.getAction() ).isEqualTo( "add" );

		// POST matches the explicit route.
		RouteMatch post = router.resolve( "/items/add", "POST" );
		assertThat( post.isExplicit() ).isTrue();
		assertThat( post.getEvent() ).isEqualTo( "Items.add" );
	}

	@DisplayName( "It resolves by convention when no explicit route matches" )
	@Test
	void testConventionResolution() {
		Router		router		= new Router();

		RouteMatch	withAction	= router.resolve( "/users/save", "GET" );
		assertThat( withAction.getHandler() ).isEqualTo( "Users" );
		assertThat( withAction.getAction() ).isEqualTo( "save" );

		RouteMatch handlerOnly = router.resolve( "/users", "GET" );
		assertThat( handlerOnly.getHandler() ).isEqualTo( "Users" );
		assertThat( handlerOnly.getAction() ).isEqualTo( "index" );
	}

	@DisplayName( "It finds routes by name for reverse lookups" )
	@Test
	void testNamedRoute() {
		Router router = new Router();
		router.get( "/items" ).withName( "items" ).to( "Items.list" );

		Route named = router.findByName( "items" );
		assertThat( named ).isNotNull();
		assertThat( named.getHandler() ).isEqualTo( "Items" );
		assertThat( named.getAction() ).isEqualTo( "list" );
	}

	@DisplayName( "It returns the first registered route when several match" )
	@Test
	void testFirstMatchWins() {
		Router router = new Router();
		router.route( "/items/:id" ).to( "Items.show" );
		router.route( "/items/featured" ).to( "Items.featured" );

		// The :id route is registered first, so it wins.
		RouteMatch match = router.resolve( "/items/featured", "GET" );
		assertThat( match.getAction() ).isEqualTo( "show" );
		assertThat( match.getParams() ).containsEntry( "id", "featured" );
	}

	@DisplayName( "It normalizes trailing slashes and missing leading slashes" )
	@Test
	void testNormalization() {
		Router router = new Router();
		router.route( "items" ).to( "Items.list" );

		assertThat( router.resolve( "/items/", "GET" ).getEvent() ).isEqualTo( "Items.list" );
		assertThat( router.resolve( "/items", "GET" ).getEvent() ).isEqualTo( "Items.list" );
	}

	@DisplayName( "A route target must be in Handler.action form" )
	@Test
	void testInvalidTarget() {
		Router router = new Router();
		assertThrows( IllegalArgumentException.class, () -> router.route( "/bad" ).to( "NoActionHere" ) );
	}

	@DisplayName( "It matches any method when the route is unconstrained" )
	@Test
	void testAnyMethod() {
		Router router = new Router();
		router.route( "/about" ).to( "Pages.about" );

		assertThat( router.resolve( "/about", "GET" ).isExplicit() ).isTrue();
		assertThat( router.resolve( "/about", "POST" ).isExplicit() ).isTrue();
		assertThat( router.resolve( "/about", "DELETE" ).isExplicit() ).isTrue();
	}
}
