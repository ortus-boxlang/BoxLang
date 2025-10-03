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
import java.util.Objects;
import java.util.Set;

import ortus.boxlang.runtime.cli.TabCompletion;

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
public class BifTabProvider extends AbstractTabProvider {

	/**
	 * ----------------------------------------------------------------------------
	 * Properties
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Set of available BIF names (case-insensitive)
	 */
	private final Set<String> bifNames;

	/**
	 * ----------------------------------------------------------------------------
	 * Constructors
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Constructor with BIF names.
	 *
	 * @param bifNames Set of available BIF names (case-insensitive)
	 */
	public BifTabProvider( Set<String> bifNames ) {
		Objects.requireNonNull( bifNames, "bifNames cannot be null" );
		this.bifNames = bifNames;
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Methods
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Can this provider handle completions for the given input?
	 * It handles function name completions when the current word
	 * starts with a letter and is not a component declaration (bx:).
	 *
	 * @param input          The input string to check.
	 * @param cursorPosition The cursor position in the input.
	 *
	 * @return true if this provider can provide completions.
	 */
	@Override
	public boolean canProvideCompletions( String input, int cursorPosition ) {
		if ( !isValidInput( input, cursorPosition ) ) {
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
		    && !currentWord.startsWith( COMPONENT_PREFIX );
	}

	/**
	 * Get completions for the given input at the cursor position.
	 */
	@Override
	public List<TabCompletion> getCompletions( String input, int cursorPosition ) {
		List<TabCompletion> completions = new ArrayList<>();

		// Early return if input is invalid
		if ( input == null || bifNames.isEmpty() ) {
			return completions;
		}

		// Get the current word at cursor position
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
				String			completionText	= bifName;
				// Find word boundaries for replacement
				int				wordStart		= findWordStart( input, cursorPosition );
				// Create completion using base class utility
				TabCompletion	completion		= createCompletion(
				    completionText,
				    "Built-in function: " + bifName,
				    wordStart,
				    cursorPosition
				);

				completions.add( completion );
			}
		}

		// Sort completions alphabetically
		sortCompletionsAlphabetically( completions );

		return completions;
	}

	@Override
	public int getPriority() {
		// Medium priority - lower than components but higher than generic
		return 150;
	}
}