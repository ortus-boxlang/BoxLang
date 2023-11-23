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
package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing an assigment statement
 */
public class BoxAssignment extends BoxStatement {

	private final BoxExpr			left;
	private final BoxExpr			right;
	private BoxAssignmentOperator	op;

	/**
	 * Creates the AST node
	 *
	 * @param left       expression on the left of the assigment
	 * @param op
	 * @param right      expression on the right of the assigment
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAssignment( BoxExpr left, BoxAssignmentOperator op, BoxExpr right, Position position, String sourceText ) {
		super( position, sourceText );
		this.left	= left;
		this.op		= op;
		this.right	= right;
		this.right.setParent( this );
	}

	public BoxExpr getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public BoxAssignmentOperator getOp() {
		return op;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "left", left.toMap() );
		map.put( "right", right.toMap() );
		map.put( "op", op.toString() );
		return map;
	}
}
