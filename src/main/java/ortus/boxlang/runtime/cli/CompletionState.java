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
 * Represents the state of tab completions at a given moment.
 *
 * This class encapsulates the original input, the list of possible completions,
 * the current selection index, and the cursor position. It provides methods to
 * navigate through completions and retrieve relevant information.
 */
public class CompletionState {

	private final String				originalInput;
	private final List<TabCompletion>	completions;
	private final int					cursorPosition;
	private int							currentIndex;
	private final int					replaceStart;
	private final int					replaceEnd;

	CompletionState( String originalInput, List<TabCompletion> completions, int cursorPosition, int replaceStart, int replaceEnd ) {
		this.originalInput	= originalInput;
		this.completions	= new ArrayList<>( completions );
		this.cursorPosition	= cursorPosition;
		this.currentIndex	= 0;
		this.replaceStart	= replaceStart;
		this.replaceEnd		= replaceEnd;
	}

	TabCompletion getCurrentCompletion() {
		return this.completions.isEmpty() ? null : this.completions.get( currentIndex );
	}

	void nextCompletion() {
		if ( !this.completions.isEmpty() ) {
			currentIndex = ( currentIndex + 1 ) % this.completions.size();
		}
	}

	boolean hasCompletions() {
		return !this.completions.isEmpty();
	}

	int getCompletionCount() {
		return this.completions.size();
	}

	/**
	 * Get all completions in this state
	 */
	List<TabCompletion> getAllCompletions() {
		return this.completions;
	}

	/**
	 * Get the current completion index
	 */
	int getCurrentIndex() {
		return this.currentIndex;
	}

	/**
	 * Get the original input before any completions
	 */
	String getOriginalInput() {
		return this.originalInput;
	}

	/**
	 * Get the replace start position
	 */
	int getReplaceStart() {
		return this.replaceStart;
	}

	/**
	 * Get the replace end position
	 */
	int getReplaceEnd() {
		return this.replaceEnd;
	}
}
