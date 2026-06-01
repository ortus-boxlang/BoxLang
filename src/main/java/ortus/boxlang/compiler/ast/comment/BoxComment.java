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
package ortus.boxlang.compiler.ast.comment;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;

/**
 * abstract class for comments
 */
public abstract class BoxComment extends BoxNode {

	private String commentText;

	/**
	 * Create a instance of a BoxComment
	 *
	 * @param position   position within the source code
	 * @param sourceText source code
	 */
	public BoxComment( String commentText, Position position, String sourceText ) {
		super( position, sourceText );
		this.commentText = commentText;
	}

	public void setCommentText( String commentText ) {
		this.commentText = commentText;
	}

	public String getCommentText() {
		return commentText;
	}

	/**
	 * Check if the comment text equals the given text (case-insensitive, after trimming both sides)
	 *
	 * @param text the text to compare against
	 *
	 * @return true if the trimmed comment text equals the given text (ignoring case)
	 */
	public boolean textEquals( String text ) {
		return this.commentText.trim().equalsIgnoreCase( text );
	}

	/**
	 * Check if the comment text equals any of the given texts (case-insensitive, after trimming)
	 *
	 * @param texts the texts to compare against
	 *
	 * @return true if the trimmed comment text equals any of the given texts (ignoring case)
	 */
	public boolean textEqualsAny( String... texts ) {
		String trimmed = this.commentText.trim();
		for ( String text : texts ) {
			if ( trimmed.equalsIgnoreCase( text ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the comment text contains the given text (case-insensitive)
	 *
	 * @param text the text to search for
	 *
	 * @return true if the comment text contains the given text (ignoring case)
	 */
	public boolean textContains( String text ) {
		return this.commentText.toLowerCase().contains( text.toLowerCase() );
	}

	/**
	 * Check if the comment text starts with the given text (case-insensitive, after trimming)
	 *
	 * @param text the prefix to check
	 *
	 * @return true if the trimmed comment text starts with the given text (ignoring case)
	 */
	public boolean textStartsWith( String text ) {
		return this.commentText.trim().toLowerCase().startsWith( text.toLowerCase() );
	}

	/**
	 * Check if the comment text matches the given regex pattern (case-insensitive)
	 *
	 * @param regex the regex pattern to test
	 *
	 * @return true if the trimmed comment text matches the regex (case-insensitive)
	 */
	public boolean textMatches( String regex ) {
		return this.commentText.trim().matches( "(?i)" + regex );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "commentText", commentText );
		return map;
	}

}
