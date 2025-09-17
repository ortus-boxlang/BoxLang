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

import java.util.List;

/**
 * Interface for providing tab completion suggestions.
 *
 * Tab providers analyze the current input context and return completion suggestions
 * when appropriate. Multiple providers can be registered with a console to provide
 * completions for different contexts (e.g., components, functions, variables, etc.).
 *
 * <h3>Usage Example:</h3>
 *
 * <pre>
 * TabProvider componentProvider = new ComponentTabProvider();
 * console.registerTabProvider( componentProvider );
 * </pre>
 */
public interface TabProvider {

	/**
	 * Determines if this provider should handle tab completion for the given input context.
	 *
	 * @param input          The current input line
	 * @param cursorPosition The position of the cursor in the input (0-based)
	 *
	 * @return true if this provider can provide completions for this context
	 */
	boolean canProvideCompletions( String input, int cursorPosition );

	/**
	 * Provides completion suggestions for the given input context.
	 *
	 * @param input          The current input line
	 * @param cursorPosition The position of the cursor in the input (0-based)
	 *
	 * @return A list of completion suggestions, or empty list if none available
	 */
	List<TabCompletion> getCompletions( String input, int cursorPosition );

	/**
	 * Returns a descriptive name for this tab provider (for debugging/logging).
	 *
	 * @return The provider name
	 */
	default String getProviderName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Returns the priority of this provider (higher numbers = higher priority).
	 * When multiple providers can handle the same input, the one with highest
	 * priority is used.
	 *
	 * @return Priority value (default: 100)
	 */
	default int getPriority() {
		return 100;
	}
}