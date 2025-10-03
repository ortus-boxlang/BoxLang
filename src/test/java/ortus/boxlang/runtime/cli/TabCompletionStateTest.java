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
package ortus.boxlang.runtime.cli;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TabCompletionStateTest {

	@Test
	@DisplayName( "Test nextCompletion cycles forward through completions" )
	public void testNextCompletion() {
		// Setup
		List<TabCompletion>	completions	= Arrays.asList(
		    new TabCompletion( "first", "First completion" ),
		    new TabCompletion( "second", "Second completion" ),
		    new TabCompletion( "third", "Third completion" )
		);

		TabCompletionState	state		= new TabCompletionState( "test", completions, 4, 0, 4 );

		// Test initial state
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "first" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		// Test cycling forward
		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "second" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 1 );

		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "third" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 2 );

		// Test wrapping around
		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "first" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test previousCompletion cycles backward through completions" )
	public void testPreviousCompletion() {
		// Setup
		List<TabCompletion>	completions	= Arrays.asList(
		    new TabCompletion( "first", "First completion" ),
		    new TabCompletion( "second", "Second completion" ),
		    new TabCompletion( "third", "Third completion" )
		);

		TabCompletionState	state		= new TabCompletionState( "test", completions, 4, 0, 4 );

		// Test initial state
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "first" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		// Test cycling backward (should wrap to end)
		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "third" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 2 );

		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "second" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 1 );

		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "first" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test combining nextCompletion and previousCompletion" )
	public void testCombinedNavigation() {
		// Setup
		List<TabCompletion>	completions	= Arrays.asList(
		    new TabCompletion( "alpha", "Alpha completion" ),
		    new TabCompletion( "beta", "Beta completion" ),
		    new TabCompletion( "gamma", "Gamma completion" )
		);

		TabCompletionState	state		= new TabCompletionState( "test", completions, 4, 0, 4 );

		// Start at first
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "alpha" );

		// Go forward to second
		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "beta" );

		// Go back to first
		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "alpha" );

		// Go back again (should wrap to last)
		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "gamma" );

		// Go forward (should wrap to first)
		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "alpha" );
	}

	@Test
	@DisplayName( "Test completion navigation with single item" )
	public void testSingleCompletion() {
		// Setup
		List<TabCompletion>	completions	= Arrays.asList(
		    new TabCompletion( "only", "Only completion" )
		);

		TabCompletionState	state		= new TabCompletionState( "test", completions, 4, 0, 4 );

		// Test initial state
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "only" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		// Test that navigation doesn't crash with single item
		state.nextCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "only" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		state.previousCompletion();
		assertThat( state.getCurrentCompletion().getText() ).isEqualTo( "only" );
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test completion navigation with empty list" )
	public void testEmptyCompletions() {
		// Setup
		List<TabCompletion>	completions	= Arrays.asList();

		TabCompletionState	state		= new TabCompletionState( "test", completions, 4, 0, 4 );

		// Test that navigation doesn't crash with empty list
		assertThat( state.getCurrentCompletion() ).isNull();
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		state.nextCompletion();
		assertThat( state.getCurrentCompletion() ).isNull();
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );

		state.previousCompletion();
		assertThat( state.getCurrentCompletion() ).isNull();
		assertThat( state.getCurrentIndex() ).isEqualTo( 0 );
	}
}