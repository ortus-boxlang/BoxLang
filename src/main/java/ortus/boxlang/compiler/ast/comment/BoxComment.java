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
	 * Check if the comment text equals the given text (exact match after trimming both sides)
	 *
	 * @param text the text to compare against
	 *
	 * @return true if the trimmed comment text equals the given text
	 */
	public boolean textEquals( String text ) {
		return this.commentText.trim().equals( text );
	}

	/**
	 * Check if the comment text equals any of the given texts (exact match after trimming)
	 *
	 * @param texts the texts to compare against
	 *
	 * @return true if the trimmed comment text equals any of the given texts
	 */
	public boolean textEqualsAny( String... texts ) {
		String trimmed = this.commentText.trim();
		for ( String text : texts ) {
			if ( trimmed.equals( text ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the comment text contains the given text (case-sensitive)
	 *
	 * @param text the text to search for
	 *
	 * @return true if the comment text contains the given text
	 */
	public boolean textContains( String text ) {
		return this.commentText.contains( text );
	}

	/**
	 * Check if the comment text starts with the given text (after trimming the comment text)
	 *
	 * @param text the prefix to check
	 *
	 * @return true if the trimmed comment text starts with the given text
	 */
	public boolean textStartsWith( String text ) {
		return this.commentText.trim().startsWith( text );
	}

	/**
	 * Check if the comment text matches the given regex pattern
	 *
	 * @param regex the regex pattern to test
	 *
	 * @return true if the trimmed comment text matches the regex
	 */
	public boolean textMatches( String regex ) {
		return this.commentText.trim().matches( regex );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "commentText", commentText );
		return map;
	}

}
