/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.ast.statement;

import java.util.List;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a switch case statement in tag-based code (e.g., {@code <cfcase>} or {@code <bx:case>}).
 * <p>
 * Unlike script-based switch cases which support fall-through behavior, tag-based cases
 * implicitly break after execution. Break statements inside a breaking case are ignored
 * by the switch and propagate up to the nearest enclosing loop.
 * </p>
 */
public class BoxSwitchBreakingCase extends BoxSwitchCase {

	/**
	 * Creates the AST node
	 *
	 * @param condition  expression representing the condition to test, null for the default
	 * @param delimiter  expression representing the delimiter for list-based case matching
	 * @param body       list of the statements to execute when the condition is true
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxSwitchBreakingCase( BoxExpression condition, BoxExpression delimiter, List<BoxStatement> body, Position position, String sourceText ) {
		super( condition, delimiter, body, position, sourceText );
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
