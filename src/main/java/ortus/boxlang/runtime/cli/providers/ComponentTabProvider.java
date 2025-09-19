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
public class ComponentTabProvider implements ITabProvider {

	private final Set<String> componentNames;

	/**
	 * Constructor with component names.
	 *
	 * @param componentNames Set of available component names (case-insensitive)
	 */
	public ComponentTabProvider( Set<String> componentNames ) {
		this.componentNames = componentNames;
	}

	@Override
	public boolean canProvideCompletions( String input, int cursorPosition ) {
		if ( input == null || input.isEmpty() ) {
			return false;
		}

		// Look for bx: pattern at or before cursor position
		String	upToCursor	= input.substring( 0, Math.min( cursorPosition, input.length() ) );

		// Check if we're currently typing a component declaration
		int		bxIndex		= upToCursor.lastIndexOf( "bx:" );
		if ( bxIndex == -1 ) {
			return false;
		}

		// Make sure there's no whitespace after bx: and before cursor
		String afterBx = upToCursor.substring( bxIndex + 3 );
		return !afterBx.contains( " " ) && !afterBx.contains( "\t" );
	}

	@Override
	public List<TabCompletion> getCompletions( String input, int cursorPosition ) {
		List<TabCompletion> completions = new ArrayList<>();

		if ( input == null || componentNames == null || componentNames.isEmpty() ) {
			return completions;
		}

		String	upToCursor	= input.substring( 0, Math.min( cursorPosition, input.length() ) );
		int		bxIndex		= upToCursor.lastIndexOf( "bx:" );

		if ( bxIndex == -1 ) {
			return completions;
		}

		// Extract the partial component name after "bx:"
		String partial = upToCursor.substring( bxIndex + 3 ).toLowerCase();

		// Find matching components
		for ( String componentName : componentNames ) {
			String lowerComponentName = componentName.toLowerCase();

			if ( lowerComponentName.startsWith( partial ) ) {
				// Create completion that replaces from "bx:" onwards
				String			fullCompletion	= "bx:" + componentName;
				String			displayText		= "\033[36m" + fullCompletion + "\033[0m"; // Cyan color

				TabCompletion	completion		= new TabCompletion(
				    fullCompletion,
				    displayText,
				    "BoxLang component: " + componentName,
				    bxIndex,  // Start replacement from "bx:"
				    cursorPosition  // End replacement at cursor
				);

				completions.add( completion );
			}
		}

		// Sort completions alphabetically
		completions.sort( ( a, b ) -> a.getText().compareToIgnoreCase( b.getText() ) );

		return completions;
	}

	@Override
	public String getProviderName() {
		return "ComponentTabProvider";
	}

	@Override
	public int getPriority() {
		return 200; // Higher priority than default for component-specific completions
	}
}