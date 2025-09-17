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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.cli.TabCompletion;
import ortus.boxlang.runtime.cli.TabProvider;

/**
 * Tab completion provider for BoxLang Built-in Functions (BIFs).
 *
 * This provider handles completions for function calls in BoxLang code.
 * It matches against available BIF names from the runtime and provides
 * intelligent completion for function names.
 *
 * <h3>Completion Examples:</h3>
 * <ul>
 * <li>"arrayL" + TAB → completes to "arrayLen("</li>
 * <li>"writeO" + TAB → completes to "writeOutput("</li>
 * <li>"structK" + TAB → completes to "structKeyExists("</li>
 * </ul>
 */
public class BifTabProvider implements TabProvider {

	private final Set<String>	bifNames;
	private final boolean		includeParentheses;

	/**
	 * Constructor with BIF names.
	 *
	 * @param bifNames Set of available BIF names (case-insensitive)
	 */
	public BifTabProvider( Set<String> bifNames ) {
		this( bifNames, true );
	}

	/**
	 * Constructor with BIF names and parentheses option.
	 *
	 * @param bifNames           Set of available BIF names (case-insensitive)
	 * @param includeParentheses Whether to include opening parenthesis in completions
	 */
	public BifTabProvider( Set<String> bifNames, boolean includeParentheses ) {
		this.bifNames			= bifNames;
		this.includeParentheses	= includeParentheses;
	}

	@Override
	public boolean canProvideCompletions( String input, int cursorPosition ) {
		if ( input == null || input.isEmpty() ) {
			return false;
		}

		// Get the word at cursor position
		String currentWord = getCurrentWord( input, cursorPosition );

		// Can provide completions if:
		// 1. We have a partial word (at least 1 character)
		// 2. It starts with a letter (function names must start with letter)
		// 3. It's not already a component declaration (starts with bx:)
		return currentWord.length() > 0
		    && Character.isLetter( currentWord.charAt( 0 ) )
		    && !currentWord.startsWith( "bx:" );
	}

	@Override
	public List<TabCompletion> getCompletions( String input, int cursorPosition ) {
		List<TabCompletion> completions = new ArrayList<>();

		if ( input == null || bifNames == null || bifNames.isEmpty() ) {
			return completions;
		}

		String currentWord = getCurrentWord( input, cursorPosition );
		if ( currentWord.isEmpty() ) {
			return completions;
		}

		String lowerCurrentWord = currentWord.toLowerCase();

		// Find matching BIFs
		for ( String bifName : bifNames ) {
			String lowerBifName = bifName.toLowerCase();

			if ( lowerBifName.startsWith( lowerCurrentWord ) ) {
				// Create completion text
				String completionText = bifName;
				if ( includeParentheses && !input.substring( cursorPosition ).startsWith( "(" ) ) {
					completionText += "(";
				}

				// Create display text with highlighting
				String			displayText	= "\033[33m" + completionText + "\033[0m"; // Yellow color for functions

				// Find word boundaries for replacement
				int				wordStart	= findWordStart( input, cursorPosition );

				TabCompletion	completion	= new TabCompletion(
				    completionText,
				    displayText,
				    "Built-in function: " + bifName,
				    wordStart,  // Start replacement from word beginning
				    cursorPosition  // End replacement at cursor
				);

				completions.add( completion );
			}
		}

		// Sort completions alphabetically
		completions.sort( ( a, b ) -> a.getText().compareToIgnoreCase( b.getText() ) );

		return completions;
	}

	/**
	 * Gets the current word being typed at the cursor position.
	 */
	private String getCurrentWord( String input, int cursorPosition ) {
		if ( input == null || input.isEmpty() ) {
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
	 * Finds the start of the current word for tab completion.
	 */
	private int findWordStart( String input, int fromPosition ) {
		if ( input.isEmpty() || fromPosition <= 0 ) {
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
	 * Checks if a character is part of a word for completion purposes.
	 */
	private boolean isWordCharacter( char c ) {
		return Character.isLetterOrDigit( c ) || c == '_';
	}

	@Override
	public String getProviderName() {
		return "BifTabProvider";
	}

	@Override
	public int getPriority() {
		return 150; // Medium priority - lower than components but higher than generic
	}
}