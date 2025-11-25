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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of tab completions at a given moment in the CLI.
 *
 * <p>
 * This class encapsulates all the information needed to manage tab completion cycling,
 * including the original input, possible completions, current selection index, and text
 * replacement boundaries. It provides methods for navigating through completions both
 * forward (TAB) and backward (SHIFT+TAB).
 * </p>
 *
 * <h3>Usage Examples:</h3>
 *
 * <h4>Basic Completion Cycling:</h4>
 *
 * <pre>{@code
 * // User types "arr" and hits TAB
 * List<TabCompletion> completions = Arrays.asList(
 *     new TabCompletion( "arrayAppend", "arrayAppend", "Append to array", 0, 3 ),
 *     new TabCompletion( "arrayLen", "arrayLen", "Get array length", 0, 3 ),
 *     new TabCompletion( "arrayNew", "arrayNew", "Create new array", 0, 3 )
 * );
 *
 * CompletionState state = new CompletionState( "arr", completions, 3, 0, 3 );
 *
 * // Initially shows "arrayAppend"
 * TabCompletion current = state.getCurrentCompletion(); // arrayAppend
 *
 * // User hits TAB again - cycles to next
 * state.nextCompletion();
 * current = state.getCurrentCompletion(); // arrayLen
 *
 * // User hits SHIFT+TAB - cycles backward
 * state.previousCompletion();
 * current = state.getCurrentCompletion(); // arrayAppend (back to first)
 * }</pre>
 *
 * <h4>Component Completion Example:</h4>
 *
 * <pre>{@code
 * // User types "bx:qu" and hits TAB for component completion
 * List<TabCompletion> completions = Arrays.asList(
 *     new TabCompletion( "bx:query", "bx:query", "BoxLang component: query", 0, 5 ),
 *     new TabCompletion( "bx:queue", "bx:queue", "BoxLang component: queue", 0, 5 )
 * );
 *
 * CompletionState state = new CompletionState( "bx:qu", completions, 5, 0, 5 );
 *
 * // Check if completions are available
 * if ( state.hasCompletions() ) {
 *     System.out.println( "Found " + state.getCompletionCount() + " completions" );
 *
 *     // Get current completion for display
 *     TabCompletion current = state.getCurrentCompletion();
 *     String replacement = current.getText(); // "bx:query"
 *
 *     // Replace text from position 0 to 5 with "bx:query"
 *     String newInput = state.getOriginalInput().substring( 0, state.getReplaceStart() ) +
 *         replacement +
 *         state.getOriginalInput().substring( state.getReplaceEnd() );
 * }
 * }</pre>
 *
 * <h4>Empty Completion Handling:</h4>
 *
 * <pre>{@code
 * // No completions found for user input
 * CompletionState emptyState = new CompletionState( "xyz", Arrays.asList(), 3, 0, 3 );
 *
 * if ( !emptyState.hasCompletions() ) {
 *     System.out.println( "No completions available" );
 * }
 *
 * // Safe to call navigation methods - they handle empty lists gracefully
 * emptyState.nextCompletion();     // No-op
 * emptyState.previousCompletion(); // No-op
 *
 * TabCompletion current = emptyState.getCurrentCompletion(); // Returns null
 * }</pre>
 *
 * <h4>Integration with MiniConsole:</h4>
 *
 * <pre>{@code
 *
 * // Typical usage in MiniConsole tab completion handling
 * public void handleTabCompletion( String input, int cursorPosition ) {
 *     List<TabCompletion> completions = gatherCompletions( input, cursorPosition );
 *
 *     if ( !completions.isEmpty() ) {
 *         // Create completion state
 *         TabCompletion first = completions.get( 0 );
 *         CompletionState state = new CompletionState(
 *             input,
 *             completions,
 *             cursorPosition,
 *             first.getReplaceStart(),
 *             first.getReplaceEnd()
 *         );
 *
 *         // Store state for subsequent TAB/SHIFT+TAB presses
 *         this.currentCompletionState = state;
 *
 *         // Apply first completion
 *         applyCompletion( state.getCurrentCompletion() );
 *     }
 * }
 * }</pre>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is not thread-safe. Each completion
 * session should use its own instance, typically created and managed by the CLI
 * input handler on the main thread.
 * </p>
 *
 * <p>
 * <strong>Memory Management:</strong> Completion states are typically short-lived,
 * created for each completion session and discarded when the user commits to a
 * completion or starts typing new input.
 * </p>
 *
 * @see TabCompletion
 * @see MiniConsole
 * @see ortus.boxlang.runtime.cli.providers.ITabProvider
 */
public class TabCompletionState {

	/** The original input string before any completions were applied */
	private final String				originalInput;

	/** List of available tab completions for this input */
	private final List<TabCompletion>	completions;

	/** Current position in the input where tab completion was triggered */
	private final int					cursorPosition;

	/** Index of the currently selected completion (for cycling) */
	private int							currentIndex;

	/** Start position in the input where text should be replaced */
	private final int					replaceStart;

	/** End position in the input where text should be replaced */
	private final int					replaceEnd;

	/**
	 * Creates a new completion state for managing tab completion cycling.
	 *
	 * @param originalInput  The original input string before completions
	 * @param completions    List of available completions (copied for safety)
	 * @param cursorPosition Position where tab completion was triggered
	 * @param replaceStart   Start position for text replacement
	 * @param replaceEnd     End position for text replacement
	 */
	TabCompletionState( String originalInput, List<TabCompletion> completions, int cursorPosition, int replaceStart, int replaceEnd ) {
		this.originalInput	= originalInput;
		this.completions	= new ArrayList<>( completions );
		this.cursorPosition	= cursorPosition;
		this.currentIndex	= 0;
		this.replaceStart	= replaceStart;
		this.replaceEnd		= replaceEnd;
	}

	/**
	 * Gets the currently selected completion.
	 *
	 * @return The current TabCompletion, or null if no completions available
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * TabCompletion current = state.getCurrentCompletion();
	 * if (current != null) {
	 *     String completionText = current.getText();
	 *     String description = current.getDescription();
	 *          }
	 * }</pre>
	 */
	TabCompletion getCurrentCompletion() {
		return this.completions.isEmpty() ? null : this.completions.get( currentIndex );
	}

	/**
	 * Advances to the next completion in the list (TAB key behavior).
	 * Cycles back to the first completion after the last one.
	 * Safe to call even when no completions are available.
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Cycle through: arrayAppend -> arrayLen -> arrayNew -> arrayAppend...
	 * state.nextCompletion(); // Move from arrayAppend to arrayLen
	 * state.nextCompletion(); // Move from arrayLen to arrayNew
	 * state.nextCompletion(); // Move from arrayNew back to arrayAppend
	 * }</pre>
	 */
	void nextCompletion() {
		if ( !this.completions.isEmpty() ) {
			currentIndex = ( currentIndex + 1 ) % this.completions.size();
		}
	}

	/**
	 * Moves to the previous completion in the list (SHIFT+TAB key behavior).
	 * Cycles to the last completion when at the first one.
	 * Safe to call even when no completions are available.
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Starting at arrayLen, cycle backward: arrayLen -> arrayAppend -> arrayNew -> arrayLen...
	 * state.previousCompletion(); // Move from arrayLen to arrayAppend
	 * state.previousCompletion(); // Move from arrayAppend to arrayNew (wraps to end)
	 * state.previousCompletion(); // Move from arrayNew back to arrayLen
	 * }</pre>
	 */
	void previousCompletion() {
		if ( !this.completions.isEmpty() ) {
			currentIndex = ( currentIndex - 1 + this.completions.size() ) % this.completions.size();
		}
	}

	/**
	 * Checks if any completions are available.
	 *
	 * @return true if completions exist, false otherwise
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * if (state.hasCompletions()) {
	 *     // Safe to call getCurrentCompletion() and navigation methods
	 *     TabCompletion current = state.getCurrentCompletion();
	 *          } else {
	 *          System.out.println( "No completions found" );
	 *          }
	 * }</pre>
	 */
	boolean hasCompletions() {
		return !this.completions.isEmpty();
	}

	/**
	 * Gets the total number of available completions.
	 *
	 * @return Number of completions (0 if none available)
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * int count = state.getCompletionCount();
	 * System.out.println("Found " + count + " possible completions");
	 *
	 * // Display completion counter: "2 of 5"
	 * System.out.println((state.getCurrentIndex() + 1) + " of " + count);
	 * }</pre>
	 */
	int getCompletionCount() {
		return this.completions.size();
	}

	/**
	 * Gets all available completions in this state.
	 * Returns a reference to the internal list - modifications will affect the state.
	 *
	 * @return List of all TabCompletion objects
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Display all available options to user
	 * List<TabCompletion> all = state.getAllCompletions();
	 * System.out.println("Available completions:");
	 * for (int i = 0; i < all.size(); i++) {
	 *     TabCompletion completion = all.get(i);
	 *     String marker = (i == state.getCurrentIndex()) ? "* " : "  ";
	 *     System.out.println(marker + completion.getDisplayText() + " - " + completion.getDescription());
	 *          }
	 * }</pre>
	 */
	List<TabCompletion> getAllCompletions() {
		return this.completions;
	}

	/**
	 * Gets the zero-based index of the currently selected completion.
	 *
	 * @return Current selection index (0 to getCompletionCount()-1), or 0 if no completions
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Display position in completion list: "2 of 5"
	 * int current = state.getCurrentIndex() + 1; // Convert to 1-based
	 * int total = state.getCompletionCount();
	 * System.out.println("Completion " + current + " of " + total);
	 * }</pre>
	 */
	int getCurrentIndex() {
		return this.currentIndex;
	}

	/**
	 * Gets the original input string before any completions were applied.
	 * Useful for resetting or building new completion text.
	 *
	 * @return The original input string
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Build complete input with current completion
	 * String original = state.getOriginalInput();
	 * TabCompletion current = state.getCurrentCompletion();
	 *
	 * String newInput = original.substring(0, state.getReplaceStart()) +
	 *                  current.getText() +
	 *                  original.substring(state.getReplaceEnd());
	 * }</pre>
	 */
	String getOriginalInput() {
		return this.originalInput;
	}

	/**
	 * Gets the start position where text should be replaced with the completion.
	 *
	 * @return Zero-based character position in the original input
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Replace "arr" with "arrayAppend" in "var result = arr"
	 * String input = "var result = arr";
	 * int start = state.getReplaceStart(); // 13
	 * int end = state.getReplaceEnd();     // 16
	 * String completion = "arrayAppend";
	 *
	 * String result = input.substring(0, start) + completion + input.substring(end);
	 * // Result: "var result = arrayAppend"
	 * }</pre>
	 */
	int getReplaceStart() {
		return this.replaceStart;
	}

	/**
	 * Gets the cursor position where tab completion was originally triggered.
	 * This can be useful for context-aware completion handling or debugging.
	 *
	 * @return Zero-based character position in the original input
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Check if completion was triggered at end of input
	 * String input = state.getOriginalInput();
	 * int cursor = state.getCursorPosition();
	 * boolean atEnd = (cursor == input.length());
	 *
	 * // Determine if there's text after completion point
	 * String textAfterCursor = input.substring(cursor);
	 * }</pre>
	 */
	int getCursorPosition() {
		return this.cursorPosition;
	}

	/**
	 * Gets the end position where text replacement should stop.
	 *
	 * @return Zero-based character position in the original input (exclusive)
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // When user types "bx:qu" and gets completion "bx:query"
	 * String input = "bx:qu";
	 * int start = state.getReplaceStart(); // 0
	 * int end = state.getReplaceEnd();     // 5 (length of "bx:qu")
	 *
	 * // Replace entire input with completion
	 * String completion = state.getCurrentCompletion().getText(); // "bx:query"
	 * String result = completion; // "bx:query"
	 * }</pre>
	 */
	int getReplaceEnd() {
		return this.replaceEnd;
	}
}
