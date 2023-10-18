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

import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.BoxExpr;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing an assigment statement
 */
public class BoxAssignment extends BoxStatement {

	private final List<BoxExpr>		left;
	private BoxExpr					right;
	private BoxAssigmentOperator	op;

	/**
	 * Creates the AST node
	 *
	 * @param left       expression on the left of the assigment
	 * @param op
	 * @param right      expression on the right of the assigment
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAssignment( List<BoxExpr> left, BoxAssigmentOperator op, BoxExpr right, Position position, String sourceText ) {
		super( position, sourceText );
		this.left = Collections.unmodifiableList( left );
		this.left.forEach( arg -> arg.setParent( this ) );
		this.op		= op;
		this.right	= right;
		this.right.setParent( this );
	}

	public List<BoxExpr> getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public BoxAssigmentOperator getOp() {
		return op;
	}
}
