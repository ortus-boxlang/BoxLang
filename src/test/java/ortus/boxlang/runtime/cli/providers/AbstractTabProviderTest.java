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
package ortus.boxlang.runtime.cli.providers;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.cli.TabCompletion;

public class AbstractTabProviderTest {

	/**
	 * Concrete implementation of AbstractTabProvider for testing purposes
	 */
	private static class TestTabProvider extends AbstractTabProvider {

		@Override
		public boolean canProvideCompletions( String input, int cursorPosition ) {
			return isValidInput( input, cursorPosition );
		}

		@Override
		public List<TabCompletion> getCompletions( String input, int cursorPosition ) {
			return new ArrayList<>();
		}
	}

	private final TestTabProvider provider = new TestTabProvider();

	@Test
	@DisplayName( "Test isValidInput validates input correctly" )
	public void testIsValidInput() {
		// Valid inputs
		assertThat( provider.isValidInput( "hello", 5 ) ).isTrue();
		assertThat( provider.isValidInput( "hello", 0 ) ).isTrue();
		assertThat( provider.isValidInput( "hello", 3 ) ).isTrue();
		assertThat( provider.isValidInput( " ", 1 ) ).isTrue();

		// Invalid inputs
		assertThat( provider.isValidInput( null, 0 ) ).isFalse();
		assertThat( provider.isValidInput( "", 0 ) ).isFalse();
		assertThat( provider.isValidInput( "hello", -1 ) ).isFalse();
		assertThat( provider.isValidInput( "hello", 6 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test getCurrentWord extracts words correctly" )
	public void testGetCurrentWord() {
		// Simple word at end
		assertThat( provider.getCurrentWord( "hello", 5 ) ).isEqualTo( "hello" );
		assertThat( provider.getCurrentWord( "test123", 7 ) ).isEqualTo( "test123" );

		// Word in middle
		assertThat( provider.getCurrentWord( "hello world", 5 ) ).isEqualTo( "hello" );
		assertThat( provider.getCurrentWord( "hello world", 3 ) ).isEqualTo( "hel" );

		// Word with underscore
		assertThat( provider.getCurrentWord( "my_function", 11 ) ).isEqualTo( "my_function" );
		assertThat( provider.getCurrentWord( "test_var", 5 ) ).isEqualTo( "test_" );

		// No word
		assertThat( provider.getCurrentWord( " hello", 1 ) ).isEqualTo( "" );
		assertThat( provider.getCurrentWord( "(test)", 1 ) ).isEqualTo( "" );

		// Invalid input
		assertThat( provider.getCurrentWord( null, 0 ) ).isEqualTo( "" );
		assertThat( provider.getCurrentWord( "", 0 ) ).isEqualTo( "" );
		assertThat( provider.getCurrentWord( "test", -1 ) ).isEqualTo( "" );
	}

	@Test
	@DisplayName( "Test findWordStart finds word boundaries correctly" )
	public void testFindWordStart() {
		// Word at beginning
		assertThat( provider.findWordStart( "hello world", 5 ) ).isEqualTo( 0 );
		assertThat( provider.findWordStart( "test", 4 ) ).isEqualTo( 0 );

		// Word in middle
		assertThat( provider.findWordStart( "hello world", 11 ) ).isEqualTo( 6 );
		assertThat( provider.findWordStart( "foo bar baz", 7 ) ).isEqualTo( 4 );

		// With special characters
		assertThat( provider.findWordStart( "func(param)", 6 ) ).isEqualTo( 5 );
		assertThat( provider.findWordStart( "obj.method", 10 ) ).isEqualTo( 4 );

		// Edge cases
		assertThat( provider.findWordStart( "hello", 0 ) ).isEqualTo( 0 );
		assertThat( provider.findWordStart( "", 0 ) ).isEqualTo( 0 );
		assertThat( provider.findWordStart( "test", -1 ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test findWordEnd finds word boundaries correctly" )
	public void testFindWordEnd() {
		// Word at beginning
		assertThat( provider.findWordEnd( "hello world", 0 ) ).isEqualTo( 5 );
		assertThat( provider.findWordEnd( "test", 0 ) ).isEqualTo( 4 );

		// Word in middle
		assertThat( provider.findWordEnd( "hello world", 6 ) ).isEqualTo( 11 );
		assertThat( provider.findWordEnd( "foo bar baz", 4 ) ).isEqualTo( 7 );

		// With special characters
		assertThat( provider.findWordEnd( "func(param)", 5 ) ).isEqualTo( 10 );
		assertThat( provider.findWordEnd( "obj.method", 4 ) ).isEqualTo( 10 );

		// Edge cases
		assertThat( provider.findWordEnd( "hello", 5 ) ).isEqualTo( 5 );
		assertThat( provider.findWordEnd( "", 0 ) ).isEqualTo( 0 );
		assertThat( provider.findWordEnd( "test", 10 ) ).isEqualTo( 10 );
	}

	@Test
	@DisplayName( "Test isWordCharacter identifies word characters correctly" )
	public void testIsWordCharacter() {
		// Word characters
		assertThat( provider.isWordCharacter( 'a' ) ).isTrue();
		assertThat( provider.isWordCharacter( 'Z' ) ).isTrue();
		assertThat( provider.isWordCharacter( '0' ) ).isTrue();
		assertThat( provider.isWordCharacter( '9' ) ).isTrue();
		assertThat( provider.isWordCharacter( '_' ) ).isTrue();

		// Non-word characters
		assertThat( provider.isWordCharacter( ' ' ) ).isFalse();
		assertThat( provider.isWordCharacter( '(' ) ).isFalse();
		assertThat( provider.isWordCharacter( ')' ) ).isFalse();
		assertThat( provider.isWordCharacter( '.' ) ).isFalse();
		assertThat( provider.isWordCharacter( ',' ) ).isFalse();
		assertThat( provider.isWordCharacter( ':' ) ).isFalse();
		assertThat( provider.isWordCharacter( ';' ) ).isFalse();
	}

	@Test
	@DisplayName( "Test isWordBoundary identifies boundaries correctly" )
	public void testIsWordBoundary() {
		// Word boundaries
		assertThat( provider.isWordBoundary( ' ' ) ).isTrue();
		assertThat( provider.isWordBoundary( '\t' ) ).isTrue();
		assertThat( provider.isWordBoundary( '(' ) ).isTrue();
		assertThat( provider.isWordBoundary( ')' ) ).isTrue();
		assertThat( provider.isWordBoundary( '{' ) ).isTrue();
		assertThat( provider.isWordBoundary( '}' ) ).isTrue();
		assertThat( provider.isWordBoundary( '[' ) ).isTrue();
		assertThat( provider.isWordBoundary( ']' ) ).isTrue();
		assertThat( provider.isWordBoundary( '<' ) ).isTrue();
		assertThat( provider.isWordBoundary( '>' ) ).isTrue();
		assertThat( provider.isWordBoundary( ',' ) ).isTrue();
		assertThat( provider.isWordBoundary( '.' ) ).isTrue();
		assertThat( provider.isWordBoundary( ';' ) ).isTrue();
		assertThat( provider.isWordBoundary( ':' ) ).isTrue();

		// Non-boundaries
		assertThat( provider.isWordBoundary( 'a' ) ).isFalse();
		assertThat( provider.isWordBoundary( 'Z' ) ).isFalse();
		assertThat( provider.isWordBoundary( '0' ) ).isFalse();
		assertThat( provider.isWordBoundary( '_' ) ).isFalse();
	}

	@Test
	@DisplayName( "Test sortCompletionsAlphabetically sorts correctly" )
	public void testSortCompletionsAlphabetically() {
		List<TabCompletion> completions = Arrays.asList(
		    new TabCompletion( "zebra", "zebra", "Z function" ),
		    new TabCompletion( "apple", "apple", "A function" ),
		    new TabCompletion( "Banana", "Banana", "B function" ),
		    new TabCompletion( "cherry", "cherry", "C function" )
		);

		provider.sortCompletionsAlphabetically( completions );

		assertThat( completions.get( 0 ).getText() ).isEqualTo( "apple" );
		assertThat( completions.get( 1 ).getText() ).isEqualTo( "Banana" );
		assertThat( completions.get( 2 ).getText() ).isEqualTo( "cherry" );
		assertThat( completions.get( 3 ).getText() ).isEqualTo( "zebra" );
	}

	@Test
	@DisplayName( "Test createCompletion creates basic completion" )
	public void testCreateCompletion() {
		TabCompletion completion = provider.createCompletion( "variable", "A variable", 5, 12 );

		assertThat( completion.getText() ).isEqualTo( "variable" );
		assertThat( completion.getDisplayText() ).isEqualTo( "variable" );
		assertThat( completion.getDescription() ).isEqualTo( "A variable" );
		assertThat( completion.getReplaceStart() ).isEqualTo( 5 );
		assertThat( completion.getReplaceEnd() ).isEqualTo( 12 );
	}

	@Test
	@DisplayName( "Test findPrefixIndex finds prefix correctly" )
	public void testFindPrefixIndex() {
		// Prefix found
		assertThat( provider.findPrefixIndex( "hello bx:test", 13, "bx:" ) ).isEqualTo( 6 );
		assertThat( provider.findPrefixIndex( "bx:component", 12, "bx:" ) ).isEqualTo( 0 );
		assertThat( provider.findPrefixIndex( "some text bx:query", 18, "bx:" ) ).isEqualTo( 10 );

		// Prefix not found
		assertThat( provider.findPrefixIndex( "hello world", 11, "bx:" ) ).isEqualTo( -1 );
		assertThat( provider.findPrefixIndex( "test", 4, "nonexistent" ) ).isEqualTo( -1 );

		// Invalid input
		assertThat( provider.findPrefixIndex( null, 0, "bx:" ) ).isEqualTo( -1 );
		assertThat( provider.findPrefixIndex( "test", 4, null ) ).isEqualTo( -1 );
		assertThat( provider.findPrefixIndex( "test", -1, "bx:" ) ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "Test getTextAfterPrefix extracts text correctly" )
	public void testGetTextAfterPrefix() {
		// Text found
		assertThat( provider.getTextAfterPrefix( "bx:component", 12, "bx:" ) ).isEqualTo( "component" );
		assertThat( provider.getTextAfterPrefix( "hello bx:test", 13, "bx:" ) ).isEqualTo( "test" );
		assertThat( provider.getTextAfterPrefix( "bx:partial", 10, "bx:" ) ).isEqualTo( "partial" );
		assertThat( provider.getTextAfterPrefix( "bx:", 3, "bx:" ) ).isEqualTo( "" );

		// Prefix not found
		assertThat( provider.getTextAfterPrefix( "hello world", 11, "bx:" ) ).isEqualTo( "" );

		// Invalid input
		assertThat( provider.getTextAfterPrefix( null, 0, "bx:" ) ).isEqualTo( "" );
		assertThat( provider.getTextAfterPrefix( "test", 4, null ) ).isEqualTo( "" );
	}

	@Test
	@DisplayName( "Test hasWhitespaceAfterPrefix detects whitespace correctly" )
	public void testHasWhitespaceAfterPrefix() {
		// Has whitespace
		assertThat( provider.hasWhitespaceAfterPrefix( "bx: component", 13, "bx:" ) ).isTrue();
		assertThat( provider.hasWhitespaceAfterPrefix( "bx:comp onent", 13, "bx:" ) ).isTrue();
		assertThat( provider.hasWhitespaceAfterPrefix( "bx:\tcomponent", 13, "bx:" ) ).isTrue();

		// No whitespace
		assertThat( provider.hasWhitespaceAfterPrefix( "bx:component", 12, "bx:" ) ).isFalse();
		assertThat( provider.hasWhitespaceAfterPrefix( "bx:comp_onent", 13, "bx:" ) ).isFalse();

		// Prefix not found
		assertThat( provider.hasWhitespaceAfterPrefix( "hello world", 11, "bx:" ) ).isFalse();
	}

	@Test
	@DisplayName( "Test default implementation methods" )
	public void testDefaultMethods() {
		// Test that canProvideCompletions uses isValidInput
		assertThat( provider.canProvideCompletions( "test", 4 ) ).isTrue();
		assertThat( provider.canProvideCompletions( null, 0 ) ).isFalse();

		// Test that getCompletions returns empty list
		List<TabCompletion> completions = provider.getCompletions( "test", 4 );
		assertThat( completions ).isNotNull();
		assertThat( completions ).isEmpty();
	}

	@Test
	@DisplayName( "Test sortCompletionsAlphabetically handles null input" )
	public void testSortCompletionsAlphabeticallyWithNull() {
		// Should not throw exception with null input
		provider.sortCompletionsAlphabetically( null );

		// Should handle empty list
		List<TabCompletion> empty = new ArrayList<>();
		provider.sortCompletionsAlphabetically( empty );
		assertThat( empty ).isEmpty();
	}
}