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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * Unit tests for the {@link MVCEvent} request event + collection ({@code rc}).
 */
class MVCEventTest {

	@BeforeAll
	static void setUp() {
		// Boot the runtime so the core type system (Struct/Key) is fully initialized.
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "It defaults to the 'main' layout and no view" )
	@Test
	void testDefaults() {
		MVCEvent event = new MVCEvent( "GET", null );
		assertThat( event.getLayout() ).isEqualTo( MVCEvent.DEFAULT_LAYOUT );
		assertThat( event.getView() ).isNull();
		assertThat( event.isNoLayout() ).isFalse();
		assertThat( event.isRelocating() ).isFalse();
		assertThat( event.getHTTPMethod() ).isEqualTo( "GET" );
	}

	@DisplayName( "setView and setLayout drive what gets rendered" )
	@Test
	void testSetViewAndLayout() {
		MVCEvent event = new MVCEvent( "GET", null );
		event.setView( "items/show" ).setLayout( "admin" );

		assertThat( event.getView() ).isEqualTo( "items/show" );
		assertThat( event.getLayout() ).isEqualTo( "admin" );
	}

	@DisplayName( "noLayout() suppresses the layout" )
	@Test
	void testNoLayout() {
		MVCEvent event = new MVCEvent( "GET", null );
		event.setView( "fragment" ).noLayout();
		assertThat( event.isNoLayout() ).isTrue();
	}

	@DisplayName( "The request collection is readable and writable" )
	@Test
	void testCollection() {
		Struct seed = new Struct();
		seed.put( Key.of( "id" ), "42" );

		MVCEvent event = new MVCEvent( seed, "GET", null );
		assertThat( event.getValue( "id" ) ).isEqualTo( "42" );
		assertThat( event.valueExists( "id" ) ).isTrue();

		event.setValue( "title", "Widget" );
		assertThat( event.getValue( "title" ) ).isEqualTo( "Widget" );
		assertThat( event.getCollection().get( Key.of( "title" ) ) ).isEqualTo( "Widget" );
		assertThat( event.getRC() ).isSameInstanceAs( event.getCollection() );
	}

	@DisplayName( "getValue returns the default when the key is absent" )
	@Test
	void testGetValueDefault() {
		MVCEvent event = new MVCEvent( "GET", null );
		assertThat( event.getValue( "missing", "fallback" ) ).isEqualTo( "fallback" );
	}

	@DisplayName( "relocate() flags the event for redirect" )
	@Test
	void testRelocate() {
		MVCEvent event = new MVCEvent( "POST", null );
		event.relocate( "/items" );
		assertThat( event.isRelocating() ).isTrue();
		assertThat( event.getRelocateTarget() ).isEqualTo( "/items" );
	}

	@DisplayName( "The flash scope is accessible from the event" )
	@Test
	void testFlashAccess() {
		FlashScope	flash	= new FlashScope();
		MVCEvent	event	= new MVCEvent( "POST", flash );
		assertThat( event.getFlash() ).isSameInstanceAs( flash );
	}
}
