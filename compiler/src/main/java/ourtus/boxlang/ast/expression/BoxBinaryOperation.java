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
package ourtus.boxlang.ast.expression;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.Position;

/**
 * AST Node representing access binary operation
 */
public class BoxBinaryOperation extends BoxExpr {

	private final BoxExpr			left;
	private final BoxExpr			right;
	private final BoxBinaryOperator	operator;

	/**
	 * Creates the AST node
	 * 
	 * @param left       left expression of binary operation
	 * @param operator   operator
	 * @param right      left expression of binary operation
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 * 
	 * @see BoxBinaryOperator for the supported operators
	 */
	public BoxBinaryOperation( BoxExpr left, BoxBinaryOperator operator, BoxExpr right, Position position, String sourceText ) {
		super( position, sourceText );
		this.left		= left;
		this.right		= right;
		this.operator	= operator;
		this.left.setParent( this );
		this.right.setParent( this );
	}

	public BoxExpr getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public BoxBinaryOperator getOperator() {
		return operator;
	}
}
