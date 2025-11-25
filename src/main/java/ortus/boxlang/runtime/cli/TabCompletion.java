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

/**
 * Represents a single tab completion suggestion.
 *
 * This class encapsulates all the information needed to display and apply
 * a completion suggestion in the console.
 */
public class TabCompletion {

	private final String	text;
	private final String	displayText;
	private final String	description;
	private final int		replaceStart;
	private final int		replaceEnd;

	/**
	 * Creates a simple completion that replaces from the cursor backwards to the start of the current word.
	 *
	 * @param text The text to insert
	 */
	public TabCompletion( String text ) {
		this( text, text, null, -1, -1 );
	}

	/**
	 * Creates a completion with custom display text.
	 *
	 * @param text        The text to insert
	 * @param displayText The text to show in the completion menu (can include formatting)
	 */
	public TabCompletion( String text, String displayText ) {
		this( text, displayText, null, -1, -1 );
	}

	/**
	 * Creates a completion with description.
	 *
	 * @param text        The text to insert
	 * @param displayText The text to show in the completion menu
	 * @param description Optional description/documentation for this completion
	 */
	public TabCompletion( String text, String displayText, String description ) {
		this( text, displayText, description, -1, -1 );
	}

	/**
	 * Creates a completion with precise replacement range.
	 *
	 * @param text         The text to insert
	 * @param displayText  The text to show in the completion menu
	 * @param description  Optional description/documentation for this completion
	 * @param replaceStart Start position for replacement (-1 for auto-detect)
	 * @param replaceEnd   End position for replacement (-1 for auto-detect)
	 */
	public TabCompletion( String text, String displayText, String description, int replaceStart, int replaceEnd ) {
		this.text			= text != null ? text : "";
		this.displayText	= displayText != null ? displayText : this.text;
		this.description	= description;
		this.replaceStart	= replaceStart;
		this.replaceEnd		= replaceEnd;
	}

	/**
	 * Gets the text to insert when this completion is selected.
	 *
	 * @return The completion text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Gets the text to display in the completion menu.
	 * This may include ANSI color codes for formatting.
	 *
	 * @return The display text
	 */
	public String getDisplayText() {
		return displayText;
	}

	/**
	 * Gets the optional description/documentation for this completion.
	 *
	 * @return The description, or null if none
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the start position for text replacement.
	 *
	 * @return The start position, or -1 for auto-detect
	 */
	public int getReplaceStart() {
		return replaceStart;
	}

	/**
	 * Gets the end position for text replacement.
	 *
	 * @return The end position, or -1 for auto-detect
	 */
	public int getReplaceEnd() {
		return replaceEnd;
	}

	/**
	 * Checks if this completion has a custom replacement range.
	 *
	 * @return true if custom range is specified
	 */
	public boolean hasCustomRange() {
		return replaceStart >= 0 && replaceEnd >= 0;
	}

	@Override
	public String toString() {
		return "TabCompletion{" +
		    "text='" + text + '\'' +
		    ", displayText='" + displayText + '\'' +
		    ", description='" + description + '\'' +
		    '}';
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;

		TabCompletion that = ( TabCompletion ) o;

		return text.equals( that.text );
	}

	@Override
	public int hashCode() {
		return text.hashCode();
	}
}