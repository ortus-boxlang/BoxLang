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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * class for single line comments
 */
public class BoxSingleLineComment extends BoxComment {

	/**
	 * Recognised formatter-ignore-start directives
	 */
	public static final String[]	FORMATTER_IGNORE_START	= { "cfformat-ignore-start", "@formatter:off", "bxformat-ignore-start" };

	/**
	 * Recognised formatter-ignore-end directives
	 */
	public static final String[]	FORMATTER_IGNORE_END	= { "cfformat-ignore-end", "@formatter:on", "bxformat-ignore-end" };

	/**
	 * Create a instance of a BoxComment
	 *
	 * @param position   position within the source code
	 * @param sourceText source code
	 */
	public BoxSingleLineComment( String commentText, Position position, String sourceText ) {
		super( commentText, position, sourceText );
	}

	/**
	 * Returns true if this comment is a formatter-ignore-start marker.
	 * Recognised directives: cfformat-ignore-start, @formatter:off, bxformat-ignore-start
	 *
	 * @return true if this is a formatter-ignore-start comment
	 */
	public boolean isFormatterIgnoreStart() {
		return textEqualsAny( FORMATTER_IGNORE_START );
	}

	/**
	 * Returns true if this comment is a formatter-ignore-end marker.
	 * Recognised directives: cfformat-ignore-end, @formatter:on, bxformat-ignore-end
	 *
	 * @return true if this is a formatter-ignore-end comment
	 */
	public boolean isFormatterIgnoreEnd() {
		return textEqualsAny( FORMATTER_IGNORE_END );
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

}
