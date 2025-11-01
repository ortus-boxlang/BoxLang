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
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for MiniConsole functionality with CustomInputStreamReader
 */
public class MiniConsoleTest {

	@Test
	@DisplayName( "Test MiniConsole can be instantiated" )
	public void testMiniConsoleInstantiation() {
		try ( MiniConsole console = new MiniConsole() ) {
			assertThat( console ).isNotNull();
			assertThat( console.getPrompt() ).isNotNull();
		}
	}

	@Test
	@DisplayName( "Test MiniConsole with custom prompt" )
	public void testCustomPrompt() {
		try ( MiniConsole console = new MiniConsole( "test> " ) ) {
			assertThat( console.getPrompt() ).isEqualTo( "test> " );
		}
	}

	@Test
	@DisplayName( "Test MiniConsole prompt setter" )
	public void testPromptSetter() {
		try ( MiniConsole console = new MiniConsole() ) {
			console.setPrompt( "custom> " );
			assertThat( console.getPrompt() ).isEqualTo( "custom> " );
		}
	}

	@Test
	@DisplayName( "Test MiniConsole history management" )
	public void testHistoryManagement() {
		try ( MiniConsole console = new MiniConsole() ) {
			// Test adding to history
			console.addToHistory( "command1" );
			console.addToHistory( "command2" );
			console.addToHistory( "command3" );

			assertThat( console.getHistory() ).hasSize( 3 );
			assertThat( console.getHistory() ).containsExactly( "command1", "command2", "command3" ).inOrder();

			// Test duplicate consecutive commands are not added
			console.addToHistory( "command3" );
			assertThat( console.getHistory() ).hasSize( 3 );

			// Test last command
			assertThat( console.getLastCommand() ).isEqualTo( "command3" );

			// Test previous command
			assertThat( console.getPreviousCommand() ).isEqualTo( "command2" );

			// Test clear history
			console.clearHistory();
			assertThat( console.getHistory() ).isEmpty();
		}
	}

	@Test
	@DisplayName( "Test MiniConsole history size limit" )
	public void testHistorySizeLimit() {
		try ( MiniConsole console = new MiniConsole() ) {
			console.setMaxHistorySize( 5 );

			// Add more than max
			for ( int i = 1; i <= 10; i++ ) {
				console.addToHistory( "command" + i );
			}

			// Should only keep the last 5
			assertThat( console.getHistory() ).hasSize( 5 );
			assertThat( console.getHistory().get( 0 ) ).isEqualTo( "command6" );
			assertThat( console.getHistory().get( 4 ) ).isEqualTo( "command10" );
		}
	}

	@Test
	@DisplayName( "Test MiniConsole getHistoryCommand" )
	public void testGetHistoryCommand() {
		try ( MiniConsole console = new MiniConsole() ) {
			console.addToHistory( "cmd1" );
			console.addToHistory( "cmd2" );
			console.addToHistory( "cmd3" );

			// Test 1-based indexing
			assertThat( console.getHistoryCommand( 1 ) ).isEqualTo( "cmd1" );
			assertThat( console.getHistoryCommand( 2 ) ).isEqualTo( "cmd2" );
			assertThat( console.getHistoryCommand( 3 ) ).isEqualTo( "cmd3" );

			// Test out of bounds
			assertThat( console.getHistoryCommand( 0 ) ).isNull();
			assertThat( console.getHistoryCommand( 4 ) ).isNull();
		}
	}

	@Test
	@DisplayName( "Test MiniConsole ignores empty commands" )
	public void testIgnoreEmptyCommands() {
		try ( MiniConsole console = new MiniConsole() ) {
			console.addToHistory( null );
			console.addToHistory( "" );
			console.addToHistory( "   " );

			assertThat( console.getHistory() ).isEmpty();
		}
	}

	@Test
	@DisplayName( "Test MiniConsole tab provider registration" )
	public void testTabProviderRegistration() {
		try ( MiniConsole console = new MiniConsole() ) {
			// Create a simple test provider
			var provider = new ortus.boxlang.runtime.cli.providers.AbstractTabProvider() {

				@Override
				public String getProviderName() {
					return "test-provider";
				}

				@Override
				public int getPriority() {
					return 100;
				}

				@Override
				public boolean canProvideCompletions( String input, int cursorPosition ) {
					return false;
				}

				@Override
				public java.util.List<TabCompletion> getCompletions( String input, int cursorPosition ) {
					return java.util.Collections.emptyList();
				}
			};

			console.registerTabProvider( provider );
			// If no exception thrown, registration succeeded
			assertThat( console ).isNotNull();
		}
	}

	@Test
	@DisplayName( "Test MiniConsole utility methods" )
	public void testUtilityMethods() {
		// Test color utility methods
		assertThat( MiniConsole.color( 255 ) ).contains( "255" );
		assertThat( MiniConsole.background( 128 ) ).contains( "128" );
		assertThat( MiniConsole.reset() ).isNotEmpty();
	}

	@Test
	@DisplayName( "Test MiniConsole CODES enum" )
	public void testCodesEnum() {
		// Test that all CODES can be accessed
		assertThat( MiniConsole.CODES.RESET.code() ).isNotEmpty();
		assertThat( MiniConsole.CODES.RED.code() ).isNotEmpty();
		assertThat( MiniConsole.CODES.GREEN.code() ).isNotEmpty();
		assertThat( MiniConsole.CODES.CURSOR_UP.code() ).isNotEmpty();
		assertThat( MiniConsole.CODES.CLEAR_LINE.code() ).isNotEmpty();

		// Test get by name
		assertThat( MiniConsole.CODES.get( "RED" ) ).isNotEmpty();
		assertThat( MiniConsole.CODES.get( "red" ) ).isNotEmpty();
		assertThat( MiniConsole.CODES.get( "RESET" ) ).isNotEmpty();
	}

	@Test
	@DisplayName( "Test MiniConsole CODES enum invalid name" )
	public void testCodesEnumInvalidName() {
		try {
			MiniConsole.CODES.get( "INVALID_CODE" );
			fail( "Should have thrown BoxRuntimeException" );
		} catch ( ortus.boxlang.runtime.types.exceptions.BoxRuntimeException e ) {
			assertThat( e.getMessage() ).contains( "Invalid ANSI code name" );
		}
	}

	@Test
	@DisplayName( "Test MiniConsole with syntax highlighter" )
	public void testSyntaxHighlighter() {
		ISyntaxHighlighter highlighter = input -> "[HIGHLIGHTED]" + input;

		try ( MiniConsole console = new MiniConsole( "> ", highlighter ) ) {
			assertThat( console.getSyntaxHighlighter() ).isNotNull();
			assertThat( console.getSyntaxHighlighter().highlight( "test" ) ).isEqualTo( "[HIGHLIGHTED]test" );
		}
	}

	@Test
	@DisplayName( "Test MiniConsole setSyntaxHighlighter" )
	public void testSetSyntaxHighlighter() {
		try ( MiniConsole console = new MiniConsole() ) {
			assertThat( console.getSyntaxHighlighter() ).isNull();

			ISyntaxHighlighter highlighter = input -> input.toUpperCase();
			console.setSyntaxHighlighter( highlighter );

			assertThat( console.getSyntaxHighlighter() ).isNotNull();
			assertThat( console.getSyntaxHighlighter().highlight( "test" ) ).isEqualTo( "TEST" );
		}
	}
}
