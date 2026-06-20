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
package ortus.boxlang.runtime.android.ui;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

/**
 * Unit tests for the {@link UINode} / {@link UI} Compose-track UI tree DSL.
 */
class UINodeTest {

	@BeforeAll
	static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "The DSL builds a typed node tree with props" )
	@Test
	void testTreeConstruction() {
		UINode tree = UI.column()
		    .child( UI.text( "Count: 0" ) )
		    .child( UI.button( "Increment" ) );

		assertThat( tree.getType() ).isEqualTo( "Column" );
		assertThat( tree.getChildren() ).hasSize( 2 );
		assertThat( tree.getChildren().get( 0 ).getType() ).isEqualTo( "Text" );
		assertThat( tree.getChildren().get( 0 ).getProp( "text" ) ).isEqualTo( "Count: 0" );
		assertThat( tree.getChildren().get( 1 ).getProp( "label" ) ).isEqualTo( "Increment" );
	}

	@DisplayName( "Event handlers are stored and retrievable by name" )
	@Test
	void testEventHandlers() {
		Runnable	handler	= () -> {
							};
		UINode		button	= UI.button( "Tap" ).on( "onClick", handler );

		assertThat( button.getHandler( "onClick" ) ).isSameInstanceAs( handler );
		assertThat( button.getHandler( "onChange" ) ).isNull();
	}

	@DisplayName( "An invokable handler can mutate state (recompose simulation)" )
	@Test
	void testHandlerInvocation() {
		AtomicInteger	count	= new AtomicInteger( 0 );
		UINode			button	= UI.button( "Increment" ).on( "onClick", ( Runnable ) count::incrementAndGet );

		// Simulate the renderer firing the click handler.
		( ( Runnable ) button.getHandler( "onClick" ) ).run();
		( ( Runnable ) button.getHandler( "onClick" ) ).run();

		assertThat( count.get() ).isEqualTo( 2 );
	}

	@DisplayName( "children(...) appends multiple nodes" )
	@Test
	void testVarargChildren() {
		UINode row = UI.row().children( UI.text( "A" ), UI.text( "B" ), UI.spacer( 8 ) );
		assertThat( row.getChildren() ).hasSize( 3 );
		assertThat( row.getChildren().get( 2 ).getProp( "size" ) ).isEqualTo( 8 );
	}

	@DisplayName( "props and chaining return the same node" )
	@Test
	void testFluentChaining() {
		UINode node = UI.text( "Hi" ).prop( "color", "#333" ).prop( "size", 16 );
		assertThat( node.getProp( "color" ) ).isEqualTo( "#333" );
		assertThat( node.getProp( "size" ) ).isEqualTo( 16 );
	}
}
