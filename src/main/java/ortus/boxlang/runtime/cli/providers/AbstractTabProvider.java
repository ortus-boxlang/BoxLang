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

import java.util.Comparator;
import java.util.List;

import ortus.boxlang.runtime.cli.TabCompletion;

/**
 * Abstract base class for tab completion providers that provides common utility methods.
 *
 * This class implements the {@link ITabProvider} interface and provides reusable utility
 * methods for word parsing, character classification, and completion management that are
 * commonly needed by tab completion providers.
 *
 * <h3>Common Utilities Provided:</h3>
 * <ul>
 * <li>Word boundary detection and parsing</li>
 * <li>Character classification for completion contexts</li>
 * <li>Completion list sorting and formatting</li>
 * <li>Input validation and cursor position handling</li>
 * </ul>
 *
 * <h3>Extending this class:</h3>
 * Subclasses must implement:
 * <ul>
 * <li>{@link #canProvideCompletions(String, int)} - to determine if they can handle the context</li>
 * <li>{@link #getCompletions(String, int)} - to provide actual completions</li>
 * </ul>
 *
 * Subclasses may override:
 * <ul>
 * <li>{@link #getProviderName()} - to provide a custom provider name</li>
 * <li>{@link #getPriority()} - to set provider priority</li>
 * <li>{@link #isWordCharacter(char)} - to customize word character detection</li>
 * </ul>
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.6.0
 */
public abstract class AbstractTabProvider implements ITabProvider {

	/**
	 * Default completion sorting comparator (case-insensitive alphabetical)
	 */
	protected static final Comparator<TabCompletion>	ALPHABETICAL_SORTER	= ( a, b ) -> a.getText().compareToIgnoreCase( b.getText() );

	/**
	 * BX Component prefix
	 */
	protected static final String						COMPONENT_PREFIX	= "bx:";

	/**
	 * Validates input parameters and returns false if invalid.
	 *
	 * @param input          The input string to validate
	 * @param cursorPosition The cursor position to validate
	 *
	 * @return true if input is valid, false otherwise
	 */
	protected boolean isValidInput( String input, int cursorPosition ) {
		return input != null &&
		    !input.isEmpty() &&
		    cursorPosition >= 0 &&
		    cursorPosition <= input.length();
	}

	/**
	 * Gets the current word being typed at the cursor position.
	 *
	 * A word is defined as a sequence of word characters (as determined by
	 * {@link #isWordCharacter(char)}) that ends at or before the cursor position.
	 *
	 * Examples:
	 * - Input: "arrayLen", Cursor: 8 → Current Word: "arrayLen"
	 * - Input: "writeOutput(", Cursor: 12 → Current Word: "writeOutput"
	 * - Input: "structKeyExists", Cursor: 16 → Current Word: "structKeyExists"
	 * - Input: "bx:query", Cursor: 8 → Current Word: "bx:query"
	 * - Input: "bx:", Cursor: 3 → Current Word: "bx:"
	 * - Input: "bx:query param", Cursor: 8 → Current Word: "bx:query"
	 *
	 * @param input          The input string
	 * @param cursorPosition The cursor position
	 *
	 * @return The current word, or empty string if none found
	 */
	protected String getCurrentWord( String input, int cursorPosition ) {
		if ( !isValidInput( input, cursorPosition ) ) {
			return "";
		}

		// If cursor is at position 0 or beyond input length, no word
		if ( cursorPosition == 0 || cursorPosition > input.length() ) {
			return "";
		}

		// Check if the character just before cursor is a word character
		// If not, we're not in a word
		char charBeforeCursor = input.charAt( cursorPosition - 1 );
		if ( !isWordCharacter( charBeforeCursor ) ) {
			return "";
		}

		int	start	= findWordStart( input, cursorPosition );
		int	end		= Math.min( cursorPosition, input.length() );

		if ( start >= end ) {
			return "";
		}

		return input.substring( start, end );
	}

	/**
	 * Finds the start position of the current word for tab completion.
	 *
	 * This method searches backwards from the given position to find the beginning
	 * of the current word (sequence of word characters).
	 *
	 * Examples:
	 * - Input: "arrayLen", From Position: 8 → Word Start: 0
	 * - Input: "writeOutput(", From Position: 12 → Word Start: 0
	 * - Input: "structKeyExists", From Position: 16 → Word Start: 0
	 * - Input: "bx:query", From Position: 8 → Word Start: 0
	 * - Input: "bx:", From Position: 3 → Word Start: 0
	 * - Input: "bx:query param", From Position: 8 → Word Start: 0
	 * - Input: "first bx:one second bx:two", From Position: 20 → Word Start: 17
	 *
	 * @param input        The input string
	 * @param fromPosition The position to search backwards from
	 *
	 * @return The start position of the current word
	 */
	protected int findWordStart( String input, int fromPosition ) {
		if ( input == null || input.isEmpty() || fromPosition <= 0 ) {
			return 0;
		}

		int pos = Math.min( fromPosition - 1, input.length() - 1 );

		// Move backwards while we see word characters
		while ( pos > 0 && isWordCharacter( input.charAt( pos ) ) ) {
			pos--;
		}

		// If we stopped on a non-word character, move forward one
		if ( pos > 0 && !isWordCharacter( input.charAt( pos ) ) ) {
			pos++;
		}

		return pos;
	}

	/**
	 * Finds the end position of the current word for tab completion.
	 *
	 * This method searches forwards from the given position to find the end
	 * of the current word (sequence of word characters).
	 *
	 * @param input        The input string
	 * @param fromPosition The position to search forwards from
	 *
	 * @return The end position of the current word
	 */
	protected int findWordEnd( String input, int fromPosition ) {
		if ( input == null || input.isEmpty() || fromPosition >= input.length() ) {
			return fromPosition;
		}

		int pos = fromPosition;

		// Move forwards while we see word characters
		while ( pos < input.length() && isWordCharacter( input.charAt( pos ) ) ) {
			pos++;
		}

		return pos;
	}

	/**
	 * Checks if a character is part of a word for completion purposes.
	 *
	 * By default, word characters are letters, digits, and underscores.
	 * Subclasses can override this to customize word boundary detection
	 * for their specific completion contexts.
	 *
	 * @param c The character to check
	 *
	 * @return true if the character is part of a word
	 */
	protected boolean isWordCharacter( char c ) {
		return Character.isLetterOrDigit( c ) || c == '_';
	}

	/**
	 * Checks if a character represents a word boundary (whitespace or special chars).
	 *
	 * @param c The character to check
	 *
	 * @return true if the character is a word boundary
	 */
	protected boolean isWordBoundary( char c ) {
		return Character.isWhitespace( c ) || "(){}[]<>,.;:".indexOf( c ) >= 0;
	}

	/**
	 * Sorts a list of completions alphabetically (case-insensitive).
	 *
	 * This is a convenience method that sorts completions in-place using
	 * the default alphabetical sorter.
	 *
	 * @param completions The list of completions to sort
	 */
	protected void sortCompletionsAlphabetically( List<TabCompletion> completions ) {
		if ( completions != null ) {
			completions.sort( ALPHABETICAL_SORTER );
		}
	}

	/**
	 * Creates a new TabCompletion with the specified properties.
	 *
	 * @param text         The completion text to insert
	 * @param description  The description of the completion
	 * @param replaceStart The start position for replacement
	 * @param replaceEnd   The end position for replacement
	 *
	 * @return A new TabCompletion instance
	 */
	protected TabCompletion createCompletion( String text, String description, int replaceStart, int replaceEnd ) {
		return new TabCompletion( text, text, description, replaceStart, replaceEnd );
	}

	/**
	 * Checks if the input contains a specific prefix at or before the cursor position.
	 *
	 * @param input          The input string
	 * @param cursorPosition The cursor position
	 * @param prefix         The prefix to search for
	 *
	 * @return The index of the prefix, or -1 if not found
	 */
	protected int findPrefixIndex( String input, int cursorPosition, String prefix ) {
		if ( !isValidInput( input, cursorPosition ) || prefix == null ) {
			return -1;
		}

		String upToCursor = input.substring( 0, Math.min( cursorPosition, input.length() ) );
		return upToCursor.lastIndexOf( prefix );
	}

	/**
	 * Extracts text after a specific prefix, up to the cursor position.
	 *
	 * @param input          The input string
	 * @param cursorPosition The cursor position
	 * @param prefix         The prefix to search for
	 *
	 * @return The text after the prefix, or empty string if prefix not found
	 */
	protected String getTextAfterPrefix( String input, int cursorPosition, String prefix ) {
		int prefixIndex = findPrefixIndex( input, cursorPosition, prefix );
		if ( prefixIndex == -1 ) {
			return "";
		}

		String upToCursor = input.substring( 0, Math.min( cursorPosition, input.length() ) );
		return upToCursor.substring( prefixIndex + prefix.length() );
	}

	/**
	 * Checks if there's whitespace in the text after a prefix.
	 *
	 * @param input          The input string
	 * @param cursorPosition The cursor position
	 * @param prefix         The prefix to check after
	 *
	 * @return true if there's whitespace after the prefix
	 */
	protected boolean hasWhitespaceAfterPrefix( String input, int cursorPosition, String prefix ) {
		String afterPrefix = getTextAfterPrefix( input, cursorPosition, prefix );
		return afterPrefix.contains( " " ) || afterPrefix.contains( "\t" );
	}
}