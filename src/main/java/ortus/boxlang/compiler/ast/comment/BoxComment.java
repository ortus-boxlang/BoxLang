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

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "commentText", commentText );
		return map;
	}

}
