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

/**
 * Tab completion provider for BoxLang components (bx:componentName syntax).
 *
 * This provider handles completions for BoxLang component declarations that start
 * with "bx:" prefix. It matches against available component names from the runtime.
 *
 * <h3>Completion Examples:</h3>
 * <ul>
 * <li>"bx:" + TAB → shows all available components</li>
 * <li>"bx:quer" + TAB → completes to "bx:query"</li>
 * <li>"bx:script" + TAB → completes to "bx:script"</li>
 * </ul>
 */
public class ComponentTabProvider extends AbstractTabProvider {

	/**
	 * ----------------------------------------------------------------------------
	 * Properties
	 * ----------------------------------------------------------------------------
	 */

	private final Set<String> componentNames;

	/**
	 * ----------------------------------------------------------------------------
	 * Constructors
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Constructor with component names.
	 *
	 * @param componentNames Set of available component names (case-insensitive)
	 */
	public ComponentTabProvider( Set<String> componentNames ) {
		this.componentNames = componentNames;
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Methods
	 * ----------------------------------------------------------------------------
	 */

	@Override
	public boolean canProvideCompletions( String input, int cursorPosition ) {
		if ( !isValidInput( input, cursorPosition ) ) {
			return false;
		}

		// Look for bx: pattern at or before cursor position
		int bxIndex = findPrefixIndex( input, cursorPosition, COMPONENT_PREFIX );
		if ( bxIndex == -1 ) {
			return false;
		}

		// Make sure there's no whitespace after bx: and before cursor
		return !hasWhitespaceAfterPrefix( input, cursorPosition, COMPONENT_PREFIX );
	}

	@Override
	public List<TabCompletion> getCompletions( String input, int cursorPosition ) {
		List<TabCompletion> completions = new ArrayList<>();

		if ( input == null || componentNames == null || componentNames.isEmpty() ) {
			return completions;
		}

		int bxIndex = findPrefixIndex( input, cursorPosition, COMPONENT_PREFIX );
		if ( bxIndex == -1 ) {
			return completions;
		}

		// Extract the partial component name after "bx:"
		String partial = getTextAfterPrefix( input, cursorPosition, COMPONENT_PREFIX ).toLowerCase();

		// Find matching components
		for ( String componentName : componentNames ) {
			String lowerComponentName = componentName.toLowerCase();

			if ( lowerComponentName.startsWith( partial ) ) {
				// Create component completion using base class utility
				TabCompletion completion = createCompletion(
				    "bx:" + componentName,
				    "BoxLang component: " + componentName,
				    bxIndex,
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
		// Higher priority than default for component-specific completions
		return 200;
	}
}